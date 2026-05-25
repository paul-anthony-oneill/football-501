package com.football501.controller;

import com.football501.service.QuestionGeneratorService;
import com.football501.service.QuestionGeneratorService.GeneratorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for the question template + generator pipeline.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   POST /api/admin/templates/generate           — run generator for ALL active templates
 *   POST /api/admin/templates/{id}/generate      — run generator for ONE template
 * </pre>
 *
 * <p>Both endpoints are idempotent: existing draft/active questions are not
 * modified; only missing combinations are created.
 */
@RestController
@RequestMapping("/api/admin/templates")
@Slf4j
public class AdminTemplateController {

    private final QuestionGeneratorService generatorService;

    public AdminTemplateController(QuestionGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    // ── POST /api/admin/templates/generate ───────────────────────────────────

    /**
     * Runs the template generator for all active templates.
     *
     * <p>Returns a JSON summary:
     * <pre>
     * {
     *   "created": 42,
     *   "skipped": 8,
     *   "total":   50,
     *   "message": "Generator complete."
     * }
     * </pre>
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateAll() {
        log.info("Admin triggered: generate all templates");
        GeneratorResult result = generatorService.generateAll();
        return ResponseEntity.ok(Map.of(
            "created", result.created(),
            "skipped", result.skipped(),
            "total",   result.total(),
            "message", "Generator complete."
        ));
    }

    // ── POST /api/admin/templates/{id}/generate ───────────────────────────────

    /**
     * Runs the template generator for a single template.
     *
     * <p>Returns the same summary shape as {@link #generateAll()}.
     *
     * @param id the template UUID
     */
    @PostMapping("/{id}/generate")
    public ResponseEntity<Map<String, Object>> generateForTemplate(@PathVariable UUID id) {
        log.info("Admin triggered: generate template {}", id);
        GeneratorResult result = generatorService.generateForTemplate(id);
        return ResponseEntity.ok(Map.of(
            "created",     result.created(),
            "skipped",     result.skipped(),
            "total",       result.total(),
            "template_id", id.toString(),
            "message",     "Generator complete for template " + id + "."
        ));
    }
}
