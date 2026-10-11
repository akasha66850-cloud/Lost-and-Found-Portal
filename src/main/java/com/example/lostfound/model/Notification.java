package com.example.lostfound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer"})
    private User recipient;

    @Column(nullable = false, length = 1000)
    private String message;

    /**
     * MATCH_FOUND | CLAIM_SUBMITTED | CLAIM_APPROVED | CLAIM_REJECTED
     * | ITEM_RETURNED | HANDOVER_REQUESTED | GENERAL
     */
    private String type;

    /** ID of the related ItemPost, Claim, or ItemMatch */
    private Long relatedId;

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
