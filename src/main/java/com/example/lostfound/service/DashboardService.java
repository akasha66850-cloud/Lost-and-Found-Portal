package com.example.lostfound.service;

import com.example.lostfound.model.ItemPost;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.ItemPostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ItemPostRepository itemRepo;
    private final ClaimRepository claimRepo;

    public DashboardService(ItemPostRepository itemRepo, ClaimRepository claimRepo) {
        this.itemRepo = itemRepo;
        this.claimRepo = claimRepo;
    }

    /**
     * Returns summary statistics for the dashboard cards.
     */
    public Map<String, Long> getSummary() {
        List<String> lostExclude  = Arrays.asList("RETURNED", "REMOVED");
        List<String> foundExclude = Arrays.asList("RESOLVED", "REMOVED");

        long totalLost    = itemRepo.countByLostTrueAndStatusNotIn(lostExclude);
        long totalFound   = itemRepo.countByLostFalseAndStatusNotIn(foundExclude);
        long matched      = claimRepo.countByStatus("COMPLETED");
        long pending      = claimRepo.countByStatus("PENDING_VERIFICATION");

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("totalLost",  totalLost);
        summary.put("totalFound", totalFound);
        summary.put("matched",    matched);
        summary.put("pending",    pending);
        return summary;
    }

    /**
     * Returns chart data grouped by period.
     * @param period "daily" (last 7 days), "weekly" (last 8 weeks), "monthly" (last 6 months)
     */
    public List<Map<String, Object>> getChartData(String period) {
        List<ItemPost> allItems = itemRepo.findAll();

        LocalDate today = LocalDate.now();

        if ("monthly".equalsIgnoreCase(period)) {
            return buildMonthlyData(allItems, today, 6);
        } else if ("weekly".equalsIgnoreCase(period)) {
            return buildWeeklyData(allItems, today, 8);
        } else {
            return buildDailyData(allItems, today, 7);
        }
    }

    private List<Map<String, Object>> buildDailyData(List<ItemPost> items, LocalDate today, int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long lost  = items.stream().filter(it -> it.isLost()  && day.equals(it.getDateEvent())).count();
            long found = items.stream().filter(it -> !it.isLost() && day.equals(it.getDateEvent())).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", day.format(fmt));
            m.put("lost",  lost);
            m.put("found", found);
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> buildWeeklyData(List<ItemPost> items, LocalDate today, int weeks) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd   = weekStart.plusDays(6);
            long lost  = items.stream().filter(it -> it.isLost()  && inRange(it.getDateEvent(), weekStart, weekEnd)).count();
            long found = items.stream().filter(it -> !it.isLost() && inRange(it.getDateEvent(), weekStart, weekEnd)).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", weekStart.format(fmt) + " - " + weekEnd.format(fmt));
            m.put("lost",  lost);
            m.put("found", found);
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> buildMonthlyData(List<ItemPost> items, LocalDate today, int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd   = monthStart.with(TemporalAdjusters.lastDayOfMonth());
            long lost  = items.stream().filter(it -> it.isLost()  && inRange(it.getDateEvent(), monthStart, monthEnd)).count();
            long found = items.stream().filter(it -> !it.isLost() && inRange(it.getDateEvent(), monthStart, monthEnd)).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", monthStart.format(fmt));
            m.put("lost",  lost);
            m.put("found", found);
            result.add(m);
        }
        return result;
    }

    private boolean inRange(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null) return false;
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Returns the most recent items (photo bytes stripped for performance).
     */
    public List<ItemPost> getRecentItems(int limit) {
        List<ItemPost> items = itemRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        items.forEach(i -> i.setPhoto(null)); // strip blobs
        return items;
    }
}
