package com.trivia501.dto;

/**
 * Lightweight DTO for a single game move, used inside {@link GameStateResponse}
 * so the frontend can reconstruct move history on reconnect or page refresh.
 */
public record MoveDto(
    String answer,
    String result,
    int scoreBefore,
    int scoreAfter,
    String matchedAnswer,
    Integer scoreValue
) {}
