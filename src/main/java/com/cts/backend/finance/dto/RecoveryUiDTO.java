package com.cts.backend.finance.dto;
import lombok.Data;

@Data
public class RecoveryUiDTO {
    private String recoveryId;
    private String claimId;
    private String treatyId;
    private Double recoveryAmount;
    private String recoveryDate; // "yyyy-MM-dd" or ISO-8601
    private String status;       // PENDING | COMPLETED | DISPUTED
}