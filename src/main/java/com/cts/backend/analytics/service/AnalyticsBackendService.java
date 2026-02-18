package com.cts.backend.analytics.service;

import com.cts.backend.analytics.dto.AnalyticsKpiDTO;
import com.cts.backend.analytics.dto.RiskExposureDTO;
import com.cts.backend.analytics.dto.TreatyPerformanceSummaryDTO;
import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.TreatyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsBackendService {

    private final TreatyRepository treatyRepo;
    private final RiskCessionRepository cessionRepo;
    private final RecoveryRepository recoveryRepo;

    private Instant parseFrom(String from) {
        if (from == null || from.isBlank()) return null;
        return LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
    private Instant parseTo(String to) {
        if (to == null || to.isBlank()) return null;
        return LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }
    private double clean(Double v) { return v == null ? 0.0 : Math.round(v * 100.0) / 100.0; }
    private double avg(Collection<Double> xs) { return xs.isEmpty() ? 0.0 : xs.stream().mapToDouble(d -> d).average().orElse(0.0); }

    public AnalyticsKpiDTO kpis(String from, String to) {
        Instant f = parseFrom(from);
        Instant t = parseTo(to);

        int active = (int) treatyRepo.countByStatus(Treaty.TreatyStatus.ACTIVE);
        int expired = (int) treatyRepo.countByStatus(Treaty.TreatyStatus.EXPIRED);

        double totalCeded = clean(cessionRepo.sumPremiumFiltered(null, f, t));
        double totalRecov = clean(recoveryRepo.sumCompletedFiltered(null, f, t));
        double outstanding = clean(totalCeded - totalRecov);

        // avg ceded% across cessions (filtered)
        List<Double> cededPcts = new ArrayList<>();
        for (Treaty t0 : treatyRepo.findAll()) {
            List<RiskCession> cs = cessionRepo.findByTreatyAndCreatedAtBetween(t0.getTreatyId(), f, t);
            for (RiskCession c : cs) {
                cededPcts.add(c.getCededPercentage());
            }
        }
        double avgCededPct = cededPcts.isEmpty() ? 0.0 : avg(cededPcts) / 100.0; // convert to fraction like Angular

        double avgLossRatio = totalCeded > 0 ? totalRecov / totalCeded : 0.0;

        AnalyticsKpiDTO dto = new AnalyticsKpiDTO();
        dto.setActiveTreaties(active);
        dto.setExpiredTreaties(expired);
        dto.setTotalCededPremiums(totalCeded);
        dto.setTotalRecoveries(totalRecov);
        dto.setOutstandingRecoveries(outstanding);
        dto.setAverageLossRatio(Math.round(avgLossRatio * 100.0) / 100.0);
        dto.setAverageCededPercentage(Math.round(avgCededPct * 100.0) / 100.0);
        dto.setGeneratedAt(ZonedDateTime.now(ZoneOffset.UTC).toString());
        dto.setSource("AnalyticsBackendService v1.0");
        return dto;
    }

    public List<TreatyPerformanceSummaryDTO> performance(String from, String to) {
        Instant f = parseFrom(from);
        Instant t = parseTo(to);

        List<TreatyPerformanceSummaryDTO> out = new ArrayList<>();
        for (Treaty tty : treatyRepo.findAll()) {
            double ceded = clean(cessionRepo.sumPremiumFiltered(tty.getTreatyId(), f, t));
            double recov = clean(recoveryRepo.sumCompletedFiltered(tty.getTreatyId(), f, t));
            double outstanding = clean(ceded - recov);

            List<RiskCession> cs = cessionRepo.findByTreatyAndCreatedAtBetween(tty.getTreatyId(), f, t);
            int allocations = cs.size();
            double avgCededPct = allocations == 0 ? 0.0 : cs.stream().mapToDouble(RiskCession::getCededPercentage).average().orElse(0.0) / 100.0;
            double lossRatio = ceded > 0 ? recov / ceded : 0.0;

            TreatyPerformanceSummaryDTO dto = new TreatyPerformanceSummaryDTO();
            dto.setTreatyId(tty.getTreatyId());
            dto.setReinsurerName(tty.getReinsurer() != null ? tty.getReinsurer().getName() : null);
            dto.setPeriodFrom(from);
            dto.setPeriodTo(to);
            dto.setTotalCededPremiums(ceded);
            dto.setTotalRecoveries(recov);
            dto.setOutstandingRecoveries(outstanding);
            dto.setAllocationsCount(allocations);
            dto.setAverageCededPercentage(Math.round(avgCededPct * 100.0) / 100.0);
            dto.setLossRatio(Math.round(lossRatio * 100.0) / 100.0);
            dto.setStatus(tty.getStatus() != null ? tty.getStatus().name() : "UNKNOWN");
            out.add(dto);
        }
        return out;
    }

    public RiskExposureDTO exposure(String treatyId, String from, String to) {
        Instant f = parseFrom(from);
        Instant t = parseTo(to);

        Treaty tty = treatyRepo.findByTreatyId(treatyId)
                .orElseThrow(() -> new IllegalArgumentException("Treaty not found: " + treatyId));
        List<RiskCession> cs = cessionRepo.findByTreatyAndCreatedAtBetween(treatyId, f, t);

        int count = cs.size();
        double totalCededPrem = clean(cs.stream().mapToDouble(RiskCession::getCededPremium).sum());
        double maxPct = count == 0 ? 0.0 : cs.stream().mapToDouble(RiskCession::getCededPercentage).max().orElse(0.0) / 100.0;
        double avgPct = count == 0 ? 0.0 : cs.stream().mapToDouble(RiskCession::getCededPercentage).average().orElse(0.0) / 100.0;

        Set<String> policies = cs.stream().map(RiskCession::getPolicyId).filter(Objects::nonNull).collect(Collectors.toSet());

        RiskExposureDTO dto = new RiskExposureDTO();
        dto.setTreatyId(tty.getTreatyId());
        dto.setTreatyType(tty.getTreatyType() == Treaty.TreatyType.NON_PROPORTIONAL ? "NON-PROPORTIONAL" : "PROPORTIONAL");
        dto.setTotalCededPremiums(totalCededPrem);
        dto.setMaxCededPercentage(Math.round(maxPct * 100.0) / 100.0);
        dto.setAverageCededPercentage(Math.round(avgPct * 100.0) / 100.0);
        dto.setCessionCount(count);
        dto.setPolicyCount(policies.size());
        dto.setGeneratedAt(ZonedDateTime.now(ZoneOffset.UTC).toString());
        dto.setSource("AnalyticsBackendService v1.0");
        return dto;
    }
}

