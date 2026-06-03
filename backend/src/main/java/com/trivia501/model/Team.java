package com.trivia501.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Football team (club or national side).
 *
 * <p>External IDs (FBref, Transfermarkt, …) live in {@link TeamExternalId}.
 * The legacy {@code fbref_id} column was dropped in V9.
 */
@Entity
@Table(
    name = "teams",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_team_name_type", columnNames = {"name", "team_type"})
    },
    indexes = {
        @Index(name = "idx_teams_normalized_name", columnList = "normalized_name")
    }
)
@EntityListeners(AuditingEntityListener.class)
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
     * Lower number = more popular / easier questions.
     * Default 10 = unknown / lower-league.
     */
    @Column(name = "popularity_rank")
    @Builder.Default
    private Integer popularityRank = 10;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
