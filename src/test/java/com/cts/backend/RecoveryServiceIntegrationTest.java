package com.cts.backend;

import com.cts.backend.recovery.dto.CreateRecoveryRequest;
import com.cts.backend.recovery.dto.RecoveryUiDTO;
import com.cts.backend.recovery.dto.UpdateStatusRequest;
import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.recovery.service.RecoveryService;
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
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RecoveryServiceIntegrationTest {

    @Autowired
    private RecoveryService recoveryService;

    @Autowired
    private RecoveryRepository recoveryRepo;

    @Autowired
    private TreatyRepository treatyRepo;

    @Autowired
    private ReinsurerRepository reinsurerRepo;

    @Autowired
    private PolicyRepository policyRepo;

    @Autowired
    private RiskCessionRepository cessionRepo;

    private final String validTreatyId = "T-100";
    private final String validPolicyId = "POL-999";

    @BeforeEach
    void setup() {
        // 1. Create and Save Reinsurer
        Reinsurer reinsurer = new Reinsurer();
        reinsurer.setReinsurerId("RE-01");
        reinsurer.setName("Global Re");
        reinsurer.setContactInfo("contact@globalre.com");
        reinsurerRepo.save(reinsurer);

        // 2. Create and Save Treaty with ALL mandatory fields to satisfy MySQL constraints
        Treaty treaty = new Treaty();
        treaty.setTreatyId(validTreatyId);
        treaty.setReinsurer(reinsurer);
        treaty.setStartDate(LocalDate.now());
        treaty.setEndDate(LocalDate.now().plusYears(1));
        treaty.setCoverageLimit(1000000.0);
        // Using Enums based on your earlier TreatyService code
        treaty.setStatus(Treaty.TreatyStatus.ACTIVE);
        treaty.setTreatyType(Treaty.TreatyType.PROPORTIONAL);
        treatyRepo.save(treaty);

        // 3. Create and Save Policy
        Policy policy = new Policy();
        policy.setPolicyId(validPolicyId);
        policy.setPremium(5000.0);
        // Ensure other @Column(nullable = false) fields are set here if they exist
        policyRepo.save(policy);
    }

    @Test
    @DisplayName("1. Create Recovery - Success")
    void create_Success() {
        CreateRecoveryRequest req = new CreateRecoveryRequest();
        req.setTreatyId(validTreatyId);
        req.setPolicyId(validPolicyId);
        req.setClaimId("CLM-001");
        req.setRecoveryAmount(1200.505);
        req.setRecoveryDate("2024-05-20");
        req.setCreatedBy("Tester");

        RecoveryUiDTO result = recoveryService.create(req);

        assertNotNull(result.getRecoveryId());
        assertEquals(1200.51, result.getRecoveryAmount()); // Verifies rounding
        assertEquals("PENDING", result.getStatus());
        assertTrue(recoveryRepo.findByRecoveryId(result.getRecoveryId()).isPresent());
    }

    @Test
    @DisplayName("2. Create Recovery - Treaty Not Found (400)")
    void create_TreatyNotFound_ThrowsException() {
        CreateRecoveryRequest req = new CreateRecoveryRequest();
        req.setTreatyId("INVALID-TREATY-ID");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recoveryService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("3. Update Status - Success")
    void updateStatus_Success() {
        // Create initial record
        CreateRecoveryRequest req = new CreateRecoveryRequest();
        req.setTreatyId(validTreatyId);
        req.setRecoveryAmount(500.0);
        req.setRecoveryDate("2024-01-01");
        RecoveryUiDTO created = recoveryService.create(req);

        // Update status
        UpdateStatusRequest statusReq = new UpdateStatusRequest();
        statusReq.setStatus("COMPLETED");
        RecoveryUiDTO updated = recoveryService.updateStatus(created.getRecoveryId(), statusReq);

        assertEquals("COMPLETED", updated.getStatus());
    }

    @Test
    @DisplayName("4. Generate from Cessions - Success and Idempotency")
    void generateFromAllCessions_Success() {
        // Manually create a Risk Cession linked to our setup data
        RiskCession cession = RiskCession.builder()
                .cessionId("CESS-888")
                .treatyId(validTreatyId)
                .policyId(validPolicyId)
                .cededPremium(250.75)
                .createdAt(Instant.now())
                .build();
        cessionRepo.save(cession);

        // Act
        List<RecoveryUiDTO> generated = recoveryService.generateFromAllCessions();

        // Assert
        assertFalse(generated.isEmpty(), "Should have generated at least one recovery");
        assertEquals("REC-CESS-888", generated.get(0).getRecoveryId());

        // Test Idempotency: Running again should return an empty list
        List<RecoveryUiDTO> secondRun = recoveryService.generateFromAllCessions();
        assertTrue(secondRun.isEmpty(), "Second run should not create duplicate recoveries");
    }

    @Test
    @DisplayName("5. List Recoveries - Filter by Treaty")
    void list_FilteredByTreaty() {
        // Create data
        CreateRecoveryRequest req = new CreateRecoveryRequest();
        req.setTreatyId(validTreatyId);
        req.setRecoveryAmount(100.0);
        req.setRecoveryDate("2024-01-01");
        recoveryService.create(req);

        // Act
        List<RecoveryUiDTO> list = recoveryService.list(validTreatyId, "PENDING");

        // Assert
        assertFalse(list.isEmpty());
        assertEquals(validTreatyId, list.get(0).getTreatyId());
    }

    @Test
    @DisplayName("6. Dispute Recovery - Success")
    void flagDispute_Success() {
        CreateRecoveryRequest req = new CreateRecoveryRequest();
        req.setTreatyId(validTreatyId);
        req.setRecoveryAmount(100.0);
        req.setRecoveryDate("2024-01-01");
        RecoveryUiDTO created = recoveryService.create(req);

        RecoveryUiDTO disputed = recoveryService.flagDispute(created.getRecoveryId());

        assertEquals("DISPUTED", disputed.getStatus());
    }
}