package com.trivia501.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain-agnostic question entity.
 *
 * <h3>Lifecycle ({@link #status})</h3>
 * <ul>
 *   <li>{@code "draft"} — created but not yet materialised; not visible in the game.</li>
 *   <li>{@code "active"} — materialised and in rotation; the game engine can select it.</li>
 *   <li>{@code "retired"} — removed from rotation; answers are kept for match replay.</li>
 * </ul>
 *
 * <p>Hand-curated questions have {@code templateId = null}.
 * Auto-generated questions have {@code templateId} set and {@code templateParams}
 * holding the concrete param bindings resolved from the template.
 */
@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_questions_category", columnList = "category_id"),
    @Index(name = "idx_questions_status",   columnList = "status"),
    @Index(name = "idx_questions_template", columnList = "template_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    /** Valid values for the {@link #status} field. */
    public static final String STATUS_DRAFT    = "draft";
    public static final String STATUS_ACTIVE   = "active";
    public static final String STATUS_RETIRED  = "retired";
    /**
     * Auto-set by {@code QuestionMaterializerService} when viability checks fail
     * at materialisation time. Can also be set manually by an admin.
     * Excluded questions never appear in game-selection queries.
     */
    public static final String STATUS_EXCLUDED = "excluded";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "metric_key", nullable = false, length = 50)
    private String metricKey;

    /**
     * Dynamic configuration used by the materialiser.
     * For hand-curated questions this is a free-form JSONB object.
     * For auto-generated questions it is a denormalised snapshot of
     * {@link #templateParams} merged with template defaults.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> config;

    @Column(name = "min_score")
    private Integer minScore;

    @Column(name = "difficulty", nullable = false)
    @Builder.Default
    private Integer difficulty = 2;

    /**
     * Question lifecycle status.
     * Use the {@code STATUS_*} constants defined on this class.
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_DRAFT;

    /**
     * References the {@link QuestionTemplate} that generated this question.
     * {@code null} for hand-curated questions.
     */
    @Column(name = "template_id")
    private UUID templateId;

    /**
     * Concrete param bindings resolved from the template (denormalised snapshot
     * for the materialiser so it does not need to re-join to the template).
     * Empty ({@code {}}) for hand-curated questions.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_params", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> templateParams = Map.of();

    // ── Difficulty metrics (populated by QuestionMaterializerService) ─────────

    /**
     * Count of valid-darts, non-bust answers scoring 100–180 (high-velocity zone).
     * Populated at materialisation time; persists between re-materialisations.
     */
    @Column(name = "high_value_count", nullable = false)
    @Builder.Default
    private int highValueCount = 0;

    /**
     * Count of valid-darts, non-bust answers scoring 20–99 (navigation zone).
     */
    @Column(name = "mid_range_count", nullable = false)
    @Builder.Default
    private int midRangeCount = 0;

    /**
     * Count of valid-darts, non-bust answers scoring 1–19 (checkout / precision zone).
     */
    @Column(name = "checkout_count", nullable = false)
    @Builder.Default
    private int checkoutCount = 0;

    /**
     * Total count of valid-darts, non-bust answers across all zones.
     * Must be ≥ {@code DifficultyConstants.MIN_ANSWER_COUNT} (15) for the question
     * to be viable.
     */
    @Column(name = "total_valid_count", nullable = false)
    @Builder.Default
    private int totalValidCount = 0;

    /**
     * Sum of scores for all valid-darts, non-bust answers.
     * Must be ≥ {@code DifficultyConstants.MIN_SCORE_POOL} (501) for the question
     * to be viable in standard single-question mode.
     */
    @Column(name = "total_score_pool", nullable = false)
    @Builder.Default
    private int totalScorePool = 0;

    /**
     * {@code true} when {@code total_score_pool >= 501} AND
     * {@code total_valid_count >= 15} (constants from {@link com.trivia501.engine.DifficultyConstants}).
     * Questions where this is {@code false} are auto-excluded at materialisation time
     * and excluded from all game-selection queries.
     */
    @Column(name = "single_question_viable", nullable = false)
    @Builder.Default
    private boolean singleQuestionViable = true;

    /**
     * {@code null} on viable questions. When {@link #singleQuestionViable} is
     * {@code false}, contains a human-readable explanation of which conditions
     * failed and by how much.
     * Example: {@code "insufficient_score_pool: 234 < 501; insufficient_answer_count: 8 < 15"}.
     */
    @Column(name = "viability_exclusion_reason", columnDefinition = "TEXT")
    private String viabilityExclusionReason;

    /**
     * Continuous difficulty score 0.00–10.00 computed by
     * {@link com.trivia501.engine.DifficultyCalculator} from the zone counts.
     * 0.00 = easiest, 10.00 = hardest.
     * Default 5.00 until materialisation computes the real value.
     */
    @Column(name = "difficulty_score", nullable = false)
    @Builder.Default
    private double difficultyScore = 5.0;

    /**
     * When {@code true} the recalibration job skips this question, preserving a
     * manually overridden {@link #difficultyScore}. Set via the admin lock endpoint.
     */
    @Column(name = "difficulty_locked", nullable = false)
    @Builder.Default
    private boolean difficultyLocked = false;

    @Column(name = "suitable_for_daily", nullable = false)
    @Builder.Default
    private boolean suitableForDaily = false;

    // ── Football template columns (V24) ───────────────────────────────────────
    // Populated by the Python materialiser for structured football questions.
    // NULL on all non-football and hand-curated questions.

    /** "league" or "club". NULL for non-football/hand-curated questions. */
    @Column(name = "q_scope", length = 20)
    private String qScope;

    /** League slug, e.g. "premier-league". */
    @Column(name = "q_league", length = 50)
    private String qLeague;

    /** Club slug, e.g. "arsenal". NULL for league-scope questions. */
    @Column(name = "q_club", length = 50)
    private String qClub;

    /**
     * Stat type slug. One of: goals | assists | appearances |
     * goals_assists | goals_appearances | assists_appearances | goals_assists_appearances.
     */
    @Column(name = "q_stat", length = 50)
    private String qStat;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Convenience helpers ───────────────────────────────────────────────────
    // @Transient prevents Hibernate 6 from treating these boolean isXxx() methods
    // as persistent properties (which would generate non-existent is_active, etc. columns).

    @Transient
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    @Transient
    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    @Transient
    public boolean isRetired() {
        return STATUS_RETIRED.equals(status);
    }

    @Transient
    public boolean isExcluded() {
        return STATUS_EXCLUDED.equals(status);
    }

    /**
     * Convenience alias for {@link #isSingleQuestionViable()}.
     * A question is viable when its score pool and answer count both meet the
     * hard minimums defined in {@link com.trivia501.engine.DifficultyConstants}.
     */
    @Transient
    public boolean isViable() {
        return singleQuestionViable;
    }
}
