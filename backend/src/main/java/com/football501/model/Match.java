package com.football501.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a match between two players.
 * A match consists of multiple games (best-of-3 or best-of-5).
 */
@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_matches_status", columnList = "status"),
    @Index(name = "idx_matches_player1", columnList = "player1_id"),
    @Index(name = "idx_matches_player2", columnList = "player2_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player1_id")
    private UUID player1Id;

    @Column(name = "player2_id")
    private UUID player2Id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchType type = MatchType.CASUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchFormat format = MatchFormat.BEST_OF_3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.IN_PROGRESS;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "player1_games_won")
    @Builder.Default
    private Integer player1GamesWon = 0;

    @Column(name = "player2_games_won")
    @Builder.Default
    private Integer player2GamesWon = 0;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MatchType {
        CASUAL,
        RANKED,
        DAILY_CHALLENGE
    }

    public enum MatchFormat {
        BEST_OF_1(1),
        BEST_OF_3(2),
        BEST_OF_5(3);

        private final int gamesToWin;

        MatchFormat(int gamesToWin) {
            this.gamesToWin = gamesToWin;
        }

        public int getGamesToWin() {
            return gamesToWin;
        }
    }

    public enum MatchStatus {
        WAITING,        // Waiting for player2
        IN_PROGRESS,
        COMPLETED,
        ABANDONED
    }
}
