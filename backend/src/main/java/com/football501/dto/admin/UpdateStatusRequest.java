package com.football501.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for the {@code PATCH /api/admin/questions/{id}/status} endpoint.
 *
 * <p>Valid values: {@code "draft"}, {@code "active"}, {@code "retired"}.
 */
@Data
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "draft|active|retired",
        message = "Status must be one of: draft, active, retired"
    )
    private String status;
}
