package com.campus.lostfound;

import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.ItemType;
import com.campus.lostfound.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MatchingServiceTest {

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        // Mock dependencies not needed for score computation
        matchingService = new MatchingService(null, null, null, null);
        matchingService.setMatchingThreshold(55);
    }

    @Test
    @DisplayName("Hard failure: different categories score 0")
    void testDifferentCategoriesReturnZero() {
        Item lost = Item.builder()
                .type(ItemType.LOST)
                .category(Category.PHONE)
                .location("Library")
                .eventDate(LocalDate.now())
                .title("iPhone")
                .description("Black")
                .build();

        Item found = Item.builder()
                .type(ItemType.FOUND)
                .category(Category.WALLET)
                .location("Library")
                .eventDate(LocalDate.now())
                .title("iPhone")
                .description("Black")
                .build();

        assertEquals(0, matchingService.computeScore(lost, found, true));
    }

    @Test
    @DisplayName("Hard failure: found date before lost date when enforced")
    void testFoundBeforeLostReturnsZero() {
        Item lost = Item.builder()
                .category(Category.KEYS)
                .eventDate(LocalDate.of(2026, 3, 10))
                .location("Block A")
                .title("Dorm Keys")
                .description("Brass key")
                .build();

        Item found = Item.builder()
                .category(Category.KEYS)
                .eventDate(LocalDate.of(2026, 3, 5))
                .location("Block A")
                .title("Dorm Keys")
                .description("Brass key")
                .build();

        assertEquals(0, matchingService.computeScore(lost, found, true));
    }

    @Test
    @DisplayName("Identical items score high and <= 100")
    void testIdenticalItemsHighConfidence() {
        Item lost = Item.builder()
                .category(Category.LAPTOP)
                .location("Library")
                .color("Silver")
                .brand("Apple")
                .title("MacBook Air M2")
                .description("Silver MacBook Air with stickers on lid")
                .eventDate(LocalDate.of(2026, 3, 10))
                .build();

        Item found = Item.builder()
                .category(Category.LAPTOP)
                .location("Library")
                .color("Silver")
                .brand("Apple")
                .title("MacBook Air M2 Silver")
                .description("MacBook Air laptop found on desk with stickers")
                .eventDate(LocalDate.of(2026, 3, 11))
                .build();

        int score = matchingService.computeScore(lost, found, true);
        // Category(20) + Location(30) + Date(20, 1 day gap) + Color(10) + Jaccard tokens (~15-20)
        assertTrue(score >= 80, "Expected high score >= 80, but got: " + score);
        assertTrue(score <= 100, "Score must not exceed 100");
    }

    @Test
    @DisplayName("Tokenization filters stop words and words < 3 characters")
    void testTokenizationAndStopWords() {
        Set<String> tokens = matchingService.tokenize("The blue bottle was lost near Block A for 2 hours");
        assertFalse(tokens.contains("the"));
        assertFalse(tokens.contains("was"));
        assertFalse(tokens.contains("lost"));
        assertFalse(tokens.contains("near"));
        assertFalse(tokens.contains("for"));
        assertFalse(tokens.contains("a"));
        assertFalse(tokens.contains("2"));
        assertTrue(tokens.contains("blue"));
        assertTrue(tokens.contains("bottle"));
        assertTrue(tokens.contains("block"));
        assertTrue(tokens.contains("hours"));
    }
}
