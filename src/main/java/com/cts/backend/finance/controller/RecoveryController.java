package com.cts.backend.finance.controller;

import com.cts.backend.finance.dto.*;
import com.cts.backend.finance.service.RecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recoveries")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RecoveryController {

    private final RecoveryService service;

    // List with optional filters: ?treatyId=T001&status=PENDING
    @GetMapping
    public List<RecoveryUiDTO> list(
            @RequestParam(required = false) String treatyId,
            @RequestParam(required = false) String status
    ) {
        return service.list(treatyId, status);
    }

    @GetMapping("/{recoveryId}")
    public RecoveryUiDTO get(@PathVariable String recoveryId) {
        return service.get(recoveryId);
    }

    @PostMapping
    public RecoveryUiDTO create(@Valid @RequestBody CreateRecoveryRequest req) {
        return service.create(req);
    }

    @PutMapping("/{recoveryId}")
    public RecoveryUiDTO update(@PathVariable String recoveryId, @RequestBody UpdateRecoveryRequest req) {
        return service.update(recoveryId, req);
    }

    @PatchMapping("/{recoveryId}/status")
    public RecoveryUiDTO updateStatus(@PathVariable String recoveryId, @Valid @RequestBody UpdateStatusRequest req) {
        return service.updateStatus(recoveryId, req);
    }

    // For your "Flag Dispute" button
    @PostMapping("/{recoveryId}/dispute")
    public RecoveryUiDTO flagDispute(@PathVariable String recoveryId) {
        return service.flagDispute(recoveryId);
    }

    @DeleteMapping("/{recoveryId}")
    public void delete(@PathVariable String recoveryId) {
        service.delete(recoveryId);
    }
    @PostMapping("/generate-from-cessions")
    public List<RecoveryUiDTO> generateFromCessions() {
        return service.generateFromAllCessions();
    }

    // com.cts.backend.finance.controller.RecoveryController.java

    @GetMapping("/get/{recoveryId}")
    public RecoveryUiDTO get1(@PathVariable String recoveryId) {
        try {
            return service.get(recoveryId); // persisted path
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            // Fallback only when it's a "not found" and we can derive from cessions
            if (ex.getStatusCode().value() == 404 && recoveryId.startsWith("REC-")) {
                return service.deriveFromCessionIdOrThrow(recoveryId);
            }
            throw ex;
        }
    }

    // To support countPendingRecoveries() in UI
    @GetMapping("/metrics/pending-count")
    public long countPending() {
        return service.countPending();
    }
}