package com.football501.controller;

import com.football501.service.EntitySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin debug endpoint for the named-entity autocomplete pool.
 *
 * Provides a quick sanity-check view showing how many entities are seeded
 * per type — useful for verifying the autocomplete pool before activating
 * a new question type (e.g. "city", "country").
 *
 * TODO: restrict to admin role once Spring Security is wired up
 *       (e.g. @PreAuthorize("hasRole('ADMIN')")).
 */
@RestController
@RequestMapping("/api/admin/entities")
@RequiredArgsConstructor
@Slf4j
public class AdminEntityController {

    private final EntitySearchService entitySearchService;

    /**
     * Returns entity counts grouped by type.
     *
     * <p>Example response:
     * <pre>
     * {
     *   "footballer": 104,
     *   "city": 0
     * }
     * </pre>
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getCounts() {
        log.debug("Admin: fetching entity counts by type");
        return ResponseEntity.ok(entitySearchService.getEntityCounts());
    }
}
