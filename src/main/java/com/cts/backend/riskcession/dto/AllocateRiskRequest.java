package com.cts.backend.riskcession.dto;


import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter
public class AllocateRiskRequest {

    private String treatyId;
    private String policyId;

    @Min(0)
    @Max(100)
    private double cededPercentage;

    private Double commissionRate;
    private String createdBy;
}
