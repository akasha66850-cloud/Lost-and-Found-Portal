package com.example.lostfound.service;

import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.ItemMatch;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.ItemMatchRepository;
import com.example.lostfound.repository.ItemPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock ItemPostRepository itemRepo;
    @Mock ItemMatchRepository matchRepo;
    @Mock NotificationService notificationService;

    @InjectMocks MatchingService matchingService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(1L).username("userA").role("USER").email("a@test.com").password("x").build();
        userB = User.builder().id(2L).username("userB").role("USER").email("b@test.com").password("x").build();
    }

    @Test
    void computeScore_perfectMatch_returns100() {
        ItemPost lost = ItemPost.builder()
                .id(1L).lost(true).title("iPhone 14").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now()).reportedBy(userA).status("ACTIVE").build();

        ItemPost found = ItemPost.builder()
                .id(2L).lost(false).title("iPhone 14 pro").category("Electronics")
                .colour("Black").location("Library Block B").dateEvent(LocalDate.now()).reportedBy(userB).status("ACTIVE").build();

        int score = matchingService.computeScore(lost, found);

        // Category=30, title has 'iphone'=25, colour=15, location has 'library'=20, date=10 => 100
        assertThat(score).isEqualTo(100);
    }

    @Test
    void computeScore_categoryMismatch_reducesScore() {
        ItemPost lost = ItemPost.builder()
                .id(1L).lost(true).title("iPhone 14").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now()).reportedBy(userA).status("ACTIVE").build();

        ItemPost found = ItemPost.builder()
                .id(2L).lost(false).title("iPhone 14").category("Clothing")
                .colour("Black").location("Library").dateEvent(LocalDate.now()).reportedBy(userB).status("ACTIVE").build();

        int score = matchingService.computeScore(lost, found);
        // No category(0) + name(25) + colour(15) + location(20) + date(10) = 70
        assertThat(score).isEqualTo(70);
    }

    @Test
    void computeScore_dateTooFarApart_reducesScore() {
        ItemPost lost = ItemPost.builder()
                .id(1L).lost(true).title("iPhone").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now().minusDays(60)).reportedBy(userA).status("ACTIVE").build();

        ItemPost found = ItemPost.builder()
                .id(2L).lost(false).title("iPhone").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now()).reportedBy(userB).status("ACTIVE").build();

        int score = matchingService.computeScore(lost, found);
        // All except date = 90
        assertThat(score).isEqualTo(90);
    }

    @Test
    void computeScore_missingFields_noException() {
        ItemPost lost = ItemPost.builder()
                .id(1L).lost(true).title("phone").reportedBy(userA).status("ACTIVE").build();
        ItemPost found = ItemPost.builder()
                .id(2L).lost(false).title("phone").reportedBy(userB).status("ACTIVE").build();

        // Should not throw even with all optional fields null
        assertThatCode(() -> matchingService.computeScore(lost, found)).doesNotThrowAnyException();
    }

    @Test
    void runMatching_createsMatchAndNotifiesWhenScoreHigh() {
        ItemPost lostItem = ItemPost.builder()
                .id(1L).lost(true).title("iPhone 14").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now())
                .reportedBy(userA).status("ACTIVE").build();

        ItemPost foundItem = ItemPost.builder()
                .id(2L).lost(false).title("iPhone 14").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now())
                .reportedBy(userB).status("ACTIVE").build();

        when(itemRepo.findByLostAndStatusNotIn(false, Arrays.asList("RETURNED", "RESOLVED", "REMOVED")))
                .thenReturn(Collections.singletonList(foundItem));
        when(matchRepo.findByLostItemAndFoundItem(lostItem, foundItem)).thenReturn(Optional.empty());
        when(matchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        matchingService.runMatching(lostItem);

        verify(matchRepo).save(argThat(m -> m.getScore() >= 50 && "POTENTIAL_MATCH".equals(m.getStatus())));
        verify(notificationService).createNotification(eq(userA), contains("possible match"), eq("MATCH_FOUND"), any());
        verify(notificationService).createNotification(eq(userB), contains("found item"), eq("MATCH_FOUND"), any());
    }

    @Test
    void runMatching_doesNotCreateDuplicateMatch() {
        ItemPost lostItem = ItemPost.builder()
                .id(1L).lost(true).title("iPhone 14").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now())
                .reportedBy(userA).status("ACTIVE").build();

        ItemPost foundItem = ItemPost.builder()
                .id(2L).lost(false).title("iPhone 14").category("Electronics")
                .colour("Black").location("Library").dateEvent(LocalDate.now())
                .reportedBy(userB).status("ACTIVE").build();

        ItemMatch existingMatch = ItemMatch.builder()
                .id(99L).lostItem(lostItem).foundItem(foundItem).score(100).status("POTENTIAL_MATCH").build();

        when(itemRepo.findByLostAndStatusNotIn(false, Arrays.asList("RETURNED", "RESOLVED", "REMOVED")))
                .thenReturn(Collections.singletonList(foundItem));
        when(matchRepo.findByLostItemAndFoundItem(lostItem, foundItem)).thenReturn(Optional.of(existingMatch));

        matchingService.runMatching(lostItem);

        // Should NOT save a new match
        verify(matchRepo, never()).save(any());
    }

    @Test
    void hasSimilarText_returnsTrue_forCommonWord() {
        assertThat(matchingService.hasSimilarText("iPhone 14 Pro", "found iphone near campus")).isTrue();
    }

    @Test
    void hasSimilarText_returnsFalse_forNoCommonLongWords() {
        // "hi" and "it" are both < 3 chars, so no word meets the >=3 threshold
        assertThat(matchingService.hasSimilarText("hi it", "go by me")).isFalse();
    }

    @Test
    void hasSimilarText_returnsTrue_whenCommonWordLongEnough() {
        // "bag" is exactly 3 chars and appears in both strings
        assertThat(matchingService.hasSimilarText("a bag", "a red bag")).isTrue();
    }
}
