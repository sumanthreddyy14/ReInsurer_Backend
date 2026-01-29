package com.cts.backend.riskcession.repository;


import com.cts.backend.riskcession.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, String> {}