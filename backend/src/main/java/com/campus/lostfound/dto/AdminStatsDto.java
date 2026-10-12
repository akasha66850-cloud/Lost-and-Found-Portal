package com.campus.lostfound.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {
    private Map<String, Long> statusCounts;
    private long totalLost;
    private long totalFound;
    private long totalReturned;
    private double recoveryRate;
    private List<LocationCountDto> topLostLocations;
    private double averageDaysToReturn;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationCountDto {
        private String location;
        private long count;
    }
}
