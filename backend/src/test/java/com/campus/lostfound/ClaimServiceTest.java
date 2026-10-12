package com.campus.lostfound;

import com.campus.lostfound.dto.ClaimCreateDto;
import com.campus.lostfound.dto.ClaimResponseDto;
import com.campus.lostfound.dto.ClaimReviewDto;
import com.campus.lostfound.entity.*;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.IllegalStateTransitionException;
import com.campus.lostfound.repository.*;
import com.campus.lostfound.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClaimServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private ClaimProofRepository claimProofRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private StateTransitionService stateTransitionService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private ClaimService claimService;
    private User student;
    private User staff;
    private Item foundItem;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(
                claimRepository, claimProofRepository, itemRepository,
                matchResultRepository, stateTransitionService, notificationService, auditService
        );

        student = User.builder().id(101L).name("Alice").email("alice@campus.edu").role(Role.STUDENT).active(true).build();
        staff = User.builder().id(202L).name("Bob").email("staff@campus.edu").role(Role.STAFF).active(true).build();

        foundItem = Item.builder()
                .id(1L)
                .type(ItemType.FOUND)
                .category(Category.WALLET)
                .title("Brown leather wallet")
                .description("Found on bench")
                .location("Canteen")
                .eventDate(LocalDate.now())
                .status(ItemStatus.LISTED)
                .storedAt("Desk")
                .createdBy(staff)
                .build();
    }

    @Test
    @DisplayName("Submit claim fails if item is not LISTED")
    void testSubmitClaimNonListedItemThrowsException() {
        foundItem.setStatus(ItemStatus.CLAIM_PENDING);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(foundItem));

        ClaimCreateDto dto = ClaimCreateDto.builder().itemId(1L).answer("My initials are on it").build();

        assertThrows(IllegalStateTransitionException.class, () -> claimService.submitClaim(dto, null, student));
    }

    @Test
    @DisplayName("Submit claim fails if claimant already has 3 rejections")
    void testSubmitClaimMaxRejectionLimit() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(foundItem));
        when(claimRepository.countByItemAndClaimantAndStatus(foundItem, student, ClaimStatus.REJECTED)).thenReturn(3L);

        ClaimCreateDto dto = ClaimCreateDto.builder().itemId(1L).answer("Please give it back").build();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> claimService.submitClaim(dto, null, student));
        assertTrue(ex.getMessage().contains("claim limit reached"));
    }

    @Test
    @DisplayName("Review APPROVE transitions item to RETURNED and claim to APPROVED")
    void testReviewApproveClaim() {
        Claim claim = Claim.builder()
                .id(50L)
                .item(foundItem)
                .claimant(student)
                .status(ClaimStatus.PENDING)
                .answer("It has 200 dollars and student ID")
                .build();

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        ClaimReviewDto reviewDto = ClaimReviewDto.builder().decision("APPROVE").note("ID matches").build();
        ClaimResponseDto result = claimService.reviewClaim(50L, reviewDto, staff);

        assertEquals(ClaimStatus.APPROVED, result.getStatus());
        verify(stateTransitionService).transition(foundItem, ItemStatus.RETURNED, "Claim #50 approved", staff.getId());
        verify(notificationService).createNotification(eq(student), contains("APPROVED"), eq("CLAIM_APPROVED"), eq(foundItem.getId()));
    }

    @Test
    @DisplayName("Review REJECT transitions item back to LISTED and claim to REJECTED")
    void testReviewRejectClaim() {
        Claim claim = Claim.builder()
                .id(50L)
                .item(foundItem)
                .claimant(student)
                .status(ClaimStatus.PENDING)
                .answer("Wrong details")
                .build();

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        ClaimReviewDto reviewDto = ClaimReviewDto.builder().decision("REJECT").note("Description mismatch").build();
        ClaimResponseDto result = claimService.reviewClaim(50L, reviewDto, staff);

        assertEquals(ClaimStatus.REJECTED, result.getStatus());
        verify(stateTransitionService).transition(foundItem, ItemStatus.LISTED, "Claim #50 rejected", staff.getId());
        verify(notificationService).createNotification(eq(student), contains("not approved"), eq("CLAIM_REJECTED"), eq(foundItem.getId()));
    }
}
