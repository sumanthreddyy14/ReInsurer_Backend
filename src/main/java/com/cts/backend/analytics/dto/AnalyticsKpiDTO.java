package com.cts.backend.analytics.dto;

import lombok.Data;

@Data
public class AnalyticsKpiDTO {
    private int activeTreaties;
    private int expiredTreaties;
    private double totalCededPremiums;
    private double totalRecoveries;
    private double outstandingRecoveries;
    private double averageLossRatio;
    private double averageCededPercentage;
    private String generatedAt;
    private String source;
}
