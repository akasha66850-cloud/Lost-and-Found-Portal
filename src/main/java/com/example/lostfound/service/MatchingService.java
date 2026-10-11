package com.example.lostfound.service;

import com.example.lostfound.model.ItemMatch;
import com.example.lostfound.model.ItemPost;
import com.example.lostfound.repository.ItemMatchRepository;
import com.example.lostfound.repository.ItemPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    // Scoring weights
    private static final int WEIGHT_CATEGORY = 30;
    private static final int WEIGHT_NAME     = 25;
    private static final int WEIGHT_COLOUR   = 15;
    private static final int WEIGHT_LOCATION = 20;
    private static final int WEIGHT_DATE     = 10;

    /** Minimum score to create a match record */
    private static final int MIN_SCORE      = 30;
    /** Minimum score to send notifications */
    private static final int NOTIFY_SCORE   = 50;

    private final ItemPostRepository itemRepo;
    private final ItemMatchRepository matchRepo;
    private final NotificationService notificationService;

    public MatchingService(ItemPostRepository itemRepo,
                           ItemMatchRepository matchRepo,
                           NotificationService notificationService) {
        this.itemRepo = itemRepo;
        this.matchRepo = matchRepo;
        this.notificationService = notificationService;
    }

    /**
     * Run matching for a newly saved item.
     * Called after every POST /api/items.
     */
    public void runMatching(ItemPost newItem) {
        List<String> excludedStatuses = Arrays.asList("RETURNED", "RESOLVED", "REMOVED");

        // Find candidate items of the opposite type
        List<ItemPost> candidates = itemRepo.findByLostAndStatusNotIn(!newItem.isLost(), excludedStatuses);

        for (ItemPost candidate : candidates) {
            ItemPost lostItem  = newItem.isLost()   ? newItem   : candidate;
            ItemPost foundItem = newItem.isLost()   ? candidate : newItem;

            int score = computeScore(lostItem, foundItem);
            if (score < MIN_SCORE) continue;

            // Avoid duplicates — unique constraint also guards at DB level
            if (matchRepo.findByLostItemAndFoundItem(lostItem, foundItem).isPresent()) continue;

            ItemMatch match = ItemMatch.builder()
                    .lostItem(lostItem)
                    .foundItem(foundItem)
                    .score(score)
                    .status("POTENTIAL_MATCH")
                    .build();

            try {
                matchRepo.save(match);
            } catch (DataIntegrityViolationException e) {
                log.warn("Duplicate match prevented by DB constraint for lost={} found={}", lostItem.getId(), foundItem.getId());
                continue;
            }

            if (score >= NOTIFY_SCORE) {
                // Notify the person who lost the item
                if (lostItem.getReportedBy() != null) {
                    notificationService.createNotification(
                            lostItem.getReportedBy(),
                            "A possible match has been found for your lost item \"" + lostItem.getTitle() +
                                    "\". Review the details and submit a claim if you believe it is yours.",
                            "MATCH_FOUND",
                            match.getId()
                    );
                }
                // Notify the person who found the item
                if (foundItem.getReportedBy() != null) {
                    notificationService.createNotification(
                            foundItem.getReportedBy(),
                            "Your found item report \"" + foundItem.getTitle() +
                                    "\" may match a lost item report. Please check the application for updates.",
                            "MATCH_FOUND",
                            match.getId()
                    );
                }
            }
        }
    }

    /**
     * Compute match score (0–100) between a lost item and a found item.
     * Missing fields contribute 0 for that dimension — no exceptions thrown.
     */
    int computeScore(ItemPost lostItem, ItemPost foundItem) {
        int score = 0;

        // Category (30 pts)
        if (isNonBlank(lostItem.getCategory()) && isNonBlank(foundItem.getCategory())
                && lostItem.getCategory().equalsIgnoreCase(foundItem.getCategory())) {
            score += WEIGHT_CATEGORY;
        }

        // Item name / title similarity (25 pts)
        if (isNonBlank(lostItem.getTitle()) && isNonBlank(foundItem.getTitle())
                && hasSimilarText(lostItem.getTitle(), foundItem.getTitle())) {
            score += WEIGHT_NAME;
        }

        // Colour (15 pts)
        if (isNonBlank(lostItem.getColour()) && isNonBlank(foundItem.getColour())
                && lostItem.getColour().equalsIgnoreCase(foundItem.getColour())) {
            score += WEIGHT_COLOUR;
        }

        // Location (20 pts)
        if (isNonBlank(lostItem.getLocation()) && isNonBlank(foundItem.getLocation())
                && hasSimilarText(lostItem.getLocation(), foundItem.getLocation())) {
            score += WEIGHT_LOCATION;
        }

        // Date proximity (10 pts) — within 30 days of each other
        if (lostItem.getDateEvent() != null && foundItem.getDateEvent() != null) {
            long days = Math.abs(ChronoUnit.DAYS.between(lostItem.getDateEvent(), foundItem.getDateEvent()));
            if (days <= 30) {
                score += WEIGHT_DATE;
            }
        }

        return score;
    }

    /**
     * Returns true if the two strings share at least one meaningful common word (length >= 3).
     */
    boolean hasSimilarText(String a, String b) {
        if (a == null || b == null) return false;
        String[] wordsA = a.toLowerCase().split("[\\s,;.\\-_/]+");
        String lowerB = b.toLowerCase();
        for (String word : wordsA) {
            if (word.length() >= 3 && lowerB.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }
}
