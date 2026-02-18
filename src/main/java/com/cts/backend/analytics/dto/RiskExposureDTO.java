package com.cts.backend.analytics.dto;

import lombok.Data;

@Data
public class RiskExposureDTO {
    private String treatyId;
    private String treatyType;
    private double totalCededPremiums;
    private double maxCededPercentage;
    private double averageCededPercentage;
    private int cessionCount;
    private Integer policyCount;
    private String generatedAt;
    private String source;
}
