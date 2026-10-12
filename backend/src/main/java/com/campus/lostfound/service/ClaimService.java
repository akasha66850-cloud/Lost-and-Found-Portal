package com.campus.lostfound.service;

import com.campus.lostfound.dto.ClaimCreateDto;
import com.campus.lostfound.dto.ClaimResponseDto;
import com.campus.lostfound.dto.ClaimReviewDto;
import com.campus.lostfound.entity.*;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.IllegalStateTransitionException;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ClaimService {

    private static final Set<String> ALLOWED_PROOF_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf"
    );

    private final ClaimRepository claimRepository;
    private final ClaimProofRepository claimProofRepository;
    private final ItemRepository itemRepository;
    private final MatchResultRepository matchResultRepository;
    private final StateTransitionService stateTransitionService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ClaimService(ClaimRepository claimRepository,
                        ClaimProofRepository claimProofRepository,
                        ItemRepository itemRepository,
                        MatchResultRepository matchResultRepository,
                        StateTransitionService stateTransitionService,
                        NotificationService notificationService,
                        AuditService auditService) {
        this.claimRepository = claimRepository;
        this.claimProofRepository = claimProofRepository;
        this.itemRepository = itemRepository;
        this.matchResultRepository = matchResultRepository;
        this.stateTransitionService = stateTransitionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public ClaimResponseDto submitClaim(ClaimCreateDto dto, MultipartFile proofFile, User claimant) {
        Item foundItem = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Found item not found with id: " + dto.getItemId()));

        if (foundItem.getType() != ItemType.FOUND) {
            throw new IllegalArgumentException("Only found items can be claimed");
        }

        if (foundItem.getStatus() != ItemStatus.LISTED) {
            throw new IllegalStateTransitionException("Item is not available for claims. Current status: " + foundItem.getStatus());
        }

        if (foundItem.getCreatedBy().getId().equals(claimant.getId())) {
            throw new ForbiddenException("You cannot claim an item you reported");
        }

        long rejectedCount = claimRepository.countByItemAndClaimantAndStatus(foundItem, claimant, ClaimStatus.REJECTED);
        if (rejectedCount >= 3) {
            throw new ForbiddenException("claim limit reached");
        }

        Item lostItem = null;
        if (dto.getLostItemId() != null) {
            lostItem = itemRepository.findById(dto.getLostItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lost item report not found with id: " + dto.getLostItemId()));
            if (!lostItem.getCreatedBy().getId().equals(claimant.getId())) {
                throw new ForbiddenException("Linked lost item must belong to the claimant");
            }
        }

        // Optimistic locking update on Item to CLAIM_PENDING
        stateTransitionService.transition(foundItem, ItemStatus.CLAIM_PENDING, "Claim submitted by " + claimant.getEmail(), claimant.getId());
        itemRepository.save(foundItem);

        Claim claim = Claim.builder()
                .item(foundItem)
                .claimant(claimant)
                .lostItem(lostItem)
                .answer(dto.getAnswer().trim())
                .status(ClaimStatus.PENDING)
                .build();

        Claim savedClaim = claimRepository.save(claim);

        // Store proof file if attached
        if (proofFile != null && !proofFile.isEmpty()) {
            if (proofFile.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Proof file cannot exceed 5 MB");
            }
            String ct = proofFile.getContentType();
            if (ct == null || !ALLOWED_PROOF_TYPES.contains(ct.toLowerCase())) {
                throw new IllegalArgumentException("Proof must be image/jpeg, image/png, or application/pdf");
            }
            try {
                ClaimProof proof = ClaimProof.builder()
                        .claim(savedClaim)
                        .fileName(proofFile.getOriginalFilename() != null ? proofFile.getOriginalFilename() : "proof")
                        .contentType(ct.toLowerCase())
                        .content(proofFile.getBytes())
                        .build();
                claimProofRepository.save(proof);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store proof file", e);
            }
        }

        auditService.log("CLAIM", savedClaim.getId(), "SUBMIT", claimant.getId(), "Claim submitted for item #" + foundItem.getId());

        notificationService.notifyStaff(
                String.format("New claim submitted for [%s] by %s (%s)", foundItem.getTitle(), claimant.getName(), claimant.getEmail()),
                "CLAIM_SUBMITTED",
                foundItem.getId()
        );

        return toDto(savedClaim, true);
    }

    public ClaimResponseDto reviewClaim(Long claimId, ClaimReviewDto dto, User reviewer) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateTransitionException("Only PENDING claims can be reviewed. Current status: " + claim.getStatus());
        }

        String decision = dto.getDecision().trim().toUpperCase();
        Item foundItem = claim.getItem();

        if ("APPROVE".equals(decision)) {
            claim.setStatus(ClaimStatus.APPROVED);
            claim.setReviewedBy(reviewer);
            claim.setReviewedAt(LocalDateTime.now());
            claim.setReviewNote(dto.getNote() != null ? dto.getNote().trim() : null);

            // Item -> RETURNED
            stateTransitionService.transition(foundItem, ItemStatus.RETURNED, "Claim #" + claim.getId() + " approved", reviewer.getId());
            itemRepository.save(foundItem);

            // Linked lost item -> CLOSED
            if (claim.getLostItem() != null) {
                Item lostItem = claim.getLostItem();
                stateTransitionService.transition(lostItem, ItemStatus.CLOSED, "Claim approved for found item #" + foundItem.getId(), reviewer.getId());
                itemRepository.save(lostItem);

                // Any match between them -> CONFIRMED
                matchResultRepository.findByLostItemAndFoundItem(lostItem, foundItem).ifPresent(m -> {
                    m.setStatus(MatchStatus.CONFIRMED);
                    matchResultRepository.save(m);
                });
            }

            // Other pending matches for this found item -> DISMISSED
            List<MatchResult> otherMatches = matchResultRepository.findByFoundItemOrderByScoreDesc(foundItem);
            for (MatchResult m : otherMatches) {
                if (m.getStatus() == MatchStatus.SUGGESTED) {
                    m.setStatus(MatchStatus.DISMISSED);
                    matchResultRepository.save(m);
                }
            }

            notificationService.createNotification(
                    claim.getClaimant(),
                    String.format("Your claim for [%s] has been APPROVED! Please visit %s with your ID for physical handover.",
                            foundItem.getTitle(), foundItem.getStoredAt() != null ? foundItem.getStoredAt() : "the campus desk"),
                    "CLAIM_APPROVED",
                    foundItem.getId()
            );

            auditService.log("CLAIM", claim.getId(), "APPROVE", reviewer.getId(), "Claim approved by " + reviewer.getEmail());

        } else if ("REJECT".equals(decision)) {
            if (dto.getNote() == null || dto.getNote().isBlank()) {
                throw new IllegalArgumentException("A review note is required when rejecting a claim");
            }

            claim.setStatus(ClaimStatus.REJECTED);
            claim.setReviewedBy(reviewer);
            claim.setReviewedAt(LocalDateTime.now());
            claim.setReviewNote(dto.getNote().trim());

            // Found item goes back to LISTED
            stateTransitionService.transition(foundItem, ItemStatus.LISTED, "Claim #" + claim.getId() + " rejected", reviewer.getId());
            itemRepository.save(foundItem);

            notificationService.createNotification(
                    claim.getClaimant(),
                    String.format("Your claim for [%s] was not approved. Note from staff: %s", foundItem.getTitle(), dto.getNote().trim()),
                    "CLAIM_REJECTED",
                    foundItem.getId()
            );

            auditService.log("CLAIM", claim.getId(), "REJECT", reviewer.getId(), "Claim rejected: " + dto.getNote().trim());

        } else {
            throw new IllegalArgumentException("Decision must be APPROVE or REJECT");
        }

        Claim saved = claimRepository.save(claim);
        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public Page<ClaimResponseDto> getClaims(ClaimStatus status, Pageable pageable) {
        if (status != null) {
            return claimRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(c -> toDto(c, true));
        }
        return claimRepository.findAll(pageable)
                .map(c -> toDto(c, true));
    }

    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getMyClaims(User user) {
        return claimRepository.findByClaimantOrderByCreatedAtDesc(user).stream()
                .map(c -> toDto(c, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaimProof getClaimProof(Long claimId) {
        return claimProofRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("No proof document for claim: " + claimId));
    }

    private ClaimResponseDto toDto(Claim claim, boolean includeHiddenDetail) {
        boolean hasProof = claimProofRepository.existsById(claim.getId());
        return ClaimResponseDto.builder()
                .id(claim.getId())
                .itemId(claim.getItem().getId())
                .itemTitle(claim.getItem().getTitle())
                .itemCategory(claim.getItem().getCategory().name())
                .hiddenDetail(includeHiddenDetail ? claim.getItem().getHiddenDetail() : null)
                .claimantId(claim.getClaimant().getId())
                .claimantName(claim.getClaimant().getName())
                .claimantEmail(claim.getClaimant().getEmail())
                .claimantRollNumber(claim.getClaimant().getRollNumber())
                .lostItemId(claim.getLostItem() != null ? claim.getLostItem().getId() : null)
                .answer(claim.getAnswer())
                .status(claim.getStatus())
                .reviewNote(claim.getReviewNote())
                .reviewedById(claim.getReviewedBy() != null ? claim.getReviewedBy().getId() : null)
                .reviewedByName(claim.getReviewedBy() != null ? claim.getReviewedBy().getName() : null)
                .reviewedAt(claim.getReviewedAt())
                .createdAt(claim.getCreatedAt())
                .hasProof(hasProof)
                .build();
    }
}
