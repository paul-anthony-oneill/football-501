package com.football501.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pre-computed valid answers for questions.
 * Populated by Python scraper querying JSONB career_stats.
 * Used for real-time answer validation during gameplay.
 */
@Entity
@Table(
    name = "question_valid_answers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_question_player", columnNames = {"question_id", "player_id"})
    },
    indexes = {
        @Index(name = "idx_qva_question", columnList = "question_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionValidAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    /**
     * The score for this answer (appearances, goals, or combined).
     * This is what gets deducted from the player's current score.
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * Whether this score is achievable with 3 darts in standard 501.
     * Pre-computed during answer population.
     */
    @Column(name = "is_valid_darts_score", nullable = false)
    private Boolean isValidDartsScore;

    /**
     * Whether this score results in an automatic bust (> 180).
     * Pre-computed during answer population.
     */
    @Column(name = "is_bust", nullable = false)
    private Boolean isBust;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_computed", nullable = false)
    private LocalDateTime lastComputed;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastComputed = LocalDateTime.now();
    }
}
