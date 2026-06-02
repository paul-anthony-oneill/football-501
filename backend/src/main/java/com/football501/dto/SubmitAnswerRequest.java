package com.football501.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for submitting an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotBlank
    private String answer;

    /**
     * The entity UUID from the autocomplete dropdown, or null if the player
     * typed a name without selecting a suggestion. When present, the backend
     * resolves it to a normalized name via the entities table for exact answer-key
     * matching, skipping fuzzy search entirely.
     */
    private UUID entityId;
}
