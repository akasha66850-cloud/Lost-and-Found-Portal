package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCreateDto {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Answer / proof description is required")
    private String answer;

    private Long lostItemId;
}
