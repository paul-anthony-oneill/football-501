package com.football501.service;

import com.football501.dto.admin.CreateQuestionRequest;
import com.football501.dto.admin.DifficultyLockRequest;
import com.football501.dto.admin.QuestionListResponse;
import com.football501.dto.admin.QuestionResponse;
import com.football501.dto.admin.UpdateQuestionRequest;
import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminQuestionService {

    private static final Set<String> VALID_STATUSES = Set.of(
        Question.STATUS_DRAFT, Question.STATUS_ACTIVE, Question.STATUS_RETIRED, Question.STATUS_EXCLUDED
    );

    private final QuestionRepository              questionRepository;
    private final CategoryRepository              categoryRepository;
    private final AnswerRepository                answerRepository;
    private final QuestionMaterializerService     materializerService;
    private final DifficultyRecalibrationService  recalibrationService;

    /**
     * {@code @Lazy} on the materializer service breaks the circular dependency
     * that would occur if any materializer depends on question/answer services.
     */
    public AdminQuestionService(
            QuestionRepository                   questionRepository,
            CategoryRepository                   categoryRepository,
            AnswerRepository                     answerRepository,
            @Lazy QuestionMaterializerService    materializerService,
            DifficultyRecalibrationService       recalibrationService
    ) {
        this.questionRepository  = questionRepository;
        this.categoryRepository  = categoryRepository;
        this.answerRepository    = answerRepository;
        this.materializerService = materializerService;
        this.recalibrationService = recalibrationService;
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
     *
     * <p><b>Materialisation hook:</b> when a question transitions from
     * {@code "draft"} to {@code "active"}, the materializer is automatically
     * invoked to compute and upsert the answer set.  If no materializer is
     * registered for the question's template key, the status is still updated
     * (the question will have 0 answers until manually populated).
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

        // Auto-materialise when promoting from draft → active.
        if (Question.STATUS_DRAFT.equals(oldStatus) && Question.STATUS_ACTIVE.equals(newStatus)) {
            triggerMaterialization(saved);
        }

        return mapToResponse(saved);
    }

    /**
     * Triggers materialisation for a question that has just been activated.
     * Errors are caught and logged but do not roll back the status change.
     */
    private void triggerMaterialization(Question question) {
        // Only attempt if the question has a template with a known materializer key.
        if (question.getTemplateId() == null) {
            log.info("Question {} is hand-curated — skipping auto-materialisation.", question.getId());
            return;
        }
        try {
            int count = materializerService.materialize(question);
            log.info("Auto-materialised question {}: {} answers computed.", question.getId(), count);
        } catch (IllegalStateException ex) {
            // No materializer registered — log as a warning, not an error.
            log.warn("Question {} activated but no materializer found: {}",
                question.getId(), ex.getMessage());
        } catch (Exception ex) {
            // Non-fatal: status was saved, but answers must be populated manually.
            log.error("Materialisation failed for question {} after activation: {}",
                question.getId(), ex.getMessage(), ex);
        }
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

    /**
     * Re-materialises the answers for an already-active question.
     *
     * <p>This is useful when underlying {@code player_season_stints} data has
     * been refreshed by the scraper and the cached answers need to be updated
     * without cycling the question through {@code retired} and back to
     * {@code active}.
     *
     * @param id the question UUID
     * @return the number of answer rows upserted
     * @throws IllegalArgumentException  if the question does not exist
     * @throws IllegalStateException     if the question is not {@code active}
     */
    @Transactional
    public int rematerialize(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        if (!Question.STATUS_ACTIVE.equals(question.getStatus())) {
            throw new IllegalStateException(
                "Only active questions can be re-materialized. Current status: " + question.getStatus());
        }

        int count = materializerService.materialize(question);
        log.info("Re-materialized question {}: {} answers upserted.", id, count);
        return count;
    }

    /**
     * Activates up to {@code limit} draft questions in one call, materialising
     * each one as it is promoted.
     *
     * <p>Each question is activated in its own transaction (via
     * {@link #updateStatus}).  Failures on individual questions are caught,
     * logged, and collected in the result — they do not abort the batch.
     *
     * <p>This method is <em>not</em> itself {@code @Transactional} so that each
     * individual {@link #updateStatus} call commits independently rather than
     * being rolled back together if one question fails.
     *
     * @param limit maximum number of draft questions to activate in this call
     * @return summary map: {@code activated}, {@code answersUpserted},
     *         {@code errors}, {@code remainingDraft}
     */
    public Map<String, Object> bulkActivateDraft(int limit) {
        List<Question> drafts = questionRepository
                .findByStatus(Question.STATUS_DRAFT,
                        org.springframework.data.domain.PageRequest.of(0, limit))
                .getContent();

        int activated      = 0;
        int answersUpserted = 0;
        List<String> errors = new ArrayList<>();

        log.info("Bulk activate: {} draft questions selected (limit={})", drafts.size(), limit);

        for (Question q : drafts) {
            try {
                QuestionResponse result = updateStatus(q.getId(), Question.STATUS_ACTIVE);
                activated++;
                answersUpserted += result.getAnswerCount();
            } catch (Exception ex) {
                String msg = "Question " + q.getId() + ": " + ex.getMessage();
                log.error("Bulk activate failed for {}: {}", q.getId(), ex.getMessage());
                errors.add(msg);
            }
        }

        long remainingDraft = questionRepository.findByStatus(Question.STATUS_DRAFT).size();

        log.info("Bulk activate complete: {} activated, {} answers upserted, {} errors, {} draft remaining",
                activated, answersUpserted, errors.size(), remainingDraft);

        return Map.of(
                "activated",       activated,
                "answersUpserted", answersUpserted,
                "errors",          errors.size(),
                "remainingDraft",  remainingDraft
        );
    }

    // ── Difficulty endpoints ──────────────────────────────────────────────────

    /**
     * Bulk-recalculates difficulty scores and viability for all unlocked questions
     * using stored zone counts. Does not touch the answers table.
     *
     * @return summary of how many questions were processed, updated, and re-excluded
     */
    @Transactional
    public DifficultyRecalibrationService.RecalibrationResult recalculateDifficulty() {
        return recalibrationService.recalculateAll();
    }

    /**
     * Locks or unlocks a question's difficulty score.
     *
     * <p>When locked, the recalibration job skips this question. If a
     * {@code difficultyScore} override is provided alongside {@code locked = true},
     * that score is applied immediately.
     *
     * @param id      question UUID
     * @param request lock/unlock request body
     * @return updated question response
     */
    @Transactional
    public QuestionResponse lockDifficulty(UUID id, DifficultyLockRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        question.setDifficultyLocked(request.getLocked());

        if (Boolean.TRUE.equals(request.getLocked()) && request.getDifficultyScore() != null) {
            question.setDifficultyScore(request.getDifficultyScore());
            log.info("Question {} difficulty locked with override score: {}",
                id, request.getDifficultyScore());
        } else {
            log.info("Question {} difficulty lock set to: {}", id, request.getLocked());
        }

        Question saved = questionRepository.save(question);
        return mapToResponse(saved);
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
        response.setTotalPointsPool(answerRepository.sumValidDartsScores(question.getId()));
        response.setHighValueAnswerCount(answerRepository.countHighValueAnswers(question.getId()));

        // Difficulty metrics (stored on the question row — no extra queries)
        response.setHighValueCount(question.getHighValueCount());
        response.setMidRangeCount(question.getMidRangeCount());
        response.setCheckoutCount(question.getCheckoutCount());
        response.setTotalValidCount(question.getTotalValidCount());
        response.setTotalScorePool(question.getTotalScorePool());
        response.setSingleQuestionViable(question.isSingleQuestionViable());
        response.setViabilityExclusionReason(question.getViabilityExclusionReason());
        response.setDifficultyScore(question.getDifficultyScore());
        response.setDifficultyLocked(question.isDifficultyLocked());

        return response;
    }
}
