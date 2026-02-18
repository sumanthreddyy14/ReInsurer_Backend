package com.cts.backend.analytics.controller;

import com.cts.backend.analytics.dto.AnalyticsKpiDTO;
import com.cts.backend.analytics.dto.RiskExposureDTO;
import com.cts.backend.analytics.dto.TreatyPerformanceSummaryDTO;
import com.cts.backend.analytics.service.AnalyticsBackendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnalyticsController {

    private final AnalyticsBackendService service;

    @GetMapping("/kpis")
    public AnalyticsKpiDTO kpis(@RequestParam(required = false) String from,
                                @RequestParam(required = false) String to) {
        return service.kpis(from, to);
    }

    @GetMapping("/performance")
    public List<TreatyPerformanceSummaryDTO> performance(@RequestParam(required = false) String from,
                                                         @RequestParam(required = false) String to) {
        return service.performance(from, to);
    }

    @GetMapping("/exposure/{treatyId}")
    public RiskExposureDTO exposure(@PathVariable String treatyId,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        return service.exposure(treatyId, from, to);
    }
}

