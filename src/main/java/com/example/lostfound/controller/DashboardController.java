package com.example.lostfound.controller;

import com.example.lostfound.model.ItemPost;
import com.example.lostfound.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** Summary stats for dashboard cards */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    /** Chart data for items overview */
    @GetMapping("/items-overview")
    public ResponseEntity<List<Map<String, Object>>> itemsOverview(
            @RequestParam(defaultValue = "weekly") String period) {
        return ResponseEntity.ok(dashboardService.getChartData(period));
    }

    /** Recent items (photo bytes stripped) */
    @GetMapping("/recent")
    public ResponseEntity<List<ItemPost>> recentItems() {
        return ResponseEntity.ok(dashboardService.getRecentItems(10));
    }
}
