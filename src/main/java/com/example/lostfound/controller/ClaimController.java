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

    // ── ADMIN: Approve ────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveClaim(@PathVariable Long id, Authentication auth) {
        try {
            Claim claim = claimService.approveClaim(id, auth.getName());
            return ResponseEntity.ok(Map.of("msg", "Claim approved", "status", claim.getStatus()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error approving claim");
        }
    }

    // ── ADMIN: Reject ─────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectClaim(@PathVariable Long id,
                                          @RequestParam(required = false, defaultValue = "") String notes,
                                          Authentication auth) {
        try {
            Claim claim = claimService.rejectClaim(id, auth.getName(), notes);
            return ResponseEntity.ok(Map.of("msg", "Claim rejected", "status", claim.getStatus()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error rejecting claim");
        }
    }

    // ── ADMIN: Confirm Return ─────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/confirm-return")
    public ResponseEntity<?> confirmReturn(@PathVariable Long id, Authentication auth) {
        try {
            Claim claim = claimService.confirmReturn(id, auth.getName());
            return ResponseEntity.ok(Map.of("msg", "Item marked as returned. Notifications sent.", "status", claim.getStatus()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error confirming return");
        }
    }

    // ── ADMIN: Legacy resolve endpoint (kept for compatibility) ─
    @PreAuthorize("hasRole('ADMIN')")
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

    // ── ADMIN: Gov Proof Image ────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/proof/gov/{id}")
    public ResponseEntity<byte[]> viewGovProof(@PathVariable Long id) throws Exception {
        var claim = claimRepo.findById(id).orElseThrow();
        return buildImageResponse(claim.getGovProofImage());
    }

    // ── ADMIN: Product Proof Image ────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/proof/product/{id}")
    public ResponseEntity<byte[]> viewProductProof(@PathVariable Long id) throws Exception {
        var claim = claimRepo.findById(id).orElseThrow();
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