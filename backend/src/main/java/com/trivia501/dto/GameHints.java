package com.trivia501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hint statistics about the remaining answer pool for a player during an active game.
 * Sent as part of every {@link GameStateResponse} so the client can display contextual cues.
 *
 * <ul>
 *   <li>When the player's score is <strong>above 180</strong>, surface {@code maxScoresLeft}:
 *       the count of remaining unused answers worth exactly 180 points (the maximum
 *       single-dart score).  These are the highest-value moves still available.</li>
 *   <li>When the player's score is <strong>180 or below</strong>, surface {@code checkoutsLeft}:
 *       the count of remaining unused answers that would end the game in one move
 *       (i.e. bring the score to exactly 0 or within the −10 to 0 winning range).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameHints {

    /**
     * Count of remaining unused answers with {@code score = 180}.
     * Relevant while the player's score is above 180 — these are the best
     * possible moves to make maximum progress toward checkout.
     */
    private int maxScoresLeft;

    /**
     * Count of remaining unused answers whose score would bring the player's
     * current total to exactly 0 or within the winning range (−10 to 0 inclusive).
     * Relevant once the player's score drops to 180 or below.
     */
    private int checkoutsLeft;
}
