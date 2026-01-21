package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Player entity with JSONB career statistics.
 * Matches the Python SQLAlchemy model in models_v3.py
 */
@Entity
@Table(name = "players", indexes = {
    @Index(name = "idx_players_normalized_name", columnList = "normalized_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fbref_id", unique = true, nullable = false, length = 50)
    private String fbrefId;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(length = 100)
    private String nationality;

    /**
     * Career statistics stored as JSONB array.
     * Each element is a season with structure:
     * {
     *   "season": "2023-2024",
     *   "team": "Manchester City",
     *   "team_id": "uuid",
     *   "competition": "Premier League",
     *   "competition_id": "uuid",
     *   "appearances": 35,
     *   "goals": 27,
     *   "assists": 5,
     *   "clean_sheets": 0,
     *   "minutes_played": 2890
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "career_stats", columnDefinition = "jsonb", nullable = false)
    private List<SeasonStats> careerStats;

    @Column(name = "last_scraped_at")
    private LocalDateTime lastScrapedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Inner class representing season statistics.
     * Maps to JSONB object structure.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonStats {
        private String season;
        private String team;
        private String teamId;
        private String competition;
        private String competitionId;
        private int appearances;
        private int goals;
        private int assists;
        private int cleanSheets;
        private int minutesPlayed;
    }
}
