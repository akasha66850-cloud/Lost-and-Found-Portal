package com.example.lostfound.controller;

import com.example.lostfound.model.Claim;
import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.Notification;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.ItemPostRepository;
import com.example.lostfound.repository.NotificationRepository;
import com.example.lostfound.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ItemPostRepository itemRepo;
    private final ClaimRepository claimRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;

    public AdminController(ItemPostRepository itemRepo,
                           ClaimRepository claimRepo,
                           UserRepository userRepo,
                           NotificationRepository notifRepo) {
        this.itemRepo = itemRepo;
        this.claimRepo = claimRepo;
        this.userRepo = userRepo;
        this.notifRepo = notifRepo;
    }

    /** 1. Quick stats for admin dashboard */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        List<ItemPost> items = itemRepo.findAll();
        List<Claim> claims = claimRepo.findAll();
        long usersCount = userRepo.count();

        long lostCount = items.stream().filter(ItemPost::isLost).count();
        long foundCount = items.stream().filter(it -> !it.isLost()).count();
        long returnedCount = claims.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        long pendingClaimsCount = claims.stream().filter(c -> "PENDING_VERIFICATION".equals(c.getStatus())).count();
        long awaitingHandoverCount = claims.stream().filter(c -> "APPROVED_AWAITING_HANDOVER".equals(c.getStatus())).count();

        stats.put("totalLost", lostCount);
        stats.put("totalFound", foundCount);
        stats.put("totalReturned", returnedCount);
        stats.put("pendingClaims", pendingClaimsCount);
        stats.put("awaitingHandover", awaitingHandoverCount);
        stats.put("totalUsers", usersCount);
        stats.put("totalItems", items.size());

        return ResponseEntity.ok(stats);
    }

    /** 2. List all items for All Items management */
    @GetMapping("/items")
    public List<ItemPost> listItems() {
        List<ItemPost> items = itemRepo.findAll();
        items.forEach(i -> i.setPhoto(null)); // strip blobs for performance
        return items;
    }

    /** 3. Admin can change item status */
    @PutMapping("/items/{id}/verify")
    public ResponseEntity<?> updateItemStatus(@PathVariable Long id,
                                               @RequestParam String action,
                                               Authentication auth) {
        ItemPost it = itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        switch (action.toLowerCase()) {
            case "verify", "active" -> it.setStatus("ACTIVE");
            case "remove"           -> it.setStatus("REMOVED");
            case "returned"         -> it.setStatus("RETURNED");
            case "resolved"         -> it.setStatus("RESOLVED");
            default                 -> { return ResponseEntity.badRequest().body("Unknown action: " + action); }
        }
        itemRepo.save(it);
        it.setPhoto(null);
        return ResponseEntity.ok(it);
    }

    /** Delete item */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        if (!itemRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        itemRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("msg", "Item deleted successfully"));
    }

    /** 4. List all claims */
    @GetMapping("/claims")
    public List<Claim> listClaims() {
        return claimRepo.findAll();
    }

    /** 5. Users management */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<User> users = userRepo.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("fullName", u.getFullName());
            map.put("role", u.getRole());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** Promote or Demote user role */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id,
                                            @RequestParam String role,
                                            Authentication auth) {
        User target = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("USER")) {
            return ResponseEntity.badRequest().body("Invalid role. Must be ADMIN or USER");
        }

        // Prevent self-demotion
        if (target.getUsername().equalsIgnoreCase(auth.getName()) && role.equalsIgnoreCase("USER")) {
            return ResponseEntity.badRequest().body("You cannot demote your own administrator account");
        }

        target.setRole(role.toUpperCase());
        userRepo.save(target);

        return ResponseEntity.ok(Map.of(
                "msg", "User role updated successfully",
                "username", target.getUsername(),
                "role", target.getRole()
        ));
    }

    /** 6. Notifications monitoring */
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifs = notifRepo.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(notifs);
    }

    /** 7. Reports & Return History */
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReports() {
        Map<String, Object> data = new HashMap<>();
        List<ItemPost> items = itemRepo.findAll();
        List<Claim> claims = claimRepo.findAll();

        // Category breakdown
        Map<String, Long> categoryCount = items.stream()
                .filter(i -> i.getCategory() != null && !i.getCategory().isBlank())
                .collect(Collectors.groupingBy(ItemPost::getCategory, Collectors.counting()));

        // Return history (completed claims)
        List<Map<String, Object>> returnHistory = claims.stream()
                .filter(c -> "COMPLETED".equals(c.getStatus()))
                .map(c -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("claimId", c.getId());
                    entry.put("itemTitle", c.getItemPost() != null ? c.getItemPost().getTitle() : "Unknown");
                    entry.put("category", c.getItemPost() != null ? c.getItemPost().getCategory() : "—");
                    entry.put("claimantName", c.getFullName());
                    entry.put("claimantMobile", c.getMobile());
                    entry.put("confirmedBy", c.getHandoverConfirmedBy() != null ? c.getHandoverConfirmedBy().getUsername() : "Admin");
                    entry.put("handoverDate", c.getHandoverConfirmedAt());
                    return entry;
                })
                .sorted((a, b) -> {
                    if (a.get("handoverDate") == null) return 1;
                    if (b.get("handoverDate") == null) return -1;
                    return ((Comparable) b.get("handoverDate")).compareTo(a.get("handoverDate"));
                })
                .collect(Collectors.toList());

        data.put("categoryDistribution", categoryCount);
        data.put("returnHistory", returnHistory);
        data.put("totalReturns", returnHistory.size());

        return ResponseEntity.ok(data);
    }
}