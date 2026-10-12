package com.campus.lostfound.repository;

import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.MatchResult;
import com.campus.lostfound.entity.MatchStatus;
import com.campus.lostfound.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    Optional<MatchResult> findByLostItemAndFoundItem(Item lostItem, Item foundItem);

    boolean existsByLostItemAndFoundItem(Item lostItem, Item foundItem);

    @EntityGraph(attributePaths = {"lostItem", "foundItem", "lostItem.createdBy", "foundItem.createdBy"})
    List<MatchResult> findByLostItem_CreatedByOrderByScoreDesc(User user);

    @EntityGraph(attributePaths = {"lostItem", "foundItem", "lostItem.createdBy", "foundItem.createdBy"})
    List<MatchResult> findByLostItemOrderByScoreDesc(Item lostItem);

    @EntityGraph(attributePaths = {"lostItem", "foundItem", "lostItem.createdBy", "foundItem.createdBy"})
    List<MatchResult> findByFoundItemOrderByScoreDesc(Item foundItem);

    @EntityGraph(attributePaths = {"lostItem", "foundItem", "lostItem.createdBy", "foundItem.createdBy"})
    List<MatchResult> findByStatusOrderByCreatedAtDesc(MatchStatus status);

    @Override
    @EntityGraph(attributePaths = {"lostItem", "foundItem", "lostItem.createdBy", "foundItem.createdBy"})
    List<MatchResult> findAll();
}
