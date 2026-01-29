package com.cts.backend.riskcession.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Policy {
    @Id
    private String policyId;
    private double premium;
}