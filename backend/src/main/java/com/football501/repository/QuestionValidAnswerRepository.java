package com.football501.repository;

import com.football501.model.QuestionValidAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for QuestionValidAnswer entity.
 * Provides fuzzy matching using PostgreSQL trigram similarity.
 */
@Repository
public interface QuestionValidAnswerRepository extends JpaRepository<QuestionValidAnswer, UUID> {

    /**
     * Find valid answer by question and exact player name match.
     *
     * @param questionId the question UUID
     * @param normalizedName the normalized player name (lowercase)
     * @return optional answer
     */
    Optional<QuestionValidAnswer> findByQuestionIdAndNormalizedName(UUID questionId, String normalizedName);

    /**
     * Find best matching answer using PostgreSQL trigram similarity.
     * Excludes already-used players and returns the best match above threshold.
     *
     * @param questionId the question UUID
     * @param normalizedInput the normalized player input (lowercase)
     * @param usedPlayerIds list of already-used player IDs
     * @param threshold minimum similarity threshold (0.0 to 1.0)
     * @return optional answer with best similarity score
     */
    @Query(value = """
        SELECT *,
               similarity(normalized_name, :normalizedInput) as sim
        FROM question_valid_answers
        WHERE question_id = :questionId
          AND (:usedPlayerIds IS NULL OR player_id NOT IN (:usedPlayerIds))
          AND similarity(normalized_name, :normalizedInput) >= :threshold
        ORDER BY sim DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<QuestionValidAnswer> findBestMatchByFuzzyName(
        @Param("questionId") UUID questionId,
        @Param("normalizedInput") String normalizedInput,
        @Param("usedPlayerIds") List<UUID> usedPlayerIds,
        @Param("threshold") double threshold
    );

    /**
     * Count available answers for a question (excluding used players).
     *
     * @param questionId the question UUID
     * @param usedPlayerIds list of already-used player IDs
     * @return count of available answers
     */
    @Query("""
        SELECT COUNT(a)
        FROM QuestionValidAnswer a
        WHERE a.questionId = :questionId
          AND (:usedPlayerIds IS NULL OR a.playerId NOT IN :usedPlayerIds)
        """)
    long countAvailableAnswers(
        @Param("questionId") UUID questionId,
        @Param("usedPlayerIds") List<UUID> usedPlayerIds
    );

    /**
     * Get top scoring answers for a question.
     *
     * @param questionId the question UUID
     * @param excludeInvalidDarts whether to exclude invalid darts scores
     * @return list of top answers
     */
    @Query("""
        SELECT a
        FROM QuestionValidAnswer a
        WHERE a.questionId = :questionId
          AND (:excludeInvalidDarts = false OR a.isValidDartsScore = true)
        ORDER BY a.score DESC
        """)
    List<QuestionValidAnswer> findTopAnswers(
        @Param("questionId") UUID questionId,
        @Param("excludeInvalidDarts") boolean excludeInvalidDarts
    );

    /**
     * Count total answers for a question.
     *
     * @param questionId the question UUID
     * @return total answer count
     */
    long countByQuestionId(UUID questionId);

    /**
     * Count valid darts scores for a question.
     *
     * @param questionId the question UUID
     * @return count of valid darts scores
     */
    long countByQuestionIdAndIsValidDartsScoreTrue(UUID questionId);
}
