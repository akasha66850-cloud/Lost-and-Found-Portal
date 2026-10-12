package com.campus.lostfound.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDto {
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
    private boolean hasPhoto;
    private boolean expiryWarned;
    private LocalDateTime returnedAt;

    public ItemDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public String getHiddenDetail() { return hiddenDetail; }
    public void setHiddenDetail(String hiddenDetail) { this.hiddenDetail = hiddenDetail; }

    public String getStoredAt() { return storedAt; }
    public void setStoredAt(String storedAt) { this.storedAt = storedAt; }

    public String getOwnerRollNumber() { return ownerRollNumber; }
    public void setOwnerRollNumber(String ownerRollNumber) { this.ownerRollNumber = ownerRollNumber; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isHasPhoto() { return hasPhoto; }
    public void setHasPhoto(boolean hasPhoto) { this.hasPhoto = hasPhoto; }

    public boolean isExpiryWarned() { return expiryWarned; }
    public void setExpiryWarned(boolean expiryWarned) { this.expiryWarned = expiryWarned; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
}
