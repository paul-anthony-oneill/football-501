package com.football501.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Question entity with filter criteria.
 * Matches the Python SQLAlchemy model in models_v3.py
 */
@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_questions_active", columnList = "is_active"),
    @Index(name = "idx_questions_filters", columnList = "team_id, competition_id, season_filter")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * Type of statistic to use for scoring.
     * Values: 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'
     */
    @Column(name = "stat_type", nullable = false, length = 50)
    private String statType;

    // Filters (NULL = no filter)
    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "competition_id")
    private UUID competitionId;

    @Column(name = "season_filter", length = 20)
    private String seasonFilter;

    @Column(name = "nationality_filter", length = 100)
    private String nationalityFilter;

    @Column(name = "min_score")
    private Integer minScore;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
}
