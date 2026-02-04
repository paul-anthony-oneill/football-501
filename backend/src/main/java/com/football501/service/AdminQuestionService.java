package com.football501.service;

import com.football501.dto.admin.CreateQuestionRequest;
import com.football501.dto.admin.QuestionListResponse;
import com.football501.dto.admin.QuestionResponse;
import com.football501.dto.admin.UpdateQuestionRequest;
import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminQuestionService {

    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AnswerRepository answerRepository;

    public AdminQuestionService(
            QuestionRepository questionRepository,
            CategoryRepository categoryRepository,
            AnswerRepository answerRepository
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new IllegalArgumentException("Category not found with id: " + request.getCategoryId());
        }

        Question question = Question.builder()
                .categoryId(request.getCategoryId())
                .questionText(request.getQuestionText())
                .metricKey(request.getMetricKey())
                .config(request.getConfig())
                .minScore(request.getMinScore())
                .isActive(false) // Default to inactive
                .build();

        Question saved = questionRepository.save(question);
        log.info("Created new question: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public QuestionResponse updateQuestion(UUID id, UpdateQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        if (!question.getCategoryId().equals(request.getCategoryId())) {
             if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new IllegalArgumentException("Category not found with id: " + request.getCategoryId());
            }
        }

        question.setCategoryId(request.getCategoryId());
        question.setQuestionText(request.getQuestionText());
        question.setMetricKey(request.getMetricKey());
        question.setConfig(request.getConfig());
        question.setMinScore(request.getMinScore());

        Question saved = questionRepository.save(question);
        log.info("Updated question: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public QuestionResponse toggleActive(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        question.setIsActive(!question.getIsActive());
        Question saved = questionRepository.save(question);
        log.info("Toggled question active status to {}: {}", saved.getIsActive(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuestionListResponse listQuestions(UUID categoryId, Boolean isActive, Pageable pageable) {
        Page<Question> page;

        if (categoryId != null && isActive != null) {
            page = questionRepository.findByCategoryIdAndIsActive(categoryId, isActive, pageable);
        } else if (categoryId != null) {
            page = questionRepository.findByCategoryId(categoryId, pageable);
        } else if (isActive != null) {
            page = questionRepository.findByIsActive(isActive, pageable);
        } else {
            page = questionRepository.findAll(pageable);
        }

        List<QuestionResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        QuestionListResponse response = new QuestionListResponse();
        response.setContent(content);
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setCurrentPage(page.getNumber());

        return response;
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));
        return mapToResponse(question);
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        if (!questionRepository.existsById(id)) {
             throw new IllegalArgumentException("Question not found with id: " + id);
        }
        questionRepository.deleteById(id);
        log.info("Deleted question with id: {}", id);
    }

    private QuestionResponse mapToResponse(Question question) {
        String categoryName = categoryRepository.findById(question.getCategoryId())
                .map(Category::getName)
                .orElse("Unknown");

        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setCategoryId(question.getCategoryId());
        response.setCategoryName(categoryName);
        response.setQuestionText(question.getQuestionText());
        response.setMetricKey(question.getMetricKey());
        response.setConfig(question.getConfig());
        response.setMinScore(question.getMinScore());
        response.setIsActive(question.getIsActive());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());

        response.setAnswerCount(answerRepository.countByQuestionId(question.getId()));
        response.setValidDartsCount(answerRepository.countByQuestionIdAndIsValidDartsTrue(question.getId()));

        return response;
    }
}
