package com.cts.backend.riskcession.repository;

import com.cts.backend.riskcession.entity.RiskCession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RiskCessionRepository extends JpaRepository<RiskCession, String> {
    List<RiskCession> findByTreatyId(String treatyId);

    @Query("SELECT SUM(c.cededPremium) FROM RiskCession c WHERE c.treatyId = ?1")
    Double sumPremiumByTreatyId(String treatyId);

    @Query("SELECT SUM(c.cededPremium) FROM RiskCession c")
    Double sumAllPremium();


    @Query("""
           SELECT SUM(c.cededPremium) 
             FROM RiskCession c
            WHERE (:treatyId IS NULL OR c.treatyId = :treatyId)
              AND (:from IS NULL OR c.createdAt >= :from)
              AND (:to   IS NULL OR c.createdAt <= :to)
           """)
    Double sumPremiumFiltered(@Param("treatyId") String treatyId,
                              @Param("from") Instant from,
                              @Param("to") Instant to);

    @Query("""
           SELECT c 
             FROM RiskCession c
            WHERE c.treatyId = :treatyId
              AND (:from IS NULL OR c.createdAt >= :from)
              AND (:to   IS NULL OR c.createdAt <= :to)
           """)
    List<RiskCession> findByTreatyAndCreatedAtBetween(@Param("treatyId") String treatyId,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to);




}

