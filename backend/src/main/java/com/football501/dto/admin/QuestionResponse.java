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
    private Boolean isActive;
    private long answerCount;
    private long validDartsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
