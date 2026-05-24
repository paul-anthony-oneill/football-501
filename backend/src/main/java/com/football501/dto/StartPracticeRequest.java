package com.football501.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for starting a practice game.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartPracticeRequest {

    @NotNull
    private UUID playerId;

    private String categorySlug;

    private Integer difficulty;
}
