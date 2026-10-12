package com.campus.lostfound.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {
    private Long id;
    private ItemDto lostItem;
    private ItemDto foundItem;
    private int score;
    private MatchStatus status;
    private MatchSource source;
    private LocalDateTime createdAt;

    public MatchDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItemDto getLostItem() { return lostItem; }
    public void setLostItem(ItemDto lostItem) { this.lostItem = lostItem; }

    public ItemDto getFoundItem() { return foundItem; }
    public void setFoundItem(ItemDto foundItem) { this.foundItem = foundItem; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public MatchSource getSource() { return source; }
    public void setSource(MatchSource source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
