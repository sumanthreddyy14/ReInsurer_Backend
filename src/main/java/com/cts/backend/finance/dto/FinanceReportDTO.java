package com.cts.backend.finance.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FinanceReportDTO {
    private String reportId;
    private String generatedDate;
    private FinanceSummaryDTO metrics;
    // Map of TreatyID -> Metrics
    private Map<String, FinanceSummaryDTO> breakdownByTreaty;
}