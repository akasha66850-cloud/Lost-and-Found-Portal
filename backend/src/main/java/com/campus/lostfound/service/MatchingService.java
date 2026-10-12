package com.campus.lostfound.service;

import com.campus.lostfound.entity.*;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.MatchResultRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchingService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "with", "for", "near", "from", "has", "have",
            "found", "lost", "was", "were", "this", "that", "some"
    );

    private final MatchResultRepository matchResultRepository;
    private final ItemRepository itemRepository;
    private final StateTransitionService stateTransitionService;
    private final NotificationService notificationService;

    @Value("${matching.threshold:55}")
    private int matchingThreshold;

    public MatchingService(MatchResultRepository matchResultRepository,
                           ItemRepository itemRepository,
                           StateTransitionService stateTransitionService,
                           NotificationService notificationService) {
        this.matchResultRepository = matchResultRepository;
        this.itemRepository = itemRepository;
        this.stateTransitionService = stateTransitionService;
        this.notificationService = notificationService;
    }

    public int getMatchingThreshold() {
        return matchingThreshold;
    }

    public void setMatchingThreshold(int matchingThreshold) {
        this.matchingThreshold = matchingThreshold;
    }

    /**
     * Compute score between a LOST item and a FOUND item.
     * Returns 0 if hard rules fail.
     */
    public int computeScore(Item lost, Item found, boolean enforceDateRule) {
        if (lost == null || found == null) return 0;
        if (lost.getCategory() != found.getCategory()) return 0;

        if (enforceDateRule && found.getEventDate().isBefore(lost.getEventDate())) {
            return 0;
        }

        // Category match: 20
        int score = 20;

        // Location: 30 if equal ignoring case
        if (lost.getLocation() != null && found.getLocation() != null &&
                lost.getLocation().trim().equalsIgnoreCase(found.getLocation().trim())) {
            score += 30;
        }

        // Date proximity: gap = found.eventDate - lost.eventDate (days)
        long gap = Math.abs(ChronoUnit.DAYS.between(lost.getEventDate(), found.getEventDate()));
        if (gap <= 2) {
            score += 20;
        } else if (gap <= 7) {
            score += 10;
        }

        // Color: 10 if both non-blank and equal ignoring case
        if (lost.getColor() != null && !lost.getColor().isBlank() &&
                found.getColor() != null && !found.getColor().isBlank() &&
                lost.getColor().trim().equalsIgnoreCase(found.getColor().trim())) {
            score += 10;
        }

        // Text similarity: Jaccard * 20
        Set<String> lostTokens = tokenize(lost.getTitle() + " " + lost.getDescription() + " " + (lost.getBrand() != null ? lost.getBrand() : ""));
        Set<String> foundTokens = tokenize(found.getTitle() + " " + found.getDescription() + " " + (found.getBrand() != null ? found.getBrand() : ""));
        double jaccard = computeJaccard(lostTokens, foundTokens);
        score += (int) Math.round(jaccard * 20.0);

        return Math.min(100, Math.max(0, score));
    }

    public Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String[] rawTokens = text.toLowerCase().split("[^a-zA-Z0-9]+");
        Set<String> tokens = new HashSet<>();
        for (String t : rawTokens) {
            if (t.length() >= 3 && !STOP_WORDS.contains(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    public double computeJaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    /**
     * Trigger matching engine for a newly created or updated item.
     */
    public void runMatching(Item item) {
        if (item.getType() == ItemType.LOST) {
            runMatchingForLostItem(item);
        } else if (item.getType() == ItemType.FOUND) {
            runMatchingForFoundItem(item);
        }
    }

    private void runMatchingForLostItem(Item lost) {
        if (lost.getStatus() != ItemStatus.OPEN && lost.getStatus() != ItemStatus.MATCHED) {
            return;
        }

        List<Item> candidates = itemRepository.findByCategoryAndTypeAndStatusIn(
                lost.getCategory(), ItemType.FOUND, List.of(ItemStatus.LISTED)
        );

        List<MatchResult> newlyCreatedMatches = new ArrayList<>();
        for (Item found : candidates) {
            if (found.getEventDate().isBefore(lost.getEventDate())) continue;

            int score = computeScore(lost, found, true);
            if (score >= matchingThreshold) {
                if (!matchResultRepository.existsByLostItemAndFoundItem(lost, found)) {
                    MatchResult match = MatchResult.builder()
                            .lostItem(lost)
                            .foundItem(found)
                            .score(score)
                            .status(MatchStatus.SUGGESTED)
                            .source(MatchSource.AUTO)
                            .build();
                    MatchResult saved = matchResultRepository.save(match);
                    newlyCreatedMatches.add(saved);
                }
            }
        }

        if (!newlyCreatedMatches.isEmpty()) {
            if (lost.getStatus() == ItemStatus.OPEN) {
                stateTransitionService.transition(lost, ItemStatus.MATCHED, "Automated match found", null);
                itemRepository.save(lost);
            }
            notifyOwnerOfTopMatches(lost);
        }
    }

    private void runMatchingForFoundItem(Item found) {
        if (found.getStatus() != ItemStatus.LISTED) {
            return;
        }

        List<Item> candidates = itemRepository.findByCategoryAndTypeAndStatusIn(
                found.getCategory(), ItemType.LOST, List.of(ItemStatus.OPEN, ItemStatus.MATCHED)
        );

        Map<Item, List<MatchResult>> matchesPerLostItem = new HashMap<>();
        for (Item lost : candidates) {
            if (found.getEventDate().isBefore(lost.getEventDate())) continue;

            int score = computeScore(lost, found, true);
            if (score >= matchingThreshold) {
                if (!matchResultRepository.existsByLostItemAndFoundItem(lost, found)) {
                    MatchResult match = MatchResult.builder()
                            .lostItem(lost)
                            .foundItem(found)
                            .score(score)
                            .status(MatchStatus.SUGGESTED)
                            .source(MatchSource.AUTO)
                            .build();
                    MatchResult saved = matchResultRepository.save(match);
                    matchesPerLostItem.computeIfAbsent(lost, k -> new ArrayList<>()).add(saved);

                    if (lost.getStatus() == ItemStatus.OPEN) {
                        stateTransitionService.transition(lost, ItemStatus.MATCHED, "Automated match found", null);
                        itemRepository.save(lost);
                    }
                }
            }
        }

        for (Item lost : matchesPerLostItem.keySet()) {
            notifyOwnerOfTopMatches(lost);
        }
    }

    private void notifyOwnerOfTopMatches(Item lost) {
        List<MatchResult> matches = matchResultRepository.findByLostItemOrderByScoreDesc(lost).stream()
                .filter(m -> m.getStatus() == MatchStatus.SUGGESTED)
                .limit(3)
                .toList();

        if (matches.isEmpty()) return;

        StringBuilder msg = new StringBuilder();
        msg.append("Possible matches found for your lost ").append(lost.getTitle()).append(": ");
        for (int i = 0; i < matches.size(); i++) {
            MatchResult m = matches.get(i);
            Item f = m.getFoundItem();
            if (i > 0) msg.append(" | ");
            msg.append(String.format("#%d: %s (%s, score: %d)", i + 1, f.getTitle(), f.getLocation(), m.getScore()));
        }

        notificationService.createNotification(lost.getCreatedBy(), msg.toString(), "MATCH_SUGGESTED", lost.getId());
    }

    public MatchResult createManualMatch(Item lost, Item found, Long actorId) {
        if (lost.getCategory() != found.getCategory()) {
            throw new IllegalArgumentException("Cannot match items with different categories");
        }
        if (matchResultRepository.existsByLostItemAndFoundItem(lost, found)) {
            throw new IllegalStateException("Match already exists between these items");
        }

        int score = computeScore(lost, found, false);
        MatchResult match = MatchResult.builder()
                .lostItem(lost)
                .foundItem(found)
                .score(score)
                .status(MatchStatus.SUGGESTED)
                .source(MatchSource.MANUAL)
                .build();
        MatchResult saved = matchResultRepository.save(match);

        if (lost.getStatus() == ItemStatus.OPEN) {
            stateTransitionService.transition(lost, ItemStatus.MATCHED, "Manual match created", actorId);
            itemRepository.save(lost);
        }

        notificationService.createNotification(
                lost.getCreatedBy(),
                String.format("Staff added a potential match for your lost item [%s]: %s (Score: %d)", lost.getTitle(), found.getTitle(), score),
                "MATCH_MANUAL",
                lost.getId()
        );

        return saved;
    }

    public void dismissMatch(Long matchId, User actor) {
        MatchResult match = matchResultRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        boolean isOwner = match.getLostItem().getCreatedBy().getId().equals(actor.getId());
        boolean isStaffOrAdmin = actor.getRole() == Role.STAFF || actor.getRole() == Role.ADMIN;
        if (!isOwner && !isStaffOrAdmin) {
            throw new ForbiddenException("Only the lost item owner or staff can dismiss a match");
        }

        match.setStatus(MatchStatus.DISMISSED);
        matchResultRepository.save(match);

        // Check if lost item has any other SUGGESTED or CONFIRMED matches
        Item lost = match.getLostItem();
        boolean hasActiveMatches = matchResultRepository.findByLostItemOrderByScoreDesc(lost).stream()
                .anyMatch(m -> m.getStatus() == MatchStatus.SUGGESTED || m.getStatus() == MatchStatus.CONFIRMED);

        if (!hasActiveMatches && lost.getStatus() == ItemStatus.MATCHED) {
            stateTransitionService.transition(lost, ItemStatus.OPEN, "All matches dismissed", actor.getId());
            itemRepository.save(lost);
        }
    }

    @Transactional(readOnly = true)
    public List<MatchResult> getMatchesForUser(User user) {
        if (user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN) {
            return matchResultRepository.findAll();
        }
        return matchResultRepository.findByLostItem_CreatedByOrderByScoreDesc(user);
    }
}
