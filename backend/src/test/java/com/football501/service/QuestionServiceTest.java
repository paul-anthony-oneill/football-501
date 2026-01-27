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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
            .isActive(true)
            .build();

        question2 = Question.builder()
            .id(UUID.randomUUID())
            .categoryId(categoryId)
            .questionText("Goals for Arsenal in Premier League 2023/24")
            .metricKey("goals")
            .isActive(true)
            .build();
    }

    @Test
    @DisplayName("Should select random active question by category")
    void shouldSelectRandomQuestionByCategory() {
        // Given
        when(questionRepository.findActiveByCategoryId(categoryId))
            .thenReturn(List.of(question1, question2));
        when(answerRepository.countByQuestionId(any(UUID.class)))
            .thenReturn(50L); // Sufficient answers

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isIn(question1, question2);
        verify(questionRepository).findActiveByCategoryId(categoryId);
    }

    @Test
    @DisplayName("Should return empty when no active questions exist")
    void shouldReturnEmptyWhenNoActiveQuestions() {
        // Given
        when(questionRepository.findActiveByCategoryId(categoryId))
            .thenReturn(List.of());

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId);

        // Then
        assertThat(result).isEmpty();
        verify(questionRepository).findActiveByCategoryId(categoryId);
    }

    @Test
    @DisplayName("Should filter out questions with insufficient answers")
    void shouldFilterInsufficientAnswers() {
        // Given
        when(questionRepository.findActiveByCategoryId(categoryId))
            .thenReturn(List.of(question1, question2));

        // question1 has enough answers, question2 doesn't
        when(answerRepository.countByQuestionId(question1.getId())).thenReturn(50L);
        when(answerRepository.countByQuestionId(question2.getId())).thenReturn(5L); // Too few

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId, 20);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(question1); // Only question1 qualifies
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
        when(questionRepository.findActiveByCategoryId(categoryId))
            .thenReturn(List.of(question1));
        when(answerRepository.countByQuestionId(question1.getId()))
            .thenReturn(30L);

        // When
        Optional<Question> result = questionService.selectRandomQuestion(categoryId);

        // Then
        assertThat(result).isPresent();
        // Should have checked answer count with default minimum (10)
        verify(answerRepository).countByQuestionId(question1.getId());
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
