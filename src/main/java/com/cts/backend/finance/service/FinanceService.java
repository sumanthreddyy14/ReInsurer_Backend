package com.cts.backend.finance.service;

import com.cts.backend.finance.dto.BalanceRowDTO;
import com.cts.backend.finance.dto.FinanceReportDTO;
import com.cts.backend.finance.dto.FinanceSummaryDTO;

import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.ReinsurerRepository;
import com.cts.backend.treaty.repositories.TreatyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final RiskCessionRepository cessionRepo;
    private final RecoveryRepository recoveryRepo;
    private final TreatyRepository treatyRepo;
    private final ReinsurerRepository reinsurerRepo;

    private Double clean(Double value) {
        return value == null ? 0.0 : Math.round(value * 100.0) / 100.0;
    }

    private Instant parseFrom(String from) {
        if (from == null || from.isBlank()) return null;
        return LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
    private Instant parseTo(String to) {
        if (to == null || to.isBlank()) return null;
        // inclusive end-of-day
        return LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }

    // 1. PAGE 1: Cumulative Summary (Top Cards)
    public FinanceSummaryDTO getCumulativeSummary() {
        Double p = clean(cessionRepo.sumAllPremium());
        Double r = clean(recoveryRepo.sumAllCompleted());
        return new FinanceSummaryDTO(p, r, clean(p - r));
    }

    // 2. PAGE 1: Balance Table (All Treaties)
    public List<BalanceRowDTO> getAllTreatyBalances() {
        // 1. Fetch all treaties from the DB
        List<Treaty> allTreaties = treatyRepo.findAll();

        // 2. Group treaties by Reinsurer ID
        return allTreaties.stream()
                .filter(t -> t.getReinsurer() != null) // Safety check
                .collect(Collectors.groupingBy(t -> t.getReinsurer().getReinsurerId()))
                .entrySet().stream()
                .map(entry -> {
                    String reinsurerId = entry.getKey();
                    List<Treaty> group = entry.getValue();

                    // 3. Calculate cumulative totals for this Reinsurer
                    double totalPremium = 0.0;
                    double totalRecoveries = 0.0;
                    List<String> treatyIds = new ArrayList<>();

                    for (Treaty t : group) {
                        totalPremium += clean(cessionRepo.sumPremiumByTreatyId(t.getTreatyId()));
                        totalRecoveries += clean(recoveryRepo.sumCompletedByTreatyId(t.getTreatyId()));
                        treatyIds.add(t.getTreatyId()); // Collect IDs for Column 5
                    }

                    // 4. Map to DTO with grouped data
                    return BalanceRowDTO.builder()
                            .key(reinsurerId)
                            .label(reinsurerId) // Column 1: Reinsurer ID
                            .cededPremiums(clean(totalPremium)) // Column 2
                            .recoveries(clean(totalRecoveries)) // Column 3
                            .outstandingBalance(clean(totalPremium - totalRecoveries)) // Column 4
                            .treaties(treatyIds) // Column 5: List of Treaty IDs
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 3. PAGE 2: Search and Validate (For Treaty or Reinsurer)
    public List<BalanceRowDTO> getDataForReport(String tId, String rId) throws Exception {
        List<BalanceRowDTO> results = new ArrayList<>();

        if (tId != null && !tId.isEmpty()) {
            // Check if Treaty exists
            if (!treatyRepo.existsByTreatyId(tId)) {
                throw new Exception("Invalid input: Treaty ID " + tId + " not found.");
            }
            results.add(getSingleTreatyRow(tId));

        } else if (rId != null && !rId.isEmpty()) {
            // Check if Reinsurer exists
            if (!reinsurerRepo.existsByReinsurerId(rId)) {
                throw new Exception("Invalid input: Reinsurer ID " + rId + " not found.");
            }
            // Get all treaties for this Reinsurer
            List<Treaty> ts = treatyRepo.findAll().stream()
                    .filter(t -> t.getReinsurer().getReinsurerId().equals(rId))
                    .toList();

            for (Treaty t : ts) {
                results.add(getSingleTreatyRow(t.getTreatyId()));
            }
        }
        return results;
    }

    // Helper for Page 2 to avoid repeating code
    private BalanceRowDTO getSingleTreatyRow(String treatyId) {
        Double p = clean(cessionRepo.sumPremiumByTreatyId(treatyId));
        Double r = clean(recoveryRepo.sumCompletedByTreatyId(treatyId));
        return BalanceRowDTO.builder()
                .key(treatyId)
                .label("Treaty " + treatyId)
                .cededPremiums(p)
                .recoveries(r)
                .outstandingBalance(clean(p - r))
                .build();
    }

    // CSV Generator
    public String generateCSV(List<BalanceRowDTO> rows) {
        // 1. Add a Report Header for a "Classic" look
        StringBuilder sb = new StringBuilder();
        sb.append("FINANCIAL RECOVERY REPORT\n");
        sb.append("Report Date,").append(java.time.LocalDate.now()).append("\n");
        sb.append("Currency,USD\n\n"); // Assuming USD, or change as needed

        // 2. Column Headers
        sb.append("Treaty ID,Reinsurer,Type,Status,Coverage Limit,Ceded Premium,Recoveries,Outstanding Balance\n");
        Double totalP = 0.0;
        Double totalR = 0.0;
        Double totalB = 0.0;

        for (BalanceRowDTO row : rows) {
            Optional<Treaty> treatyOpt = treatyRepo.findByTreatyId(row.getKey());
            String reinsurerName = "N/A";
            String type = "N/A";
            String status = "N/A";
            Double limit = 0.0;

            if (treatyOpt.isPresent()) {
                Treaty t = treatyOpt.get();
                reinsurerName = t.getReinsurer() != null ? t.getReinsurer().getName() : "N/A";
                type = t.getTreatyType() != null ? t.getTreatyType().toString() : "N/A";
                status = t.getStatus() != null ? t.getStatus().toString() : "N/A";
                limit = clean(t.getCoverageLimit());
            }

            // Add to totals for the footer
            totalP += row.getCededPremiums();
            totalR += row.getRecoveries();
            totalB += row.getOutstandingBalance();

            sb.append(row.getKey()).append(",")
                    .append("\"").append(reinsurerName).append("\",") // Quotes handle names with commas
                    .append(type).append(",")
                    .append(status).append(",")
                    .append(limit).append(",")
                    .append(row.getCededPremiums()).append(",")
                    .append(row.getRecoveries()).append(",")
                    .append(row.getOutstandingBalance()).append("\n");
        }

        // 3. Add a Footer Row for Totals (Very helpful for Finance Reports)
        sb.append("\n");
        sb.append("TOTALS,,,,,").append(clean(totalP)).append(",")
                .append(clean(totalR)).append(",").append(clean(totalB)).append("\n");

        return sb.toString();
    }

    @GetMapping("/report") // This is the endpoint the UI is looking for
    public ResponseEntity<?> getReportData(
            @RequestParam(required = false) String tId,
            @RequestParam(required = false) String rId) {
        try {
            // Reuse your logic to get the List of BalanceRowDTO
            List<BalanceRowDTO> data = getDataForReport(tId, rId);
            // Return as JSON for the Angular table
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ---------- Filtered Summary ----------
    public FinanceSummaryDTO getCumulativeSummaryFiltered(String from, String to) {
        Instant f = parseFrom(from);
        Instant t = parseTo(to);

        Double p = clean(cessionRepo.sumPremiumFiltered(null, f, t));
        Double r = clean(recoveryRepo.sumCompletedFiltered(null, f, t));
        return new FinanceSummaryDTO(p, r, clean(p - r));
    }

    // ---------- Filtered Balance Table ----------
    public List<BalanceRowDTO> getBalances(String groupBy, String from, String to) {
        Instant f = parseFrom(from);
        Instant t = parseTo(to);

        if ("reinsurer".equalsIgnoreCase(groupBy)) {
            // group across treaties per reinsurer
            Map<String, List<Treaty>> byR = treatyRepo.findAll().stream()
                    .collect(Collectors.groupingBy(t1 -> t1.getReinsurer().getReinsurerId()));
            List<BalanceRowDTO> rows = new ArrayList<>();
            for (Map.Entry<String, List<Treaty>> e : byR.entrySet()) {
                String reinsurerId = e.getKey();
                List<Treaty> ts = e.getValue();
                double p = 0.0;
                double r = 0.0;
                List<String> treaties = new ArrayList<>();
                for (Treaty tty : ts) {
                    Double ct = clean(cessionRepo.sumPremiumFiltered(tty.getTreatyId(), f, t));
                    Double rt = clean(recoveryRepo.sumCompletedFiltered(tty.getTreatyId(), f, t));
                    p += ct;
                    r += rt;
                    treaties.add(tty.getTreatyId());
                }
                rows.add(BalanceRowDTO.builder()
                        .key(reinsurerId)
                        .label(reinsurerRepo.findByReinsurerId(reinsurerId).map(rn -> rn.getName()).orElse(reinsurerId))
                        .cededPremiums(clean(p))
                        .recoveries(clean(r))
                        .outstandingBalance(clean(p - r))
                        .treaties(treaties)
                        .build());
            }
            return rows;
        }

        // default: treaty grouping
        return treatyRepo.findAll().stream().map(t2 -> {
            Double p = clean(cessionRepo.sumPremiumFiltered(t2.getTreatyId(), f, t));
            Double r = clean(recoveryRepo.sumCompletedFiltered(t2.getTreatyId(), f, t));
            return BalanceRowDTO.builder()
                    .key(t2.getTreatyId())
                    .label(t2.getTreatyId())
                    .cededPremiums(p)
                    .recoveries(r)
                    .outstandingBalance(clean(p - r))
                    .build();
        }).collect(Collectors.toList());
    }

    // ---------- Reports (computed on the fly) ----------
    public List<FinanceReportDTO> listReports(String from, String to) {
        Instant f = parseFrom(from);
        Instant t = parseTo(to);
        // Here we return 1 computed report (you can expand to history later)
        FinanceSummaryDTO metrics = getCumulativeSummaryFiltered(from, to);
        Map<String, FinanceSummaryDTO> byTreaty = new LinkedHashMap<>();
        for (Treaty tty : treatyRepo.findAll().stream()
                .sorted(Comparator.comparing(Treaty::getTreatyId)).toList()) {

            Double p = clean(cessionRepo.sumPremiumFiltered(tty.getTreatyId(), f, t));
            Double r = clean(recoveryRepo.sumCompletedFiltered(tty.getTreatyId(), f, t));
            byTreaty.put(tty.getTreatyId(), new FinanceSummaryDTO(p, r, clean(p - r)));
        }
        FinanceReportDTO report = new FinanceReportDTO();
        report.setReportId("FR-" + System.currentTimeMillis());
        report.setGeneratedDate(java.time.ZonedDateTime.now(ZoneOffset.UTC).toString());
        report.setMetrics(metrics);
        report.setBreakdownByTreaty(byTreaty);
        return List.of(report);
    }
}