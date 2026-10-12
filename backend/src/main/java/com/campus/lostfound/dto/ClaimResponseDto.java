package com.campus.lostfound.dto;

import com.campus.lostfound.entity.ClaimStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponseDto {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private String itemCategory;
    private String hiddenDetail;
    private Long claimantId;
    private String claimantName;
    private String claimantEmail;
    private String claimantRollNumber;
    private Long lostItemId;
    private String answer;
    private ClaimStatus status;
    private String reviewNote;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private boolean hasProof;
}
