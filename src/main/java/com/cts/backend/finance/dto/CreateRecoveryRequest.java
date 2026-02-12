package com.cts.backend.finance.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class CreateRecoveryRequest {
    @NotBlank
    private String claimId;

    @NotBlank
    private String treatyId;

    private String policyId; // optional

    @NotNull
    @PositiveOrZero
    private Double recoveryAmount;

    @NotBlank // Expecting "yyyy-MM-dd" from Angular <input type="date">
    private String recoveryDate;

    private String status;     // optional; default PENDING
    private String createdBy;  // optional
}