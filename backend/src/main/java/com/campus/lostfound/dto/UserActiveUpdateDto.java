package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActiveUpdateDto {
    @NotNull(message = "Active status is required")
    private Boolean active;
}
