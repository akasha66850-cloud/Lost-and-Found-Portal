package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualMatchRequestDto {

    @NotNull(message = "Lost item ID is required")
    private Long lostId;

    @NotNull(message = "Found item ID is required")
    private Long foundId;
}
