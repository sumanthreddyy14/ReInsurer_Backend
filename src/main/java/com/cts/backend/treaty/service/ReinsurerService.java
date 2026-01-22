package com.cts.backend.treaty.service;


import com.cts.backend.treaty.dto.ReinsurerUiDTO;
import com.cts.backend.treaty.entity.Reinsurer;
import com.cts.backend.treaty.repositories.ReinsurerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ReinsurerService {

    private final ReinsurerRepository repo;

    public ReinsurerService(ReinsurerRepository repo) {
        this.repo = repo;
    }

    public List<ReinsurerUiDTO> list() {
        return repo.findAll().stream().map(this::toUi).toList();
    }

    public ReinsurerUiDTO get(String reinsurerId) {
        Reinsurer r = repo.findByReinsurerId(reinsurerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reinsurer not found: " + reinsurerId));
        return toUi(r);
    }


    public ReinsurerUiDTO create(ReinsurerUiDTO dto) {
        // 1) Validate input
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }

        // 2) Create entity
        Reinsurer r = new Reinsurer();
        r.setName(dto.getName());
        r.setContactInfo(dto.getContactInfo());

        // 3) If caller provided reinsurerId, set it BEFORE saving
        if (dto.getReinsurerId() != null && !dto.getReinsurerId().isBlank()) {
            if (repo.existsByReinsurerId(dto.getReinsurerId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "reinsurerId already exists");
            }
            r.setReinsurerId(dto.getReinsurerId());
            Reinsurer saved = repo.save(r); // single insert with non-null value
            return toUi(saved);
        } else {
            // 4) If caller did NOT provide reinsurerId, generate one BEFORE saving
            // (Option A) Quick & okay for dev: derive from next numeric ID guess
            Long nextNumeric = repo.findAll().stream()
                    .map(Reinsurer::getId).max(Long::compareTo).orElse(0L) + 1;
            String generated = "R" + String.format("%03d", nextNumeric);
            // If collision (rare), bump until unique
            while (repo.existsByReinsurerId(generated)) {
                nextNumeric++;
                generated = "R" + String.format("%03d", nextNumeric);
            }
            r.setReinsurerId(generated);

            // (Option B) Alternatively, make the column nullable and do two-step save; see below.

            Reinsurer saved = repo.save(r);
            return toUi(saved);
        }
    }

    public void delete(String reinsurerId) {
        Reinsurer r = repo.findByReinsurerId(reinsurerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reinsurer not found: " + reinsurerId));
        repo.delete(r);
    }

    // mapping
    private ReinsurerUiDTO toUi(Reinsurer r) {
        ReinsurerUiDTO dto = new ReinsurerUiDTO();
        dto.setReinsurerId(r.getReinsurerId());
        dto.setName(r.getName());
        dto.setContactInfo(r.getContactInfo());
        return dto;
    }
}
