package com.example.lostfound.repository;

import com.example.lostfound.model.ItemMatch;
import com.example.lostfound.model.ItemPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {

    Optional<ItemMatch> findByLostItemAndFoundItem(ItemPost lostItem, ItemPost foundItem);

    List<ItemMatch> findByLostItem(ItemPost lostItem);

    List<ItemMatch> findByFoundItem(ItemPost foundItem);

    List<ItemMatch> findByStatus(String status);

    @Query("SELECT m FROM ItemMatch m WHERE m.lostItem.reportedBy.id = :userId OR m.foundItem.reportedBy.id = :userId")
    List<ItemMatch> findByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM ItemMatch m WHERE m.foundItem = :foundItem AND m.status IN ('POTENTIAL_MATCH', 'CLAIM_IN_PROGRESS')")
    List<ItemMatch> findActiveByFoundItem(@Param("foundItem") ItemPost foundItem);
}
