package com.campus.lostfound.controller;

import com.campus.lostfound.dto.MatchResponseDto;
import com.campus.lostfound.entity.MatchResult;
import com.campus.lostfound.security.UserPrincipal;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchingService matchingService;
    private final ItemService itemService;

    public MatchController(MatchingService matchingService, ItemService itemService) {
        this.matchingService = matchingService;
        this.itemService = itemService;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<MatchResponseDto>> getMatches(@AuthenticationPrincipal UserPrincipal principal) {
        List<MatchResult> matches = matchingService.getMatchesForUser(principal.getUser());
        List<MatchResponseDto> dtoList = matches.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, String>> dismissMatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        matchingService.dismissMatch(id, principal.getUser());
        return ResponseEntity.ok(Map.of("message", "Match dismissed successfully"));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, String>> dismissMatchPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        matchingService.dismissMatch(id, principal.getUser());
        return ResponseEntity.ok(Map.of("message", "Match dismissed successfully"));
    }

    private MatchResponseDto toDto(MatchResult m) {
        return MatchResponseDto.builder()
                .id(m.getId())
                .lostItem(itemService.toPublicDto(m.getLostItem()))
                .foundItem(itemService.toPublicDto(m.getFoundItem()))
                .score(m.getScore())
                .status(m.getStatus())
                .source(m.getSource())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
