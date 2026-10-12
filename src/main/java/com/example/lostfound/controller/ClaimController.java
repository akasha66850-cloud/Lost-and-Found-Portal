package com.example.lostfound.controller;

import com.example.lostfound.model.Claim;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.UserRepository;
import com.example.lostfound.service.ClaimService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimRepository claimRepo;
    private final UserRepository userRepo;

    public ClaimController(ClaimService claimService,
                           ClaimRepository claimRepo,
                           UserRepository userRepo) {
        this.claimService = claimService;
        this.claimRepo = claimRepo;
        this.userRepo = userRepo;
    }

    // ── USER: Submit Claim ────────────────────────────────────
    @PostMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitClaim(
            @PathVariable Long itemId,
            @RequestParam String fullName,
            @RequestParam Integer age,
            @RequestParam String mobile,
            @RequestParam String govProofName,
            @RequestParam String reason,
            @RequestParam MultipartFile govProofImage,
            @RequestParam MultipartFile productProofImage,
            Authentication auth) {

        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");

        if (govProofImage == null || govProofImage.isEmpty()
                || productProofImage == null || productProofImage.isEmpty()) {
            return ResponseEntity.badRequest().body("Both proof images are required");
        }

        try {
            Claim claim = claimService.submitClaim(
                    itemId, auth.getName(), fullName, age, mobile,
                    govProofName, reason,
                    govProofImage.getBytes(), productProofImage.getBytes());
            return ResponseEntity.ok(Map.of("msg", "Claim submitted successfully", "claimId", claim.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error submitting claim");
        }
    }

    // ── USER: My Claims ───────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> myClaims(Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
        var user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(claimRepo.findByClaimant(user));
    }

    // ── FINDER: Incoming Claims on items reported by current user ───
    @GetMapping("/incoming")
    public ResponseEntity<?> getIncomingClaims(Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
        var user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(claimRepo.findIncomingClaimsForFinder(user));
    }

    // ── ADMIN: List Pending Claims ────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<Claim>> getPendingClaims() {
        return ResponseEntity.ok(claimRepo.findByStatus("PENDING_VERIFICATION"));
    }

    // ── ADMIN: List All Claims ────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Claim>> getAllClaims() {
        return ResponseEntity.ok(claimRepo.findAll());
    }

    // ── ADMIN OR FINDER: Approve ──────────────────────────────
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveClaim(@PathVariable Long id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
        try {
            Claim claim = claimService.approveClaim(id, auth.getName());
            return ResponseEntity.ok(Map.of("msg", "Claim approved", "status", claim.getStatus()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error approving claim: " + e.getMessage());
        }
    }

    // ── ADMIN OR FINDER: Reject ───────────────────────────────
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectClaim(@PathVariable Long id,
                                          @RequestParam(required = false, defaultValue = "") String notes,
                                          Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
        try {
            Claim claim = claimService.rejectClaim(id, auth.getName(), notes);
            return ResponseEntity.ok(Map.of("msg", "Claim rejected", "status", claim.getStatus()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error rejecting claim: " + e.getMessage());
        }
    }

    // ── ADMIN OR FINDER: Confirm Return ───────────────────────
    @PostMapping("/{id}/confirm-return")
    public ResponseEntity<?> confirmReturn(@PathVariable Long id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
        try {
            Claim claim = claimService.confirmReturn(id, auth.getName());
            return ResponseEntity.ok(Map.of("msg", "Item marked as returned. Notifications sent.", "status", claim.getStatus()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error confirming return: " + e.getMessage());
        }
    }

    // ── ADMIN OR FINDER: Legacy resolve endpoint ──────────────
    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveClaim(@PathVariable Long id,
                                           @RequestParam String status,
                                           Authentication auth) {
        if ("APPROVED".equalsIgnoreCase(status)) {
            return approveClaim(id, auth);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            return rejectClaim(id, "", auth);
        }
        return ResponseEntity.badRequest().body("Invalid status. Use approve or reject endpoints.");
    }

    // ── ADMIN, FINDER, OR CLAIMANT: Gov Proof Image ───────────
    @GetMapping("/proof/gov/{id}")
    public ResponseEntity<byte[]> viewGovProof(@PathVariable Long id, Authentication auth) throws Exception {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        var claim = claimRepo.findById(id).orElseThrow();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isClaimant = claim.getClaimant() != null && claim.getClaimant().getId().equals(user.getId());
        boolean isFinder = claim.getItemPost() != null &&
                claim.getItemPost().getReportedBy() != null &&
                claim.getItemPost().getReportedBy().getId().equals(user.getId());
        if (!isAdmin && !isClaimant && !isFinder) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return buildImageResponse(claim.getGovProofImage());
    }

    // ── ADMIN, FINDER, OR CLAIMANT: Product Proof Image ───────
    @GetMapping("/proof/product/{id}")
    public ResponseEntity<byte[]> viewProductProof(@PathVariable Long id, Authentication auth) throws Exception {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        var claim = claimRepo.findById(id).orElseThrow();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isClaimant = claim.getClaimant() != null && claim.getClaimant().getId().equals(user.getId());
        boolean isFinder = claim.getItemPost() != null &&
                claim.getItemPost().getReportedBy() != null &&
                claim.getItemPost().getReportedBy().getId().equals(user.getId());
        if (!isAdmin && !isClaimant && !isFinder) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return buildImageResponse(claim.getProductProofImage());
    }

    private ResponseEntity<byte[]> buildImageResponse(byte[] image) throws Exception {
        if (image == null) return ResponseEntity.notFound().build();
        String mimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(image));
        if (mimeType == null) mimeType = "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(image);
    }
}