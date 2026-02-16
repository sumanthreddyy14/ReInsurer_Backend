package com.cts.backend.riskcession.controller;

import com.cts.backend.riskcession.entity.Policy;
import com.cts.backend.riskcession.service.PolicyService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(
        origins = "http://localhost:4200",
        allowCredentials = "true"
)
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @GetMapping
    public List<Policy> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public Policy getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public Policy create(@RequestBody Policy policy) {
        return service.create(policy);
    }
}
