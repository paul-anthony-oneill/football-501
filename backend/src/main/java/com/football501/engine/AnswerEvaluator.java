package com.football501.engine;

import com.football501.model.QuestionValidAnswer;
import com.football501.repository.QuestionValidAnswerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for evaluating player answers during gameplay.
 *
 * Core responsibilities:
 * - Validate player input against pre-populated answers
 * - Perform fuzzy matching for typos and variations
 * - Apply darts scoring rules
 * - Determine win/bust conditions
 * - Track used answers to prevent reuse
 *
 * Critical: All validation uses pre-cached data from question_valid_answers table.
 * NO external API calls are made during gameplay.
 */
@Service
@Slf4j
public class AnswerEvaluator {

    private final QuestionValidAnswerRepository answerRepository;
    private final ScoringService scoringService;

    /**
     * Minimum similarity threshold for fuzzy matching (0.0 to 1.0).
     * Adjust based on UX requirements - lower = more lenient matching.
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * Checkout range constants.
     */
    private static final int CHECKOUT_MIN = -10;
    private static final int CHECKOUT_MAX = 0;

    public AnswerEvaluator(
        QuestionValidAnswerRepository answerRepository,
        ScoringService scoringService
    ) {
        this.answerRepository = answerRepository;
        this.scoringService = scoringService;
    }

    /**
     * Evaluate a player's answer during gameplay.
     *
     * @param questionId the question UUID
     * @param playerInput the player name as entered by user
     * @param currentScore the player's current score (starts at 501)
     * @param usedPlayerIds set of already-used player UUIDs
     * @return AnswerResult with validation details
     */
    @Transactional(readOnly = true)
    public AnswerResult evaluateAnswer(
        UUID questionId,
        String playerInput,
        int currentScore,
        List<UUID> usedPlayerIds
    ) {
        // Normalize input
        String normalizedInput = normalizePlayerName(playerInput);

        if (normalizedInput.isEmpty()) {
            return AnswerResult.invalid("Empty answer");
        }

        // Find matching answer using fuzzy matching
        Optional<QuestionValidAnswer> matchOpt = findMatchingAnswer(
            questionId,
            normalizedInput,
            usedPlayerIds != null ? usedPlayerIds : new ArrayList<>()
        );

        if (matchOpt.isEmpty()) {
            return AnswerResult.invalid("Player not found or already used");
        }

        QuestionValidAnswer answer = matchOpt.get();

        // Calculate new score using ScoringService
        ScoreResult scoreResult = scoringService.calculateScore(currentScore, answer.getScore());

        // Determine bust condition
        boolean isBust = scoreResult.isBust() || !answer.getIsValidDartsScore();
        String reason = null;

        if (isBust) {
            if (!answer.getIsValidDartsScore()) {
                reason = "Invalid darts score";
            } else if (scoreResult.isBust()) {
                reason = "Bust";
            }
        } else if (scoreResult.isCheckout()) {
            reason = "Win!";
        }

        // Build result
        return AnswerResult.valid(
            answer.getPlayerName(),
            answer.getPlayerId(),
            answer.getScore(),
            answer.getIsValidDartsScore(),
            isBust,
            scoreResult.getNewScore(),
            scoreResult.isCheckout(),
            reason,
            null // Similarity not currently returned by repository
        );
    }

    /**
     * Find matching answer using exact or fuzzy matching.
     *
     * Strategy:
     * 1. Try exact normalized match first (fast path)
     * 2. Fall back to fuzzy trigram matching (handles typos)
     *
     * @param questionId the question UUID
     * @param normalizedInput the normalized player input
     * @param usedPlayerIds list of used player IDs
     * @return optional matching answer
     */
    private Optional<QuestionValidAnswer> findMatchingAnswer(
        UUID questionId,
        String normalizedInput,
        List<UUID> usedPlayerIds
    ) {
        // Try exact match first (fast path)
        Optional<QuestionValidAnswer> exactMatch = answerRepository
            .findByQuestionIdAndNormalizedName(questionId, normalizedInput);

        if (exactMatch.isPresent() && !usedPlayerIds.contains(exactMatch.get().getPlayerId())) {
            log.debug("Exact match found: {}", exactMatch.get().getPlayerName());
            return exactMatch;
        }

        // Fall back to fuzzy matching
        Optional<QuestionValidAnswer> fuzzyMatch = answerRepository
            .findBestMatchByFuzzyName(
                questionId,
                normalizedInput,
                usedPlayerIds.isEmpty() ? null : usedPlayerIds,
                SIMILARITY_THRESHOLD
            );

        if (fuzzyMatch.isPresent()) {
            log.debug("Fuzzy match found: {} for input '{}'",
                fuzzyMatch.get().getPlayerName(), normalizedInput);
        }

        return fuzzyMatch;
    }

    /**
     * Normalize player name for matching.
     * - Trim whitespace
     * - Convert to lowercase
     *
     * @param input raw player input
     * @return normalized name
     */
    private String normalizePlayerName(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase();
    }

    /**
     * Get count of remaining valid answers for a question.
     *
     * @param questionId the question UUID
     * @param usedPlayerIds list of used player IDs
     * @return count of available answers
     */
    @Transactional(readOnly = true)
    public long getAvailableAnswerCount(UUID questionId, List<UUID> usedPlayerIds) {
        return answerRepository.countAvailableAnswers(
            questionId,
            usedPlayerIds != null && !usedPlayerIds.isEmpty() ? usedPlayerIds : null
        );
    }

    /**
     * Get top scoring answers for a question.
     * Useful for analytics, testing, or admin purposes.
     *
     * @param questionId the question UUID
     * @param limit maximum number of results
     * @param excludeInvalidDarts whether to exclude invalid darts scores
     * @return list of top answers
     */
    @Transactional(readOnly = true)
    public List<QuestionValidAnswer> getTopAnswers(
        UUID questionId,
        int limit,
        boolean excludeInvalidDarts
    ) {
        List<QuestionValidAnswer> results = answerRepository.findTopAnswers(
            questionId,
            excludeInvalidDarts
        );

        // Manually limit results since @Query doesn't support dynamic LIMIT
        return results.stream().limit(limit).toList();
    }

    /**
     * Get statistics about a question's answers.
     *
     * @param questionId the question UUID
     * @return answer statistics
     */
    @Transactional(readOnly = true)
    public AnswerStats getAnswerStats(UUID questionId) {
        long totalAnswers = answerRepository.countByQuestionId(questionId);
        long validDartsAnswers = answerRepository.countByQuestionIdAndIsValidDartsScoreTrue(questionId);

        return new AnswerStats(totalAnswers, validDartsAnswers);
    }

    /**
     * Simple record for answer statistics.
     */
    public record AnswerStats(long totalAnswers, long validDartsAnswers) {
        public long invalidOrBustAnswers() {
            return totalAnswers - validDartsAnswers;
        }
    }
}
