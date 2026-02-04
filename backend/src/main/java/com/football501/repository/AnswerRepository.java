package com.football501.repository;

import com.football501.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Answer entity.
 * Provides fuzzy matching using PostgreSQL trigram similarity.
 */
@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    /**
     * Find valid answer by question and exact answer key match.
     *
     * @param questionId the question UUID
     * @param answerKey the normalized answer key (lowercase)
     * @return optional answer
     */
    Optional<Answer> findByQuestionIdAndAnswerKey(UUID questionId, String answerKey);

    /**
     * Find best matching answer using PostgreSQL trigram similarity.
     * Excludes already-used answers and returns the best match above threshold.
     *
     * @param questionId the question UUID
     * @param normalizedInput the normalized input string (lowercase)
     * @param usedAnswerIds list of already-used answer IDs
     * @param threshold minimum similarity threshold (0.0 to 1.0)
     * @return optional answer with best similarity score
     */
    @Query(value = """
        SELECT *,
               similarity(answer_key, :normalizedInput) as sim
        FROM answers
        WHERE question_id = :questionId
          AND (:usedAnswerIds IS NULL OR id NOT IN (:usedAnswerIds))
          AND similarity(answer_key, :normalizedInput) >= :threshold
        ORDER BY sim DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Answer> findBestMatchByFuzzyName(
        @Param("questionId") UUID questionId,
        @Param("normalizedInput") String normalizedInput,
        @Param("usedAnswerIds") List<UUID> usedAnswerIds,
        @Param("threshold") double threshold
    );

    /**
     * Count available answers for a question (excluding used answers).
     *
     * @param questionId the question UUID
     * @param usedAnswerIds list of already-used answer IDs
     * @return count of available answers
     */
    @Query("""
        SELECT COUNT(a)
        FROM Answer a
        WHERE a.questionId = :questionId
          AND (:usedAnswerIds IS NULL OR a.id NOT IN :usedAnswerIds)
        """)
    long countAvailableAnswers(
        @Param("questionId") UUID questionId,
        @Param("usedAnswerIds") List<UUID> usedAnswerIds
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
        FROM Answer a
        WHERE a.questionId = :questionId
          AND (:excludeInvalidDarts = false OR a.isValidDarts = true)
        ORDER BY a.score DESC
        """)
    List<Answer> findTopAnswers(
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
    long countByQuestionIdAndIsValidDartsTrue(UUID questionId);

    /**
     * Get top N scoring answers for a question.
     *
     * @param questionId the question UUID
     * @param limit maximum number of results
     * @return list of top N answers ordered by score descending
     */
    @Query(value = """
        SELECT *
        FROM answers
        WHERE question_id = :questionId
        ORDER BY score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Answer> findTopNByQuestionIdOrderByScoreDesc(
        @Param("questionId") UUID questionId,
        @Param("limit") int limit
    );

    List<Answer> findByQuestionIdOrderByScoreDesc(UUID questionId);
    boolean existsByQuestionIdAndAnswerKey(UUID questionId, String answerKey);
}
