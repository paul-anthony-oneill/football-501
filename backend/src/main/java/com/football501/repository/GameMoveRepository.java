package com.football501.repository;

import com.football501.model.GameMove;
import com.football501.model.GameMove.MoveResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for GameMove entity.
 */
@Repository
public interface GameMoveRepository extends JpaRepository<GameMove, UUID> {

    /**
     * Find all moves for a game, ordered by move number.
     *
     * @param gameId the game UUID
     * @return list of moves
     */
    List<GameMove> findByGameIdOrderByMoveNumberAsc(UUID gameId);

    /**
     * Find moves by a specific player in a game.
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     * @return list of moves
     */
    List<GameMove> findByGameIdAndPlayerIdOrderByMoveNumberAsc(UUID gameId, UUID playerId);

    /**
     * Get all used answer IDs for a game (to prevent duplicate answers).
     *
     * @param gameId the game UUID
     * @return list of used answer IDs
     */
    @Query("""
        SELECT m.matchedAnswerId FROM GameMove m
        WHERE m.gameId = :gameId
          AND m.matchedAnswerId IS NOT NULL
        """)
    List<UUID> findUsedAnswerIdsByGameId(@Param("gameId") UUID gameId);

    /**
     * Count moves by result type for a game.
     *
     * @param gameId the game UUID
     * @param result the move result
     * @return count of moves
     */
    long countByGameIdAndResult(UUID gameId, MoveResult result);

    /**
     * Count successful moves (VALID or CHECKOUT) for a player in a game.
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     * @return count of successful moves
     */
    @Query("""
        SELECT COUNT(m) FROM GameMove m
        WHERE m.gameId = :gameId
          AND m.playerId = :playerId
          AND (m.result = 'VALID' OR m.result = 'CHECKOUT')
        """)
    long countSuccessfulMovesByPlayer(
        @Param("gameId") UUID gameId,
        @Param("playerId") UUID playerId
    );

    /**
     * Get the last move in a game.
     *
     * @param gameId the game UUID
     * @return list with single move or empty
     */
    @Query("""
        SELECT m FROM GameMove m
        WHERE m.gameId = :gameId
        ORDER BY m.moveNumber DESC
        LIMIT 1
        """)
    List<GameMove> findLatestMoveByGameId(@Param("gameId") UUID gameId);

    /**
     * Count total moves in a game.
     *
     * @param gameId the game UUID
     * @return count of moves
     */
    long countByGameId(UUID gameId);

    /**
     * Get recent moves by a player (for timeout tracking).
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     * @param limit number of recent moves to fetch
     * @return list of recent moves
     */
    @Query(value = """
        SELECT * FROM game_moves
        WHERE game_id = :gameId
          AND player_id = :playerId
        ORDER BY move_number DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<GameMove> findRecentMovesByPlayer(
        @Param("gameId") UUID gameId,
        @Param("playerId") UUID playerId,
        @Param("limit") int limit
    );
}
