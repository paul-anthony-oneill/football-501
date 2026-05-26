package com.football501.service;

import com.football501.dto.PlayerSearchResponse;
import com.football501.model.NamedEntity;
import com.football501.model.Player;
import com.football501.repository.NamedEntityRepository;
import com.football501.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search and registration service for the {@code entities} autocomplete table.
 * <p>
 * <h3>How it fits into the game</h3>
 * During a turn the player types a name; after 4 characters the frontend calls
 * {@code GET /api/entities/search?type=footballer&query=aguer} and receives a
 * dropdown of matching {@link NamedEntity} display names.  The player selects
 * one and submits it; the game engine then validates the exact name against the
 * {@code answers} table for the active question.
 * <p>
 * <h3>Why the tables are separate</h3>
 * The {@code entities} table holds <em>all</em> known names of a given type,
 * not just those that are valid for the current question.  This means a name
 * appearing in autocomplete tells the player nothing about whether it is a
 * valid answer — a city, footballer, or director might appear in the dropdown
 * and still score zero if it is not a pre-computed answer for that question.
 * <p>
 * <h3>Accent-insensitive matching</h3>
 * {@link #stripAccents} mirrors PostgreSQL's {@code unaccent()} so that the
 * {@code normalized_name} stored in Java and the query target in SQL are
 * computed identically.  Typing "aguero" finds "Sergio Agüero"; typing
 * "sao paulo" finds "São Paulo".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntitySearchService {

    private static final int BACKFILL_BATCH_SIZE = 500;

    private final NamedEntityRepository namedEntityRepository;
    private final PlayerRepository      playerRepository;

    /**
     * Accent-insensitive, type-scoped entity search for the autocomplete
     * dropdown.  Returns up to {@code limit} results ranked so that
     * prefix-matches appear before substring-matches.
     *
     * @param entityType the pool to search, e.g. "footballer" or "city"
     * @param query      raw user input (may or may not contain accents)
     * @param limit      maximum results to return
     */
    @Transactional(readOnly = true)
    public List<PlayerSearchResponse> search(String entityType, String query, int limit) {
        return namedEntityRepository
                .searchByType(entityType, query, limit)
                .stream()
                .map(e -> PlayerSearchResponse.builder()
                        .id(e.getId())
                        .name(e.getDisplayName())
                        .nationality(e.getHint())
                        .build())
                .toList();
    }

    /**
     * Idempotent upsert of a named entity into the autocomplete registry.
     * <p>
     * Uses {@code (entityType, normalizedName)} as the unique key, so calling
     * this multiple times for the same name and type is safe.  Called by
     * {@link AdminAnswerService} each time answers are created or bulk-imported.
     *
     * @param displayText the full name as it should appear in the UI,
     *                    e.g. "Sergio Agüero" or "New York City"
     * @param entityType  the type pool this name belongs to, e.g. "footballer"
     * @param hint        optional short context label (country code, continent…);
     *                    may be {@code null}
     */
    @Transactional
    public void upsertEntity(String displayText, String entityType, String hint) {
        if (displayText == null || displayText.isBlank()) {
            return;
        }

        String normalized = stripAccents(displayText.toLowerCase().trim());

        if (namedEntityRepository
                .findByEntityTypeAndNormalizedName(entityType, normalized)
                .isPresent()) {
            return; // Already registered — nothing to do.
        }

        NamedEntity entity = NamedEntity.builder()
                .entityType(entityType)
                .displayName(displayText.trim())
                .normalizedName(normalized)
                .hint(hint)
                .createdAt(LocalDateTime.now())
                .build();

        namedEntityRepository.save(entity);
        log.debug("Registered '{}' as entity type '{}' for autocomplete", displayText, entityType);
    }

    /**
     * Returns entity counts grouped by entity type, ordered by count descending.
     * Used by the admin debug panel to verify the autocomplete pool is seeded
     * before a question of that type is activated.
     *
     * @return map of entityType → count, e.g. {@code {"footballer": 104, "city": 0}}
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getEntityCounts() {
        List<Object[]> rows = namedEntityRepository.countByEntityType();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    /**
     * Backfills the {@code entities} table from the {@code players} source table.
     *
     * <p>This is a one-time (idempotent) operation that populates autocomplete
     * coverage without having to materialise every question first.  The scraper
     * already loaded all known footballers into {@code players}; this method
     * registers each one as a {@code "footballer"} entity so the autocomplete
     * dropdown has data immediately.
     *
     * <p>Processes players in pages of {@value #BACKFILL_BATCH_SIZE} to avoid
     * loading 17 k+ rows into memory at once.  Each page runs in its own
     * transaction via {@link #upsertEntity}, so the method itself is not
     * {@code @Transactional}.
     *
     * @return a map with keys {@code inserted} and {@code skipped}
     */
    public Map<String, Long> backfillFromPlayers() {
        long inserted = 0;
        long skipped  = 0;
        int  pageNum  = 0;

        log.info("Starting entity backfill from players table…");

        Page<Player> page;
        do {
            page = playerRepository.findAll(PageRequest.of(pageNum, BACKFILL_BATCH_SIZE));
            for (Player player : page.getContent()) {
                String normalized = stripAccents(player.getName().toLowerCase().trim());
                if (namedEntityRepository
                        .findByEntityTypeAndNormalizedName("footballer", normalized)
                        .isPresent()) {
                    skipped++;
                } else {
                    NamedEntity entity = NamedEntity.builder()
                            .entityType("footballer")
                            .displayName(player.getName().trim())
                            .normalizedName(normalized)
                            .hint(player.getNationality())
                            .createdAt(LocalDateTime.now())
                            .build();
                    namedEntityRepository.save(entity);
                    inserted++;
                }
            }
            log.debug("Backfill page {}/{}: {} inserted, {} skipped so far",
                    pageNum + 1, page.getTotalPages(), inserted, skipped);
            pageNum++;
        } while (page.hasNext());

        log.info("Entity backfill complete: {} inserted, {} skipped (already existed).",
                inserted, skipped);
        return Map.of("inserted", inserted, "skipped", skipped);
    }

    // -------------------------------------------------------------------------
    // Package-visible for unit testing
    // -------------------------------------------------------------------------

    /**
     * Strips combining diacritical marks, e.g. {@code "agüero"} → {@code "aguero"}.
     * Mirrors PostgreSQL {@code unaccent()} so Java-computed keys and SQL query
     * targets are always identical.
     */
    static String stripAccents(String input) {
        return Normalizer
                .normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }
}
