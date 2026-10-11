package com.example.lostfound.repository;

import com.example.lostfound.model.ItemPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemPostRepository extends JpaRepository<ItemPost, Long> {

    @Query("SELECT i FROM ItemPost i WHERE " +
           "(:q IS NULL OR lower(i.title) LIKE lower(concat('%',:q,'%')) OR lower(i.description) LIKE lower(concat('%',:q,'%'))) " +
           "AND (:loc IS NULL OR lower(i.location) LIKE lower(concat('%',:loc,'%'))) " +
           "AND (:cat IS NULL OR i.category = :cat) " +
           "AND (:isLost IS NULL OR i.lost = :isLost) " +
           "AND (:status IS NULL OR i.status = :status)")
    List<ItemPost> search(
            @Param("q") String q,
            @Param("loc") String loc,
            @Param("cat") String cat,
            @Param("isLost") Boolean isLost,
            @Param("status") String status,
            Pageable pageable);

    List<ItemPost> findByReportedByUsername(String username, Sort sort);

    List<ItemPost> findByLostAndStatusNotIn(boolean lost, List<String> statuses);

    long countByLostTrueAndStatusNotIn(List<String> statuses);

    long countByLostFalseAndStatusNotIn(List<String> statuses);

    List<ItemPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
