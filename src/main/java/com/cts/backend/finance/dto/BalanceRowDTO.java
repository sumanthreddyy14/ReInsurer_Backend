package com.cts.backend.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BalanceRowDTO {
    private String key;                // treatyId or reinsurerId
    private String label;
    private Double cededPremiums;
    private Double recoveries;
    private Double outstandingBalance;
    private List<String> treaties;
}