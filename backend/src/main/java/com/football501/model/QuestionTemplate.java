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
 * A question template that drives auto-generation of {@link Question} rows.
 *
 * <p>The design is hybrid: metadata (text template, param schema, materialiser key)
 * lives in this DB row; the actual SQL that produces the answer set lives in a Java
 * {@code QuestionMaterializer} implementation registered under
 * {@link #materializerKey}.
 *
 * <p>The generator job reads active templates, enumerates valid param combinations,
 * and inserts draft {@link Question} rows.  An admin then promotes drafts to active,
 * which triggers the materialiser.
 */
@Entity
@Table(
    name = "question_templates",
    uniqueConstraints = {
        @UniqueConstraint(name = "question_templates_slug_key", columnNames = "slug")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Foreign key to the owning category. */
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    /**
     * URL-safe unique identifier, e.g. {@code "team_competition_metric_since"}.
     */
    @Column(nullable = false, length = 64)
    private String slug;

    /** Human-readable label shown in the admin UI. */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Question text with named {@code {placeholders}} for each param.
     * Example: {@code "Goals for {team_name} in {competition_name} since {start_year}"}
     */
    @Column(name = "text_template", nullable = false, columnDefinition = "TEXT")
    private String textTemplate;

    /**
     * JSONB schema describing required params and how to enumerate them.
     * <pre>{@code
     * {
     *   "params": {
     *     "team_id":        {"type": "team_ref",        "enumerate": "competition_slugs:[epl,laliga]"},
     *     "competition_id": {"type": "competition_ref", "enumerate": "slugs:[epl,laliga]"},
     *     "start_year":     {"type": "int",             "enumerate": "values:[2000]"}
     *   }
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "param_schema", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> paramSchema;

    /**
     * Key matching a registered {@code QuestionMaterializer} bean.
     * Example: {@code "football.team_competition_metric_since"}
     */
    @Column(name = "materializer_key", nullable = false, length = 64)
    private String materializerKey;

    /**
     * Default metric key passed to the materialiser.
     * Example: {@code "goals"}, {@code "assists"}, {@code "appearances"}
     */
    @Column(name = "metric_key", nullable = false, length = 50)
    private String metricKey;

    @Column(name = "default_min_score")
    private Integer defaultMinScore;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
