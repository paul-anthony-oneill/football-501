package com.football501.service;

import com.football501.model.Category;
import com.football501.model.DailyChallenge;
import com.football501.model.Match;
import com.football501.model.Question;
import com.football501.repository.CategoryRepository;
import com.football501.repository.DailyChallengeRepository;
import com.football501.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyChallengeServiceTest {

    @Mock private DailyChallengeRepository challengeRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MatchService matchService;
    @Mock private GameService gameService;

    private DailyChallengeService service;

    private final UUID categoryId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final Category category = Category.builder()
            .id(categoryId).slug("football").name("Football").build();
    private final Question question = Question.builder()
            .id(questionId).categoryId(categoryId).questionText("Test question")
            .status(Question.STATUS_ACTIVE).difficultyScore(3.0).totalScorePool(600).build();

    @BeforeEach
    void setUp() {
        service = new DailyChallengeService(
                challengeRepository, questionRepository, categoryRepository, matchService, gameService);
    }

    @Test
    void shouldReturnExistingChallengeForToday() {
        DailyChallenge existing = DailyChallenge.builder()
                .challengeDate(LocalDate.now()).categoryId(categoryId).questionId(questionId)
                .startingScore(301).status("active").build();

        when(challengeRepository.findByChallengeDateAndCategoryId(LocalDate.now(), categoryId))
                .thenReturn(Optional.of(existing));

        DailyChallenge result = service.getTodaysChallenge(categoryId);

        assertThat(result).isEqualTo(existing);
        assertThat(result.getStartingScore()).isEqualTo(301);
    }

    @Test
    void shouldCreateChallengeLazilyWhenNoneExists() {
        when(challengeRepository.findByChallengeDateAndCategoryId(LocalDate.now(), categoryId))
                .thenReturn(Optional.empty());
        when(questionRepository.findRandomDailyQuestion(eq(categoryId), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(question));
        when(challengeRepository.save(any(DailyChallenge.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DailyChallenge result = service.getTodaysChallenge(categoryId);

        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getQuestionId()).isEqualTo(questionId);
        assertThat(result.getChallengeDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldThrowWhenNoViableQuestionExists() {
        when(challengeRepository.findByChallengeDateAndCategoryId(LocalDate.now(), categoryId))
                .thenReturn(Optional.empty());
        // No question found for any score — all fallback attempts fail
        when(questionRepository.findRandomDailyQuestion(eq(categoryId), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTodaysChallenge(categoryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("suitable_for_daily");
    }

    @Test
    void shouldReturnAllTodaysChallenges() {
        DailyChallenge footballChallenge = DailyChallenge.builder()
                .challengeDate(LocalDate.now()).categoryId(categoryId).startingScore(501).build();
        when(challengeRepository.findByChallengeDate(LocalDate.now()))
                .thenReturn(List.of(footballChallenge));

        List<DailyChallenge> results = service.getTodaysChallenges();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStartingScore()).isEqualTo(501);
    }

    @Test
    void shouldPickValidStartingScore() {
        int[] validScores = {501, 401, 351, 301, 251, 201, 167, 125, 101};

        for (int i = 0; i < 50; i++) {
            int score = service.pickStartingScore();
            assertThat(score).isIn((Object[]) java.util.Arrays.stream(validScores).boxed().toArray());
        }
    }

    @Test
    void shouldResolveByCategorySlug() {
        when(categoryRepository.findBySlug("football")).thenReturn(Optional.of(category));
        when(challengeRepository.findByChallengeDateAndCategoryId(LocalDate.now(), categoryId))
                .thenReturn(Optional.empty());
        when(questionRepository.findRandomDailyQuestion(eq(categoryId), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(question));
        when(challengeRepository.save(any(DailyChallenge.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DailyChallenge result = service.getTodaysChallenge("football");

        assertThat(result.getCategoryId()).isEqualTo(categoryId);
    }

    @Test
    void shouldThrowForUnknownCategorySlug() {
        when(categoryRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTodaysChallenge("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }
}
