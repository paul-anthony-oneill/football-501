package com.football501.service;

import com.football501.dto.GameHints;
import com.football501.repository.AnswerRepository;
import com.football501.repository.GameMoveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Computes in-game hint statistics about the remaining answer pool.
 *
 * <p>Hints are re-computed after every move by running filtered {@code COUNT}
 * queries against the already-indexed {@code answers} table.  Each query is
 * sub-millisecond on typical answer-set sizes (hundreds of rows) and runs on
 * the same PostgreSQL connection as the rest of the game flow — no extra
 * round-trips or caching infrastructure is needed.
 *
 * <p>Two hint types are produced:
 * <ul>
 *   <li>{@code maxScoresLeft} — answers worth exactly 180 points (the maximum
 *       darts score).  Shown while the player's score is above 180 so they can
 *       see how many "max dart" moves are still available.</li>
 *   <li>{@code checkoutsLeft} — answers that would bring the player to exactly 0
 *       or within the winning range (−10 to 0).  Shown once the score is 180 or
 *       below so the player knows how many one-move wins remain.</li>
 * </ul>
 */
@Service
@Slf4j
public class GameHintsService {

    /**
     * The winning range extends {@value} points below zero.
     * An answer that brings the score to −10 through 0 is a valid checkout.
     */
    private static final int CHECKOUT_WINDOW = 10;

    private final AnswerRepository answerRepository;
    private final GameMoveRepository gameMoveRepository;

    public GameHintsService(AnswerRepository answerRepository, GameMoveRepository gameMoveRepository) {
        this.answerRepository = answerRepository;
        this.gameMoveRepository = gameMoveRepository;
    }

    /**
     * Compute hint statistics for a player in an active game.
     *
     * <p>Already-used answer IDs are fetched from {@code game_moves} and excluded
     * from both counts so the hints reflect genuinely available moves.
     *
     * @param gameId       the game UUID — used to look up already-used answers
     * @param questionId   the question UUID — used to scope the answer queries
     * @param currentScore the player's current score
     * @return hint statistics for the remaining answer pool
     */
    @Transactional(readOnly = true)
    public GameHints computeHints(UUID gameId, UUID questionId, int currentScore) {
        List<UUID> usedAnswerIds = gameMoveRepository.findUsedAnswerIdsByGameId(gameId);
        return computeHints(gameId, questionId, currentScore, usedAnswerIds);
    }

    /**
     * Compute hint statistics for a player in an active game, using
     * caller-supplied used-answer IDs to avoid a redundant database query.
     *
     * @param gameId        the game UUID (for logging only)
     * @param questionId    the question UUID — used to scope the answer queries
     * @param currentScore  the player's current score
     * @param usedAnswerIds already-fetched used answer IDs (must not be null)
     * @return hint statistics for the remaining answer pool
     */
    @Transactional(readOnly = true)
    public GameHints computeHints(UUID gameId, UUID questionId, int currentScore, List<UUID> usedAnswerIds) {

        int maxScoresLeft = (int) answerRepository.countRemainingMaxScores(questionId, usedAnswerIds);

        // Checkout answers: score ∈ [currentScore, currentScore + CHECKOUT_WINDOW]
        // Subtracting such a score lands the player in [−10, 0] → win condition.
        // Guard currentScore > 0 so a completed game (score ≤ 0) always reports 0.
        int checkoutsLeft = 0;
        if (currentScore > 0) {
            checkoutsLeft = (int) answerRepository.countRemainingCheckouts(
                questionId,
                currentScore,
                currentScore + CHECKOUT_WINDOW,
                usedAnswerIds
            );
        }

        log.debug("Hints [game={}]: maxScoresLeft={}, checkoutsLeft={} (score={})",
            gameId, maxScoresLeft, checkoutsLeft, currentScore);

        return GameHints.builder()
            .maxScoresLeft(maxScoresLeft)
            .checkoutsLeft(checkoutsLeft)
            .build();
    }
}
