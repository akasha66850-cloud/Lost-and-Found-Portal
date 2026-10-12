package com.campus.lostfound.repository;

import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.ItemStatus;
import com.campus.lostfound.entity.ItemType;
import com.campus.lostfound.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    List<Item> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    Page<Item> findByCreatedBy(User createdBy, Pageable pageable);

    List<Item> findByTypeAndStatusIn(ItemType type, List<ItemStatus> statuses);

    List<Item> findByCategoryAndTypeAndStatusIn(Category category, ItemType type, List<ItemStatus> statuses);

    long countByStatus(ItemStatus status);

    long countByType(ItemType type);

    long countByTypeAndStatus(ItemType type, ItemStatus status);

    List<Item> findByTypeAndStatus(ItemType type, ItemStatus status);

    @Query("SELECT i.location, COUNT(i) FROM Item i WHERE i.type = com.campus.lostfound.entity.ItemType.LOST GROUP BY i.location ORDER BY COUNT(i) DESC")
    List<Object[]> findTopLostLocations();

    List<Item> findByTypeAndStatusAndEventDateBefore(ItemType type, ItemStatus status, LocalDate thresholdDate);

    List<Item> findByTypeAndStatusAndExpiryWarnedFalseAndEventDateBefore(ItemType type, ItemStatus status, LocalDate thresholdDate);
}
