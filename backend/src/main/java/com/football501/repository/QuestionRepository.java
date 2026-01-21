package com.football501.repository;

import com.football501.model.Question;
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
     * Find questions by stat type.
     *
     * @param statType the stat type
     * @return list of questions
     */
    List<Question> findByStatType(String statType);

    /**
     * Find active questions by competition.
     *
     * @param competitionId the competition UUID
     * @return list of questions
     */
    List<Question> findByCompetitionIdAndIsActiveTrue(UUID competitionId);
}
