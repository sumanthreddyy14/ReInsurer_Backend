package com.cts.backend.finance.controller;

import com.cts.backend.finance.dto.BalanceRowDTO;
import com.cts.backend.finance.dto.FinanceSummaryDTO;
import com.cts.backend.finance.service.FinanceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance")
@CrossOrigin(origins = "http://localhost:4200")
public class FinanceController {

    private final FinanceService financeService;
    //HOME PAGE: Top Cards
    @GetMapping("/summary")
    public FinanceSummaryDTO getSummary() {
        return financeService.getCumulativeSummary();
    }

    //HOME PAGE: Balance Table
    @GetMapping("/balances")
    public List<BalanceRowDTO> getBalances() {
        return financeService.getAllTreatyBalances();
    }

    //REPORT PAGE: Generate Report & Download
    @GetMapping("/report/export")
    public ResponseEntity<?> exportReport(
            @RequestParam(required = false) String tId,
            @RequestParam(required = false) String rId) {
        try {
            // 1. Get the data
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReportData(
            @RequestParam(required = false) String tId,
            @RequestParam(required = false) String rId) {
        try {
            // Reuse your logic to get the List of BalanceRowDTO
            List<BalanceRowDTO> data = financeService.getDataForReport(tId, rId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}