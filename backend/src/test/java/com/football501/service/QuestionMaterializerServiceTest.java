package com.football501.service;

import com.football501.engine.DifficultyConstants;
import com.football501.materializer.MaterializationContext;
import com.football501.materializer.MaterializedAnswer;
import com.football501.materializer.QuestionMaterializer;
import com.football501.model.Answer;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.QuestionRepository;
import com.football501.repository.QuestionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for difficulty-metrics and viability logic in
 * {@link QuestionMaterializerService}.
 *
 * <p>The materializer strategy itself is mocked; these tests focus exclusively on
 * the zone-count accumulation, difficulty calculation, viability evaluation, and
 * auto-exclusion logic added in Phase 4.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionMaterializerService — difficulty & viability")
class QuestionMaterializerServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock private QuestionMaterializer        stubMaterializer;
    @Mock private AnswerRepository            answerRepository;
    @Mock private QuestionRepository          questionRepository;
    @Mock private QuestionTemplateRepository  templateRepository;
    @Mock private EntitySearchService         entitySearchService;

    private QuestionMaterializerService service;

    /** A minimal question with no template (hand-curated path). */
    private Question question;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // The stub materializer identifies itself so the service can dispatch to it.
        when(stubMaterializer.getMaterializerKey()).thenReturn("test.materializer");

        service = new QuestionMaterializerService(
            List.of(stubMaterializer),
            answerRepository,
            questionRepository,
            templateRepository,
            entitySearchService
        );

        question = Question.builder()
            .id(UUID.randomUUID())
            .categoryId(UUID.randomUUID())
            .questionText("Test question")
            .metricKey("appearances")
            .config(Map.of("materializer_key", "test.materializer"))
            .status(Question.STATUS_DRAFT)
            .build();

        // Default: all answer lookups return empty (new inserts)
        when(answerRepository.findByQuestionIdAndAnswerKey(any(), anyString()))
            .thenReturn(Optional.empty());
        when(answerRepository.save(any(Answer.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(entitySearchService).upsertEntity(anyString(), anyString(), any());
    }

    // ── Zone count tests ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Zone count accumulation")
    class ZoneCountTests {

        @Test
        @DisplayName("Answers are correctly assigned to their zones")
        void zoneCounts_correctlyAssigned() {
            // 3 high-value (100–180), 2 mid-range (20–99), 1 checkout (1–19)
            // plus 1 bust (181) and 1 invalid-darts (179) — both excluded from counts
            List<MaterializedAnswer> answers = List.of(
                new MaterializedAnswer("player-a", "Player A", 150),  // high-value
                new MaterializedAnswer("player-b", "Player B", 120),  // high-value
                new MaterializedAnswer("player-c", "Player C", 100),  // high-value
                new MaterializedAnswer("player-d", "Player D",  50),  // mid-range
                new MaterializedAnswer("player-e", "Player E",  30),  // mid-range
                new MaterializedAnswer("player-f", "Player F",  10),  // checkout
                new MaterializedAnswer("player-g", "Player G", 200),  // bust  — excluded
                new MaterializedAnswer("player-h", "Player H", 179)   // invalid darts — excluded
            );
            when(stubMaterializer.materialize(any(MaterializationContext.class))).thenReturn(answers);

            service.materialize(question);

            Question saved = captureQuestionSave();
            assertThat(saved.getHighValueCount()).isEqualTo(3);
            assertThat(saved.getMidRangeCount()).isEqualTo(2);
            assertThat(saved.getCheckoutCount()).isEqualTo(1);
            assertThat(saved.getTotalValidCount()).isEqualTo(6);
            assertThat(saved.getTotalScorePool()).isEqualTo(150 + 120 + 100 + 50 + 30 + 10);
        }
    }

    // ── Viability tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Viability evaluation")
    class ViabilityTests {

        @Test
        @DisplayName("Question is viable when both conditions pass")
        void viable_whenBothConditionsPass() {
            when(stubMaterializer.materialize(any())).thenReturn(buildViableAnswerSet());

            service.materialize(question);

            Question saved = captureQuestionSave();
            assertThat(saved.isSingleQuestionViable()).isTrue();
            assertThat(saved.getViabilityExclusionReason()).isNull();
            assertThat(saved.getStatus()).isNotEqualTo(Question.STATUS_EXCLUDED);
        }

        @Test
        @DisplayName("Auto-excluded when score pool < MIN_SCORE_POOL (501)")
        void excluded_onInsufficientScorePool() {
            // 20 valid answers but all tiny scores → pool < 501
            List<MaterializedAnswer> answers = buildAnswers(20, 10); // 20 × score=10 → pool=200
            when(stubMaterializer.materialize(any())).thenReturn(answers);

            service.materialize(question);

            Question saved = captureQuestionSave();
            assertThat(saved.isSingleQuestionViable()).isFalse();
            assertThat(saved.getStatus()).isEqualTo(Question.STATUS_EXCLUDED);
            assertThat(saved.getViabilityExclusionReason()).contains("insufficient_score_pool");
            assertThat(saved.getViabilityExclusionReason()).contains("< 501");
        }

        @Test
        @DisplayName("Auto-excluded when answer count < MIN_ANSWER_COUNT (15)")
        void excluded_onInsufficientAnswerCount() {
            // 8 answers at score 100 each → pool=800 but count=8 < 15
            List<MaterializedAnswer> answers = buildAnswers(8, 100);
            when(stubMaterializer.materialize(any())).thenReturn(answers);

            service.materialize(question);

            Question saved = captureQuestionSave();
            assertThat(saved.isSingleQuestionViable()).isFalse();
            assertThat(saved.getStatus()).isEqualTo(Question.STATUS_EXCLUDED);
            assertThat(saved.getViabilityExclusionReason()).contains("insufficient_answer_count");
            assertThat(saved.getViabilityExclusionReason()).contains("< 15");
        }

        @Test
        @DisplayName("Auto-excluded when both conditions fail — reason contains both messages")
        void excluded_whenBothConditionsFail() {
            // 5 answers at score 50 each → pool=250, count=5 — both fail
            List<MaterializedAnswer> answers = buildAnswers(5, 50);
            when(stubMaterializer.materialize(any())).thenReturn(answers);

            service.materialize(question);

            Question saved = captureQuestionSave();
            assertThat(saved.isSingleQuestionViable()).isFalse();
            assertThat(saved.getViabilityExclusionReason())
                .contains("insufficient_score_pool")
                .contains("insufficient_answer_count");
        }

        @Test
        @DisplayName("viabilityExclusionReason is null on a viable question")
        void viabilityExclusionReason_nullWhenViable() {
            when(stubMaterializer.materialize(any())).thenReturn(buildViableAnswerSet());

            service.materialize(question);

            Question saved = captureQuestionSave();
            assertThat(saved.getViabilityExclusionReason()).isNull();
        }
    }

    // ── Difficulty locked tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("difficultyLocked flag")
    class DifficultyLockedTests {

        @Test
        @DisplayName("difficultyLocked=true prevents zone counts and score from being overwritten")
        void locked_preventsMetricsOverwrite() {
            Question lockedQuestion = Question.builder()
                .id(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .questionText("Locked question")
                .metricKey("appearances")
                .config(Map.of("materializer_key", "test.materializer"))
                .status(Question.STATUS_ACTIVE)
                .difficultyLocked(true)
                .difficultyScore(6.5)       // manually set
                .highValueCount(99)          // pre-existing (should not change)
                .build();

            when(stubMaterializer.materialize(any())).thenReturn(buildViableAnswerSet());

            service.materialize(lockedQuestion);

            // questionRepository.save must NOT be called for metrics update
            verify(questionRepository, never()).save(any());

            // Counts and score are unchanged
            assertThat(lockedQuestion.getHighValueCount()).isEqualTo(99);
            assertThat(lockedQuestion.getDifficultyScore()).isEqualTo(6.5, within(0.001));
        }
    }

    // ── Difficulty score tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("difficulty_score computation")
    class DifficultyScoreTests {

        @Test
        @DisplayName("difficulty_score is computed and stored after materialisation")
        void difficultyScoreIsComputed() {
            when(stubMaterializer.materialize(any())).thenReturn(buildViableAnswerSet());

            service.materialize(question);

            Question saved = captureQuestionSave();
            // Score should be in valid range and not the default 5.0 placeholder
            assertThat(saved.getDifficultyScore()).isBetween(0.0, 10.0);
        }
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /**
     * Builds a viable answer set: 20 answers at score 130 each.
     * pool = 20 × 130 = 2600 ≥ 501 ✓, count = 20 ≥ 15 ✓
     */
    private List<MaterializedAnswer> buildViableAnswerSet() {
        return buildAnswers(20, 130); // all high-value zone
    }

    /** Builds {@code count} answers all with the given score. */
    private List<MaterializedAnswer> buildAnswers(int count, int score) {
        var list = new java.util.ArrayList<MaterializedAnswer>(count);
        for (int i = 0; i < count; i++) {
            list.add(new MaterializedAnswer("player-" + i, "Player " + i, score));
        }
        return list;
    }

    /** Captures the Question instance passed to {@code questionRepository.save()}. */
    private Question captureQuestionSave() {
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        return captor.getValue();
    }
}
