package com.football501.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateAnswerRequest {
    @NotBlank(message = "Display text is required")
    private String displayText;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 300, message = "Score must be at most 300") // Allow busts
    private Integer score;

    private Map<String, Object> metadata;
}
