package com.cts.backend.recovery.repository;

import com.cts.backend.recovery.entity.Recovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryRepository extends JpaRepository<Recovery, Long> {

    List<Recovery> findByTreatyId(String treatyId);

    Optional<Recovery> findByRecoveryId(String recoveryId);

    // Sum of completed recoveries for a specific treaty
    @Query("SELECT SUM(r.recoveryAmount) FROM Recovery r WHERE r.treatyId = ?1 AND r.status = 'COMPLETED'")
    Double sumCompletedByTreatyId(String treatyId);

    // Sum of all completed recoveries in the system (for the Dashboard Summary)
    @Query("SELECT SUM(r.recoveryAmount) FROM Recovery r WHERE r.status = 'COMPLETED'")
    Double sumAllCompleted();

    List<Recovery> findByStatus(Recovery.RecoveryStatus status);
    long countByStatus(Recovery.RecoveryStatus status);


    @Query("SELECT r FROM Recovery r " +
            "WHERE (:treatyId IS NULL OR r.treatyId = :treatyId) " +
            "AND (:status IS NULL OR r.status = :status)")
    List<Recovery> search(
            @Param("treatyId") String treatyId,
            @Param("status") Recovery.RecoveryStatus status
    );


    @Query("""
           SELECT SUM(r.recoveryAmount) 
             FROM Recovery r
            WHERE r.status = 'COMPLETED'
              AND (:treatyId IS NULL OR r.treatyId = :treatyId)
              AND (:from IS NULL OR r.recoveryDate >= :from)
              AND (:to   IS NULL OR r.recoveryDate <= :to)
           """)
    Double sumCompletedFiltered(@Param("treatyId") String treatyId,
                                @Param("from") java.time.Instant from,
                                @Param("to") java.time.Instant to);


}