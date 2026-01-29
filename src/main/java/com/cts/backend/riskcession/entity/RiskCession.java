package com.cts.backend.riskcession.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskCession {

    @Id
    private String cessionId;

    private String treatyId;
    private String policyId;

    private double cededPercentage;
    private double cededPremium;
    private Double commission;

    private Instant createdAt;
    private String createdBy;
}