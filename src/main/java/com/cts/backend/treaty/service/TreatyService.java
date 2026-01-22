package com.cts.backend.treaty.service;

import com.cts.backend.treaty.dto.TreatyUiDTO;
import com.cts.backend.treaty.entity.Reinsurer;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.ReinsurerRepository;
import com.cts.backend.treaty.repositories.TreatyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class TreatyService {

    private final TreatyRepository repo;
    private final ReinsurerRepository reinsurerRepo;

    public TreatyService(TreatyRepository repo, ReinsurerRepository reinsurerRepo) {
        this.repo = repo;
        this.reinsurerRepo = reinsurerRepo;
    }

    public List<TreatyUiDTO> list() {
        return repo.findAll().stream().map(this::toUi).toList();
    }

    public TreatyUiDTO get(String treatyId) {
        Treaty t = repo.findByTreatyId(treatyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Treaty not found: " + treatyId));
        return toUi(t);
    }


    public TreatyUiDTO create(TreatyUiDTO dto) {
        Reinsurer reinsurer = reinsurerRepo.findByReinsurerId(dto.getReinsurerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reinsurer not found: " + dto.getReinsurerId()));

        Treaty t = new Treaty();
        t.setReinsurer(reinsurer);
        t.setCoverageLimit(dto.getCoverageLimit());
        t.setStartDate(LocalDate.parse(dto.getStartDate()));
        t.setEndDate(LocalDate.parse(dto.getEndDate()));
        t.setRenewalDate(dto.getRenewalDate() == null ? null : LocalDate.parse(dto.getRenewalDate()));
        t.setStatus(Treaty.TreatyStatus.valueOf(dto.getStatus()));
        t.setTreatyType("NON-PROPORTIONAL".equalsIgnoreCase(dto.getTreatyType())
                ? Treaty.TreatyType.NON_PROPORTIONAL
                : Treaty.TreatyType.PROPORTIONAL);

        // Set treatyId BEFORE save if provided
        if (dto.getTreatyId() != null && !dto.getTreatyId().isBlank()) {
            if (repo.existsByTreatyId(dto.getTreatyId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "treatyId already exists");
            }
            t.setTreatyId(dto.getTreatyId());
            return toUi(repo.save(t));
        }

        // Else generate BEFORE save
        Long nextNumeric = repo.findAll().stream().map(Treaty::getId).max(Long::compareTo).orElse(0L) + 1;
        String generated = "T" + String.format("%03d", nextNumeric);
        while (repo.existsByTreatyId(generated)) {
            nextNumeric++;
            generated = "T" + String.format("%03d", nextNumeric);
        }
        t.setTreatyId(generated);

        return toUi(repo.save(t));
    }


    public TreatyUiDTO update(String treatyId, TreatyUiDTO dto) {
        Treaty t = repo.findByTreatyId(treatyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Treaty not found: " + treatyId));

        if (dto.getReinsurerId() != null && !dto.getReinsurerId().isBlank()) {
            Reinsurer r = reinsurerRepo.findByReinsurerId(dto.getReinsurerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reinsurer not found: " + dto.getReinsurerId()));
            t.setReinsurer(r);
        }

        if (dto.getCoverageLimit() != null) t.setCoverageLimit(dto.getCoverageLimit());
        if (dto.getStartDate() != null) t.setStartDate(parseDate(dto.getStartDate()));
        if (dto.getEndDate() != null) t.setEndDate(parseDate(dto.getEndDate()));
        if (dto.getRenewalDate() != null) t.setRenewalDate(parseDate(dto.getRenewalDate()));
        if (dto.getTreatyType() != null) t.setTreatyType(parseType(dto.getTreatyType()));
        if (dto.getStatus() != null) t.setStatus(parseStatus(dto.getStatus()));

        return toUi(repo.save(t));
    }

    public void delete(String treatyId) {
        Treaty t = repo.findByTreatyId(treatyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Treaty not found: " + treatyId));
        repo.delete(t);
    }



    public List<TreatyUiDTO> getUpcomingRenewals(Integer days, List<String> includeStatuses) {
        int window = (days == null || days < 0) ? 90 : days;

        // Default statuses = ACTIVE
        List<Treaty.TreatyStatus> statuses = (includeStatuses == null || includeStatuses.isEmpty())
                ? List.of(Treaty.TreatyStatus.ACTIVE)
                : includeStatuses.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Treaty.TreatyStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Invalid status: " + s + ". Allowed: ACTIVE, EXPIRED, ARCHIVED");
                    }
                })
                .collect(Collectors.toList());

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(window);

        List<Treaty> found = repo.findUpcomingRenewals(from, to, statuses);

        return found.stream().map(this::toUi).toList();
    }



    // ---------- mapping helpers ----------
    private TreatyUiDTO toUi(Treaty t) {
        TreatyUiDTO dto = new TreatyUiDTO();
        dto.setTreatyId(t.getTreatyId());
        dto.setReinsurerId(t.getReinsurer().getReinsurerId());
        dto.setReinsurerName(t.getReinsurer().getName());
        dto.setCoverageLimit(t.getCoverageLimit());
        dto.setStartDate(t.getStartDate() != null ? t.getStartDate().toString() : null);
        dto.setEndDate(t.getEndDate() != null ? t.getEndDate().toString() : null);
        dto.setRenewalDate(t.getRenewalDate() != null ? t.getRenewalDate().toString() : null);
        dto.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        dto.setTreatyType(t.getTreatyType() == Treaty.TreatyType.NON_PROPORTIONAL ? "NON-PROPORTIONAL" : "PROPORTIONAL");
        return dto;
    }

    private static <T> T req(T val, String field) {
        if (val == null || (val instanceof String s && s.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return val;
    }

    private static String defaultIfNull(String v, String d) { return v == null ? d : v; }

    private static LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }

    private static Treaty.TreatyType parseType(String s) {
        if (s == null) return Treaty.TreatyType.PROPORTIONAL;
        return s.equalsIgnoreCase("NON-PROPORTIONAL")
                ? Treaty.TreatyType.NON_PROPORTIONAL
                : Treaty.TreatyType.PROPORTIONAL;
    }

    private static Treaty.TreatyStatus parseStatus(String s) {
        return Treaty.TreatyStatus.valueOf(s.toUpperCase());
    }
}
