package com.example.lostfound.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "item_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    private String mobile;

    private LocalDate dateEvent;

    private String category;

    /** Colour of the item e.g. "Black", "Silver" */
    private String colour;

    /** true = LOST report, false = FOUND report */
    private boolean lost;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] photo;

    /**
     * Status values:
     *  ACTIVE       – default for new reports
     *  RETURNED     – lost item returned to owner
     *  RESOLVED     – found item handed back
     *  REMOVED      – admin removed
     */
    @Builder.Default
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.EAGER)
    private User reportedBy;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
