package com.cts.backend.finance.dto;

import lombok.Data;

@Data
public class UpdateRecoveryRequest {
    private Double recoveryAmount; // nullable = no change
    private String recoveryDate;   // "yyyy-MM-dd" or ISO; nullable = no change
    private String status;         // PENDING | COMPLETED | DISPUTED; nullable = no change
}
