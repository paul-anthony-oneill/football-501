package com.football501.controller;

import com.football501.service.EntitySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
 */
@RestController
@RequestMapping("/api/admin/entities")
@PreAuthorize("hasRole('ADMIN')")
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

    /**
     * Backfills the {@code entities} autocomplete table from the {@code players}
     * source table.
     *
     * <p>The scraper has already loaded all known footballers into {@code players}.
     * This endpoint registers each one as a {@code "footballer"} entity so the
     * autocomplete dropdown has full coverage without having to materialise every
     * question first.  The operation is idempotent — running it multiple times is safe.
     *
     * <p>Example response:
     * <pre>
     * { "inserted": 16840, "skipped": 444 }
     * </pre>
     */
    @PostMapping("/backfill-from-players")
    public ResponseEntity<Map<String, Long>> backfillFromPlayers() {
        log.info("Admin: triggering entity backfill from players table");
        Map<String, Long> result = entitySearchService.backfillFromPlayers();
        log.info("Admin: backfill complete — {}", result);
        return ResponseEntity.ok(result);
    }
}
