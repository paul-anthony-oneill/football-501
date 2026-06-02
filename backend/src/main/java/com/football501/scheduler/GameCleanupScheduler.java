package com.football501.scheduler;

import com.football501.model.Game;
import com.football501.repository.GameRepository;
import com.football501.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodic cleanup of stale in-progress games.
 *
 * <p>Runs every 60 seconds and abandons any {@code IN_PROGRESS} game that has
 * seen no activity (no moves, no state updates) for more than 5 minutes. This
 * prevents orphaned rows from piling up when a player closes the browser tab
 * or refreshes the page without explicitly abandoning their game.
 */
@Component
@Slf4j
public class GameCleanupScheduler {

    /** Games with no activity for this many minutes are considered abandoned. */
    private static final int STALE_MINUTES = 5;

    private final GameRepository gameRepository;
    private final GameService gameService;

    public GameCleanupScheduler(GameRepository gameRepository, GameService gameService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupStaleGames() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<Game> staleGames = gameRepository.findStaleGames(cutoff);

        if (staleGames.isEmpty()) {
            return;
        }

        log.info("Stale-game cleanup: found {} games with no activity since {}", staleGames.size(), cutoff);
        gameService.abandonStaleGames(staleGames);
    }
}
