package com.football501.repository;

import com.football501.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for the {@link Question} entity.
 *
 * <p>All "active question" queries filter on {@code status = 'active'}.
 * Use {@link Question#STATUS_ACTIVE} rather than hard-coding the string in callers.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    // ── Simple status/category lookups ────────────────────────────────────────

    List<Question> findByStatus(String status);

    List<Question> findByCategoryIdAndStatus(UUID categoryId, String status);

    List<Question> findByCategoryIdAndDifficultyAndStatus(UUID categoryId, Integer difficulty, String status);

    /**
     * Convenience alias — returns all questions with {@code status = 'active'}
     * for the given category.
     */
    default List<Question> findActiveByCategoryId(UUID categoryId) {
        return findByCategoryIdAndStatus(categoryId, Question.STATUS_ACTIVE);
    }

    // ── Paged lookups ─────────────────────────────────────────────────────────

    Page<Question> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Question> findByCategoryIdAndStatus(UUID categoryId, String status, Pageable pageable);

    Page<Question> findByStatus(String status, Pageable pageable);

    // ── Answer-count-aware lookups (avoids N+1) ───────────────────────────────

    /**
     * Returns active questions for a category that have at least {@code minAnswers}
     * pre-materialised answers. Uses a single correlated subquery to avoid N+1.
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.categoryId = :categoryId
          AND q.status     = 'active'
          AND (SELECT COUNT(a) FROM Answer a WHERE a.questionId = q.id) >= :minAnswers
        """)
    List<Question> findActiveWithMinAnswers(
        @Param("categoryId") UUID categoryId,
        @Param("minAnswers") int minAnswers
    );

    /**
     * Same as {@link #findActiveWithMinAnswers} with an additional difficulty filter.
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.categoryId = :categoryId
          AND q.status     = 'active'
          AND q.difficulty = :difficulty
          AND (SELECT COUNT(a) FROM Answer a WHERE a.questionId = q.id) >= :minAnswers
        """)
    List<Question> findActiveWithMinAnswersByDifficulty(
        @Param("categoryId") UUID categoryId,
        @Param("difficulty") Integer difficulty,
        @Param("minAnswers") int minAnswers
    );

    // ── Counts ────────────────────────────────────────────────────────────────

    long countByCategoryId(UUID categoryId);
}
