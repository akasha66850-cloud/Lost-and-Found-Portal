package com.campus.lostfound.service;

import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.ItemStatus;
import com.campus.lostfound.entity.ItemType;
import com.campus.lostfound.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StateTransitionService {

    private final AuditService auditService;

    public StateTransitionService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void transition(Item item, ItemStatus targetStatus, String reason, Long actorId) {
        ItemStatus current = item.getStatus();
        if (current == targetStatus) {
            return;
        }

        if (item.getType() == ItemType.LOST) {
            validateLostItemTransition(current, targetStatus, item.getId());
        } else if (item.getType() == ItemType.FOUND) {
            validateFoundItemTransition(current, targetStatus, item.getId());
        } else {
            throw new IllegalStateTransitionException("Unknown item type: " + item.getType());
        }

        item.setStatus(targetStatus);
        if (targetStatus == ItemStatus.RETURNED) {
            item.setReturnedAt(LocalDateTime.now());
        }

        auditService.log(
                "ITEM",
                item.getId(),
                "STATE_TRANSITION",
                actorId,
                String.format("Transitioned from %s to %s. Reason: %s", current, targetStatus, reason)
        );
    }

    private void validateLostItemTransition(ItemStatus current, ItemStatus target, Long itemId) {
        boolean valid = switch (current) {
            case OPEN -> target == ItemStatus.MATCHED || target == ItemStatus.CLOSED || target == ItemStatus.CANCELLED;
            case MATCHED -> target == ItemStatus.OPEN || target == ItemStatus.CLOSED || target == ItemStatus.CANCELLED;
            case CLOSED, CANCELLED -> false;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateTransitionException(
                    String.format("Illegal state transition for LOST item [%d]: cannot transition from %s to %s", itemId, current, target)
            );
        }
    }

    private void validateFoundItemTransition(ItemStatus current, ItemStatus target, Long itemId) {
        boolean valid = switch (current) {
            case LISTED -> target == ItemStatus.CLAIM_PENDING || target == ItemStatus.EXPIRED || target == ItemStatus.CANCELLED;
            case CLAIM_PENDING -> target == ItemStatus.RETURNED || target == ItemStatus.LISTED;
            case EXPIRED -> target == ItemStatus.DONATED || target == ItemStatus.DISPOSED;
            case RETURNED, DONATED, DISPOSED, CANCELLED -> false;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateTransitionException(
                    String.format("Illegal state transition for FOUND item [%d]: cannot transition from %s to %s", itemId, current, target)
            );
        }
    }
}
