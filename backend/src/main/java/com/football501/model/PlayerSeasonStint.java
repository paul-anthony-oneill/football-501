package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per (player, season, team, competition) — the keystone of the football
 * source layer.
 *
 * <p>A player who moved clubs mid-season produces two rows (one per club).
 * A player who played the league and a cup for the same club produces two rows
 * (one per competition).  The {@code UNIQUE} constraint on the four FK columns
 * enforces exactly one row per logical stint.
 *
 * <p>This table replaces {@code players.career_stats} JSONB once the V8 Python
 * backfill is verified and V9 drops the old column.
 */
@Entity
@Table(
    name = "player_season_stints",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "player_season_stints_player_season_team_comp_key",
            columnNames = {"player_id", "season_id", "team_id", "competition_id"}
        )
    },
    indexes = {
        @Index(name = "idx_stints_team_comp_season",  columnList = "team_id, competition_id, season_id"),
        @Index(name = "idx_stints_player_comp",       columnList = "player_id, competition_id"),
        @Index(name = "idx_stints_comp_season",       columnList = "competition_id, season_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSeasonStint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    // ── Appearance breakdown ──────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private Short appearances = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short starts = 0;

    @Column(name = "sub_appearances", nullable = false)
    @Builder.Default
    private Short subAppearances = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer minutes = 0;

    // ── Outfield stats ────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private Short goals = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short assists = 0;

    // ── Discipline ────────────────────────────────────────────────────────────

    @Column(name = "yellow_cards", nullable = false)
    @Builder.Default
    private Short yellowCards = 0;

    @Column(name = "red_cards", nullable = false)
    @Builder.Default
    private Short redCards = 0;

    // ── Goalkeeper stats (always 0 for outfield players) ─────────────────────

    @Column(name = "clean_sheets", nullable = false)
    @Builder.Default
    private Short cleanSheets = 0;

    @Column(name = "goals_conceded", nullable = false)
    @Builder.Default
    private Short goalsConceded = 0;

    @Column(name = "is_goalkeeper", nullable = false)
    @Builder.Default
    private Boolean isGoalkeeper = false;

    // ── Ingest provenance ─────────────────────────────────────────────────────

    /** Source slug, e.g. {@code "fbref"}. */
    @Column(nullable = false, length = 32)
    private String source;

    /** Timestamp of the scrape run that wrote this row. */
    @Column(name = "source_scraped_at", nullable = false)
    private LocalDateTime sourceScrapedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
