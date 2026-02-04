package com.football501.repository;

import com.football501.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Alias for findByCategoryIdAndIsActiveTrue.
     *
     * @param categoryId the category UUID
     * @return list of active questions
     */
    default List<Question> findActiveByCategoryId(UUID categoryId) {
        return findByCategoryIdAndIsActiveTrue(categoryId);
    }

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
