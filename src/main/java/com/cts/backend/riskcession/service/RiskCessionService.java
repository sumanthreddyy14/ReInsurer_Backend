package com.cts.backend.riskcession.service;

import com.cts.backend.riskcession.dto.AllocateRiskRequest;
import com.cts.backend.riskcession.entity.Policy;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.repository.PolicyRepository;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RiskCessionService {

    private final RiskCessionRepository cessionRepo;
    private final PolicyRepository policyRepo;

    private final double defaultCommissionRate;

    public RiskCessionService(
            RiskCessionRepository cessionRepo,
            PolicyRepository policyRepo,
            @Value("${app.default-commission-rate:0.1}") double defaultCommissionRate
    ) {
        this.cessionRepo = cessionRepo;
        this.policyRepo = policyRepo;
        this.defaultCommissionRate = defaultCommissionRate;
    }

    public java.util.List<RiskCession> listAll() {
        return cessionRepo.findAll();
    }

    public java.util.List<RiskCession> listByTreaty(String treatyId) {
        return cessionRepo.findByTreatyId(treatyId);
    }

    @Transactional
    public RiskCession allocate(AllocateRiskRequest req) {

        Policy policy = policyRepo.findById(req.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        double cededPremium = round(policy.getPremium() * (req.getCededPercentage() / 100.0));
        double commissionRate = req.getCommissionRate() != null ? req.getCommissionRate() : defaultCommissionRate;
        double commission = round(cededPremium * commissionRate);

        String id = nextCessionId();

        RiskCession c = RiskCession.builder()
                .cessionId(id)
                .treatyId(req.getTreatyId())
                .policyId(req.getPolicyId())
                .cededPercentage(req.getCededPercentage())
                .cededPremium(cededPremium)
                .commission(commission)
                .createdBy(req.getCreatedBy())
                .createdAt(Instant.now())
                .build();

        return cessionRepo.save(c);
    }

    private String nextCessionId() {
        long count = cessionRepo.count() + 1;
        return "C" + String.format("%04d", count);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}