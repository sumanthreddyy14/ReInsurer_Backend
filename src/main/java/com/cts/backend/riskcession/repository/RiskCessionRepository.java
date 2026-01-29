package com.cts.backend.riskcession.repository;

import com.cts.backend.riskcession.entity.RiskCession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskCessionRepository extends JpaRepository<RiskCession, String> {
    List<RiskCession> findByTreatyId(String treatyId);
}

