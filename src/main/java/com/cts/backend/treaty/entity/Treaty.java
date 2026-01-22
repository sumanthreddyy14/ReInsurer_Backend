package com.cts.backend.treaty.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "treaty")
@Data
public class Treaty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // External string ID used by Angular (e.g., "T001")
    @Column(name = "treaty_id", unique = true, nullable = false)
    private String treatyId;

    @ManyToOne
    @JoinColumn(name = "reinsurer_fk", nullable = false)
    private Reinsurer reinsurer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TreatyType treatyType; // PROPORTIONAL | NON_PROPORTIONAL (underscore in DB)

    @Column(nullable = false)
    private Double coverageLimit;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private TreatyStatus status;   // ACTIVE | EXPIRED | ARCHIVED

    private LocalDate renewalDate;

    public enum TreatyType {
        PROPORTIONAL, NON_PROPORTIONAL
    }

    public enum TreatyStatus {
        ACTIVE, EXPIRED, ARCHIVED
    }
}

