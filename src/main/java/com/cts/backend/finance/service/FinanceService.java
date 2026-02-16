package com.cts.backend.finance.service;

import com.cts.backend.finance.dto.BalanceRowDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    /**
     * 1. PAGE 1: Cumulative Summary (Top Cards)
     */
    public FinanceSummaryDTO getCumulativeSummary() {
        Double p = clean(cessionRepo.sumAllPremium());
        Double r = clean(recoveryRepo.sumAllCompleted());
        return new FinanceSummaryDTO(p, r, clean(p - r));
    }

    /**
     * 2. PAGE 1: Balance Table (All Treaties)
     */
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

    /**
     * 3. PAGE 2: Search and Validate (For Treaty or Reinsurer)
     * Returns a list of rows to be converted to CSV
     */
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

    /**
     * 3 (Continued). CSV Generator
     */
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
}