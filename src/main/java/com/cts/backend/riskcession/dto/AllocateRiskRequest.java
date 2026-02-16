package com.cts.backend.riskcession.dto;


import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter
public class AllocateRiskRequest {

    @NotNull
    private String treatyId;
    
    @NotNull
    private String policyId;

    @Min(0)
    @Max(100)
    private double cededPercentage;

    private Double commissionRate;
    private String createdBy;
}
