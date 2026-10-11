package com.example.lostfound.controller;

import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.ItemPostRepository;
import com.example.lostfound.repository.UserRepository;
import com.example.lostfound.service.MatchingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemPostRepository itemRepo;
    private final UserRepository userRepo;
    private final MatchingService matchingService;

    public ItemController(ItemPostRepository itemRepo,
                          UserRepository userRepo,
                          MatchingService matchingService) {
        this.itemRepo = itemRepo;
        this.userRepo = userRepo;
        this.matchingService = matchingService;
    }

    // ── PUBLIC SEARCH ────────────────────────────────────────
    @GetMapping
    public List<ItemPost> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isLost,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ItemPost> items = itemRepo.search(
                q, location, category, isLost, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        // Strip blob bytes to keep response small
        items.forEach(i -> i.setPhoto(null));
        return items;
    }

    // ── PUBLIC VIEW SINGLE ITEM ───────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ItemPost> getItemById(@PathVariable Long id) {
        return itemRepo.findById(id)
                .map(item -> {
                    // Don't strip photo here — needed for detail page
                    return ResponseEntity.ok(item);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── PUBLIC PHOTO ──────────────────────────────────────────
    @GetMapping("/photo/{id}")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return itemRepo.findById(id)
                .filter(i -> i.getPhoto() != null)
                .map(i -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(i.getPhoto()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── AUTHENTICATED: MY REPORTS ─────────────────────────────
    @GetMapping("/my-reports")
    public ResponseEntity<?> myReports(Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
        List<ItemPost> items = itemRepo.findByReportedByUsername(
                auth.getName(), Sort.by(Sort.Direction.DESC, "createdAt"));
        items.forEach(i -> i.setPhoto(null));
        return ResponseEntity.ok(items);
    }

    // ── POST LOST / FOUND ITEM ────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String location,
            @RequestParam String dateEvent,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String mobile,
            @RequestParam boolean lost,
            @RequestParam(required = false) MultipartFile photo,
            Authentication auth) throws Exception {

        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login required to post items");
        }

        User reporter = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ItemPost item = new ItemPost();
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(location);
        item.setMobile(mobile);
        item.setCategory(category);
        item.setColour(colour);
        item.setLost(lost);
        item.setDateEvent(LocalDate.parse(dateEvent));
        item.setReportedBy(reporter);
        item.setStatus("ACTIVE");

        if (photo != null && !photo.isEmpty()) {
            if (photo.getSize() > 20 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Image must be under 20MB");
            }
            String ct = photo.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }
            item.setPhoto(photo.getBytes());
        }

        ItemPost saved = itemRepo.save(item);

        // Trigger async-style matching (synchronous for simplicity)
        try {
            matchingService.runMatching(saved);
        } catch (Exception e) {
            // Matching failure should not fail the report submission
        }

        // Strip photo from response
        saved.setPhoto(null);
        return ResponseEntity.ok(saved);
    }
}
