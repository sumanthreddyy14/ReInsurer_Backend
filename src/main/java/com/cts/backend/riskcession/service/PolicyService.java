package com.cts.backend.riskcession.service;

import com.cts.backend.riskcession.entity.Policy;
import com.cts.backend.riskcession.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PolicyService {

    private final PolicyRepository policyRepo;

    public PolicyService(PolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    public List<Policy> listAll() {
        return policyRepo.findAll();
    }

    public Policy getById(String id) {
        return policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));
    }

    public Policy create(Policy policy) {
        return policyRepo.save(policy);
    }
}
