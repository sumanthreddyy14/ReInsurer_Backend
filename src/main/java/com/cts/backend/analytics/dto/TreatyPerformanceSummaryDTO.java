package com.cts.backend.analytics.dto;

import lombok.Data;

@Data
public class TreatyPerformanceSummaryDTO {
    private String treatyId;
    private String reinsurerName;
    private String periodFrom;
    private String periodTo;
    private double totalCededPremiums;
    private double totalRecoveries;
    private double outstandingRecoveries;
    private int allocationsCount;
    private Double averageCededPercentage;
    private double lossRatio;
    private String status;
}
