package com.example.lostfound.service;

import com.example.lostfound.model.Claim;
import com.example.lostfound.model.ItemMatch;
import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.ItemMatchRepository;
import com.example.lostfound.repository.ItemPostRepository;
import com.example.lostfound.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class ClaimService {

    private final ClaimRepository claimRepo;
    private final ItemPostRepository itemRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final ItemMatchRepository matchRepo;

    public ClaimService(ClaimRepository claimRepo,
                        ItemPostRepository itemRepo,
                        UserRepository userRepo,
                        NotificationService notificationService,
                        ItemMatchRepository matchRepo) {
        this.claimRepo = claimRepo;
        this.itemRepo = itemRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.matchRepo = matchRepo;
    }

    // =========================================================
    // USER: Submit Claim
    // =========================================================
    public Claim submitClaim(Long itemId, String username,
                             String fullName, Integer age, String mobile,
                             String proofType, String reason,
                             byte[] govProofImage, byte[] productProofImage) {

        User claimant = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ItemPost item = itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Validation
        if (item.isLost()) {
            throw new IllegalArgumentException("Lost items cannot be claimed — only FOUND items can be claimed");
        }
        if (item.getReportedBy() != null && item.getReportedBy().getId().equals(claimant.getId())) {
            throw new IllegalArgumentException("You cannot claim an item you yourself reported");
        }
        if ("RESOLVED".equals(item.getStatus()) || "REMOVED".equals(item.getStatus())) {
            throw new IllegalArgumentException("This item is no longer available for claiming");
        }

        // Prevent duplicate active claims
        List<String> activeStatuses = Arrays.asList("PENDING_VERIFICATION", "APPROVED_AWAITING_HANDOVER");
        if (claimRepo.existsByClaimantAndItemPostAndStatusNotIn(claimant, item,
                Arrays.asList("REJECTED", "COMPLETED"))) {
            throw new IllegalArgumentException("You already have an active claim for this item");
        }

        Claim claim = Claim.builder()
                .itemPost(item)
                .claimant(claimant)
                .fullName(fullName)
                .age(age)
                .mobile(mobile)
                .proofType(proofType)
                .reason(reason)
                .govProofImage(govProofImage)
                .productProofImage(productProofImage)
                .status("PENDING_VERIFICATION")
                .build();

        Claim saved = claimRepo.save(claim);

        // Update related matches to CLAIM_IN_PROGRESS
        List<ItemMatch> activeMatches = matchRepo.findActiveByFoundItem(item);
        activeMatches.forEach(m -> {
            m.setStatus("CLAIM_IN_PROGRESS");
            matchRepo.save(m);
        });

        // Notify the finder that a claim was submitted
        if (item.getReportedBy() != null) {
            notificationService.createNotification(
                    item.getReportedBy(),
                    "Someone has submitted a claim for the found item you reported: \"" + item.getTitle() +
                            "\". An administrator will review it.",
                    "CLAIM_SUBMITTED",
                    saved.getId()
            );
        }

        return saved;
    }

    // =========================================================
    // ADMIN: Approve Claim
    // =========================================================
    public Claim approveClaim(Long claimId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Claim claim = claimRepo.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isFinder = claim.getItemPost() != null &&
                claim.getItemPost().getReportedBy() != null &&
                claim.getItemPost().getReportedBy().getId().equals(user.getId());

        if (!isAdmin && !isFinder) {
            throw new SecurityException("Only an Administrator or the student who reported this found item can approve this claim");
        }

        if (!"PENDING_VERIFICATION".equals(claim.getStatus())) {
            throw new IllegalStateException("Only PENDING_VERIFICATION claims can be approved");
        }

        claim.setStatus("APPROVED_AWAITING_HANDOVER");
        claim.setResolvedBy(user);
        claim.setResolvedAt(Instant.now());
        Claim saved = claimRepo.save(claim);

        notificationService.createNotification(
                claim.getClaimant(),
                "Your claim for \"" + claim.getItemPost().getTitle() +
                        "\" has been approved. Please coordinate handover to collect your item.",
                "CLAIM_APPROVED",
                saved.getId()
        );

        return saved;
    }

    // =========================================================
    // ADMIN OR FINDER: Reject Claim
    // =========================================================
    public Claim rejectClaim(Long claimId, String username, String notes) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Claim claim = claimRepo.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isFinder = claim.getItemPost() != null &&
                claim.getItemPost().getReportedBy() != null &&
                claim.getItemPost().getReportedBy().getId().equals(user.getId());

        if (!isAdmin && !isFinder) {
            throw new SecurityException("Only an Administrator or the student who reported this found item can reject this claim");
        }

        if (!"PENDING_VERIFICATION".equals(claim.getStatus())) {
            throw new IllegalStateException("Only PENDING_VERIFICATION claims can be rejected");
        }

        claim.setStatus("REJECTED");
        claim.setResolvedBy(user);
        claim.setResolvedAt(Instant.now());
        claim.setReviewNotes(notes);
        Claim saved = claimRepo.save(claim);

        String noteMsg = (notes != null && !notes.isBlank()) ? " Reason: " + notes : "";
        notificationService.createNotification(
                claim.getClaimant(),
                "Your claim for \"" + claim.getItemPost().getTitle() +
                        "\" was not approved." + noteMsg,
                "CLAIM_REJECTED",
                saved.getId()
        );

        // Revert match status back to POTENTIAL_MATCH so other claimants may try
        List<ItemMatch> matches = matchRepo.findActiveByFoundItem(claim.getItemPost());
        matches.forEach(m -> {
            m.setStatus("POTENTIAL_MATCH");
            matchRepo.save(m);
        });

        return saved;
    }

    // =========================================================
    // ADMIN OR FINDER: Confirm Physical Handover (Transactional)
    // =========================================================
    @Transactional
    public Claim confirmReturn(Long claimId, String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Claim claim = claimRepo.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isFinder = claim.getItemPost() != null &&
                claim.getItemPost().getReportedBy() != null &&
                claim.getItemPost().getReportedBy().getId().equals(user.getId());

        if (!isAdmin && !isFinder) {
            throw new SecurityException("Only an Administrator or the student who reported this found item can confirm the return");
        }

        if (!"APPROVED_AWAITING_HANDOVER".equals(claim.getStatus())) {
            throw new IllegalStateException(
                    "Only APPROVED_AWAITING_HANDOVER claims can be confirmed as returned. " +
                    "Current status: " + claim.getStatus());
        }

        // Mark claim completed
        claim.setStatus("COMPLETED");
        claim.setHandoverConfirmedBy(user);
        claim.setHandoverConfirmedAt(Instant.now());

        // Mark the found item as RESOLVED
        ItemPost foundItem = claim.getItemPost();
        foundItem.setStatus("RESOLVED");
        itemRepo.save(foundItem);

        // Find any associated lost item via ItemMatch and mark it RETURNED
        List<ItemMatch> matches = matchRepo.findByFoundItem(foundItem);
        for (ItemMatch m : matches) {
            m.setStatus("RESOLVED");
            matchRepo.save(m);
            ItemPost lostItem = m.getLostItem();
            if (lostItem != null && !"RETURNED".equals(lostItem.getStatus())) {
                lostItem.setStatus("RETURNED");
                itemRepo.save(lostItem);
            }
        }

        Claim saved = claimRepo.save(claim);

        // Notify claimant
        notificationService.createNotification(
                claim.getClaimant(),
                "Your claim is complete. \"" + foundItem.getTitle() +
                        "\" has been marked as returned to you. Thank you for using Lost & Found Portal!",
                "ITEM_RETURNED",
                saved.getId()
        );

        // Notify finder
        if (foundItem.getReportedBy() != null) {
            notificationService.createNotification(
                    foundItem.getReportedBy(),
                    "The item you found (\"" + foundItem.getTitle() +
                            "\") has been returned to its verified owner. Thank you for your honesty!",
                    "ITEM_RETURNED",
                    saved.getId()
            );
        }

        return saved;
    }
}
