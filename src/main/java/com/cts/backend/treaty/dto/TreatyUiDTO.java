package com.cts.backend.treaty.dto;

import lombok.Data;

@Data
public class TreatyUiDTO {
    private String treatyId;         // "T001"
    private String reinsurerId;      // "R001"
    private String reinsurerName;    // "Swiss Re"
    private String treatyType;       // "PROPORTIONAL" | "NON-PROPORTIONAL"
    private Double coverageLimit;
    private String startDate;        // "YYYY-MM-DD"
    private String endDate;          // "YYYY-MM-DD"
    private String status;           // "ACTIVE" | "EXPIRED" | "ARCHIVED"
    private String renewalDate;      // optional
}
