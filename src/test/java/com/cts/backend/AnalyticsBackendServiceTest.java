package com.cts.backend;

import com.cts.backend.analytics.dto.AnalyticsKpiDTO;
import com.cts.backend.analytics.dto.RiskExposureDTO;
import com.cts.backend.analytics.dto.TreatyPerformanceSummaryDTO;
import com.cts.backend.analytics.service.AnalyticsBackendService;
import com.cts.backend.recovery.repository.RecoveryRepository;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.repository.RiskCessionRepository;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.TreatyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsBackendServiceTest {

    @Mock private TreatyRepository treatyRepo;
    @Mock private RiskCessionRepository cessionRepo;
    @Mock private RecoveryRepository recoveryRepo;

    @InjectMocks private AnalyticsBackendService service;

    // ---------------- Helpers ----------------

    private Instant startOfDayUtc(String yyyyMmDd) {
        return LocalDate.parse(yyyyMmDd).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant endOfDayInclusiveUtc(String yyyyMmDd) {
        return LocalDate.parse(yyyyMmDd)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .minusMillis(1);
    }

    private Treaty treaty(String id, Treaty.TreatyStatus status, Treaty.TreatyType type) {
        Treaty t = mock(Treaty.class, withSettings().lenient());
        when(t.getTreatyId()).thenReturn(id);
        when(t.getStatus()).thenReturn(status);
        when(t.getTreatyType()).thenReturn(type);
        when(t.getReinsurer()).thenReturn(null); // keep simple; service handles null
        return t;
    }

    private RiskCession cession(double premium, double pct, String policyId) {
        RiskCession rc = mock(RiskCession.class, withSettings().lenient());
        when(rc.getCededPremium()).thenReturn(premium);
        when(rc.getCededPercentage()).thenReturn(pct);
        when(rc.getPolicyId()).thenReturn(policyId);
        return rc;
    }

    // ---------------- Tests ----------------

    @Test
    void kpis_withDateRange_success() {
        String from = "2024-01-01";
        String to   = "2024-01-31";

        Treaty t1 = treaty("T1", Treaty.TreatyStatus.ACTIVE,  Treaty.TreatyType.PROPORTIONAL);
        Treaty t2 = treaty("T2", Treaty.TreatyStatus.EXPIRED, Treaty.TreatyType.NON_PROPORTIONAL);

        when(treatyRepo.countByStatus(Treaty.TreatyStatus.ACTIVE)).thenReturn(1L);
        when(treatyRepo.countByStatus(Treaty.TreatyStatus.EXPIRED)).thenReturn(1L);
        when(treatyRepo.findAll()).thenReturn(List.of(t1, t2));

        // Use typed matchers to avoid mismatch
        when(cessionRepo.sumPremiumFiltered(isNull(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(1200.0);
        when(recoveryRepo.sumCompletedFiltered(isNull(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(300.0);

        when(cessionRepo.findByTreatyAndCreatedAtBetween(eq("T1"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        cession(400, 40, "P1"),
                        cession(800, 60, "P2")
                ));
        when(cessionRepo.findByTreatyAndCreatedAtBetween(eq("T2"), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        AnalyticsKpiDTO dto = service.kpis(from, to);

        assertEquals(1, dto.getActiveTreaties());
        assertEquals(1, dto.getExpiredTreaties());
        assertEquals(1200.0, dto.getTotalCededPremiums(), 1e-9);
        assertEquals(300.0, dto.getTotalRecoveries(), 1e-9);
        assertEquals(900.0, dto.getOutstandingRecoveries(), 1e-9);

        // avg ceded %: (40 + 60)/2 = 50% -> 0.50 (fraction)
        assertEquals(0.50, dto.getAverageCededPercentage(), 1e-9);
        // loss ratio: 300 / 1200 = 0.25
        assertEquals(0.25, dto.getAverageLossRatio(), 1e-9);
        assertNotNull(dto.getGeneratedAt());
        assertEquals("AnalyticsBackendService v1.0", dto.getSource());
    }

    @Test
    void kpis_withNullDates_zeroes() {
        when(treatyRepo.countByStatus(Treaty.TreatyStatus.ACTIVE)).thenReturn(0L);
        when(treatyRepo.countByStatus(Treaty.TreatyStatus.EXPIRED)).thenReturn(0L);
        when(treatyRepo.findAll()).thenReturn(Collections.emptyList());

        // If repo returns Double (wrapper), you can return null; if primitive, see Variant B
        when(cessionRepo.sumPremiumFiltered(isNull(String.class), isNull(Instant.class), isNull(Instant.class)))
                .thenReturn(null); // service clean() -> 0.0
        when(recoveryRepo.sumCompletedFiltered(isNull(String.class), isNull(Instant.class), isNull(Instant.class)))
                .thenReturn(null); // service clean() -> 0.0

        AnalyticsKpiDTO dto = service.kpis(null, null);

        assertEquals(0, dto.getActiveTreaties());
        assertEquals(0, dto.getExpiredTreaties());
        assertEquals(0.0, dto.getTotalCededPremiums(), 1e-9);
        assertEquals(0.0, dto.getTotalRecoveries(), 1e-9);
        assertEquals(0.0, dto.getOutstandingRecoveries(), 1e-9);
        assertEquals(0.0, dto.getAverageLossRatio(), 1e-9);
        assertEquals(0.0, dto.getAverageCededPercentage(), 1e-9);
    }

    @Test
    void performance_perTreaty_summaries() {
        String from = "2024-02-01";
        String to   = "2024-02-29";

        Treaty t1 = treaty("T1", Treaty.TreatyStatus.ACTIVE,  Treaty.TreatyType.PROPORTIONAL);
        Treaty t2 = treaty("T2", Treaty.TreatyStatus.EXPIRED, Treaty.TreatyType.NON_PROPORTIONAL);
        when(treatyRepo.findAll()).thenReturn(List.of(t1, t2));

        when(cessionRepo.sumPremiumFiltered(eq("T1"), any(Instant.class), any(Instant.class))).thenReturn(1000.0);
        when(cessionRepo.sumPremiumFiltered(eq("T2"), any(Instant.class), any(Instant.class))).thenReturn(2000.0);

        when(recoveryRepo.sumCompletedFiltered(eq("T1"), any(Instant.class), any(Instant.class))).thenReturn(400.0);
        when(recoveryRepo.sumCompletedFiltered(eq("T2"), any(Instant.class), any(Instant.class))).thenReturn(0.0);

        when(cessionRepo.findByTreatyAndCreatedAtBetween(eq("T1"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(cession(400, 40, "P1"), cession(600, 60, "P2")));
        when(cessionRepo.findByTreatyAndCreatedAtBetween(eq("T2"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(cession(2000, 25, "P3")));

        List<TreatyPerformanceSummaryDTO> list = service.performance(from, to);
        assertEquals(2, list.size());

        TreatyPerformanceSummaryDTO d1 = list.stream().filter(d -> "T1".equals(d.getTreatyId())).findFirst().orElseThrow();
        assertEquals(1000.0, d1.getTotalCededPremiums(), 1e-9);
        assertEquals(400.0, d1.getTotalRecoveries(), 1e-9);
        assertEquals(600.0, d1.getOutstandingRecoveries(), 1e-9);
        assertEquals(2, d1.getAllocationsCount());
        assertEquals(0.50, d1.getAverageCededPercentage(), 1e-9);
        assertEquals(0.40, d1.getLossRatio(), 1e-9);
        assertEquals("ACTIVE", d1.getStatus());
        assertEquals(from, d1.getPeriodFrom());
        assertEquals(to, d1.getPeriodTo());

        TreatyPerformanceSummaryDTO d2 = list.stream().filter(d -> "T2".equals(d.getTreatyId())).findFirst().orElseThrow();
        assertEquals(2000.0, d2.getTotalCededPremiums(), 1e-9);
        assertEquals(0.0, d2.getTotalRecoveries(), 1e-9);
        assertEquals(2000.0, d2.getOutstandingRecoveries(), 1e-9);
        assertEquals(1, d2.getAllocationsCount());
        assertEquals(0.25, d2.getAverageCededPercentage(), 1e-9);
        assertEquals(0.0, d2.getLossRatio(), 1e-9);
        assertEquals("EXPIRED", d2.getStatus());
    }

    @Test
    void exposure_metrics_success() {
        String treatyId = "T1";
        String from = "2024-03-01";
        String to   = "2024-03-31";

        Treaty t = treaty(treatyId, Treaty.TreatyStatus.ACTIVE, Treaty.TreatyType.NON_PROPORTIONAL);
        when(treatyRepo.findByTreatyId(eq(treatyId))).thenReturn(Optional.of(t));

        when(cessionRepo.findByTreatyAndCreatedAtBetween(eq(treatyId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        cession(500, 30, "P1"),
                        cession(700, 70, "P2"),
                        cession(800, 50, "P3"),
                        cession(0,   0,   null)
                ));

        RiskExposureDTO dto = service.exposure(treatyId, from, to);

        assertEquals(treatyId, dto.getTreatyId());
        assertEquals("NON-PROPORTIONAL", dto.getTreatyType());
        assertEquals(2000.0, dto.getTotalCededPremiums(), 1e-9);
        assertEquals(0.70, dto.getMaxCededPercentage(), 1e-9);
        assertEquals(0.38, dto.getAverageCededPercentage(), 1e-9);
        assertEquals(4, dto.getCessionCount());
        assertEquals(3, dto.getPolicyCount());
        assertNotNull(dto.getGeneratedAt());
        assertEquals("AnalyticsBackendService v1.0", dto.getSource());

        // Minimal date verification
        ArgumentCaptor<Instant> fCap = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> tCap = ArgumentCaptor.forClass(Instant.class);
        verify(cessionRepo).findByTreatyAndCreatedAtBetween(eq(treatyId), fCap.capture(), tCap.capture());
        assertEquals(startOfDayUtc(from), fCap.getValue());
        assertEquals(endOfDayInclusiveUtc(to), tCap.getValue());
    }

    @Test
    void exposure_treatyNotFound_throws() {
        when(treatyRepo.findByTreatyId("NOPE")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.exposure("NOPE", "2024-01-01", "2024-01-31"));
    }

    @Test
    void rounding_twoDecimals() {
        Treaty t1 = treaty("T1", Treaty.TreatyStatus.ACTIVE, Treaty.TreatyType.PROPORTIONAL);
        when(treatyRepo.findAll()).thenReturn(List.of(t1));
        when(treatyRepo.countByStatus(any())).thenReturn(0L);

        when(cessionRepo.sumPremiumFiltered(isNull(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(333.3333);
        when(recoveryRepo.sumCompletedFiltered(isNull(String.class), any(Instant.class), any(Instant.class)))
                .thenReturn(111.1111);

        when(cessionRepo.findByTreatyAndCreatedAtBetween(eq("T1"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        cession(0, 33.3333, "P1"),
                        cession(0, 66.6666, "P2")
                ));

        AnalyticsKpiDTO dto = service.kpis("2024-01-01", "2024-01-31");
        assertEquals(333.33, dto.getTotalCededPremiums(), 1e-9);
        assertEquals(111.11, dto.getTotalRecoveries(), 1e-9);
        assertEquals(0.33, dto.getAverageLossRatio(), 1e-9);
        assertEquals(0.50, dto.getAverageCededPercentage(), 1e-9);
    }
}