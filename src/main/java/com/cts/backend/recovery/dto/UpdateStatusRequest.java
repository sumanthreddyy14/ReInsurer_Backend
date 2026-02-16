package com.cts.backend.recovery.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UpdateStatusRequest {
    @NotBlank
    private String status; // PENDING | COMPLETED | DISPUTED
}