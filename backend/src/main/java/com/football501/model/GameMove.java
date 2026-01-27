package com.football501.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single move/turn in a game.
 * Tracks the player's answer, validation result, and score change.
 */
@Entity
@Table(name = "game_moves", indexes = {
    @Index(name = "idx_game_moves_game", columnList = "game_id"),
    @Index(name = "idx_game_moves_player", columnList = "player_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "move_number", nullable = false)
    private Integer moveNumber;

    @Column(name = "submitted_answer", nullable = false)
    private String submittedAnswer;

    @Column(name = "matched_answer_id")
    private UUID matchedAnswerId;

    @Column(name = "matched_display_text")
    private String matchedDisplayText;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private MoveResult result;

    @Column(name = "score_value")
    private Integer scoreValue;

    @Column(name = "score_before", nullable = false)
    private Integer scoreBefore;

    @Column(name = "score_after", nullable = false)
    private Integer scoreAfter;

    @Column(name = "is_timeout")
    @Builder.Default
    private Boolean isTimeout = false;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MoveResult {
        VALID,          // Valid answer, score deducted
        BUST,           // Invalid darts score or would take below -10
        INVALID,        // Answer not found or already used
        TIMEOUT,        // Player timed out
        CHECKOUT        // Player reached exact 0 or within -10 to 0
    }

    /**
     * Check if this move resulted in a win.
     */
    public boolean isWinningMove() {
        return result == MoveResult.CHECKOUT;
    }

    /**
     * Check if this move was unsuccessful (bust, invalid, or timeout).
     */
    public boolean isUnsuccessfulMove() {
        return result == MoveResult.BUST
            || result == MoveResult.INVALID
            || result == MoveResult.TIMEOUT;
    }
}
