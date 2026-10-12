package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email or Roll Number is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
