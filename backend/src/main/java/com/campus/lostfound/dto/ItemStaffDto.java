package com.campus.lostfound.dto;

import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.ItemStatus;
import com.campus.lostfound.entity.ItemType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemStaffDto {
    private Long id;
    private ItemType type;
    private Category category;
    private String title;
    private String description;
    private String color;
    private String brand;
    private String location;
    private LocalDate eventDate;
    private ItemStatus status;
    private String hiddenDetail;
    private String storedAt;
    private String ownerRollNumber;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean expiryWarned;
    private LocalDateTime returnedAt;
    private Long mergedIntoId;
    private Long version;
    private boolean hasPhoto;
}
