package com.cts.backend.treaty.repositories;


import com.cts.backend.treaty.entity.Reinsurer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReinsurerRepository extends JpaRepository<Reinsurer, Long> {
    Optional<Reinsurer> findByReinsurerId(String reinsurerId);
    boolean existsByReinsurerId(String reinsurerId);
}
