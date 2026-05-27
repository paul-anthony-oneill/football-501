package com.football501.controller;

import com.football501.dto.PlayerSearchResponse;
import com.football501.model.EntityType;
import com.football501.service.EntitySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller for named-entity autocomplete search.
 * <p>
 * Backed by the {@code entities} table — a global registry that is
 * intentionally decoupled from the {@code answers} table.  A name appearing
 * in autocomplete results does <em>not</em> indicate it is a valid answer to
 * the current question; all answer validation happens server-side in
 * {@code AnswerEvaluator}.
 * <p>
 * The {@code type} parameter scopes the search to a specific entity pool.
 * It should match the {@code entity_type} value in the active question's
 * {@code config} JSONB:
 * <pre>
 *   GET /api/entities/search?type=footballer&amp;query=aguer   → "Sergio Agüero"
 *   GET /api/entities/search?type=city&amp;query=new+yor       → "New York City"
 * </pre>
 */
@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
@Slf4j
public class EntityController {

    /** Default entity type used when the question config does not specify one. */
    private static final String DEFAULT_ENTITY_TYPE = EntityType.FOOTBALLER;

    private final EntitySearchService entitySearchService;

    /**
     * Accent-insensitive entity name search for the autocomplete dropdown.
     * Returns an empty list for queries shorter than 4 characters to avoid
     * overly broad result sets at the start of typing.
     *
     * @param type  entity type pool to search (defaults to "footballer")
     * @param query at least 4 characters; may contain or omit accents
     * @return up to 10 matching entity display names
     */
    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchResponse>> search(
            @RequestParam(defaultValue = DEFAULT_ENTITY_TYPE) String type,
            @RequestParam String query) {

        if (query == null || query.trim().length() < 4) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(
                entitySearchService.search(type, query.trim(), 10)
        );
    }
}
