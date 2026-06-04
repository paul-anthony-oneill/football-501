package com.trivia501.dto;

import java.util.UUID;

/**
 * Lightweight DTO for the debug answers endpoint.
 * Exposes only the fields displayed in the debug panel.
 */
public record AnswerDebugResponse(
    UUID id,
    String displayText,
    Integer score,
    Boolean isValidDarts,
    Boolean isBust
) {}
