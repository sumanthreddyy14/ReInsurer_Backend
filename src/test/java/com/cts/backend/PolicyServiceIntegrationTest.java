package com.cts.backend;

import com.cts.backend.riskcession.entity.Policy;
import com.cts.backend.riskcession.repository.PolicyRepository;
import com.cts.backend.riskcession.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Ensures test data is rolled back in MySQL after each test
class PolicyServiceIntegrationTest {

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PolicyRepository policyRepo;

    @Test
    @DisplayName("Integration: Should save a policy to MySQL and retrieve it")
    void create_and_getById_Success() {
        // Arrange
        Policy policy = new Policy();
        // Assuming 'id' is a String based on your getById(String id) method
        policy.setPolicyId("POL-101");
        policy.setPremium(1500.0);
        // Add other required fields based on your Policy entity

        // Act
        Policy saved = policyService.create(policy);
        Policy fetched = policyService.getById(saved.getPolicyId());

        // Assert
        assertNotNull(fetched);
        assertEquals("POL-101", fetched.getPolicyId());
        assertEquals(1500.0, fetched.getPremium());
    }

    @Test
    @DisplayName("Integration: Should throw exception when policy does not exist")
    void getById_NotFound_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            policyService.getById("NON-EXISTENT-ID");
        });
    }

    @Test
    @DisplayName("Integration: Should list all policies from the database")
    void listAll_ReturnsPopulatedList() {
        // Arrange
        Policy p1 = new Policy();
        p1.setPolicyId("P1");
        p1.setPremium(100.0);
        policyService.create(p1);

        Policy p2 = new Policy();
        p2.setPolicyId("P2");
        p2.setPremium(200.0);
        policyService.create(p2);

        // Act
        List<Policy> allPolicies = policyService.listAll();

        // Assert
        assertNotNull(allPolicies);
        assertTrue(allPolicies.size() >= 2);
    }
}