package com.example.lostfound.service;

import com.example.lostfound.model.Claim;
import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.ItemMatchRepository;
import com.example.lostfound.repository.ItemPostRepository;
import com.example.lostfound.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock ClaimRepository claimRepo;
    @Mock ItemPostRepository itemRepo;
    @Mock UserRepository userRepo;
    @Mock NotificationService notificationService;
    @Mock ItemMatchRepository matchRepo;

    @InjectMocks ClaimService claimService;

    private User claimant;
    private User admin;
    private User finder;
    private ItemPost foundItem;

    @BeforeEach
    void setUp() {
        claimant = User.builder().id(1L).username("alice").role("USER").email("a@t.com").password("x").build();
        admin    = User.builder().id(2L).username("admin").role("ADMIN").email("admin@t.com").password("x").build();
        finder   = User.builder().id(3L).username("bob").role("USER").email("b@t.com").password("x").build();

        foundItem = ItemPost.builder()
                .id(10L).title("iPhone 14").category("Electronics")
                .lost(false).status("ACTIVE").reportedBy(finder).dateEvent(LocalDate.now()).build();
    }

    @Test
    void submitClaim_success() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(claimant));
        when(itemRepo.findById(10L)).thenReturn(Optional.of(foundItem));
        when(claimRepo.existsByClaimantAndItemPostAndStatusNotIn(any(), any(), any())).thenReturn(false);
        when(matchRepo.findActiveByFoundItem(foundItem)).thenReturn(Collections.emptyList());
        when(claimRepo.save(any())).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            c = Claim.builder().id(100L).itemPost(foundItem).claimant(claimant)
                    .fullName("Alice").age(20).mobile("9999").proofType("ID")
                    .reason("Mine").govProofImage(new byte[]{1}).productProofImage(new byte[]{1})
                    .status("PENDING_VERIFICATION").build();
            return c;
        });

        Claim result = claimService.submitClaim(10L, "alice", "Alice", 20, "9999",
                "ID", "Mine", new byte[]{1}, new byte[]{1});

        assertThat(result.getStatus()).isEqualTo("PENDING_VERIFICATION");
        verify(notificationService).createNotification(eq(finder), contains("submitted a claim"), any(), any());
    }

    @Test
    void submitClaim_cannotClaimOwnItem_throws() {
        foundItem.setReportedBy(claimant); // Alice found and is trying to claim own item
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(claimant));
        when(itemRepo.findById(10L)).thenReturn(Optional.of(foundItem));

        assertThatThrownBy(() ->
                claimService.submitClaim(10L, "alice", "Alice", 20, "9999", "ID", "Mine", new byte[]{1}, new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot claim");
    }

    @Test
    void submitClaim_lostItemCannotBeClaimed_throws() {
        foundItem.setLost(true); // trying to claim a LOST item
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(claimant));
        when(itemRepo.findById(10L)).thenReturn(Optional.of(foundItem));

        assertThatThrownBy(() ->
                claimService.submitClaim(10L, "alice", "Alice", 20, "9999", "ID", "Mine", new byte[]{1}, new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lost items cannot be claimed");
    }

    @Test
    void approveClaim_success_byAdmin() {
        Claim claim = buildPendingClaim();
        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim result = claimService.approveClaim(100L, "admin");

        assertThat(result.getStatus()).isEqualTo("APPROVED_AWAITING_HANDOVER");
        verify(notificationService).createNotification(eq(claimant), contains("approved"), any(), any());
    }

    @Test
    void approveClaim_success_byFinder() {
        Claim claim = buildPendingClaim();
        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(finder));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim result = claimService.approveClaim(100L, "bob");

        assertThat(result.getStatus()).isEqualTo("APPROVED_AWAITING_HANDOVER");
        verify(notificationService).createNotification(eq(claimant), contains("approved"), any(), any());
    }

    @Test
    void approveClaim_unauthorizedUser_throws() {
        Claim claim = buildPendingClaim();
        User stranger = User.builder().id(99L).username("charlie").role("USER").build();
        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("charlie")).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> claimService.approveClaim(100L, "charlie"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only an Administrator or the student who reported this found item");
    }

    @Test
    void approveClaim_wrongStatus_throws() {
        Claim claim = buildPendingClaim();
        claim.setStatus("REJECTED");
        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> claimService.approveClaim(100L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_VERIFICATION");
    }

    @Test
    void rejectClaim_success() {
        Claim claim = buildPendingClaim();
        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepo.findActiveByFoundItem(foundItem)).thenReturn(Collections.emptyList());

        Claim result = claimService.rejectClaim(100L, "admin", "Not enough evidence");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getReviewNotes()).contains("Not enough evidence");
        verify(notificationService).createNotification(eq(claimant), contains("not approved"), any(), any());
    }

    @Test
    void confirmReturn_success_updatesAllRecords() {
        Claim claim = buildPendingClaim();
        claim.setStatus("APPROVED_AWAITING_HANDOVER");

        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepo.findByFoundItem(foundItem)).thenReturn(Collections.emptyList());

        Claim result = claimService.confirmReturn(100L, "admin");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(foundItem.getStatus()).isEqualTo("RESOLVED");
        verify(notificationService).createNotification(eq(claimant), contains("returned to you"), any(), any());
        verify(notificationService).createNotification(eq(finder), contains("returned to its verified owner"), any(), any());
    }

    @Test
    void confirmReturn_success_byFinder() {
        Claim claim = buildPendingClaim();
        claim.setStatus("APPROVED_AWAITING_HANDOVER");

        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(finder));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepo.findByFoundItem(foundItem)).thenReturn(Collections.emptyList());

        Claim result = claimService.confirmReturn(100L, "bob");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(foundItem.getStatus()).isEqualTo("RESOLVED");
        verify(notificationService).createNotification(eq(claimant), contains("returned to you"), any(), any());
    }

    @Test
    void confirmReturn_alreadyCompleted_throws() {
        Claim claim = buildPendingClaim();
        claim.setStatus("COMPLETED"); // already done

        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> claimService.confirmReturn(100L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED_AWAITING_HANDOVER");
    }

    @Test
    void confirmReturn_onlyPendingClaim_throws() {
        Claim claim = buildPendingClaim();
        // Still PENDING_VERIFICATION — not yet approved
        when(claimRepo.findById(100L)).thenReturn(Optional.of(claim));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> claimService.confirmReturn(100L, "admin"))
                .isInstanceOf(IllegalStateException.class);
    }

    private Claim buildPendingClaim() {
        return Claim.builder()
                .id(100L).itemPost(foundItem).claimant(claimant)
                .fullName("Alice").age(20).mobile("9999").proofType("ID")
                .reason("Mine").govProofImage(new byte[]{1}).productProofImage(new byte[]{1})
                .status("PENDING_VERIFICATION").build();
    }
}
