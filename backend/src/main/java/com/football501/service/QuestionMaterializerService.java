package com.football501.service;

import com.football501.engine.DartsValidator;
import com.football501.engine.DifficultyCalculator;
import com.football501.engine.DifficultyConstants;
import com.football501.materializer.MaterializationContext;
import com.football501.materializer.MaterializedAnswer;
import com.football501.materializer.QuestionMaterializer;
import com.football501.model.Answer;
import com.football501.model.EntityType;
import com.football501.model.Question;
import com.football501.model.QuestionTemplate;
import com.football501.repository.AnswerRepository;
import com.football501.repository.QuestionRepository;
import com.football501.repository.QuestionTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dispatches materialisation requests to the correct {@link QuestionMaterializer}
 * implementation and upserts the resulting answer rows.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Auto-discover all {@link QuestionMaterializer} beans at startup.</li>
 *   <li>Resolve the materializer by key from the question's template.</li>
 *   <li>Call {@link QuestionMaterializer#materialize} with a populated context.</li>
 *   <li>Upsert returned rows into {@code answers} (and {@code entities} via
 *       {@link EntitySearchService}).</li>
 * </ol>
 *
 * <h3>When is this called?</h3>
 * <ul>
 *   <li>Admin promotes a draft question to {@code "active"} via the status endpoint.</li>
 *   <li>The stale-answer detector finds answers whose {@code materialized_at} is
 *       older than the stint {@code updated_at} (future job).</li>
 *   <li>Admin triggers a manual re-materialise for a specific question.</li>
 * </ul>
 */
@Service
@Slf4j
public class QuestionMaterializerService {

    private final Map<String, QuestionMaterializer> materializersByKey;
    private final AnswerRepository                  answerRepository;
    private final QuestionRepository                questionRepository;
    private final QuestionTemplateRepository        templateRepository;
    private final EntitySearchService               entitySearchService;

    /**
     * Spring injects all {@link QuestionMaterializer} beans from the application
     * context.  This makes registration automatic — just add a new {@code @Component}
     * implementation and it will be picked up.
     */
    public QuestionMaterializerService(
            List<QuestionMaterializer>    materializers,
            AnswerRepository              answerRepository,
            QuestionRepository            questionRepository,
            QuestionTemplateRepository    templateRepository,
            EntitySearchService           entitySearchService
    ) {
        this.materializersByKey = materializers.stream()
            .collect(Collectors.toMap(QuestionMaterializer::getMaterializerKey, Function.identity()));
        this.answerRepository    = answerRepository;
        this.questionRepository  = questionRepository;
        this.templateRepository  = templateRepository;
        this.entitySearchService = entitySearchService;

        log.info("QuestionMaterializerService: {} materializer(s) registered: {}",
            materializersByKey.size(), materializersByKey.keySet());
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Materialises a question: computes answer rows and upserts them into
     * {@code answers} and {@code entities}.
     *
     * <p>For hand-curated questions (no template), a materializer is still
     * required; the question's {@code metric_key} is used and params come
     * from {@code question.config}.
     *
     * @param question the question to materialise
     * @throws IllegalStateException if no materializer is registered for the key
     */
    @Transactional
    public int materialize(Question question) {
        QuestionTemplate template = resolveTemplate(question);
        QuestionMaterializer materializer = resolveMaterializer(question, template);

        Map<String, Object> params = question.getTemplateParams() != null
            ? question.getTemplateParams()
            : Map.of();

        MaterializationContext ctx = new MaterializationContext(question, template, params);

        List<MaterializedAnswer> computed = materializer.materialize(ctx);

        if (computed.isEmpty()) {
            log.warn("Materializer returned 0 answers for question {} — answers not updated.",
                question.getId());
            return 0;
        }

        int upserted = upsertAnswers(question, computed);
        log.info("Materialised question {}: {} answers upserted.", question.getId(), upserted);
        return upserted;
    }

    /**
     * Re-materialises a list of questions (e.g. stale-answer batch).
     *
     * @return total number of answer rows upserted across all questions
     */
    @Transactional
    public int rematerializeAll(List<Question> questions) {
        int total = 0;
        for (Question q : questions) {
            try {
                total += materialize(q);
            } catch (Exception ex) {
                log.error("Failed to rematerialize question {}: {}", q.getId(), ex.getMessage(), ex);
            }
        }
        return total;
    }

    /** Returns {@code true} if a materializer is registered for the given key. */
    public boolean hasMaterializer(String key) {
        return materializersByKey.containsKey(key);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private QuestionTemplate resolveTemplate(Question question) {
        if (question.getTemplateId() == null) {
            return null;
        }
        return templateRepository.findById(question.getTemplateId()).orElse(null);
    }

    private QuestionMaterializer resolveMaterializer(Question question, QuestionTemplate template) {
        String key;
        if (template != null) {
            key = template.getMaterializerKey();
        } else {
            // Hand-curated: fall back to config-defined key
            Object configKey = question.getConfig().get("materializer_key");
            if (configKey == null) {
                throw new IllegalStateException(
                    "Question " + question.getId() + " has no template and no 'materializer_key' in config. " +
                    "Hand-curated questions must include {\"materializer_key\": \"<key>\"} in their config.");
            }
            key = configKey.toString();
        }

        QuestionMaterializer m = materializersByKey.get(key);
        if (m == null) {
            throw new IllegalStateException(
                "No materializer registered for key: '" + key + "'. " +
                "Available keys: " + materializersByKey.keySet());
        }
        return m;
    }

    /**
     * Upserts {@link MaterializedAnswer} rows into {@code answers}, registers each
     * player in the {@code entities} autocomplete pool, accumulates zone counts, and
     * (unless {@code difficultyLocked}) recomputes difficulty metrics and evaluates
     * question viability.
     *
     * <p>Upsert logic: if an answer row with the same {@code (question_id, answer_key)}
     * exists, update its score and {@code materialized_at}; otherwise insert.
     *
     * <p>Auto-exclusion: if viability fails (score pool {@literal <} 501 or valid answer
     * count {@literal <} 15) the question's status is set to {@code "excluded"} and
     * a human-readable reason is stored in {@code viabilityExclusionReason}.
     * The question is saved once at the end of the loop.
     */
    private int upsertAnswers(Question question, List<MaterializedAnswer> computed) {
        LocalDateTime now = LocalDateTime.now();

        // Zone accumulators
        int highValueCount  = 0;
        int midRangeCount   = 0;
        int checkoutCount   = 0;
        int totalValidCount = 0;
        int totalScorePool  = 0;

        int count = 0;

        for (MaterializedAnswer ma : computed) {
            boolean isValidDarts = DartsValidator.isValidDartsScore(ma.score());
            boolean isBust       = ma.score() > 180;

            Optional<Answer> existing = answerRepository
                .findByQuestionIdAndAnswerKey(question.getId(), ma.answerKey());

            if (existing.isPresent()) {
                Answer a = existing.get();
                a.setScore(ma.score());
                a.setDisplayText(ma.displayText());
                a.setIsValidDarts(isValidDarts);
                a.setIsBust(isBust);
                a.setMetadata(ma.metadata());
                a.setMaterializedAt(now);
                answerRepository.save(a);
            } else {
                Answer a = Answer.builder()
                    .questionId(question.getId())
                    .answerKey(ma.answerKey())
                    .displayText(ma.displayText())
                    .score(ma.score())
                    .isValidDarts(isValidDarts)
                    .isBust(isBust)
                    .metadata(ma.metadata())
                    .materializedAt(now)
                    .build();
                answerRepository.save(a);
            }

            // Register the player name in the entities autocomplete pool
            entitySearchService.upsertEntity(ma.displayText(), EntityType.FOOTBALLER, null);

            // Accumulate zone counts for difficulty / viability calculation
            if (isValidDarts && !isBust) {
                totalValidCount++;
                totalScorePool += ma.score();
                int s = ma.score();
                if      (s >= DifficultyConstants.HIGH_VALUE_SCORE_MIN) highValueCount++;
                else if (s >= DifficultyConstants.MID_RANGE_SCORE_MIN)  midRangeCount++;
                else                                                      checkoutCount++;
            }

            count++;
        }

        // ── Persist difficulty metrics (skip if locked) ───────────────────────
        if (!question.isDifficultyLocked()) {
            question.setHighValueCount(highValueCount);
            question.setMidRangeCount(midRangeCount);
            question.setCheckoutCount(checkoutCount);
            question.setTotalValidCount(totalValidCount);
            question.setTotalScorePool(totalScorePool);

            double score = DifficultyCalculator.calculate(
                highValueCount, midRangeCount, checkoutCount, totalValidCount);
            question.setDifficultyScore(score);

            // ── Viability check — two hard conditions, both must pass ─────────
            boolean viable = totalScorePool  >= DifficultyConstants.MIN_SCORE_POOL
                          && totalValidCount >= DifficultyConstants.MIN_ANSWER_COUNT;
            question.setSingleQuestionViable(viable);

            if (!viable) {
                String reason = buildViabilityReason(totalScorePool, totalValidCount);
                question.setViabilityExclusionReason(reason);
                question.setStatus(Question.STATUS_EXCLUDED);
                log.warn("Question {} auto-excluded at materialisation: {}", question.getId(), reason);
            } else {
                question.setViabilityExclusionReason(null);
            }

            questionRepository.save(question);
            log.info("Question {} materialised — hv={} mid={} co={} total={} pool={} score={} viable={}",
                question.getId(), highValueCount, midRangeCount, checkoutCount,
                totalValidCount, totalScorePool, String.format("%.2f", score), viable);
        } else {
            log.info("Question {} materialised (difficulty locked — metrics not updated). answers={}",
                question.getId(), count);
        }

        return count;
    }

    /**
     * Builds a human-readable explanation of which viability conditions failed.
     * Never returns an empty string — at least one condition must have failed
     * for this method to be called.
     */
    private String buildViabilityReason(int scorePool, int validCount) {
        List<String> reasons = new ArrayList<>();
        if (scorePool < DifficultyConstants.MIN_SCORE_POOL) {
            reasons.add("insufficient_score_pool: " + scorePool
                      + " < " + DifficultyConstants.MIN_SCORE_POOL);
        }
        if (validCount < DifficultyConstants.MIN_ANSWER_COUNT) {
            reasons.add("insufficient_answer_count: " + validCount
                      + " < " + DifficultyConstants.MIN_ANSWER_COUNT);
        }
        return String.join("; ", reasons);
    }
}
