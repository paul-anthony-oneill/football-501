package com.trivia501.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class QuestionResponse {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String questionText;
    private String metricKey;
    private Map<String, Object> config;
    private Integer minScore;

    /**
     * Legacy discrete difficulty (1/2/3). Deprecated — use {@link #difficultyScore} instead.
     * Retained until all callers migrate to the continuous score.
     */
    private Integer difficulty;

    /** Lifecycle status: {@code "draft"}, {@code "active"}, {@code "retired"}, or {@code "excluded"}. */
    private String status;
    private UUID templateId;

    // ── Answer stats (computed at query time from the answers table) ──────────

    private long answerCount;
    private long validDartsCount;
    /**
     * Sum of scores for all valid-darts, non-bust answers.
     * @deprecated Superseded by {@link #totalScorePool} (mirrors the Question model field).
     */
    @Deprecated
    private long totalPointsPool;
    /**
     * Count of valid-darts answers with score in the 100–180 range.
     * @deprecated Superseded by {@link #highValueCount} (mirrors the Question model field).
     */
    @Deprecated
    private long highValueAnswerCount;

    // ── Difficulty-metrics columns (stored on questions table) ────────────────

    /** Count of valid-darts, non-bust answers scoring 100–180 (velocity zone). */
    private int highValueCount;

    /** Count of valid-darts, non-bust answers scoring 20–99 (navigation zone). */
    private int midRangeCount;

    /** Count of valid-darts, non-bust answers scoring 1–19 (checkout zone). */
    private int checkoutCount;

    /** Total count of valid-darts, non-bust answers across all zones. */
    private int totalValidCount;

    /**
     * Sum of scores for all valid-darts, non-bust answers.
     * Must be ≥ 501 for {@link #singleQuestionViable} to be {@code true}.
     */
    private int totalScorePool;

    // ── Viability ─────────────────────────────────────────────────────────────

    /**
     * {@code true} when {@code totalScorePool >= 501} AND {@code totalValidCount >= 15}.
     * Questions where this is {@code false} are excluded from game selection.
     */
    private boolean singleQuestionViable;

    /**
     * Populated when {@link #singleQuestionViable} is {@code false}.
     * Human-readable explanation of which viability conditions failed.
     * Example: {@code "insufficient_score_pool: 234 < 501"}.
     * {@code null} on viable questions.
     */
    private String viabilityExclusionReason;

    // ── Continuous difficulty score ───────────────────────────────────────────

    /**
     * Continuous difficulty score 0.00–10.00. 0.00 = easiest, 10.00 = hardest.
     * Computed by {@code DifficultyCalculator} from the zone counts at materialisation
     * time. Can be re-computed in bulk via the recalibration endpoint.
     */
    private double difficultyScore;

    /**
     * When {@code true}, the recalibration endpoint will not overwrite
     * {@link #difficultyScore}. Set via {@code PATCH .../difficulty-lock}.
     */
    private boolean difficultyLocked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
