package com.cts.backend.treaty.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "reinsurer")
@Data
public class Reinsurer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // External string ID used by Angular (e.g., "R001")
    @Column(name = "reinsurer_id", unique = true, nullable = false)
    private String reinsurerId;

    @Column(nullable = false)
    private String name;

    private String contactInfo;
}

