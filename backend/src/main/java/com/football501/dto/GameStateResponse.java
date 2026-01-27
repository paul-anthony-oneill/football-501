package com.football501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for game state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {

    /**
     * Game ID.
     */
    private UUID gameId;

    /**
     * Match ID.
     */
    private UUID matchId;

    /**
     * Question ID.
     */
    private UUID questionId;

    /**
     * The question text.
     */
    private String questionText;

    /**
     * Current score (starts at 501).
     */
    private Integer currentScore;

    /**
     * Number of turns taken.
     */
    private Integer turnCount;

    /**
     * Game status (IN_PROGRESS, COMPLETED).
     */
    private String status;

    /**
     * Whether the player won (reached 0 or checkout range).
     */
    private Boolean isWin;

    /**
     * Turn timer in seconds.
     */
    private Integer turnTimerSeconds;
}
