package com.campus.lostfound.dto;

import com.campus.lostfound.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateDto {
    @NotNull(message = "Role is required")
    private Role role;
}
