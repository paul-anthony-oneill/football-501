package com.football501.dto.admin;

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
    private Integer difficulty;
    /** Lifecycle status: {@code "draft"}, {@code "active"}, or {@code "retired"}. */
    private String status;
    private UUID templateId;
    private long answerCount;
    private long validDartsCount;
    /**
     * Sum of scores for all valid-darts, non-bust answers.
     * A pool below 501 means the question cannot be finished from 501 points.
     */
    private long totalPointsPool;
    /**
     * Count of valid-darts answers with score in the 101–180 range.
     * Indicates the question's "finishing power" — higher is easier to complete.
     */
    private long highValueAnswerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
