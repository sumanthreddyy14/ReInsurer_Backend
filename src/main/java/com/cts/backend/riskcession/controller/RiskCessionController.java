package com.cts.backend.riskcession.controller;

import com.cts.backend.riskcession.dto.AllocateRiskRequest;
import com.cts.backend.riskcession.entity.RiskCession;
import com.cts.backend.riskcession.service.RiskCessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@CrossOrigin(
        origins = "http://localhost:4200",
        allowCredentials = "true"
)
@RestController
@RequestMapping("/api/cessions")
public class RiskCessionController {

    private final RiskCessionService service;

    public RiskCessionController(RiskCessionService service) {
        this.service = service;
    }

    @GetMapping
    public List<RiskCession> listAll() {
        return service.listAll();
    }

    @GetMapping("/by-treaty/{treatyId}")
    public List<RiskCession> listByTreaty(@PathVariable String treatyId) {
        return service.listByTreaty(treatyId);
    }

    @PostMapping("/allocate")
    public RiskCession allocate(@RequestBody AllocateRiskRequest req) {
        return service.allocate(req);
    }
}