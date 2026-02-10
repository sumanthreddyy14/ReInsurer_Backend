package com.cts.backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "recoveries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String recoveryId; // e.g., "R0001"

    private String claimId;
    private String treatyId;   // FK to Treaty
    private String policyId;

    private double recoveryAmount;
    private Instant recoveryDate;

    @Enumerated(EnumType.STRING)
    private RecoveryStatus status; // PENDING, COMPLETED, DISPUTED

    private Instant createdAt;
    private String createdBy;

    public enum RecoveryStatus {
        PENDING, COMPLETED, DISPUTED
    }
}