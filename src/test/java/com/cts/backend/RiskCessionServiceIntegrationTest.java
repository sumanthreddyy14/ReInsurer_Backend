package com.cts.backend;

import com.cts.backend.riskcession.dto.AllocateRiskRequest;
import com.cts.backend.riskcession.entity.Policy;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.repository.PolicyRepository;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.riskcession.service.RiskCessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Automatically rolls back MySQL changes after each test
class RiskCessionServiceIntegrationTest {

    @Autowired
    private RiskCessionService riskCessionService;

    @Autowired
    private PolicyRepository policyRepo;

    @Autowired
    private RiskCessionRepository cessionRepo;

    private String savedPolicyId;

    @BeforeEach
    void setUp() {
        // Create a dummy policy to use in allocation tests
        Policy p = new Policy();
        p.setPolicyId("POL-123");
        p.setPremium(2000.0);
        // Set other required policy fields here
        Policy saved = policyRepo.save(p);
        savedPolicyId = saved.getPolicyId();
    }

    @Test
    @DisplayName("Should correctly allocate risk and calculate premiums/commissions")
    void allocate_CalculationSuccess() {
        // Arrange
        AllocateRiskRequest req = new AllocateRiskRequest();
        req.setPolicyId(savedPolicyId);
        req.setTreatyId("TREATY-01");
        req.setCededPercentage(25.0); // 25% of 2000 = 500
        req.setCommissionRate(0.1);   // 10% of 500 = 50
        req.setCreatedBy("Admin");

        // Act
        RiskCession result = riskCessionService.allocate(req);

        // Assert
        assertNotNull(result);
        assertEquals(500.0, result.getCededPremium());
        assertEquals(50.0, result.getCommission());
        assertTrue(result.getCessionId().startsWith("C"));

        // Verify database state
        assertTrue(cessionRepo.findByTreatyId("TREATY-01").size() > 0);
    }

    @Test
    @DisplayName("Should use default commission rate when not provided in request")
    void allocate_UseDefaultCommission() {
        // Arrange
        AllocateRiskRequest req = new AllocateRiskRequest();
        req.setPolicyId(savedPolicyId);
        req.setTreatyId("TREATY-02");
        req.setCededPercentage(50.0); // 50% of 2000 = 1000
        req.setCommissionRate(null);  // Should use @Value default (0.1)

        // Act
        RiskCession result = riskCessionService.allocate(req);

        // Assert
        // 1000 * 0.1 = 100.0
        assertEquals(100.0, result.getCommission());
    }

    @Test
    @DisplayName("Should generate unique sequential Cession IDs")
    void allocate_IdGenerationSequence() {
        // Arrange
        AllocateRiskRequest req = new AllocateRiskRequest();
        req.setPolicyId(savedPolicyId);
        req.setTreatyId("T-1");
        req.setCededPercentage(10.0);

        // Act
        RiskCession first = riskCessionService.allocate(req);

        req.setTreatyId("T-2");
        RiskCession second = riskCessionService.allocate(req);

        // Assert
        assertNotEquals(first.getCessionId(), second.getCessionId());
        // If it was the first, it should be C0001 then C0002
    }

    @Test
    @DisplayName("Should throw exception if policy does not exist in MySQL")
    void allocate_PolicyNotFound() {
        // Arrange
        AllocateRiskRequest req = new AllocateRiskRequest();
        req.setPolicyId("INVALID_ID");
        req.setCededPercentage(10.0);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> riskCessionService.allocate(req));
    }

    @Test
    @DisplayName("Should list cessions by Treaty ID")
    void listByTreaty_Success() {
        // Arrange
        AllocateRiskRequest req = new AllocateRiskRequest();
        req.setPolicyId(savedPolicyId);
        req.setTreatyId("TARGET-TREATY");
        req.setCededPercentage(10.0);
        riskCessionService.allocate(req);

        // Act
        List<RiskCession> list = riskCessionService.listByTreaty("TARGET-TREATY");

        // Assert
        assertEquals(1, list.size());
        assertEquals("TARGET-TREATY", list.get(0).getTreatyId());
    }
}