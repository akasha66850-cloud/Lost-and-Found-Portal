package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemMergeRequestDto {
    @NotNull(message = "Keep item ID is required")
    private Long keepId;

    @NotNull(message = "Remove item ID is required")
    private Long removeId;
}
