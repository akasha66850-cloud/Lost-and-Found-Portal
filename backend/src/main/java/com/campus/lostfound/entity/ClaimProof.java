package com.campus.lostfound.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "claim_proofs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimProof {

    @Id
    private Long claimId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] content;
}
