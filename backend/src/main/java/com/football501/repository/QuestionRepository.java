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
 * Repository for Question entity.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /**
     * Find all active questions.
     *
     * @return list of active questions
     */
    List<Question> findByIsActiveTrue();

    /**
     * Find active questions by category.
     *
     * @param categoryId the category UUID
     * @return list of questions
     */
    List<Question> findByCategoryIdAndIsActiveTrue(UUID categoryId);

    /**
     * Find active questions by category and difficulty.
     *
     * @param categoryId the category UUID
     * @param difficulty the difficulty level
     * @return list of questions
     */
    List<Question> findByCategoryIdAndDifficultyAndIsActiveTrue(UUID categoryId, Integer difficulty);

    /**
     * Alias for findByCategoryIdAndIsActiveTrue.
     *
     * @param categoryId the category UUID
     * @return list of active questions
     */
    default List<Question> findActiveByCategoryId(UUID categoryId) {
        return findByCategoryIdAndIsActiveTrue(categoryId);
    }

    /**
     * Find active questions for a category that have at least minAnswers answers.
     * Single query — avoids N+1 from per-question COUNT calls.
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.categoryId = :categoryId
          AND q.isActive = true
          AND (SELECT COUNT(a) FROM Answer a WHERE a.questionId = q.id) >= :minAnswers
        """)
    List<Question> findActiveWithMinAnswers(
        @Param("categoryId") UUID categoryId,
        @Param("minAnswers") int minAnswers
    );

    /**
     * Find active questions for a category + difficulty that have at least minAnswers answers.
     * Single query — avoids N+1 from per-question COUNT calls.
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.categoryId = :categoryId
          AND q.isActive = true
          AND q.difficulty = :difficulty
          AND (SELECT COUNT(a) FROM Answer a WHERE a.questionId = q.id) >= :minAnswers
        """)
    List<Question> findActiveWithMinAnswersByDifficulty(
        @Param("categoryId") UUID categoryId,
        @Param("difficulty") Integer difficulty,
        @Param("minAnswers") int minAnswers
    );

    /**
     * Count questions by category ID.
     *
     * @param categoryId the category UUID
     * @return count of questions
     */
    long countByCategoryId(UUID categoryId);

    Page<Question> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Question> findByCategoryIdAndIsActive(UUID categoryId, Boolean isActive, Pageable pageable);
    Page<Question> findByIsActive(Boolean isActive, Pageable pageable);
}
