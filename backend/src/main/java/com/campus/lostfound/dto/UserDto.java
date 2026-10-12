package com.campus.lostfound.dto;

import com.campus.lostfound.entity.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String rollNumber;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}
