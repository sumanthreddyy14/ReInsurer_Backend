package com.cts.backend.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceSummaryDTO {
    private Double cededPremiums;
    private Double recoveries;
    private Double outstandingBalance;
}