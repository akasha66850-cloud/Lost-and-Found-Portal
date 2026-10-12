package com.campus.lostfound.controller;

import com.campus.lostfound.dto.ClaimCreateDto;
import com.campus.lostfound.dto.ClaimResponseDto;
import com.campus.lostfound.dto.ClaimReviewDto;
import com.campus.lostfound.dto.PageResponse;
import com.campus.lostfound.entity.ClaimProof;
import com.campus.lostfound.entity.ClaimStatus;
import com.campus.lostfound.security.UserPrincipal;
import com.campus.lostfound.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimResponseDto> submitClaimMultipart(
            @RequestPart("data") @Valid ClaimCreateDto dto,
            @RequestPart(value = "proof", required = false) MultipartFile proofFile,
            @AuthenticationPrincipal UserPrincipal principal) {
        ClaimResponseDto created = claimService.submitClaim(dto, proofFile, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClaimResponseDto> submitClaimJson(
            @Valid @RequestBody ClaimCreateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        ClaimResponseDto created = claimService.submitClaim(dto, null, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ClaimResponseDto>> getMyClaims(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(claimService.getMyClaims(principal.getUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<PageResponse<ClaimResponseDto>> getClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ClaimResponseDto> results = claimService.getClaims(status, pageable);
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @RequestMapping(value = "/{id}/review", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ClaimResponseDto> reviewClaim(
            @PathVariable Long id,
            @Valid @RequestBody ClaimReviewDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(claimService.reviewClaim(id, dto, principal.getUser()));
    }

    @GetMapping("/{id}/proof")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<byte[]> getClaimProof(@PathVariable Long id) {
        ClaimProof proof = claimService.getClaimProof(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, proof.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + proof.getFileName() + "\"")
                .body(proof.getContent());
    }
}
