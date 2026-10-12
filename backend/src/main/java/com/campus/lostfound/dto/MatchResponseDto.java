package com.campus.lostfound.dto;

import com.campus.lostfound.entity.MatchSource;
import com.campus.lostfound.entity.MatchStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponseDto {
    private Long id;
    private ItemPublicDto lostItem;
    private ItemPublicDto foundItem;
    private int score;
    private MatchStatus status;
    private MatchSource source;
    private LocalDateTime createdAt;
}
