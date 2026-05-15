# Game Engine Implementation Guide

## Darts Score Validation

**Invalid scores (can never be achieved with 3 darts):** 163, 166, 169, 172, 173, 175, 176, 178, 179

```java
@Service
public class GameEngineService {

    private static final Set<Integer> INVALID_DARTS_SCORES =
        Set.of(163, 166, 169, 172, 173, 175, 176, 178, 179);

    public boolean isValidDartsScore(int score) {
        return score >= 1 && score <= 180 && !INVALID_DARTS_SCORES.contains(score);
    }

    public TurnResult processTurn(int currentScore, int statValue) {
        // Bust conditions
        if (statValue > 180) return TurnResult.bust("Score > 180");
        if (!isValidDartsScore(statValue)) return TurnResult.bust("Invalid darts score: " + statValue);

        int newScore = currentScore - statValue;
        if (newScore < -10) return TurnResult.bust("Below -10");

        // Checkout range: -10 to 0
        if (newScore <= 0) return TurnResult.checkout(newScore);

        return TurnResult.valid(newScore);
    }
}
```

## Close Finish Rule

If Player 1 checks out, Player 2 ALWAYS gets one final turn to get closer to 0.

```java
public GameResult checkGameEnd(GameState state) {
    if (state.isPlayer1CheckedOut() && state.isPlayer2HadFinalTurn()) {
        // Compare scores — closest to 0 wins
        int p1Distance = Math.abs(state.getPlayer1Score());
        int p2Distance = Math.abs(state.getPlayer2Score());
        return p1Distance <= p2Distance ? GameResult.player1Wins() : GameResult.player2Wins();
    }

    if (state.isPlayer1CheckedOut() && !state.isPlayer2HadFinalTurn()) {
        return GameResult.pendingFinalTurn(); // Player 2 still has final turn
    }

    return GameResult.ongoing();
}
```

## Consecutive Timeout Handling

```java
public int getTimerForTurn(int consecutiveTimeouts) {
    return switch (consecutiveTimeouts) {
        case 0 -> 45;  // Default
        case 1 -> 30;
        case 2 -> 15;
        default -> 0;  // Forfeit
    };
}

// Reset on non-consecutive — tracked per player
public boolean isConsecutive(List<GameMove> moves, String playerId) {
    if (moves.isEmpty()) return false;
    GameMove last = moves.get(moves.size() - 1);
    return last.getPlayerId().equals(playerId) && last.isTimeout();
}
```

## Fuzzy Name Matching

Player name matching uses PostgreSQL `pg_trgm` extension — always query the database, never implement fuzzy matching in Java.

```java
// Repository — native query using pg_trgm similarity
@Query(value = """
    SELECT * FROM answers
    WHERE question_id = :questionId
    AND similarity(player_name, :name) > 0.3
    ORDER BY similarity(player_name, :name) DESC
    LIMIT 1
    """, nativeQuery = true)
Optional<Answer> findByFuzzyName(@Param("questionId") Long questionId,
                                  @Param("name") String playerName);
```
