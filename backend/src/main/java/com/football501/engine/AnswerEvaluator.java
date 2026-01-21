package com.football501.engine;

import com.football501.model.Answer;
import com.football501.repository.AnswerRepository;
import lombok.extern.slf4j.Slf4j;
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
 * - Validate user input against pre-populated answers
 * - Perform fuzzy matching for typos and variations
 * - Apply darts scoring rules
 * - Determine win/bust conditions
 * - Track used answers to prevent reuse
 *
 * Critical: All validation uses pre-cached data from answers table.
 * NO external API calls are made during gameplay.
 */
@Service
@Slf4j
public class AnswerEvaluator {

    private final AnswerRepository answerRepository;
    private final ScoringService scoringService;

    /**
     * Minimum similarity threshold for fuzzy matching (0.0 to 1.0).
     * Adjust based on UX requirements - lower = more lenient matching.
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    public AnswerEvaluator(
        AnswerRepository answerRepository,
        ScoringService scoringService
    ) {
        this.answerRepository = answerRepository;
        this.scoringService = scoringService;
    }

    /**
     * Evaluate a player's answer during gameplay.
     *
     * @param questionId the question UUID
     * @param userInput the answer as entered by user
     * @param currentScore the player's current score (starts at 501)
     * @param usedAnswerIds list of already-used answer UUIDs
     * @return AnswerResult with validation details
     */
    @Transactional(readOnly = true)
    public AnswerResult evaluateAnswer(
        UUID questionId,
        String userInput,
        int currentScore,
        List<UUID> usedAnswerIds
    ) {
        // Normalize input
        String normalizedInput = normalizeInput(userInput);

        if (normalizedInput.isEmpty()) {
            return AnswerResult.invalid("Empty answer");
        }

        // Find matching answer using fuzzy matching
        Optional<Answer> matchOpt = findMatchingAnswer(
            questionId,
            normalizedInput,
            usedAnswerIds != null ? usedAnswerIds : new ArrayList<>()
        );

        if (matchOpt.isEmpty()) {
            return AnswerResult.invalid("Answer not found or already used");
        }

        Answer answer = matchOpt.get();

        // Calculate new score using ScoringService
        ScoreResult scoreResult = scoringService.calculateScore(currentScore, answer.getScore());

        // Determine bust condition
        boolean isBust = scoreResult.isBust() || !answer.getIsValidDarts();
        String reason = null;

        if (isBust) {
            if (!answer.getIsValidDarts()) {
                reason = "Invalid darts score";
            } else if (scoreResult.isBust()) {
                reason = "Bust";
            }
        } else if (scoreResult.isCheckout()) {
            reason = "Win!";
        }

        // Build result
        return AnswerResult.valid(
            answer.getDisplayText(),
            answer.getId(),
            answer.getScore(),
            answer.getIsValidDarts(),
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
     * @param normalizedInput the normalized input
     * @param usedAnswerIds list of used answer IDs
     * @return optional matching answer
     */
    private Optional<Answer> findMatchingAnswer(
        UUID questionId,
        String normalizedInput,
        List<UUID> usedAnswerIds
    ) {
        // Try exact match first (fast path)
        Optional<Answer> exactMatch = answerRepository
            .findByQuestionIdAndAnswerKey(questionId, normalizedInput);

        if (exactMatch.isPresent()) {
            // Check if already used
            if (usedAnswerIds != null && usedAnswerIds.contains(exactMatch.get().getId())) {
                log.debug("Exact match found but already used: {}", exactMatch.get().getDisplayText());
                return Optional.empty();
            }
            log.debug("Exact match found: {}", exactMatch.get().getDisplayText());
            return exactMatch;
        }

        // Fall back to fuzzy matching
        Optional<Answer> fuzzyMatch = answerRepository
            .findBestMatchByFuzzyName(
                questionId,
                normalizedInput,
                (usedAnswerIds == null || usedAnswerIds.isEmpty()) ? null : usedAnswerIds,
                SIMILARITY_THRESHOLD
            );

        if (fuzzyMatch.isPresent()) {
            log.debug("Fuzzy match found: {} for input '{}'",
                fuzzyMatch.get().getDisplayText(), normalizedInput);
        }

        return fuzzyMatch;
    }

    /**
     * Normalize input string for matching.
     * - Trim whitespace
     * - Convert to lowercase
     *
     * @param input raw input
     * @return normalized string
     */
    private String normalizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase();
    }

    /**
     * Get count of remaining valid answers for a question.
     *
     * @param questionId the question UUID
     * @param usedAnswerIds list of used answer IDs
     * @return count of available answers
     */
    @Transactional(readOnly = true)
    public long getAvailableAnswerCount(UUID questionId, List<UUID> usedAnswerIds) {
        return answerRepository.countAvailableAnswers(
            questionId,
            usedAnswerIds != null && !usedAnswerIds.isEmpty() ? usedAnswerIds : null
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
    public List<Answer> getTopAnswers(
        UUID questionId,
        int limit,
        boolean excludeInvalidDarts
    ) {
        List<Answer> results = answerRepository.findTopAnswers(
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
        long validDartsAnswers = answerRepository.countByQuestionIdAndIsValidDartsTrue(questionId);

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
