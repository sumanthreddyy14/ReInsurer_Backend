package com.cts.backend.finance.controller;

import com.cts.backend.finance.dto.BalanceRowDTO;
import com.cts.backend.finance.dto.FinanceSummaryDTO;
import com.cts.backend.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class FinanceController {

    private final FinanceService financeService;

    /**
     * PAGE 1: Top Cards
     * GET http://localhost:8080/api/v1/finance/summary
     */
    @GetMapping("/summary")
    public FinanceSummaryDTO getSummary() {
        return financeService.getCumulativeSummary();
    }

    /**
     * PAGE 1: Balance Table
     * GET http://localhost:8080/api/v1/finance/balances
     */
    @GetMapping("/balances")
    public List<BalanceRowDTO> getBalances() {
        // Returns the list of all individual treaties for the table
        return financeService.getAllTreatyBalances();
    }

    /**
     * PAGE 2: Report Search & Download
     * This endpoint handles both Treaty ID and Reinsurer ID via Query Params
     * GET http://localhost:8080/api/v1/finance/report/export?tId=T001
     */
    @GetMapping("/report/export")
    public ResponseEntity<?> exportReport(
            @RequestParam(required = false) String tId,
            @RequestParam(required = false) String rId) {

        try {
            // 1. Get the data (This validates if ID exists)
            List<BalanceRowDTO> data = financeService.getDataForReport(tId, rId);

            // 2. Generate CSV String
            String csvContent = financeService.generateCSV(data);
            byte[] out = csvContent.getBytes();

            // 3. Prepare headers for browser download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            String filename = (tId != null ? tId : rId) + "_Report.csv";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(out, headers, HttpStatus.OK);

        } catch (Exception e) {
            // Returns "Invalid input: Treaty ID T001 not found" with 404 Status
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/report") // This is the endpoint the UI is looking for
    public ResponseEntity<?> getReportData(
            @RequestParam(required = false) String tId,
            @RequestParam(required = false) String rId) {
        try {
            // Reuse your logic to get the List of BalanceRowDTO
            List<BalanceRowDTO> data = financeService.getDataForReport(tId, rId);

            // Return as JSON for the Angular table
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}