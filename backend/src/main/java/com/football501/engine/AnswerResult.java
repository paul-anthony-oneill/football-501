package com.football501.engine;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Result of answer validation during gameplay.
 * Contains all information needed to update game state and notify players.
 */
@Data
@Builder
public class AnswerResult {

    /**
     * Whether the answer was found in the database for this question.
     */
    private final boolean valid;

    /**
     * The matched answer display text (null if invalid).
     */
    private final String displayText;

    /**
     * The answer UUID (null if invalid).
     * Used to track used answers.
     */
    private final UUID answerId;

    /**
     * The score for this answer (appearances/goals/combined).
     */
    private final Integer score;

    /**
     * Whether the score is achievable with 3 darts in standard 501.
     */
    private final boolean validDartsScore;

    /**
     * Whether this turn resulted in a bust.
     * Bust = no score deducted, turn wasted.
     */
    private final boolean bust;

    /**
     * The new total score after deduction.
     * If bust, this equals the current score (unchanged).
     */
    private final int newTotal;

    /**
     * Whether the player won with this answer.
     * Win = new total in range [-10, 0].
     */
    private final boolean win;

    /**
     * Reason for invalid/bust/win.
     * Examples: "Answer not found", "Already used", "Invalid darts score", "Win!"
     */
    private final String reason;

    /**
     * Fuzzy match similarity score (0.0 to 1.0).
     * Null if not applicable.
     */
    private final Double similarity;

    /**
     * Create an invalid answer result.
     *
     * @param reason the reason why answer is invalid
     * @return invalid AnswerResult
     */
    public static AnswerResult invalid(String reason) {
        return AnswerResult.builder()
            .valid(false)
            .bust(false)
            .win(false)
            .newTotal(0)
            .validDartsScore(false)
            .reason(reason)
            .build();
    }

    /**
     * Create a valid answer result.
     *
     * @param displayText the matched answer text
     * @param answerId the answer UUID
     * @param score the answer score
     * @param validDartsScore whether score is valid in darts
     * @param bust whether turn is a bust
     * @param newTotal the new score after deduction
     * @param win whether player won
     * @param reason optional reason (for bust/win)
     * @param similarity fuzzy match similarity
     * @return valid AnswerResult
     */
    public static AnswerResult valid(
        String displayText,
        UUID answerId,
        int score,
        boolean validDartsScore,
        boolean bust,
        int newTotal,
        boolean win,
        String reason,
        Double similarity
    ) {
        return AnswerResult.builder()
            .valid(true)
            .displayText(displayText)
            .answerId(answerId)
            .score(score)
            .validDartsScore(validDartsScore)
            .bust(bust)
            .newTotal(newTotal)
            .win(win)
            .reason(reason)
            .similarity(similarity)
            .build();
    }
}
