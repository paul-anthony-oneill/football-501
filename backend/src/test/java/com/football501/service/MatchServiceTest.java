package com.football501.service;

import com.football501.model.Game;
import com.football501.model.Match;
import com.football501.model.Question;
import com.football501.repository.GameRepository;
import com.football501.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for MatchService.
 *
 * Test scenarios:
 * 1. Create new match with players and format
 * 2. Start first game in match
 * 3. Start next game after previous completes
 * 4. Determine match winner in best-of-3
 * 5. Determine match winner in best-of-5
 * 6. Handle game completion and update match state
 * 7. Check if match is complete
 * 8. Get active matches for a player
 * 9. Get match statistics (wins, losses)
 * 10. Prevent starting new game when match is complete
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService Tests")
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameService gameService;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private MatchService matchService;

    private UUID player1Id;
    private UUID player2Id;
    private UUID categoryId;
    private UUID matchId;
    private UUID questionId;
    private Match match;
    private Question question;

    @BeforeEach
    void setUp() {
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        questionId = UUID.randomUUID();

        match = Match.builder()
            .id(matchId)
            .player1Id(player1Id)
            .player2Id(player2Id)
            .type(Match.MatchType.CASUAL)
            .format(Match.MatchFormat.BEST_OF_3)
            .status(Match.MatchStatus.IN_PROGRESS)
            .categoryId(categoryId)
            .player1GamesWon(0)
            .player2GamesWon(0)
            .build();

        question = Question.builder()
            .id(questionId)
            .categoryId(categoryId)
            .questionText("Test Question")
            .metricKey("goals")
            .isActive(true)
            .build();
    }

    @Test
    @DisplayName("Should create new match with players and format")
    void shouldCreateNewMatch() {
        // Given
        when(matchRepository.save(any(Match.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Match result = matchService.createMatch(
            player1Id,
            player2Id,
            categoryId,
            Match.MatchType.CASUAL,
            Match.MatchFormat.BEST_OF_3
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPlayer1Id()).isEqualTo(player1Id);
        assertThat(result.getPlayer2Id()).isEqualTo(player2Id);
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getType()).isEqualTo(Match.MatchType.CASUAL);
        assertThat(result.getFormat()).isEqualTo(Match.MatchFormat.BEST_OF_3);
        assertThat(result.getStatus()).isEqualTo(Match.MatchStatus.IN_PROGRESS);
        assertThat(result.getPlayer1GamesWon()).isEqualTo(0);
        assertThat(result.getPlayer2GamesWon()).isEqualTo(0);

        verify(matchRepository).save(any(Match.class));
    }

    @Test
    @DisplayName("Should start first game in match")
    void shouldStartFirstGame() {
        // Given
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(questionService.selectRandomQuestion(categoryId)).thenReturn(Optional.of(question));
        when(gameRepository.countByMatchIdAndStatus(matchId, Game.GameStatus.COMPLETED))
            .thenReturn(0L);

        Game createdGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(1)
            .questionId(questionId)
            .status(Game.GameStatus.IN_PROGRESS)
            .build();

        when(gameService.createGame(matchId, questionId, 1)).thenReturn(createdGame);

        // When
        Game result = matchService.startNextGame(matchId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getGameNumber()).isEqualTo(1);
        assertThat(result.getQuestionId()).isEqualTo(questionId);

        verify(questionService).selectRandomQuestion(categoryId);
        verify(gameService).createGame(matchId, questionId, 1);
    }

    @Test
    @DisplayName("Should start next game after previous completes")
    void shouldStartNextGameAfterPreviousCompletes() {
        // Given - 1 game already completed
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(questionService.selectRandomQuestion(categoryId)).thenReturn(Optional.of(question));
        when(gameRepository.countByMatchIdAndStatus(matchId, Game.GameStatus.COMPLETED))
            .thenReturn(1L);

        Game createdGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(2)
            .questionId(questionId)
            .status(Game.GameStatus.IN_PROGRESS)
            .build();

        when(gameService.createGame(matchId, questionId, 2)).thenReturn(createdGame);

        // When
        Game result = matchService.startNextGame(matchId);

        // Then
        assertThat(result.getGameNumber()).isEqualTo(2);
        verify(gameService).createGame(matchId, questionId, 2);
    }

    @Test
    @DisplayName("Should determine match winner in best-of-3 when player wins 2 games")
    void shouldDetermineMatchWinnerBestOf3() {
        // Given - Player 1 has 1 win, about to win 2nd game (reaching 2 total)
        match.setPlayer1GamesWon(1);
        match.setPlayer2GamesWon(1);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Game completedGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(3)
            .status(Game.GameStatus.COMPLETED)
            .winnerId(player1Id)
            .build();

        // When
        matchService.handleGameCompletion(completedGame);

        // Then
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        Match savedMatch = matchCaptor.getValue();

        assertThat(savedMatch.getStatus()).isEqualTo(Match.MatchStatus.COMPLETED);
        assertThat(savedMatch.getWinnerId()).isEqualTo(player1Id);
        assertThat(savedMatch.getPlayer1GamesWon()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should determine match winner in best-of-5 when player wins 3 games")
    void shouldDetermineMatchWinnerBestOf5() {
        // Given - Best-of-5 match, Player 2 has 2 wins, about to win 3rd game
        match.setFormat(Match.MatchFormat.BEST_OF_5);
        match.setPlayer1GamesWon(1);
        match.setPlayer2GamesWon(2);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Game completedGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(4)
            .status(Game.GameStatus.COMPLETED)
            .winnerId(player2Id)
            .build();

        // When
        matchService.handleGameCompletion(completedGame);

        // Then
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        Match savedMatch = matchCaptor.getValue();

        assertThat(savedMatch.getStatus()).isEqualTo(Match.MatchStatus.COMPLETED);
        assertThat(savedMatch.getWinnerId()).isEqualTo(player2Id);
        assertThat(savedMatch.getPlayer2GamesWon()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle game completion and update match wins")
    void shouldHandleGameCompletionAndUpdateWins() {
        // Given
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Game completedGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(1)
            .status(Game.GameStatus.COMPLETED)
            .winnerId(player1Id)
            .build();

        // When
        matchService.handleGameCompletion(completedGame);

        // Then
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        Match savedMatch = matchCaptor.getValue();

        assertThat(savedMatch.getPlayer1GamesWon()).isEqualTo(1);
        assertThat(savedMatch.getPlayer2GamesWon()).isEqualTo(0);
        assertThat(savedMatch.getStatus()).isEqualTo(Match.MatchStatus.IN_PROGRESS); // Not complete yet
    }

    @Test
    @DisplayName("Should check if match is complete (best-of-3)")
    void shouldCheckIfMatchIsComplete() {
        // Given - Player 1 has 2 wins in best-of-3
        match.setPlayer1GamesWon(2);
        match.setPlayer2GamesWon(0);

        // When
        boolean result = matchService.isMatchComplete(match);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if match is NOT complete")
    void shouldCheckIfMatchIsNotComplete() {
        // Given - Player 1 has 1 win in best-of-3 (needs 2 to win)
        match.setPlayer1GamesWon(1);
        match.setPlayer2GamesWon(0);

        // When
        boolean result = matchService.isMatchComplete(match);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get active matches for a player")
    void shouldGetActiveMatchesForPlayer() {
        // Given
        Match match1 = Match.builder()
            .id(UUID.randomUUID())
            .player1Id(player1Id)
            .player2Id(player2Id)
            .status(Match.MatchStatus.IN_PROGRESS)
            .build();

        Match match2 = Match.builder()
            .id(UUID.randomUUID())
            .player1Id(player2Id)
            .player2Id(player1Id)
            .status(Match.MatchStatus.IN_PROGRESS)
            .build();

        when(matchRepository.findActiveMatchesByPlayerId(player1Id))
            .thenReturn(List.of(match1, match2));

        // When
        List<Match> result = matchService.getActiveMatchesForPlayer(player1Id);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(match1, match2);
    }

    @Test
    @DisplayName("Should get match by ID")
    void shouldGetMatchById() {
        // Given
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // When
        Optional<Match> result = matchService.getMatchById(matchId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(match);
    }

    @Test
    @DisplayName("Should throw exception when starting game on completed match")
    void shouldThrowExceptionWhenStartingGameOnCompletedMatch() {
        // Given
        match.setStatus(Match.MatchStatus.COMPLETED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // When/Then
        assertThatThrownBy(() ->
            matchService.startNextGame(matchId)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Match is not in progress");
    }

    @Test
    @DisplayName("Should throw exception when no question available")
    void shouldThrowExceptionWhenNoQuestionAvailable() {
        // Given
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(questionService.selectRandomQuestion(categoryId)).thenReturn(Optional.empty());
        // Note: gameRepository stub not needed - exception thrown before it's accessed

        // When/Then
        assertThatThrownBy(() ->
            matchService.startNextGame(matchId)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No question available");
    }

    @Test
    @DisplayName("Should get match statistics for a player")
    void shouldGetMatchStatisticsForPlayer() {
        // Given
        when(matchRepository.countWinsByPlayerId(player1Id)).thenReturn(15L);
        when(matchRepository.countLossesByPlayerId(player1Id)).thenReturn(8L);

        // When
        MatchService.MatchStats result = matchService.getMatchStats(player1Id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.wins()).isEqualTo(15);
        assertThat(result.losses()).isEqualTo(8);
        assertThat(result.total()).isEqualTo(23);
    }

    @Test
    @DisplayName("Should get all games for a match")
    void shouldGetAllGamesForMatch() {
        // Given
        Game game1 = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(1)
            .build();

        Game game2 = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(2)
            .build();

        when(gameRepository.findByMatchIdOrderByGameNumberAsc(matchId))
            .thenReturn(List.of(game1, game2));

        // When
        List<Game> result = matchService.getGamesForMatch(matchId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(game1, game2);
        verify(gameRepository).findByMatchIdOrderByGameNumberAsc(matchId);
    }

    @Test
    @DisplayName("Should get current active game for a match")
    void shouldGetCurrentActiveGame() {
        // Given
        Game activeGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(2)
            .status(Game.GameStatus.IN_PROGRESS)
            .build();

        when(gameRepository.findByMatchIdAndStatus(matchId, Game.GameStatus.IN_PROGRESS))
            .thenReturn(Optional.of(activeGame));

        // When
        Optional<Game> result = matchService.getCurrentGame(matchId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(activeGame);
    }

    @Test
    @DisplayName("Should handle best-of-1 match completion")
    void shouldHandleBestOf1MatchCompletion() {
        // Given - Best-of-1 (single game)
        match.setFormat(Match.MatchFormat.BEST_OF_1);
        match.setPlayer1GamesWon(0);
        match.setPlayer2GamesWon(0);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Game completedGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(1)
            .status(Game.GameStatus.COMPLETED)
            .winnerId(player2Id)
            .build();

        // When
        matchService.handleGameCompletion(completedGame);

        // Then
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        Match savedMatch = matchCaptor.getValue();

        assertThat(savedMatch.getPlayer2GamesWon()).isEqualTo(1);
        assertThat(savedMatch.getStatus()).isEqualTo(Match.MatchStatus.COMPLETED);
        assertThat(savedMatch.getWinnerId()).isEqualTo(player2Id);
    }

    @Test
    @DisplayName("Should continue match when neither player has enough wins")
    void shouldContinueMatchWhenNotEnoughWins() {
        // Given - Best-of-5, P1 has 1 win, about to get 2nd (need 3 to win)
        match.setFormat(Match.MatchFormat.BEST_OF_5);
        match.setPlayer1GamesWon(1);
        match.setPlayer2GamesWon(1);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Game completedGame = Game.builder()
            .id(UUID.randomUUID())
            .matchId(matchId)
            .gameNumber(3)
            .status(Game.GameStatus.COMPLETED)
            .winnerId(player1Id)
            .build();

        // When
        matchService.handleGameCompletion(completedGame);

        // Then
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        Match savedMatch = matchCaptor.getValue();

        assertThat(savedMatch.getPlayer1GamesWon()).isEqualTo(2); // Now has 2 wins
        assertThat(savedMatch.getStatus()).isEqualTo(Match.MatchStatus.IN_PROGRESS); // Still ongoing
        assertThat(savedMatch.getWinnerId()).isNull(); // No winner yet
    }

    @Test
    @DisplayName("Should create match with player 2 as null (waiting for opponent)")
    void shouldCreateMatchWithNullPlayer2() {
        // Given
        when(matchRepository.save(any(Match.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Match result = matchService.createMatch(
            player1Id,
            null, // No player 2 yet
            categoryId,
            Match.MatchType.RANKED,
            Match.MatchFormat.BEST_OF_3
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPlayer1Id()).isEqualTo(player1Id);
        assertThat(result.getPlayer2Id()).isNull();
        assertThat(result.getStatus()).isEqualTo(Match.MatchStatus.WAITING); // Waiting for player 2
    }
}
