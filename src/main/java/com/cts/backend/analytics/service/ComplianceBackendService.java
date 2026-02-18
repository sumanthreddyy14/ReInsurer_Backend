package com.cts.backend.analytics.service;


import com.cts.backend.analytics.dto.ComplianceIssueDTO;
import com.cts.backend.analytics.dto.ComplianceRulesDTO;

import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.TreatyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComplianceBackendService {

    private final TreatyRepository treatyRepo;
    private final RiskCessionRepository cessionRepo;
    private final RecoveryRepository recoveryRepo;

    private Instant parseFrom(String from) {
        if (from == null || from.isBlank()) return null;
        return LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
    private Instant parseTo(String to) {
        if (to == null || to.isBlank()) return null;
        return LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }
    private String nowIso() { return ZonedDateTime.now(ZoneOffset.UTC).toString(); }
    private double num(Double v) { return v == null ? 0.0 : v; }
    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    public List<ComplianceIssueDTO> listIssues(String from, String to, ComplianceRulesDTO rules) {
        int maxPendingDays = rules.getMaxPendingDays() == null ? 90 : rules.getMaxPendingDays();
        double minUtil = rules.getMinUtilization() == null ? 0.25 : rules.getMinUtilization();
        double maxOutstanding = rules.getMaxOutstandingPerTreaty() == null ? 0.0 : rules.getMaxOutstandingPerTreaty();
        Double maxLossRatio = rules.getMaxLossRatio(); // nullable

        Instant f = parseFrom(from);
        Instant t = parseTo(to);

        List<ComplianceIssueDTO> issues = new ArrayList<>();
        String now = nowIso();

        // Precompute balances per treaty
        Map<String, Double> cededByTreaty = new HashMap<>();
        Map<String, Double> recovByTreaty = new HashMap<>();
        for (Treaty tty : treatyRepo.findAll()) {
            String tid = tty.getTreatyId();
            double ceded = num(cessionRepo.sumPremiumFiltered(tid, f, t));
            double recov = num(recoveryRepo.sumCompletedFiltered(tid, f, t));
            cededByTreaty.put(tid, ceded);
            recovByTreaty.put(tid, recov);
        }

        // Rule 1: Active treaty with zero cessions -> MEDIUM
        for (Treaty tty : treatyRepo.findAll()) {
            String tid = tty.getTreatyId();
            double ceded = cededByTreaty.getOrDefault(tid, 0.0);
            if (tty.getStatus() == Treaty.TreatyStatus.ACTIVE && ceded == 0.0) {
                issues.add(issue("CMP-T-NOCESSION-" + tid, "TREATY", tid,
                        "Active treaty has no risk cessions recorded.", "MEDIUM", now));
            }
        }

        // Rule 2: Pending recovery older than threshold -> HIGH
        long maxPendingMs = (long) maxPendingDays * 24 * 60 * 60 * 1000L;
        recoveryRepo.findByStatus(com.cts.backend.recovery.entity.Recovery.RecoveryStatus.PENDING)
                .forEach(r -> {
                    Instant dateForAge = r.getRecoveryDate() != null ? r.getRecoveryDate()
                            : (r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now());
                    long age = System.currentTimeMillis() - dateForAge.toEpochMilli();
                    if (age > maxPendingMs) {
                        issues.add(issue("CMP-R-OLDPENDING-" + r.getRecoveryId(), "RECOVERY", r.getRecoveryId(),
                                "Pending recovery exceeds " + maxPendingDays + " days.", "HIGH", now));
                    }
                });

        // Rule 3: Treaty utilization below threshold -> MEDIUM
        for (Treaty tty : treatyRepo.findAll()) {
            if (tty.getStatus() != Treaty.TreatyStatus.ACTIVE) continue;
            String tid = tty.getTreatyId();
            double ceded = cededByTreaty.getOrDefault(tid, 0.0);
            double limit = tty.getCoverageLimit() == null ? 0.0 : tty.getCoverageLimit();
            double util = limit > 0 ? ceded / limit : 0.0;
            if (util < minUtil) {
                issues.add(issue("CMP-T-LOWUTIL-" + tid, "TREATY", tid,
                        String.format("Treaty utilization below threshold (%.2f%% < %.2f%%).", util * 100.0, minUtil * 100.0),
                        "MEDIUM", now));
            }
        }

        // Rule 4: Outstanding balance per treaty above threshold -> HIGH
        for (Treaty tty : treatyRepo.findAll()) {
            String tid = tty.getTreatyId();
            double ceded = cededByTreaty.getOrDefault(tid, 0.0);
            double recov = recovByTreaty.getOrDefault(tid, 0.0);
            double outstanding = round(ceded - recov);
            if (outstanding > maxOutstanding) {
                issues.add(issue("CMP-T-OUTSTANDING-" + tid, "FINANCIAL_REPORT", tid,
                        String.format("Outstanding balance (%.2f) exceeds threshold (%.2f).", outstanding, maxOutstanding),
                        "HIGH", now));
            }
        }

        // Rule 5: Loss ratio above threshold -> MEDIUM/HIGH (optional)
        if (maxLossRatio != null) {
            for (Treaty tty : treatyRepo.findAll()) {
                String tid = tty.getTreatyId();
                double ceded = cededByTreaty.getOrDefault(tid, 0.0);
                double recov = recovByTreaty.getOrDefault(tid, 0.0);
                double lr = ceded > 0 ? recov / ceded : 0.0;
                if (lr > maxLossRatio) {
                    issues.add(issue("CMP-T-LOSSRATIO-" + tid, "TREATY", tid,
                            String.format("Loss ratio (%.2f%%) exceeds threshold (%.2f%%).", lr * 100.0, maxLossRatio * 100.0),
                            (lr > maxLossRatio * 1.2) ? "HIGH" : "MEDIUM", now));
                }
            }
        }

        // Rule 6: Financial report with zero totals -> LOW (computed over full system)
        // Here treated as: if ceded==0 and recovered==0 for a treaty within period
        for (Treaty tty : treatyRepo.findAll()) {
            String tid = tty.getTreatyId();
            if (cededByTreaty.getOrDefault(tid, 0.0) == 0.0 && recovByTreaty.getOrDefault(tid, 0.0) == 0.0) {
                issues.add(issue("CMP-FR-ZERO-" + tid, "FINANCIAL_REPORT", tid,
                        "Financial report has zero premiums and recoveries.", "LOW", now));
            }
        }

        return issues;
    }

    private ComplianceIssueDTO issue(String id, String type, String entityId, String msg, String severity, String now) {
        ComplianceIssueDTO i = new ComplianceIssueDTO();
        i.setId(id);
        i.setEntityType(type);
        i.setEntityId(entityId);
        i.setMessage(msg);
        i.setSeverity(severity);
        i.setDetectedAt(now);
        i.setResolved(false);
        return i;
    }
}

