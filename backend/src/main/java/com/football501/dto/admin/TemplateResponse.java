package com.football501.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API response shape for a {@link com.football501.model.QuestionTemplate}.
 *
 * <p>Includes live counts of draft and active questions generated from this
 * template so the admin UI can show progress at a glance, and a
 * {@code hasMaterializer} flag so operators know immediately if the template
 * is wired to a Java implementation.
 */
@Data
public class TemplateResponse {
    private UUID    id;
    private UUID    categoryId;
    private String  slug;
    private String  displayName;
    private String  textTemplate;
    private String  materializerKey;
    private String  metricKey;
    private Integer defaultMinScore;
    private boolean active;

    /**
     * {@code true} when a {@link com.football501.materializer.QuestionMaterializer}
     * bean is registered for this template's {@code materializerKey}.  A template
     * with {@code hasMaterializer = false} will produce draft questions but
     * materialisation will fail when they are activated.
     */
    private boolean hasMaterializer;

    /** Number of {@code draft} questions currently generated from this template. */
    private long draftCount;

    /** Number of {@code active} questions currently generated from this template. */
    private long activeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
