package com.football501.service;

import com.football501.dto.GameHints;
import com.football501.repository.AnswerRepository;
import com.football501.repository.GameMoveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Computes in-game hint statistics about the remaining answer pool.
 *
 * <p>Answer scores are loaded once at game start into an in-memory cache
 * ({@code answerId → score}) so that subsequent hint computations are pure
 * in-memory operations — zero database queries during gameplay.
 *
 * <p>Two hint types are produced:
 * <ul>
 *   <li>{@code maxScoresLeft} — answers worth exactly 180 points (the maximum
 *       darts score).</li>
 *   <li>{@code checkoutsLeft} — answers whose score would bring the player within
 *       the winning range (score ∈ [current, current + 10]).</li>
 * </ul>
 */
@Service
@Slf4j
public class GameHintsService {

    private static final int CHECKOUT_WINDOW = 10;
    private static final int MAX_SCORE = 180;

    private final AnswerRepository answerRepository;
    private final GameMoveRepository gameMoveRepository;

    /** Per-question cache of answerId → score for valid non-bust answers. */
    private final Map<UUID, Map<UUID, Integer>> scoreCaches = new ConcurrentHashMap<>();

    public GameHintsService(AnswerRepository answerRepository, GameMoveRepository gameMoveRepository) {
        this.answerRepository = answerRepository;
        this.gameMoveRepository = gameMoveRepository;
    }

    /**
     * Pre-load the score cache for a question. Call once at game start.
     * Subsequent calls for the same question return immediately (cache hit).
     */
    @Transactional(readOnly = true)
    public void loadScoreCache(UUID questionId) {
        scoreCaches.computeIfAbsent(questionId, qid -> {
            List<Object[]> rows = answerRepository.findValidNonBustAnswerScores(qid);
            Map<UUID, Integer> cache = rows.stream()
                .collect(Collectors.toMap(
                    row -> (UUID) row[0],
                    row -> ((Number) row[1]).intValue()
                ));
            log.debug("Score cache loaded: questionId={}, entries={}", qid, cache.size());
            return cache;
        });
    }

    /**
     * Compute hints from the in-memory score cache — zero database queries.
     *
     * @param questionId    the question UUID (cache key)
     * @param usedAnswerIds answer IDs already used (excluded from counts)
     * @param currentScore  the player's current score
     */
    public GameHints computeHintsFromCache(UUID questionId, List<UUID> usedAnswerIds, int currentScore) {
        Map<UUID, Integer> cache = scoreCaches.get(questionId);
        if (cache == null) {
            log.warn("Score cache miss for question {} — falling back to DB query", questionId);
            return computeHints(questionId, questionId, currentScore, usedAnswerIds);
        }

        int maxScoresLeft = 0;
        int checkoutsMin = currentScore;
        int checkoutsMax = currentScore + CHECKOUT_WINDOW;
        int checkoutsLeft = 0;

        for (Map.Entry<UUID, Integer> entry : cache.entrySet()) {
            if (usedAnswerIds != null && usedAnswerIds.contains(entry.getKey())) {
                continue;
            }
            int score = entry.getValue();
            if (score == MAX_SCORE) {
                maxScoresLeft++;
            }
            if (currentScore > 0 && score >= checkoutsMin && score <= checkoutsMax) {
                checkoutsLeft++;
            }
        }

        log.debug("Hints [cache]: maxScoresLeft={}, checkoutsLeft={} (score={})",
            maxScoresLeft, checkoutsLeft, currentScore);

        return GameHints.builder()
            .maxScoresLeft(maxScoresLeft)
            .checkoutsLeft(checkoutsLeft)
            .build();
    }

    /** Remove the score cache for a question (call when game ends). */
    public void evictScoreCache(UUID questionId) {
        scoreCaches.remove(questionId);
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
