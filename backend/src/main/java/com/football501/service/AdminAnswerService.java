package com.football501.service;

import com.football501.dto.admin.AnswerResponse;
import com.football501.dto.admin.BulkCreateAnswersRequest;
import com.football501.dto.admin.BulkCreateAnswersResponse;
import com.football501.dto.admin.CreateAnswerRequest;
import com.football501.model.Answer;
import com.football501.repository.AnswerRepository;
import com.football501.repository.QuestionRepository;
import com.football501.util.DartsScoreValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public AnswerResponse createAnswer(UUID questionId, CreateAnswerRequest request) {
        if (!questionRepository.existsById(questionId)) {
            throw new IllegalArgumentException("Question not found with id: " + questionId);
        }

        String answerKey = normalizeAnswerKey(request.getDisplayText());
        if (answerRepository.existsByQuestionIdAndAnswerKey(questionId, answerKey)) {
             throw new IllegalArgumentException("Answer already exists: " + request.getDisplayText());
        }

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setDisplayText(request.getDisplayText());
        answer.setAnswerKey(answerKey);
        answer.setScore(request.getScore());
        answer.setMetadata(request.getMetadata());
        answer.setIsValidDarts(isValidDartsScore(request.getScore()));
        answer.setIsBust(isBust(request.getScore()));
        answer.setCreatedAt(LocalDateTime.now());

        Answer saved = answerRepository.save(answer);
        log.info("Created answer '{}' for question {}", saved.getDisplayText(), questionId);
        return mapToResponse(saved);
    }

    @Transactional
    public BulkCreateAnswersResponse bulkCreateAnswers(UUID questionId, BulkCreateAnswersRequest request) {
        if (!questionRepository.existsById(questionId)) {
            throw new IllegalArgumentException("Question not found with id: " + questionId);
        }

        BulkCreateAnswersResponse response = new BulkCreateAnswersResponse();
        response.setErrors(new ArrayList<>());
        int created = 0;
        int skipped = 0;

        for (CreateAnswerRequest item : request.getAnswers()) {
            try {
                String answerKey = normalizeAnswerKey(item.getDisplayText());
                if (answerRepository.existsByQuestionIdAndAnswerKey(questionId, answerKey)) {
                    skipped++;
                    continue;
                }

                Answer answer = new Answer();
                answer.setQuestionId(questionId);
                answer.setDisplayText(item.getDisplayText());
                answer.setAnswerKey(answerKey);
                answer.setScore(item.getScore());
                answer.setMetadata(item.getMetadata());
                answer.setIsValidDarts(isValidDartsScore(item.getScore()));
                answer.setIsBust(isBust(item.getScore()));
                answer.setCreatedAt(LocalDateTime.now());
                
                answerRepository.save(answer);
                created++;

            } catch (Exception e) {
                log.error("Error creating answer: {}", item.getDisplayText(), e);
                response.getErrors().add("Error for '" + item.getDisplayText() + "': " + e.getMessage());
            }
        }

        response.setCreated(created);
        response.setSkipped(skipped);
        return response;
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> listAnswers(UUID questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new IllegalArgumentException("Question not found with id: " + questionId);
        }
        return answerRepository.findByQuestionIdOrderByScoreDesc(questionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnswerResponse updateAnswer(UUID id, CreateAnswerRequest request) {
        Answer answer = answerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found with id: " + id));

        String newKey = normalizeAnswerKey(request.getDisplayText());
        
        // check duplicate if key changed
        if (!answer.getAnswerKey().equals(newKey)) {
            if (answerRepository.existsByQuestionIdAndAnswerKey(answer.getQuestionId(), newKey)) {
                throw new IllegalArgumentException("Answer already exists: " + request.getDisplayText());
            }
        }

        answer.setDisplayText(request.getDisplayText());
        answer.setAnswerKey(newKey);
        answer.setScore(request.getScore());
        answer.setMetadata(request.getMetadata());
        answer.setIsValidDarts(isValidDartsScore(request.getScore()));
        answer.setIsBust(isBust(request.getScore()));

        Answer saved = answerRepository.save(answer);
        log.info("Updated answer: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAnswer(UUID id) {
        if (!answerRepository.existsById(id)) {
            throw new IllegalArgumentException("Answer not found with id: " + id);
        }
        answerRepository.deleteById(id);
        log.info("Deleted answer with id: {}", id);
    }

    @Transactional
    public void deleteAnswers(List<UUID> ids) {
        answerRepository.deleteAllById(ids);
        log.info("Deleted {} answers", ids.size());
    }

    private String normalizeAnswerKey(String displayText) {
        return displayText.toLowerCase().trim();
    }

    private boolean isValidDartsScore(int score) {
        return DartsScoreValidator.isValid(score);
    }

    private boolean isBust(int score) {
        return score > 180;
    }

    private AnswerResponse mapToResponse(Answer answer) {
        AnswerResponse response = new AnswerResponse();
        response.setId(answer.getId());
        response.setQuestionId(answer.getQuestionId());
        response.setAnswerKey(answer.getAnswerKey());
        response.setDisplayText(answer.getDisplayText());
        response.setScore(answer.getScore());
        response.setIsValidDarts(answer.getIsValidDarts());
        response.setIsBust(answer.getIsBust());
        response.setMetadata(answer.getMetadata());
        response.setCreatedAt(answer.getCreatedAt());
        return response;
    }
}
