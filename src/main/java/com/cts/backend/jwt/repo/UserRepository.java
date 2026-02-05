package com.cts.backend.jwt.repo;


import com.cts.backend.jwt.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, String> {
    AppUser findByUsername(String username);
}

