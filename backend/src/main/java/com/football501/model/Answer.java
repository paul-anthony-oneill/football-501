package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A pre-materialised valid answer for a question.
 *
 * <p>This is the sole source of truth for the game engine during match validation.
 * The game engine never joins to {@code player_season_stints} or any other
 * football-source table.
 *
 * <p>{@link #materializedAt} is used by the stale-answer detector: after a nightly
 * stint refresh, answers whose {@code materializedAt} predates the stint
 * {@code updated_at} are queued for re-materialisation.
 */
@Entity
@Table(
    name = "answers",
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_answers_question_key", columnNames = {"question_id", "answer_key"})
    },
    indexes = {
        @Index(name = "idx_answers_question_score", columnList = "question_id, score DESC")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    /** Normalised key for matching (e.g. {@code "erling haaland"}). */
    @Column(name = "answer_key", nullable = false)
    private String answerKey;

    /** Display text for UI (e.g. {@code "Erling Haaland"}). */
    @Column(name = "display_text", nullable = false)
    private String displayText;

    /** The computed score value (e.g. 35 goals). */
    @Column(nullable = false)
    private Integer score;

    /** {@code true} if score is 1–180 and not an impossible darts checkout. */
    @Column(name = "is_valid_darts", nullable = false)
    private Boolean isValidDarts;

    /** {@code true} if score &gt; 180. */
    @Column(name = "is_bust", nullable = false)
    private Boolean isBust;

    /** Optional metadata for context/UI (e.g. {@code {"player_id": "...", "team": "Man City"}}). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Timestamp of the last materialisation.
     * The stale-answer detector compares this against {@code player_season_stints.updated_at}
     * to decide whether re-materialisation is needed.
     */
    @Column(name = "materialized_at", nullable = false)
    @Builder.Default
    private LocalDateTime materializedAt = null; // set in onCreate/onUpdate

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // createdAt is set by @CreatedDate / AuditingEntityListener.
        // materializedAt is a business timestamp (last scraper run), not a generic audit field.
        if (materializedAt == null) {
            materializedAt = LocalDateTime.now();
        }
    }
}
