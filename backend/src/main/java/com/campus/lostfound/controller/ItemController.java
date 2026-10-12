package com.campus.lostfound.controller;

import com.campus.lostfound.dto.*;
import com.campus.lostfound.entity.*;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.security.UserPrincipal;
import com.campus.lostfound.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createItemMultipart(
            @RequestPart("data") @Valid ItemCreateDto dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal principal) {
        return handleCreateItem(dto, photo, principal.getUser());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createItemJson(
            @Valid @RequestBody ItemCreateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return handleCreateItem(dto, null, principal.getUser());
    }

    private ResponseEntity<?> handleCreateItem(ItemCreateDto dto, MultipartFile photo, User user) {
        ItemType type = dto.getType();
        if (type == null) {
            type = (user.getRole() == Role.STUDENT) ? ItemType.LOST : ItemType.FOUND;
        }

        if (type == ItemType.FOUND) {
            if (user.getRole() == Role.STUDENT) {
                throw new ForbiddenException("Students cannot register found items directly. Please hand items to campus staff.");
            }
            ItemStaffDto created = itemService.createFoundItem(dto, photo, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } else {
            ItemPublicDto created = itemService.createLostItem(dto, photo, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<?>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<?> results = itemService.searchItems(q, category, location, status, type, dateFrom, dateTo, pageable, principal.getUser());
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ItemPublicDto>> getMyReports(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(itemService.getMyReports(principal.getUser()));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<PageResponse<ItemStaffDto>> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageResponse.of(itemService.getInventory(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(itemService.getItemById(id, principal.getUser()));
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getItemPhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ItemImage photo = itemService.getItemPhoto(id, principal.getUser());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, photo.getContentType())
                .body(photo.getContent());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemPublicDto> editLostItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(itemService.editLostItem(id, dto, principal.getUser()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ItemPublicDto> cancelLostItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(itemService.cancelLostItem(id, principal.getUser()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ItemPublicDto> cancelLostItemPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(itemService.cancelLostItem(id, principal.getUser()));
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ItemStaffDto> disposeItem(
            @PathVariable Long id,
            @Valid @RequestBody DisposeRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(itemService.disposeItem(id, dto.getAction(), principal.getUser()));
    }
}
