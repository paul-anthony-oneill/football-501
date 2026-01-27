package com.football501.dto;

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

    /**
     * Player ID (can be guest ID or authenticated user ID).
     */
    private UUID playerId;

    /**
     * Category slug (e.g., "football").
     * If not provided, defaults to "football".
     */
    private String categorySlug;
}
