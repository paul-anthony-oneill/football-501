package com.football501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    /**
     * The player's answer (e.g., "Erling Haaland").
     */
    private String answer;
}
