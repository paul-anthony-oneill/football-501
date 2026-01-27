package com.football501.service;

import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Service for managing questions and question selection.
 *
 * Responsibilities:
 * - Select random questions for games
 * - Ensure questions have sufficient answers
 * - Manage categories
 */
@Service
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AnswerRepository answerRepository;
    private final Random random;

    /**
     * Default minimum number of answers required for a question to be selectable.
     */
    private static final int DEFAULT_MIN_ANSWERS = 10;

    public QuestionService(
        QuestionRepository questionRepository,
        CategoryRepository categoryRepository,
        AnswerRepository answerRepository
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.answerRepository = answerRepository;
        this.random = new Random();
    }

    /**
     * Select a random active question for a category.
     * Uses default minimum answer count.
     *
     * @param categoryId the category UUID
     * @return optional question (empty if none available)
     */
    @Transactional(readOnly = true)
    public Optional<Question> selectRandomQuestion(UUID categoryId) {
        return selectRandomQuestion(categoryId, DEFAULT_MIN_ANSWERS);
    }

    /**
     * Select a random active question for a category with minimum answer requirement.
     *
     * @param categoryId the category UUID
     * @param minAnswers minimum number of answers required
     * @return optional question (empty if none available)
     */
    @Transactional(readOnly = true)
    public Optional<Question> selectRandomQuestion(UUID categoryId, int minAnswers) {
        log.debug("Selecting random question for category {} with minAnswers {}", categoryId, minAnswers);

        // Get all active questions for category
        List<Question> activeQuestions = questionRepository.findActiveByCategoryId(categoryId);

        if (activeQuestions.isEmpty()) {
            log.warn("No active questions found for category {}", categoryId);
            return Optional.empty();
        }

        // Filter questions that have sufficient answers
        List<Question> eligibleQuestions = activeQuestions.stream()
            .filter(q -> hasMinimumAnswers(q.getId(), minAnswers))
            .toList();

        if (eligibleQuestions.isEmpty()) {
            log.warn("No questions with sufficient answers ({}) for category {}", minAnswers, categoryId);
            return Optional.empty();
        }

        // Select random question
        Question selected = eligibleQuestions.get(random.nextInt(eligibleQuestions.size()));
        log.debug("Selected question: {} (ID: {})", selected.getQuestionText(), selected.getId());

        return Optional.of(selected);
    }

    /**
     * Get question by ID.
     *
     * @param questionId the question UUID
     * @return optional question
     */
    @Transactional(readOnly = true)
    public Optional<Question> getQuestionById(UUID questionId) {
        return questionRepository.findById(questionId);
    }

    /**
     * Check if a question has minimum number of answers.
     *
     * @param questionId the question UUID
     * @param minAnswers minimum required answers
     * @return true if question has sufficient answers
     */
    @Transactional(readOnly = true)
    public boolean hasMinimumAnswers(UUID questionId, int minAnswers) {
        long answerCount = answerRepository.countByQuestionId(questionId);
        return answerCount >= minAnswers;
    }

    /**
     * Get category by slug.
     *
     * @param slug the category slug (e.g., "football")
     * @return optional category
     */
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    /**
     * Get all active questions for a category.
     *
     * @param categoryId the category UUID
     * @return list of active questions
     */
    @Transactional(readOnly = true)
    public List<Question> getActiveQuestions(UUID categoryId) {
        return questionRepository.findActiveByCategoryId(categoryId);
    }

    /**
     * Get total count of answers for a question.
     *
     * @param questionId the question UUID
     * @return answer count
     */
    @Transactional(readOnly = true)
    public long getAnswerCount(UUID questionId) {
        return answerRepository.countByQuestionId(questionId);
    }
}
