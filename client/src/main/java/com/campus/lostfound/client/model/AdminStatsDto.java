package com.campus.lostfound.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminStatsDto {
    private Map<String, Long> statusCounts;
    private long totalLost;
    private long totalFound;
    private long totalReturned;
    private double recoveryRate;
    private List<LocationCountDto> topLostLocations;
    private double averageDaysToReturn;

    public AdminStatsDto() {}

    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }

    public long getTotalLost() { return totalLost; }
    public void setTotalLost(long totalLost) { this.totalLost = totalLost; }

    public long getTotalFound() { return totalFound; }
    public void setTotalFound(long totalFound) { this.totalFound = totalFound; }

    public long getTotalReturned() { return totalReturned; }
    public void setTotalReturned(long totalReturned) { this.totalReturned = totalReturned; }

    public double getRecoveryRate() { return recoveryRate; }
    public void setRecoveryRate(double recoveryRate) { this.recoveryRate = recoveryRate; }

    public List<LocationCountDto> getTopLostLocations() { return topLostLocations; }
    public void setTopLostLocations(List<LocationCountDto> topLostLocations) { this.topLostLocations = topLostLocations; }

    public double getAverageDaysToReturn() { return averageDaysToReturn; }
    public void setAverageDaysToReturn(double averageDaysToReturn) { this.averageDaysToReturn = averageDaysToReturn; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationCountDto {
        private String location;
        private long count;

        public LocationCountDto() {}

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}
