package com.example.lostfound.repository;

import com.example.lostfound.model.Claim;
import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByClaimant(User user);

    List<Claim> findByStatus(String status);

    List<Claim> findByItemPost(ItemPost itemPost);

    Optional<Claim> findByClaimantAndItemPost(User claimant, ItemPost itemPost);

    long countByStatus(String status);

    boolean existsByClaimantAndItemPostAndStatusNotIn(User claimant, ItemPost itemPost, List<String> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Claim c WHERE c.itemPost.reportedBy = :finder ORDER BY c.createdAt DESC")
    List<Claim> findIncomingClaimsForFinder(@org.springframework.data.repository.query.Param("finder") User finder);
}
