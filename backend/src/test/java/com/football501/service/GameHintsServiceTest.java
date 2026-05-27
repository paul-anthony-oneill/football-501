package com.football501.service;

import com.football501.dto.GameHints;
import com.football501.repository.AnswerRepository;
import com.football501.repository.GameMoveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GameHintsService}.
 *
 * Verifies:
 * <ul>
 *   <li>maxScoresLeft counts unused 180-point answers</li>
 *   <li>checkoutsLeft counts answers in the checkout window [score, score+10]</li>
 *   <li>checkoutsLeft is always 0 when the score is already ≤ 0 (game over guard)</li>
 *   <li>Used answer IDs from game_moves are correctly forwarded to the repository</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameHintsService Tests")
class GameHintsServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private GameMoveRepository gameMoveRepository;

    private GameHintsService service;

    private UUID gameId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        service = new GameHintsService(answerRepository, gameMoveRepository);
        gameId = UUID.randomUUID();
        questionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Returns maxScoresLeft from repository when score > 180")
    void maxScoresLeft_returnedFromRepository_whenAbove180() {
        UUID usedId = UUID.randomUUID();
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of(usedId));
        when(answerRepository.countRemainingMaxScores(questionId, List.of(usedId))).thenReturn(4L);
        // checkout query will also be called — stub to 0 for a score of 250
        when(answerRepository.countRemainingCheckouts(eq(questionId), eq(250), eq(260), eq(List.of(usedId))))
            .thenReturn(0L);

        GameHints hints = service.computeHints(gameId, questionId, 250);

        assertThat(hints.getMaxScoresLeft()).isEqualTo(4);
        verify(answerRepository).countRemainingMaxScores(questionId, List.of(usedId));
    }

    @Test
    @DisplayName("Returns checkoutsLeft from repository when score ≤ 180")
    void checkoutsLeft_returnedFromRepository_whenAt180OrBelow() {
        UUID usedId = UUID.randomUUID();
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of(usedId));
        when(answerRepository.countRemainingMaxScores(questionId, List.of(usedId))).thenReturn(0L);
        // Score = 35; checkout window = [35, 45]
        when(answerRepository.countRemainingCheckouts(questionId, 35, 45, List.of(usedId))).thenReturn(2L);

        GameHints hints = service.computeHints(gameId, questionId, 35);

        assertThat(hints.getCheckoutsLeft()).isEqualTo(2);
        verify(answerRepository).countRemainingCheckouts(questionId, 35, 45, List.of(usedId));
    }

    @Test
    @DisplayName("checkoutsLeft is 0 when currentScore is exactly 0 (game over guard)")
    void checkoutsLeft_isZero_whenScoreIsZero() {
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of());
        when(answerRepository.countRemainingMaxScores(eq(questionId), anyList())).thenReturn(0L);

        GameHints hints = service.computeHints(gameId, questionId, 0);

        assertThat(hints.getCheckoutsLeft()).isEqualTo(0);
        // No checkout query should fire for score ≤ 0
        verify(answerRepository, never()).countRemainingCheckouts(any(), anyInt(), anyInt(), anyList());
    }

    @Test
    @DisplayName("checkoutsLeft is 0 when currentScore is negative (already won)")
    void checkoutsLeft_isZero_whenScoreIsNegative() {
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of());
        when(answerRepository.countRemainingMaxScores(eq(questionId), anyList())).thenReturn(0L);

        GameHints hints = service.computeHints(gameId, questionId, -5);

        assertThat(hints.getCheckoutsLeft()).isEqualTo(0);
        verify(answerRepository, never()).countRemainingCheckouts(any(), anyInt(), anyInt(), anyList());
    }

    @Test
    @DisplayName("Checkout window is [currentScore, currentScore + 10] inclusive")
    void checkoutWindow_isTenPointsWide() {
        int score = 42;
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of());
        when(answerRepository.countRemainingMaxScores(eq(questionId), anyList())).thenReturn(0L);
        when(answerRepository.countRemainingCheckouts(questionId, 42, 52, List.of())).thenReturn(1L);

        service.computeHints(gameId, questionId, score);

        verify(answerRepository).countRemainingCheckouts(questionId, 42, 52, List.of());
    }

    @Test
    @DisplayName("Used answer IDs from game_moves are forwarded to both repository queries")
    void usedAnswerIds_forwardedToRepositoryQueries() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of(id1, id2));
        when(answerRepository.countRemainingMaxScores(questionId, List.of(id1, id2))).thenReturn(0L);
        when(answerRepository.countRemainingCheckouts(questionId, 100, 110, List.of(id1, id2))).thenReturn(0L);

        service.computeHints(gameId, questionId, 100);

        verify(answerRepository).countRemainingMaxScores(questionId, List.of(id1, id2));
        verify(answerRepository).countRemainingCheckouts(questionId, 100, 110, List.of(id1, id2));
    }

    @Test
    @DisplayName("No used answers — dispatches to no-exclusion variant via empty list")
    void noUsedAnswers_emptyListPassedThrough() {
        when(gameMoveRepository.findUsedAnswerIdsByGameId(gameId)).thenReturn(List.of());
        when(answerRepository.countRemainingMaxScores(eq(questionId), eq(List.of()))).thenReturn(5L);
        when(answerRepository.countRemainingCheckouts(eq(questionId), eq(90), eq(100), eq(List.of())))
            .thenReturn(1L);

        GameHints hints = service.computeHints(gameId, questionId, 90);

        assertThat(hints.getMaxScoresLeft()).isEqualTo(5);
        assertThat(hints.getCheckoutsLeft()).isEqualTo(1);
    }
}
