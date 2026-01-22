package com.cts.backend.treaty.controller;


import com.cts.backend.treaty.dto.ReinsurerUiDTO;
import com.cts.backend.treaty.service.ReinsurerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reinsurers")
@CrossOrigin(origins = "http://localhost:4200")
public class ReinsurerController {

    private final ReinsurerService service;

    public ReinsurerController(ReinsurerService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReinsurerUiDTO> list() { return service.list(); }

    @GetMapping("/{reinsurerId}")
    public ReinsurerUiDTO get(@PathVariable String reinsurerId) { return service.get(reinsurerId); }

    @PostMapping
    public ReinsurerUiDTO create(@RequestBody ReinsurerUiDTO dto) { return service.create(dto); }

    @DeleteMapping("/{reinsurerId}")
    public void delete(@PathVariable String reinsurerId) { service.delete(reinsurerId); }
}
