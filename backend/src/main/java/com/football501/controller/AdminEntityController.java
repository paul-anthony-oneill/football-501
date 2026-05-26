package com.football501.controller;

import com.football501.repository.NamedEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin debug endpoint for the named-entity autocomplete pool.
 *
 * Provides a quick sanity-check view showing how many entities are seeded
 * per type — useful for verifying the autocomplete pool before activating
 * a new question type (e.g. "city", "country").
 */
@RestController
@RequestMapping("/api/admin/entities")
@RequiredArgsConstructor
@Slf4j
public class AdminEntityController {

    private final NamedEntityRepository namedEntityRepository;

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
        List<Object[]> rows = namedEntityRepository.countByEntityType();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return ResponseEntity.ok(result);
    }
}
