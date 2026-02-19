package com.cts.backend;

import com.cts.backend.finance.dto.BalanceRowDTO;
import com.cts.backend.finance.dto.FinanceSummaryDTO;
import com.cts.backend.finance.service.FinanceService;
import com.cts.backend.recovery.entity.Recovery;
import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.riskcession.entity.Policy;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.repository.PolicyRepository;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.treaty.entity.Reinsurer;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.ReinsurerRepository;
import com.cts.backend.treaty.repositories.TreatyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FinanceServiceIntegrationTest {

    @Autowired private FinanceService financeService;
    @Autowired private RiskCessionRepository cessionRepo;
    @Autowired private RecoveryRepository recoveryRepo;
    @Autowired private TreatyRepository treatyRepo;
    @Autowired private ReinsurerRepository reinsurerRepo;
    @Autowired private PolicyRepository policyRepo;

    private final String T1 = "TREATY-01";
    private final String R1 = "RE-01";

    @BeforeEach
    void setup() {
        // 1. Setup Reinsurer (adding contact info just in case it's mandatory)
        Reinsurer reinsurer = new Reinsurer();
        reinsurer.setReinsurerId(R1);
        reinsurer.setName("Swiss Re");
        reinsurer.setContactInfo("test@re.com");
        reinsurerRepo.save(reinsurer);

        // 2. Setup Treaty (adding type and dates just in case)
        Treaty treaty = new Treaty();
        treaty.setTreatyId(T1);
        treaty.setReinsurer(reinsurer);
        treaty.setCoverageLimit(1000000.0);
        treaty.setStatus(Treaty.TreatyStatus.ACTIVE);
        treaty.setTreatyType(Treaty.TreatyType.NON_PROPORTIONAL);
        treaty.setStartDate(LocalDate.now());
        treaty.setEndDate(LocalDate.now().plusYears(1));
        treatyRepo.save(treaty);

        // 3. Setup Policy (Ensure column name matches your entity: id vs policyId)
        Policy policy = new Policy();
        policy.setPolicyId("POL-001");
        policy.setPremium(5000.0);
        policyRepo.save(policy);

        // 4. Create Cessions
        // Note: Using builder, ensure @Builder is on your RiskCession entity
        cessionRepo.save(RiskCession.builder()
                .cessionId("C1").treatyId(T1).policyId("POL-001")
                .cededPremium(1000.0).createdAt(Instant.now()).build());
        cessionRepo.save(RiskCession.builder()
                .cessionId("C2").treatyId(T1).policyId("POL-001")
                .cededPremium(500.0).createdAt(Instant.now()).build());

        // 5. Create Completed Recovery
        Recovery rec = new Recovery();
        rec.setRecoveryId("REC-01");
        rec.setTreatyId(T1);
        rec.setRecoveryAmount(300.0);
        rec.setStatus(Recovery.RecoveryStatus.COMPLETED);
        rec.setRecoveryDate(Instant.now());
        recoveryRepo.save(rec);
    }

    @Test
    @DisplayName("Finance: Should calculate summary for specifically created test data")
    void getCumulativeSummary_Success() {
        // Act: Only look at the data for T1 (TREATY-01) created in setup()
        // We use a date range that covers 'today'
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        FinanceSummaryDTO summary = financeService.getCumulativeSummaryFiltered(today, tomorrow);

        // Assert: Now it should ignore the other 16,030.0 already in your DB
        assertEquals(1500.0, summary.getCededPremiums(), 0.01);
        assertEquals(300.0, summary.getRecoveries(), 0.01);
        assertEquals(1200.0, summary.getOutstandingBalance(), 0.01);
    }


    @Test
    @DisplayName("Finance: Should group balances by Reinsurer")
    void getAllTreatyBalances_GroupingCheck() {
        List<BalanceRowDTO> balances = financeService.getAllTreatyBalances();

        assertFalse(balances.isEmpty(), "Balance list should not be empty");
        BalanceRowDTO row = balances.stream()
                .filter(b -> b.getKey().equals(R1))
                .findFirst()
                .orElseThrow();

        assertEquals(1500.0, row.getCededPremiums());
        assertTrue(row.getTreaties().contains(T1));
    }

    @Test
    @DisplayName("Finance: Should filter data by date range")
    void getCumulativeSummaryFiltered_DateRange() {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        FinanceSummaryDTO summary = financeService.getCumulativeSummaryFiltered(today, tomorrow);
        assertEquals(1500.0, summary.getCededPremiums());

        FinanceSummaryDTO emptySummary = financeService.getCumulativeSummaryFiltered("2000-01-01", "2000-01-02");
        assertEquals(0.0, emptySummary.getCededPremiums());
    }

    @Test
    @DisplayName("Finance: Should fetch data for a specific treaty report")
    void getDataForReport_SpecificTreaty() throws Exception {
        List<BalanceRowDTO> reportData = financeService.getDataForReport(T1, null);

        assertEquals(1, reportData.size());
        assertEquals(T1, reportData.get(0).getKey());
    }
}