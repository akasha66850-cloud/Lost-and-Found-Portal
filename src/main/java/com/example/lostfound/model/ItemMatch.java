package com.example.lostfound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "item_matches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lost_item_id", "found_item_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ItemMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lost_item_id", nullable = false)
    @JsonIgnoreProperties({"photo", "hibernateLazyInitializer"})
    private ItemPost lostItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "found_item_id", nullable = false)
    @JsonIgnoreProperties({"photo", "hibernateLazyInitializer"})
    private ItemPost foundItem;

    private int score;

    /**
     * POTENTIAL_MATCH | CLAIM_IN_PROGRESS | RESOLVED | REJECTED
     */
    @Column(nullable = false)
    private String status;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
