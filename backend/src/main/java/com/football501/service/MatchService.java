package com.football501.service;

import com.football501.model.Game;
import com.football501.model.Match;
import com.football501.model.Question;
import com.football501.repository.GameRepository;
import com.football501.repository.MatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing matches and match orchestration.
 *
 * Responsibilities:
 * - Create new matches
 * - Start and manage games within matches
 * - Track game wins per player
 * - Determine match winners (best-of-1/3/5 logic)
 * - Handle match completion
 * - Provide match statistics
 */
@Service
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final GameRepository gameRepository;
    private final GameService gameService;
    private final QuestionService questionService;

    public MatchService(
        MatchRepository matchRepository,
        GameRepository gameRepository,
        GameService gameService,
        QuestionService questionService
    ) {
        this.matchRepository = matchRepository;
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.questionService = questionService;
    }

    /**
     * Create a new match.
     *
     * @param player1Id the first player UUID
     * @param player2Id the second player UUID (can be null for waiting matches)
     * @param categoryId the category UUID
     * @param type the match type
     * @param format the match format (best-of-1/3/5)
     * @return the created match
     */
    @Transactional
    public Match createMatch(
        UUID player1Id,
        UUID player2Id,
        UUID categoryId,
        Match.MatchType type,
        Match.MatchFormat format
    ) {
        log.debug("Creating match: player1={}, player2={}, format={}", player1Id, player2Id, format);

        Match match = Match.builder()
            .player1Id(player1Id)
            .player2Id(player2Id)
            .categoryId(categoryId)
            .type(type)
            .format(format)
            .status(Match.MatchStatus.IN_PROGRESS) // Practice mode starts immediately
            .player1GamesWon(0)
            .player2GamesWon(0)
            .build();

        Match savedMatch = matchRepository.save(match);
        log.info("Match created: id={}, format={}", savedMatch.getId(), format);

        return savedMatch;
    }

    /**
     * Start the next game in a match.
     *
     * @param matchId the match UUID
     * @return the created game
     * @throws IllegalStateException if match is not in progress or no question available
     */
    @Transactional
    public Game startNextGame(UUID matchId) {
        log.debug("Starting next game for match {}", matchId);

        Match match = getMatchOrThrow(matchId);

        // Validate match is in progress
        if (match.getStatus() != Match.MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Match is not in progress");
        }

        // Select random question
        Optional<Question> questionOpt = questionService.selectRandomQuestion(match.getCategoryId());
        if (questionOpt.isEmpty()) {
            throw new IllegalStateException("No question available for category");
        }

        Question question = questionOpt.get();

        // Determine game number (number of completed games + 1)
        long completedGames = gameRepository.countByMatchIdAndStatus(matchId, Game.GameStatus.COMPLETED);
        int gameNumber = (int) completedGames + 1;

        // Create game
        Game game = gameService.createGame(matchId, question.getId(), gameNumber);

        log.info("Game started: matchId={}, gameNumber={}, questionId={}",
            matchId, gameNumber, question.getId());

        return game;
    }

    /**
     * Handle game completion - update match state and check for match winner.
     *
     * @param completedGame the completed game
     */
    @Transactional
    public void handleGameCompletion(Game completedGame) {
        log.debug("Handling game completion: gameId={}, winner={}",
            completedGame.getId(), completedGame.getWinnerId());

        Match match = getMatchOrThrow(completedGame.getMatchId());

        // Increment win count for winner
        if (completedGame.getWinnerId().equals(match.getPlayer1Id())) {
            match.setPlayer1GamesWon(match.getPlayer1GamesWon() + 1);
        } else if (completedGame.getWinnerId().equals(match.getPlayer2Id())) {
            match.setPlayer2GamesWon(match.getPlayer2GamesWon() + 1);
        }

        // Check if match is complete
        if (isMatchComplete(match)) {
            completeMatch(match);
        }

        matchRepository.save(match);
        log.info("Match updated: matchId={}, score={}-{}, status={}",
            match.getId(), match.getPlayer1GamesWon(), match.getPlayer2GamesWon(), match.getStatus());
    }

    /**
     * Check if a match is complete (one player has reached required wins).
     *
     * @param match the match
     * @return true if match is complete
     */
    public boolean isMatchComplete(Match match) {
        int requiredWins = match.getFormat().getGamesToWin();
        return match.getPlayer1GamesWon() >= requiredWins
            || match.getPlayer2GamesWon() >= requiredWins;
    }

    /**
     * Get active matches for a player.
     *
     * @param playerId the player UUID
     * @return list of active matches
     */
    @Transactional(readOnly = true)
    public List<Match> getActiveMatchesForPlayer(UUID playerId) {
        return matchRepository.findActiveMatchesByPlayerId(playerId);
    }

    /**
     * Get match by ID.
     *
     * @param matchId the match UUID
     * @return optional match
     */
    @Transactional(readOnly = true)
    public Optional<Match> getMatchById(UUID matchId) {
        return matchRepository.findById(matchId);
    }

    /**
     * Get all games for a match.
     *
     * @param matchId the match UUID
     * @return list of games ordered by game number
     */
    @Transactional(readOnly = true)
    public List<Game> getGamesForMatch(UUID matchId) {
        return gameRepository.findByMatchIdOrderByGameNumberAsc(matchId);
    }

    /**
     * Get current active game for a match.
     *
     * @param matchId the match UUID
     * @return optional active game
     */
    @Transactional(readOnly = true)
    public Optional<Game> getCurrentGame(UUID matchId) {
        return gameRepository.findByMatchIdAndStatus(matchId, Game.GameStatus.IN_PROGRESS);
    }

    /**
     * Get match statistics for a player.
     *
     * @param playerId the player UUID
     * @return match statistics
     */
    @Transactional(readOnly = true)
    public MatchStats getMatchStats(UUID playerId) {
        long wins = matchRepository.countWinsByPlayerId(playerId);
        long losses = matchRepository.countLossesByPlayerId(playerId);
        return new MatchStats(wins, losses);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private Match getMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
    }

    private void completeMatch(Match match) {
        match.setStatus(Match.MatchStatus.COMPLETED);

        // Determine winner
        if (match.getPlayer1GamesWon() > match.getPlayer2GamesWon()) {
            match.setWinnerId(match.getPlayer1Id());
        } else {
            match.setWinnerId(match.getPlayer2Id());
        }

        log.info("Match completed: matchId={}, winner={}, score={}-{}",
            match.getId(), match.getWinnerId(),
            match.getPlayer1GamesWon(), match.getPlayer2GamesWon());
    }

    /**
     * Simple record for match statistics.
     */
    public record MatchStats(long wins, long losses) {
        public long total() {
            return wins + losses;
        }
    }
}
