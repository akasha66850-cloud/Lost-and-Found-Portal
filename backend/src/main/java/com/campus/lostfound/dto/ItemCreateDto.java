package com.campus.lostfound.dto;

import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateDto {

    @NotNull(message = "Item type is required")
    private ItemType type;

    @NotNull(message = "Category is required")
    private Category category;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 5, message = "Description must be at least 5 characters")
    private String description;

    private String color;

    private String brand;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    private String hiddenDetail;

    private String storedAt;

    private String ownerRollNumber;
}
