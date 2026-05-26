package com.football501.controller;

import com.football501.dto.admin.CreateQuestionRequest;
import com.football501.dto.admin.QuestionListResponse;
import com.football501.dto.admin.QuestionResponse;
import com.football501.dto.admin.UpdateQuestionRequest;
import com.football501.dto.admin.UpdateStatusRequest;
import com.football501.service.AdminQuestionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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
    public ResponseEntity<QuestionResponse> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request) {
        log.info("Create question for category: {}", request.getCategoryId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminQuestionService.createQuestion(request));
    }

    /**
     * List questions with optional filters.
     *
     * @param categoryId filter by category UUID
     * @param status     filter by lifecycle status: {@code draft}, {@code active}, {@code retired}
     */
    @GetMapping
    public ResponseEntity<QuestionListResponse> listQuestions(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminQuestionService.listQuestions(categoryId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable UUID id) {
        return ResponseEntity.ok(adminQuestionService.getQuestion(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        log.info("Update question: {}", id);
        return ResponseEntity.ok(adminQuestionService.updateQuestion(id, request));
    }

    /**
     * Transition a question's lifecycle status.
     *
     * <p>Body: {@code {"status": "active"}} — valid values: {@code draft}, {@code active},
     * {@code retired}.
     *
     * <p>Promoting to {@code active} is the point at which the question enters the
     * game rotation (future: triggers materialisation).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<QuestionResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("Update question {} status → {}", id, request.getStatus());
        return ResponseEntity.ok(adminQuestionService.updateStatus(id, request.getStatus()));
    }

    /**
     * Re-materializes the answer set for an active question.
     *
     * <p>Use this after the scraper has refreshed {@code player_season_stints} data
     * and you want to update cached answers without cycling the question status.
     * Only works on questions with {@code status = "active"}.
     *
     * <p>Returns:
     * <pre>
     * { "questionId": "...", "answersUpserted": 47 }
     * </pre>
     */
    @PostMapping("/{id}/rematerialize")
    public ResponseEntity<Map<String, Object>> rematerialize(@PathVariable UUID id) {
        log.info("Admin triggered re-materialize for question: {}", id);
        int count = adminQuestionService.rematerialize(id);
        return ResponseEntity.ok(Map.of(
            "questionId",      id.toString(),
            "answersUpserted", count
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable UUID id) {
        log.info("Delete question: {}", id);
        adminQuestionService.deleteQuestion(id);
    }
}
