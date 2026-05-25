package com.football501.service;

import com.football501.dto.admin.CreateQuestionRequest;
import com.football501.dto.admin.QuestionListResponse;
import com.football501.dto.admin.QuestionResponse;
import com.football501.dto.admin.UpdateQuestionRequest;
import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminQuestionService {

    private static final Set<String> VALID_STATUSES = Set.of(
        Question.STATUS_DRAFT, Question.STATUS_ACTIVE, Question.STATUS_RETIRED
    );

    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AnswerRepository answerRepository;

    public AdminQuestionService(
            QuestionRepository questionRepository,
            CategoryRepository categoryRepository,
            AnswerRepository answerRepository
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.answerRepository = answerRepository;
    }

    /**
     * Creates a new question with {@code status = 'draft'}.
     * The admin must explicitly promote it to {@code 'active'} (which will trigger
     * materialisation) before it enters the game rotation.
     */
    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new IllegalArgumentException("Category not found with id: " + request.getCategoryId());
        }

        Question question = Question.builder()
                .categoryId(request.getCategoryId())
                .questionText(request.getQuestionText())
                .metricKey(request.getMetricKey())
                .config(request.getConfig())
                .minScore(request.getMinScore())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 2)
                .status(Question.STATUS_DRAFT)
                .build();

        Question saved = questionRepository.save(question);
        log.info("Created new question (draft): {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public QuestionResponse updateQuestion(UUID id, UpdateQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        if (!question.getCategoryId().equals(request.getCategoryId())) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new IllegalArgumentException("Category not found with id: " + request.getCategoryId());
            }
        }

        question.setCategoryId(request.getCategoryId());
        question.setQuestionText(request.getQuestionText());
        question.setMetricKey(request.getMetricKey());
        question.setConfig(request.getConfig());
        question.setMinScore(request.getMinScore());
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }

        Question saved = questionRepository.save(question);
        log.info("Updated question: {}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Transitions a question to a new lifecycle status.
     *
     * <p>Valid values: {@code "draft"}, {@code "active"}, {@code "retired"}.
     * Promoting to {@code "active"} is the trigger point for future materialiser
     * integration (not yet wired — materialisation is currently manual via
     * the bulk-import endpoints).
     */
    @Transactional
    public QuestionResponse updateStatus(UUID id, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException(
                "Invalid status '" + newStatus + "'. Must be one of: " + VALID_STATUSES
            );
        }

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        String oldStatus = question.getStatus();
        question.setStatus(newStatus);
        Question saved = questionRepository.save(question);
        log.info("Question {} status: {} → {}", id, oldStatus, newStatus);
        return mapToResponse(saved);
    }

    /**
     * Lists questions with optional filters on category and status.
     *
     * @param categoryId filter by category UUID (nullable)
     * @param status     filter by status string, e.g. {@code "active"} (nullable)
     * @param pageable   pagination / sorting
     */
    @Transactional(readOnly = true)
    public QuestionListResponse listQuestions(UUID categoryId, String status, Pageable pageable) {
        Page<Question> page;

        if (categoryId != null && status != null) {
            page = questionRepository.findByCategoryIdAndStatus(categoryId, status, pageable);
        } else if (categoryId != null) {
            page = questionRepository.findByCategoryId(categoryId, pageable);
        } else if (status != null) {
            page = questionRepository.findByStatus(status, pageable);
        } else {
            page = questionRepository.findAll(pageable);
        }

        List<QuestionResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        QuestionListResponse response = new QuestionListResponse();
        response.setContent(content);
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setCurrentPage(page.getNumber());

        return response;
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));
        return mapToResponse(question);
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        if (!questionRepository.existsById(id)) {
            throw new IllegalArgumentException("Question not found with id: " + id);
        }
        questionRepository.deleteById(id);
        log.info("Deleted question: {}", id);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private QuestionResponse mapToResponse(Question question) {
        String categoryName = categoryRepository.findById(question.getCategoryId())
                .map(Category::getName)
                .orElse("Unknown");

        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setCategoryId(question.getCategoryId());
        response.setCategoryName(categoryName);
        response.setQuestionText(question.getQuestionText());
        response.setMetricKey(question.getMetricKey());
        response.setConfig(question.getConfig());
        response.setMinScore(question.getMinScore());
        response.setDifficulty(question.getDifficulty());
        response.setStatus(question.getStatus());
        response.setTemplateId(question.getTemplateId());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
        response.setAnswerCount(answerRepository.countByQuestionId(question.getId()));
        response.setValidDartsCount(answerRepository.countByQuestionIdAndIsValidDartsTrue(question.getId()));

        return response;
    }
}
