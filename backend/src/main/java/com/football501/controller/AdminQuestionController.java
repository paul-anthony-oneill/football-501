package com.football501.controller;

import com.football501.dto.admin.CreateQuestionRequest;
import com.football501.dto.admin.QuestionListResponse;
import com.football501.dto.admin.QuestionResponse;
import com.football501.dto.admin.UpdateQuestionRequest;
import com.football501.service.AdminQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/questions")
@Slf4j
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    public AdminQuestionController(AdminQuestionService adminQuestionService) {
        this.adminQuestionService = adminQuestionService;
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        log.info("Received request to create question for category: {}", request.getCategoryId());
        QuestionResponse response = adminQuestionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<QuestionListResponse> listQuestions(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminQuestionService.listQuestions(categoryId, isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable UUID id) {
        return ResponseEntity.ok(adminQuestionService.getQuestion(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        log.info("Received request to update question id: {}", id);
        return ResponseEntity.ok(adminQuestionService.updateQuestion(id, request));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<QuestionResponse> toggleActive(@PathVariable UUID id) {
        log.info("Received request to toggle active status for question id: {}", id);
        return ResponseEntity.ok(adminQuestionService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable UUID id) {
        log.info("Received request to delete question id: {}", id);
        adminQuestionService.deleteQuestion(id);
    }
}
