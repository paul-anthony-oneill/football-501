package com.football501.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Football team (club or national side).
 *
 * <p>The {@code fbref_id} column is kept here until V9 drops it after the
 * V8 Python backfill has been verified. External IDs are also stored in
 * {@code team_external_ids} (see {@link TeamExternalId}).
 */
@Entity
@Table(
    name = "teams",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_team_name_type", columnNames = {"name", "team_type"})
    },
    indexes = {
        @Index(name = "idx_teams_normalized_name", columnList = "normalized_name"),
        @Index(name = "idx_teams_fbref",           columnList = "fbref_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    /**
     * Either {@code "club"} or {@code "national"}.
     */
    @Column(name = "team_type", nullable = false, length = 50)
    private String teamType;

    @Column(length = 100)
    private String country;

    /**
     * FBref team identifier — kept until V9, then moved entirely to
     * {@code team_external_ids}.
     */
    @Column(name = "fbref_id", length = 100)
    private String fbrefId;

    /**
     * Lower number = more popular / easier questions.
     * Default 10 = unknown / lower-league.
     */
    @Column(name = "popularity_rank")
    @Builder.Default
    private Integer popularityRank = 10;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
