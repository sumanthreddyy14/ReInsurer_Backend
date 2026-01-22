package com.cts.backend.treaty.controller;

import com.cts.backend.treaty.dto.TreatyUiDTO;
import com.cts.backend.treaty.service.TreatyService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/treaties")
@CrossOrigin(origins = "http://localhost:4200")
public class TreatyController {

    private final TreatyService service;

    public TreatyController(TreatyService service) {
        this.service = service;
    }

    @GetMapping
    public List<TreatyUiDTO> list() { return service.list(); }

    @GetMapping("/{treatyId}")
    public TreatyUiDTO get(@PathVariable String treatyId) { return service.get(treatyId); }

    @PostMapping
    public TreatyUiDTO create(@RequestBody TreatyUiDTO dto) { return service.create(dto); }

    @PutMapping("/{treatyId}")
    public TreatyUiDTO update(@PathVariable String treatyId, @RequestBody TreatyUiDTO dto) {
        return service.update(treatyId, dto);
    }




    @GetMapping("/renewals")
    public List<TreatyUiDTO> getUpcomingRenewals(
            @RequestParam(name = "days", required = false) Integer days,
            @RequestParam(name = "includeStatuses", required = false) String includeStatusesCsv
    ) {
        List<String> statuses = (includeStatusesCsv == null || includeStatusesCsv.isBlank())
                ? List.of()
                : Arrays.stream(includeStatusesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return service.getUpcomingRenewals(days, statuses);
    }



    @DeleteMapping("/{treatyId}")
    public void delete(@PathVariable String treatyId) { service.delete(treatyId); }
}

