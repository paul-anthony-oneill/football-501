package com.football501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for submitting an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {

    /**
     * The result of the move (VALID, BUST, INVALID, CHECKOUT).
     */
    private String result;

    /**
     * The matched answer display text (if valid).
     */
    private String matchedAnswer;

    /**
     * The score value from the answer.
     */
    private Integer scoreValue;

    /**
     * Score before this move.
     */
    private Integer scoreBefore;

    /**
     * Score after this move.
     */
    private Integer scoreAfter;

    /**
     * Reason for the result (e.g., "Invalid darts score", "Answer not found").
     */
    private String reason;

    /**
     * Whether this move resulted in a win.
     */
    private Boolean isWin;

    /**
     * Updated game state after this move.
     */
    private GameStateResponse gameState;
}
