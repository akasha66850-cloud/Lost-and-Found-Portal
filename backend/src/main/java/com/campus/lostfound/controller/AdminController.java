package com.campus.lostfound.controller;

import com.campus.lostfound.dto.*;
import com.campus.lostfound.entity.AuditLog;
import com.campus.lostfound.security.UserPrincipal;
import com.campus.lostfound.service.AdminService;
import com.campus.lostfound.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserDto created = adminService.createUser(dto, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/users/{id}/active")
    public ResponseEntity<UserDto> updateUserActive(
            @PathVariable Long id,
            @Valid @RequestBody UserActiveUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.updateUserActive(id, dto.getActive(), principal.getUser()));
    }

    @PostMapping("/users/{id}/active")
    public ResponseEntity<UserDto> updateUserActivePost(
            @PathVariable Long id,
            @Valid @RequestBody UserActiveUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.updateUserActive(id, dto.getActive(), principal.getUser()));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.updateUserRole(id, dto.getRole(), principal.getUser()));
    }

    @PostMapping("/users/{id}/role")
    public ResponseEntity<UserDto> updateUserRolePost(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.updateUserRole(id, dto.getRole(), principal.getUser()));
    }

    @PostMapping("/matches")
    public ResponseEntity<MatchResponseDto> createManualMatch(
            @Valid @RequestBody ManualMatchRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.createManualMatch(dto, principal.getUser()));
    }

    @PostMapping("/items/merge")
    public ResponseEntity<Map<String, String>> mergeItems(
            @Valid @RequestBody ItemMergeRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        adminService.mergeItems(dto, principal.getUser());
        return ResponseEntity.ok(Map.of("message", "Items merged successfully"));
    }

    @GetMapping("/audit")
    public ResponseEntity<PageResponse<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(PageResponse.of(logs));
    }
}
