package com.football501.service;

import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.football501.service.QuestionService.DEFAULT_MIN_ANSWERS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for QuestionService.
 *
 * Test scenarios:
 * 1. Select random active question by category
 * 2. Return empty if no active questions exist
 * 3. Only select questions with minimum answer count
 * 4. Get question by ID
 * 5. Verify question has sufficient answers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService Tests")
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private QuestionService questionService;

    private UUID categoryId;
    private Category footballCategory;
    private Question question1;
    private Question question2;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        footballCategory = Category.builder()
            .id(categoryId)
            .name("Football")
            .slug("football")
            .build();

        question1 = Question.builder()
            .id(UUID.randomUUID())
            .categoryId(categoryId)
            .questionText("Appearances for Manchester City in Premier League 2023/24")
            .metricKey("appearances")
            .status(Question.STATUS_ACTIVE)
            .build();

        question2 = Question.builder()
            .id(UUID.randomUUID())
            .categoryId(categoryId)
            .questionText("Goals for Arsenal in Premier League 2023/24")
            .metricKey("goals")
            .status(Question.STATUS_ACTIVE)
            .build();
    }

    @Test
    @DisplayName("Should select random active question by category")
    void shouldSelectRandomQuestionByCategory() {
        // Given
        when(questionRepository.findRandomActiveQuestion(categoryId, null, DEFAULT_MIN_ANSWERS))
            .thenReturn(Optional.of(question1));

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(question1);
        verify(questionRepository).findRandomActiveQuestion(categoryId, null, DEFAULT_MIN_ANSWERS);
        verifyNoInteractions(answerRepository);
    }

    @Test
    @DisplayName("Should return empty when no active questions exist")
    void shouldReturnEmptyWhenNoActiveQuestions() {
        // Given
        when(questionRepository.findRandomActiveQuestion(categoryId, null, DEFAULT_MIN_ANSWERS))
            .thenReturn(Optional.empty());

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId);

        // Then
        assertThat(result).isEmpty();
        verify(questionRepository).findRandomActiveQuestion(categoryId, null, DEFAULT_MIN_ANSWERS);
        verifyNoInteractions(answerRepository);
    }

    @Test
    @DisplayName("Should filter out questions with insufficient answers")
    void shouldFilterInsufficientAnswers() {
        // The repository now handles the filtering in a single query — only qualifying
        // questions are returned, so question2 (too few answers) never appears.
        when(questionRepository.findRandomActiveQuestion(categoryId, null, 20))
            .thenReturn(Optional.of(question1));

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId, null, 20);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(question1);
        verify(questionRepository).findRandomActiveQuestion(categoryId, null, 20);
        verifyNoInteractions(answerRepository);
    }

    @Test
    @DisplayName("Should get question by ID")
    void shouldGetQuestionById() {
        // Given
        UUID questionId = question1.getId();
        when(questionRepository.findById(questionId))
            .thenReturn(Optional.of(question1));

        // When
        Optional<Question> result = questionService.getQuestionById(questionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(question1);
        verify(questionRepository).findById(questionId);
    }

    @Test
    @DisplayName("Should verify question has sufficient answers")
    void shouldVerifyQuestionHasSufficientAnswers() {
        // Given
        UUID questionId = question1.getId();
        when(answerRepository.countByQuestionId(questionId))
            .thenReturn(50L);

        // When
        boolean result = questionService.hasMinimumAnswers(questionId, 20);

        // Then
        assertThat(result).isTrue();
        verify(answerRepository).countByQuestionId(questionId);
    }

    @Test
    @DisplayName("Should return false when question has insufficient answers")
    void shouldReturnFalseWhenInsufficientAnswers() {
        // Given
        UUID questionId = question1.getId();
        when(answerRepository.countByQuestionId(questionId))
            .thenReturn(10L);

        // When
        boolean result = questionService.hasMinimumAnswers(questionId, 20);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should use default minimum answer count when not specified")
    void shouldUseDefaultMinimumAnswerCount() {
        // Given
        when(questionRepository.findRandomActiveQuestion(categoryId, null, DEFAULT_MIN_ANSWERS))
            .thenReturn(Optional.of(question1));

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId);

        // Then
        assertThat(result).isPresent();
        verify(questionRepository).findRandomActiveQuestion(categoryId, null, DEFAULT_MIN_ANSWERS);
        verifyNoInteractions(answerRepository);
    }

    @Test
    @DisplayName("Should select question by difficulty")
    void shouldSelectQuestionByDifficulty() {
        // Given
        question1.setDifficulty(1);
        when(questionRepository.findRandomActiveQuestion(categoryId, 1, 10))
            .thenReturn(Optional.of(question1));

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId, 1, 10);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(question1);
        verify(questionRepository).findRandomActiveQuestion(categoryId, 1, 10);
        verifyNoInteractions(answerRepository);
    }

    @Test
    @DisplayName("Should get category by slug")
    void shouldGetCategoryBySlug() {
        // Given
        String slug = "football";
        when(categoryRepository.findBySlug(slug))
            .thenReturn(Optional.of(footballCategory));

        // When
        Optional<Category> result = questionService.getCategoryBySlug(slug);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(footballCategory);
        verify(categoryRepository).findBySlug(slug);
    }
}
