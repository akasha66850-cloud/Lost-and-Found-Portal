package com.campus.lostfound.scheduler;

import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.ItemStatus;
import com.campus.lostfound.entity.ItemType;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.service.AuditService;
import com.campus.lostfound.service.NotificationService;
import com.campus.lostfound.service.StateTransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final ItemRepository itemRepository;
    private final StateTransitionService stateTransitionService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${campus.retention.days:90}")
    private int retentionDays;

    public RetentionScheduler(ItemRepository itemRepository,
                              StateTransitionService stateTransitionService,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.itemRepository = itemRepository;
        this.stateTransitionService = stateTransitionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${campus.retention.cron:0 0 2 * * *}")
    @Transactional
    public void runRetentionPolicy() {
        log.info("Running daily retention policy check (retention period: {} days)...", retentionDays);
        expireOldItems();
        sendExpiryWarnings();
    }

    public void expireOldItems() {
        LocalDate expiryCutoff = LocalDate.now().minusDays(retentionDays);
        List<Item> expiredCandidates = itemRepository.findByTypeAndStatusAndEventDateBefore(
                ItemType.FOUND, ItemStatus.LISTED, expiryCutoff
        );

        for (Item item : expiredCandidates) {
            log.info("Marking item #{} ({}) as EXPIRED", item.getId(), item.getTitle());
            stateTransitionService.transition(
                    item,
                    ItemStatus.EXPIRED,
                    String.format("Retention period expired (> %d days since %s)", retentionDays, item.getEventDate()),
                    null
            );
            itemRepository.save(item);
            auditService.log("ITEM", item.getId(), "RETENTION_EXPIRED", null,
                    String.format("Automatically marked as EXPIRED after %d days", retentionDays));
        }
    }

    public void sendExpiryWarnings() {
        // Items within 7 days of expiration (i.e. eventDate older than retentionDays - 7)
        LocalDate warningCutoff = LocalDate.now().minusDays(Math.max(1, retentionDays - 7));
        List<Item> warningCandidates = itemRepository.findByTypeAndStatusAndExpiryWarnedFalseAndEventDateBefore(
                ItemType.FOUND, ItemStatus.LISTED, warningCutoff
        );

        for (Item item : warningCandidates) {
            item.setExpiryWarned(true);
            itemRepository.save(item);

            String msg = String.format("Found item #%d [%s] held at %s will expire soon (retention limit %d days).",
                    item.getId(), item.getTitle(), item.getStoredAt() != null ? item.getStoredAt() : "Desk", retentionDays);
            notificationService.notifyStaff(msg, "EXPIRY_WARNING", item.getId());
            log.info("Dispatched expiry warning for item #{}", item.getId());
        }
    }
}
