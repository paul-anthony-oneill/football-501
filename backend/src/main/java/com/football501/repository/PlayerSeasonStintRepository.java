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
     * Projection for {@link #aggregateByTeamCompetitionSince}.
     */
    interface StintAggregate {
        UUID  getPlayerId();
        long  getTotalGoals();
        long  getTotalAppearances();
        long  getTotalAssists();
        long  getTotalCleanSheets();
    }
}
