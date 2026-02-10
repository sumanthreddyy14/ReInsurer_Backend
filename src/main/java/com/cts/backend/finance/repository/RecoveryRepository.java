package com.cts.backend.finance.repository;

import com.cts.backend.finance.entity.Recovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}