package com.trivia501.repository;

import com.trivia501.model.PlayerSeasonStint;
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
        SELECT s.playerId              AS playerId,
               SUM(s.goals)            AS totalGoals,
               SUM(s.appearances)      AS totalAppearances,
               SUM(s.assists)          AS totalAssists,
               SUM(s.cleanSheets)      AS totalCleanSheets,
               SUM(s.subAppearances)   AS totalSubAppearances
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
     * <p>Used by {@link com.trivia501.materializer.FootballTeamCompetitionSeasonMaterializer}
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
        SELECT s.playerId              AS playerId,
               SUM(s.goals)            AS totalGoals,
               SUM(s.appearances)      AS totalAppearances,
               SUM(s.assists)          AS totalAssists,
               SUM(s.cleanSheets)      AS totalCleanSheets,
               SUM(s.subAppearances)   AS totalSubAppearances
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
     * Aggregate totals for all players in a competition (no team filter), filtered
     * by season start year.
     *
     * <p>Used by {@link com.trivia501.materializer.FootballPlayerCompetitionMetricSinceMaterializer}
     * for league-wide questions such as "Goals in the Premier League since 2000"
     * and "Goals + Assists in La Liga since 2000".
     *
     * @param competitionId the competition UUID
     * @param startYear     inclusive lower bound on the season start year
     * @return aggregated stats per player across all teams in the competition
     */
    @Query("""
        SELECT s.playerId              AS playerId,
               SUM(s.goals)            AS totalGoals,
               SUM(s.appearances)      AS totalAppearances,
               SUM(s.assists)          AS totalAssists,
               SUM(s.cleanSheets)      AS totalCleanSheets,
               SUM(s.subAppearances)   AS totalSubAppearances
          FROM PlayerSeasonStint s
          JOIN Season sn ON sn.id = s.seasonId
         WHERE s.competitionId = :competitionId
           AND sn.startYear   >= :startYear
         GROUP BY s.playerId
        HAVING SUM(s.appearances) > 0
        """)
    List<StintAggregate> aggregateByCompetitionSince(
        @Param("competitionId") UUID competitionId,
        @Param("startYear")     int  startYear
    );

    /**
     * Aggregate career totals for all players, restricted to the given set of
     * competition IDs and a minimum season start year.
     *
     * <p>Used by {@link com.trivia501.materializer.FootballPlayerCareerMetricMaterializer}
     * for questions such as "Career goals in top-flight football since 2000".
     * The caller (materializer) resolves which competitions to include and passes
     * their IDs here to avoid a JPQL join on {@code competition_type}.
     *
     * @param startYear      inclusive lower bound on season start year
     * @param competitionIds the set of competition UUIDs to aggregate over
     * @return aggregated career stats per player
     */
    @Query("""
        SELECT s.playerId              AS playerId,
               SUM(s.goals)            AS totalGoals,
               SUM(s.appearances)      AS totalAppearances,
               SUM(s.assists)          AS totalAssists,
               SUM(s.cleanSheets)      AS totalCleanSheets,
               SUM(s.subAppearances)   AS totalSubAppearances
          FROM PlayerSeasonStint s
          JOIN Season sn ON sn.id = s.seasonId
         WHERE sn.startYear       >= :startYear
           AND s.competitionId    IN :competitionIds
         GROUP BY s.playerId
        HAVING SUM(s.appearances) > 0
        """)
    List<StintAggregate> aggregateCareerTotalsSince(
        @Param("startYear")      int       startYear,
        @Param("competitionIds") List<UUID> competitionIds
    );

    /**
     * Returns all distinct competition UUIDs that have at least one stint row
     * where the season started on or after {@code startYear}.
     *
     * <p>Used by {@link com.trivia501.materializer.FootballPlayerCompetitionMetricSinceMaterializer}
     * during enumeration to skip competitions that have no data yet.
     */
    @Query("""
        SELECT DISTINCT s.competitionId
          FROM PlayerSeasonStint s
          JOIN Season sn ON sn.id = s.seasonId
         WHERE sn.startYear >= :startYear
        """)
    List<UUID> findDistinctCompetitionIdsSince(@Param("startYear") int startYear);

    /**
     * Projection for all aggregate queries in this repository.
     * Every query that returns per-player totals must select all six columns
     * so this interface remains the single shared projection type.
     */
    interface StintAggregate {
        UUID  getPlayerId();
        long  getTotalGoals();
        long  getTotalAppearances();
        long  getTotalAssists();
        long  getTotalCleanSheets();
        long  getTotalSubAppearances();
    }

    /**
     * Projection for {@link #findDistinctTeamSeasonByCompetition}.
     */
    interface TeamSeasonPair {
        UUID getTeamId();
        UUID getSeasonId();
    }
}
