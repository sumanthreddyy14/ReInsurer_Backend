package com.cts.backend.riskcession.repository;

import com.cts.backend.riskcession.entity.RiskCession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RiskCessionRepository extends JpaRepository<RiskCession, String> {
    List<RiskCession> findByTreatyId(String treatyId);

    @Query("SELECT SUM(c.cededPremium) FROM RiskCession c WHERE c.treatyId = ?1")
    Double sumPremiumByTreatyId(String treatyId);

    @Query("SELECT SUM(c.cededPremium) FROM RiskCession c")
    Double sumAllPremium();
}

