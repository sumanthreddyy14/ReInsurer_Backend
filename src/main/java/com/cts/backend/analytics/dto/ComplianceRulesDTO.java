package com.cts.backend.analytics.dto;

import lombok.Data;

@Data
public class ComplianceRulesDTO {
    private Integer maxPendingDays;         // default 90
    private Double minUtilization;          // default 0.25
    private Double maxOutstandingPerTreaty; // default 0
    private Double maxLossRatio;            // optional, e.g. 1.2 (120%)
}

