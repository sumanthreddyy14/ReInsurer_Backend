package com.cts.backend.analytics.dto;


import lombok.Data;

@Data
public class ComplianceIssueDTO {
    private String id;
    private String entityType; // TREATY | CESSION | RECOVERY | FINANCIAL_REPORT
    private String entityId;
    private String message;
    private String severity; // LOW | MEDIUM | HIGH
    private String detectedAt; // ISO
    private boolean resolved;
}
