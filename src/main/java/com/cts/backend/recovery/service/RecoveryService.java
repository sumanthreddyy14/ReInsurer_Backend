package com.cts.backend.recovery.service;

import com.cts.backend.recovery.dto.CreateRecoveryRequest;
import com.cts.backend.recovery.dto.RecoveryUiDTO;
import com.cts.backend.recovery.dto.UpdateRecoveryRequest;
import com.cts.backend.recovery.dto.UpdateStatusRequest;
import com.cts.backend.recovery.entity.Recovery;
import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.repository.PolicyRepository;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.treaty.repositories.TreatyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecoveryService {

    private final RecoveryRepository repo;
    private final RiskCessionRepository cessionRepo;
    private final TreatyRepository treatyRepo;
    private final PolicyRepository policyRepo; // optional validation

    // ---------- Queries ----------
    @Transactional(readOnly = true)
    public List<RecoveryUiDTO> list(String treatyId, String status) {
        Recovery.RecoveryStatus st = parseStatusOrNull(status);
        List<Recovery> found = repo.search(
                (treatyId == null || treatyId.isBlank()) ? null : treatyId,
                st
        );
        return found.stream().map(this::toUi).toList();
    }

    @Transactional(readOnly = true)
    public RecoveryUiDTO get(String recoveryId) {
        Recovery r = repo.findByRecoveryId(recoveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId));
        return toUi(r);
    }

    // ---------- Commands ----------
    public RecoveryUiDTO create(CreateRecoveryRequest req) {
        // validate treaty
        if (!treatyRepo.existsByTreatyId(req.getTreatyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Treaty not found: " + req.getTreatyId());
        }
        // optional: validate policy if provided
        if (req.getPolicyId() != null && !req.getPolicyId().isBlank()) {
            if (!policyRepo.existsById(req.getPolicyId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy not found: " + req.getPolicyId());
            }
        }

        Recovery r = new Recovery();
        r.setRecoveryId(nextId()); // generate "REC###"
        r.setClaimId(req.getClaimId());
        r.setTreatyId(req.getTreatyId());
        r.setPolicyId(req.getPolicyId());
        r.setRecoveryAmount(round(req.getRecoveryAmount()));
        r.setRecoveryDate(parseToInstant(req.getRecoveryDate())); // from yyyy-MM-dd
        r.setStatus(parseStatusOrDefault(req.getStatus(), Recovery.RecoveryStatus.PENDING));
        r.setCreatedAt(Instant.now());
        r.setCreatedBy(req.getCreatedBy());

        return toUi(repo.save(r));
    }

    public RecoveryUiDTO update(String recoveryId, UpdateRecoveryRequest req) {
        Recovery r = repo.findByRecoveryId(recoveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId));

        if (req.getRecoveryAmount() != null) {
            r.setRecoveryAmount(round(req.getRecoveryAmount()));
        }
        if (req.getRecoveryDate() != null && !req.getRecoveryDate().isBlank()) {
            r.setRecoveryDate(parseToInstant(req.getRecoveryDate()));
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            r.setStatus(parseStatus(req.getStatus()));
        }

        return toUi(repo.save(r));
    }

    public RecoveryUiDTO updateStatus(String recoveryId, UpdateStatusRequest req) {
        Recovery r = repo.findByRecoveryId(recoveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId));
        r.setStatus(parseStatus(req.getStatus()));
        return toUi(repo.save(r));
    }



    public RecoveryUiDTO deriveFromCessionIdOrThrow(String recoveryId) {
        // Expect format: "REC-<cessionId>"
        if (!recoveryId.startsWith("REC-")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId);
        }
        String cessionId = recoveryId.substring("REC-".length());
        var cOpt = cessionRepo.findById(cessionId);
        if (cOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId);
        }
        var c = cOpt.get();
        RecoveryUiDTO dto = new RecoveryUiDTO();
        dto.setRecoveryId("REC-" + c.getCessionId());
        dto.setClaimId("CLM-" + c.getPolicyId());
        dto.setTreatyId(c.getTreatyId());
        dto.setRecoveryAmount(c.getCededPremium());
        var date = (c.getCreatedAt() != null)
                ? java.time.LocalDate.ofInstant(c.getCreatedAt(), java.time.ZoneOffset.UTC).toString()
                : java.time.LocalDate.now().toString();
        dto.setRecoveryDate(date);
        dto.setStatus("PENDING"); // or compute a rule
        return dto;
    }

    public List<RecoveryUiDTO> generateFromAllCessions() {
        List<RiskCession> cessions = cessionRepo.findAll();
        List<RecoveryUiDTO> created = new java.util.ArrayList<>();

        for (RiskCession c : cessions) {
            // Idempotent: reuse cessionId to avoid duplicates if re-run
            String rid = "REC-" + c.getCessionId();
            if (repo.findByRecoveryId(rid).isPresent()) continue;

            Recovery r = new Recovery();
            r.setRecoveryId(rid);
            r.setClaimId("CLM-" + c.getPolicyId());
            r.setTreatyId(c.getTreatyId());
            r.setPolicyId(c.getPolicyId());
            r.setRecoveryAmount(round(c.getCededPremium()));
            r.setRecoveryDate(
                    (c.getCreatedAt() != null)
                            ? java.time.LocalDate.ofInstant(c.getCreatedAt(), java.time.ZoneOffset.UTC).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                            : java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
            );
            r.setStatus(Recovery.RecoveryStatus.PENDING);
            r.setCreatedAt(java.time.Instant.now());
            r.setCreatedBy("system");

            repo.save(r);
            created.add(toUi(r));
        }
        return created;
    }

    public RecoveryUiDTO flagDispute(String recoveryId) {
        Recovery r = repo.findByRecoveryId(recoveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId));
        r.setStatus(Recovery.RecoveryStatus.DISPUTED);
        return toUi(repo.save(r));
    }

    public void delete(String recoveryId) {
        Recovery r = repo.findByRecoveryId(recoveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery not found: " + recoveryId));
        repo.delete(r);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repo.countByStatus(Recovery.RecoveryStatus.PENDING);
    }

    // ---------- Helpers ----------
    private String nextId() {
        long count = repo.count() + 1; // simple generator (OK for dev/demo)
        String candidate = "REC" + String.format("%03d", count);
        // If a rare collision, bump forward
        while (repo.findByRecoveryId(candidate).isPresent()) {
            count++;
            candidate = "REC" + String.format("%03d", count);
        }
        return candidate;
    }

    private RecoveryUiDTO toUi(Recovery r) {
        RecoveryUiDTO dto = new RecoveryUiDTO();
        dto.setRecoveryId(r.getRecoveryId());
        dto.setClaimId(r.getClaimId());
        dto.setTreatyId(r.getTreatyId());
        dto.setRecoveryAmount(r.getRecoveryAmount());
        // UI uses yyyy-MM-dd; normalize output to that
        dto.setRecoveryDate(r.getRecoveryDate() == null ? null : LocalDate.ofInstant(r.getRecoveryDate(), ZoneOffset.UTC).toString());
        dto.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
        return dto;
    }

    private Recovery.RecoveryStatus parseStatus(String s) {
        try {
            return Recovery.RecoveryStatus.valueOf(s.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + s + " (allowed: PENDING, COMPLETED, DISPUTED)");
        }
    }

    private Recovery.RecoveryStatus parseStatusOrDefault(String s, Recovery.RecoveryStatus d) {
        return (s == null || s.isBlank()) ? d : parseStatus(s);
    }

    private Recovery.RecoveryStatus parseStatusOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return parseStatus(s);
    }

    private Instant parseToInstant(String date) {
        // Accept plain "yyyy-MM-dd" (Angular date input) or full ISO-8601
        try {
            if (date.length() == 10) {
                // "yyyy-MM-dd" => start of day UTC
                LocalDate ld = LocalDate.parse(date);
                return ld.atStartOfDay(ZoneOffset.UTC).toInstant();
            } else {
                return Instant.parse(date);
            }
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date: " + date + ". Use yyyy-MM-dd or ISO-8601.");
        }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}