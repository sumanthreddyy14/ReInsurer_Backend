package com.cts.backend.analytics.controller;
import com.cts.backend.analytics.dto.ComplianceIssueDTO;
import com.cts.backend.analytics.dto.ComplianceRulesDTO;
import com.cts.backend.analytics.service.ComplianceBackendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceBackendService service;

    @GetMapping("/issues")
    public List<ComplianceIssueDTO> listIssues(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) Integer maxPendingDays,
                                               @RequestParam(required = false) Double minUtilization,
                                               @RequestParam(required = false) Double maxOutstandingPerTreaty,
                                               @RequestParam(required = false) Double maxLossRatio) {
        var rules = new ComplianceRulesDTO();
        rules.setMaxPendingDays(maxPendingDays);
        rules.setMinUtilization(minUtilization);
        rules.setMaxOutstandingPerTreaty(maxOutstandingPerTreaty);
        rules.setMaxLossRatio(maxLossRatio);
        return service.listIssues(from, to, rules);
    }

    @GetMapping(value = "/issues/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        List<ComplianceIssueDTO> issues = service.listIssues(from, to, new ComplianceRulesDTO());
        StringBuilder sb = new StringBuilder();
        sb.append("Compliance Issues\n");
        sb.append("ID,EntityType,EntityId,Severity,DetectedAt,Resolved,Message\n");
        for (var i : issues) {
            sb.append(i.getId()).append(",")
                    .append(i.getEntityType()).append(",")
                    .append(i.getEntityId()).append(",")
                    .append(i.getSeverity()).append(",")
                    .append(i.getDetectedAt()).append(",")
                    .append(i.isResolved()).append(",")
                    .append("\"").append((i.getMessage() == null ? "" : i.getMessage().replace("\"","'"))).append("\"\n");
        }
        byte[] out = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fn = URLEncoder.encode("compliance_issues.csv", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fn).build());
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }
}