package com.football501.repository;

import com.football501.model.PlayerSeasonStint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerSeasonStintRepository extends JpaRepository<PlayerSeasonStint, UUID> {

    List<PlayerSeasonStint> findByPlayerId(UUID playerId);

    List<PlayerSeasonStint> findByTeamIdAndCompetitionId(UUID teamId, UUID competitionId);

    Optional<PlayerSeasonStint> findByPlayerIdAndSeasonIdAndTeamIdAndCompetitionId(
        UUID playerId, UUID seasonId, UUID teamId, UUID competitionId
    );

    /**
     * Aggregate totals for a single metric across all seasons at a specific
     * (team, competition), filtered by season start year.
     *
     * <p>This is the primary query used by the football materialiser to produce
     * answer rows. Example use-case: "Goals for Man United in the Premier League
     * since 2000".
     *
     * @param teamId        the team UUID
     * @param competitionId the competition UUID
     * @param startYear     inclusive lower bound on the season start year
     * @return projection list of (playerId, totalGoals, totalAppearances, totalAssists)
     */
    @Query("""
        SELECT s.playerId         AS playerId,
               SUM(s.goals)       AS totalGoals,
               SUM(s.appearances) AS totalAppearances,
               SUM(s.assists)     AS totalAssists,
               SUM(s.cleanSheets) AS totalCleanSheets
          FROM PlayerSeasonStint s
          JOIN Season sn ON sn.id = s.seasonId
         WHERE s.teamId        = :teamId
           AND s.competitionId = :competitionId
           AND sn.startYear   >= :startYear
         GROUP BY s.playerId
        HAVING SUM(s.appearances) > 0
        """)
    List<StintAggregate> aggregateByTeamCompetitionSince(
        @Param("teamId")        UUID teamId,
        @Param("competitionId") UUID competitionId,
        @Param("startYear")     int startYear
    );

    /**
     * Returns distinct team UUIDs that have at least one player-season-stint in
     * the given competition where the season started on or after {@code startYear}.
     *
     * <p>Used by the template generator to enumerate valid (team, competition)
     * parameter sets.
     *
     * @param competitionId the competition UUID
     * @param startYear     inclusive lower bound on season start year
     * @return list of distinct team UUIDs
     */
    @Query("""
        SELECT DISTINCT s.teamId
          FROM PlayerSeasonStint s
          JOIN Season sn ON sn.id = s.seasonId
         WHERE s.competitionId = :competitionId
           AND sn.startYear   >= :startYear
        """)
    List<UUID> findDistinctTeamIdsByCompetitionSince(
        @Param("competitionId") UUID competitionId,
        @Param("startYear")     int  startYear
    );

    /**
     * Returns all distinct (teamId, seasonId) pairs that exist for the given
     * competition.
     *
     * <p>Used by {@link com.football501.materializer.FootballTeamCompetitionSeasonMaterializer}
     * to enumerate valid (team, competition, season) parameter sets for per-season
     * question templates (e.g. "Goals for Arsenal in the Premier League 2023-24").
     *
     * @param competitionId the competition UUID
     * @return list of distinct (teamId, seasonId) pairs where stint data exists
     */
    @Query("""
        SELECT DISTINCT s.teamId AS teamId, s.seasonId AS seasonId
          FROM PlayerSeasonStint s
         WHERE s.competitionId = :competitionId
        """)
    List<TeamSeasonPair> findDistinctTeamSeasonByCompetition(
        @Param("competitionId") UUID competitionId);

    /**
     * Aggregate totals for a single metric for a specific (team, competition,
     * season) combination — no date-range filter.
     *
     * <p>Counterpart to {@link #aggregateByTeamCompetitionSince} for per-season
     * question templates.
     *
     * @param teamId        the team UUID
     * @param competitionId the competition UUID
     * @param seasonId      the exact season UUID
     * @return projection list of (playerId, totalGoals, totalAppearances, totalAssists, totalCleanSheets)
     */
    @Query("""
        SELECT s.playerId         AS playerId,
               SUM(s.goals)       AS totalGoals,
               SUM(s.appearances) AS totalAppearances,
               SUM(s.assists)     AS totalAssists,
               SUM(s.cleanSheets) AS totalCleanSheets
          FROM PlayerSeasonStint s
         WHERE s.teamId        = :teamId
           AND s.competitionId = :competitionId
           AND s.seasonId      = :seasonId
         GROUP BY s.playerId
        HAVING SUM(s.appearances) > 0
        """)
    List<StintAggregate> aggregateByTeamCompetitionSeason(
        @Param("teamId")        UUID teamId,
        @Param("competitionId") UUID competitionId,
        @Param("seasonId")      UUID seasonId
    );

    /**
     * Projection for {@link #aggregateByTeamCompetitionSince} and
     * {@link #aggregateByTeamCompetitionSeason}.
     */
    interface StintAggregate {
        UUID  getPlayerId();
        long  getTotalGoals();
        long  getTotalAppearances();
        long  getTotalAssists();
        long  getTotalCleanSheets();
    }

    /**
     * Projection for {@link #findDistinctTeamSeasonByCompetition}.
     */
    interface TeamSeasonPair {
        UUID getTeamId();
        UUID getSeasonId();
    }
}
