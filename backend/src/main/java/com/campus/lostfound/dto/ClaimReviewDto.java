package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewDto {

    @NotBlank(message = "Decision is required (APPROVE or REJECT)")
    private String decision;

    private String note;
}
