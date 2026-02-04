package com.football501.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CreateQuestionRequest {
    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Metric key is required")
    private String metricKey;

    private Map<String, Object> config;

    @Min(value = 0, message = "Minimum score must be non-negative")
    private Integer minScore;
}
