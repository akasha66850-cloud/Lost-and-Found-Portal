package com.campus.lostfound.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String message;
    private String type;
    private Long relatedItemId;
    private boolean read;
    private LocalDateTime createdAt;
}
