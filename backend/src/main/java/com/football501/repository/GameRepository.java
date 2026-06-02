package com.football501.repository;

import com.football501.model.Game;
import com.football501.model.Game.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Game entity.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    /**
     * Find all games for a match, ordered by game number.
     *
     * @param matchId the match UUID
     * @return list of games
     */
    List<Game> findByMatchIdOrderByGameNumberAsc(UUID matchId);

    /**
     * Find current active game for a match.
     *
     * @param matchId the match UUID
     * @return optional active game
     */
    Optional<Game> findByMatchIdAndStatus(UUID matchId, GameStatus status);

    /**
     * Find the latest game in a match.
     *
     * @param matchId the match UUID
     * @return optional game
     */
    @Query("""
        SELECT g FROM Game g
        WHERE g.matchId = :matchId
        ORDER BY g.gameNumber DESC
        LIMIT 1
        """)
    Optional<Game> findLatestGameByMatchId(@Param("matchId") UUID matchId);

    /**
     * Count completed games in a match.
     *
     * @param matchId the match UUID
     * @return number of completed games
     */
    long countByMatchIdAndStatus(UUID matchId, GameStatus status);

    /**
     * Count games won by a specific player in a match.
     *
     * @param matchId the match UUID
     * @param playerId the player UUID
     * @return number of games won
     */
    long countByMatchIdAndWinnerId(UUID matchId, UUID playerId);

    /**
     * Find all completed games for a match.
     *
     * @param matchId the match UUID
     * @return list of completed games
     */
    List<Game> findByMatchIdAndStatusOrderByGameNumberAsc(UUID matchId, GameStatus status);

    /**
     * Find the most recent in-progress game for a player (across all their matches).
     */
    @Query("""
        SELECT g FROM Game g
        JOIN Match m ON g.matchId = m.id
        WHERE (m.player1Id = :playerId OR m.player2Id = :playerId)
          AND g.status = 'IN_PROGRESS'
        ORDER BY g.updatedAt DESC
        LIMIT 1
        """)
    Optional<Game> findActiveGameByPlayerId(@Param("playerId") UUID playerId);

    /**
     * Find all in-progress games last updated before the given cutoff.
     */
    @Query("""
        SELECT g FROM Game g
        WHERE g.status = 'IN_PROGRESS'
          AND g.updatedAt < :cutoff
        """)
    List<Game> findStaleGames(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find all in-progress games for a player (across all their matches).
     */
    @Query("""
        SELECT g FROM Game g
        JOIN Match m ON g.matchId = m.id
        WHERE (m.player1Id = :playerId OR m.player2Id = :playerId)
          AND g.status = 'IN_PROGRESS'
        """)
    List<Game> findActiveGamesByPlayerId(@Param("playerId") UUID playerId);
}
