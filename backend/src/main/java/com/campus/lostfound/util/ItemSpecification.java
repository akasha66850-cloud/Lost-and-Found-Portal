package com.campus.lostfound.util;

import com.campus.lostfound.entity.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ItemSpecification {

    public static Specification<Item> buildSpecification(
            String q,
            Category category,
            String location,
            ItemStatus status,
            ItemType type,
            LocalDate dateFrom,
            LocalDate dateTo,
            User currentUser
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Visibility enforcement
            if (currentUser.getRole() == Role.STUDENT) {
                Predicate isVisibleFound = cb.and(
                        cb.equal(root.get("type"), ItemType.FOUND),
                        root.get("status").in(List.of(ItemStatus.LISTED, ItemStatus.CLAIM_PENDING))
                );
                Predicate isOwnLost = cb.and(
                        cb.equal(root.get("type"), ItemType.LOST),
                        cb.equal(root.get("createdBy").get("id"), currentUser.getId())
                );
                predicates.add(cb.or(isVisibleFound, isOwnLost));
            }

            // Text search across title, description, brand, location, color
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descPred = cb.like(cb.lower(root.get("description")), pattern);
                Predicate brandPred = cb.like(cb.lower(root.get("brand")), pattern);
                Predicate locPred = cb.like(cb.lower(root.get("location")), pattern);
                Predicate colorPred = cb.like(cb.lower(root.get("color")), pattern);
                predicates.add(cb.or(titlePred, descPred, brandPred, locPred, colorPred));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (location != null && !location.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("location")), location.trim().toLowerCase()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
