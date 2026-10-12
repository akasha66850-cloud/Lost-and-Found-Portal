package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisposeRequestDto {
    @NotBlank(message = "Action is required (DONATED or DISPOSED)")
    private String action;
}
