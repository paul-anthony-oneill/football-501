package com.trivia501.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for {@code PATCH /api/admin/questions/{id}/difficulty-lock}.
 *
 * <p>When {@code locked = true} the recalibration job will skip this question.
 * If {@code difficultyScore} is provided it overrides the stored score immediately.
 * When {@code locked = false} the score is left as-is; the next recalibration run
 * will recompute it from stored counts.
 */
@Data
public class DifficultyLockRequest {

    /**
     * Whether to lock or unlock this question's difficulty score.
     * Required.
     */
    @NotNull(message = "locked is required")
    private Boolean locked;

    /**
     * Optional difficulty score override. Only applied when {@code locked = true}.
     * Must be in the range [0.0, 10.0] if provided.
     */
    @DecimalMin(value = "0.0", message = "difficultyScore must be >= 0.0")
    @DecimalMax(value = "10.0", message = "difficultyScore must be <= 10.0")
    private Double difficultyScore;
}
