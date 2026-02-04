package com.football501.controller;

import com.football501.dto.admin.AnswerResponse;
import com.football501.dto.admin.BulkCreateAnswersRequest;
import com.football501.dto.admin.BulkCreateAnswersResponse;
import com.football501.dto.admin.CreateAnswerRequest;
import com.football501.service.AdminAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminAnswerController {

    private final AdminAnswerService adminAnswerService;

    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<AnswerResponse> createAnswer(
            @PathVariable UUID questionId,
            @Valid @RequestBody CreateAnswerRequest request) {
        log.info("Received request to create answer for question: {}", questionId);
        AnswerResponse response = adminAnswerService.createAnswer(questionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/questions/{questionId}/answers/bulk")
    public ResponseEntity<BulkCreateAnswersResponse> bulkCreateAnswers(
            @PathVariable UUID questionId,
            @Valid @RequestBody BulkCreateAnswersRequest request) {
        log.info("Received request to bulk create answers for question: {}", questionId);
        return ResponseEntity.ok(adminAnswerService.bulkCreateAnswers(questionId, request));
    }

    @GetMapping("/questions/{questionId}/answers")
    public ResponseEntity<List<AnswerResponse>> listAnswers(@PathVariable UUID questionId) {
        return ResponseEntity.ok(adminAnswerService.listAnswers(questionId));
    }

    @PutMapping("/answers/{id}")
    public ResponseEntity<AnswerResponse> updateAnswer(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAnswerRequest request) {
        log.info("Received request to update answer id: {}", id);
        return ResponseEntity.ok(adminAnswerService.updateAnswer(id, request));
    }

    @DeleteMapping("/answers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswer(@PathVariable UUID id) {
        log.info("Received request to delete answer id: {}", id);
        adminAnswerService.deleteAnswer(id);
    }

    @DeleteMapping("/answers/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswers(@RequestBody Map<String, List<UUID>> body) {
        List<UUID> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            log.info("Received request to delete {} answers", ids.size());
            adminAnswerService.deleteAnswers(ids);
        }
    }
}
