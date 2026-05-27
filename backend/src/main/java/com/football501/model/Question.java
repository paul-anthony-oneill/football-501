package com.football501.model;

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
    public static final String STATUS_DRAFT   = "draft";
    public static final String STATUS_ACTIVE  = "active";
    public static final String STATUS_RETIRED = "retired";

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
}
