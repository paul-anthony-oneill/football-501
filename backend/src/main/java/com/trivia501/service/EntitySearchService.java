package com.trivia501.service;

import com.trivia501.dto.PlayerSearchResponse;
import com.trivia501.model.NamedEntity;
import com.trivia501.repository.NamedEntityRepository;
import com.trivia501.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .hint(dedupHint(hint))
                .build();

        namedEntityRepository.save(entity);
        log.debug("Registered '{}' as entity type '{}' for autocomplete", displayText, entityType);
    }

    /**
     * Batch variant of {@link #upsertEntity} — pre-fetches existing entities in one
     * query, then saves only new ones via {@code saveAll}.  Eliminates the N+1
     * lookup pattern when materializing a question with hundreds of answers.
     *
     * @param entries list of (displayText, entityType, hint) triples to register
     * @return number of entities actually inserted (already-present entries skipped)
     */
    @Transactional
    public int batchUpsertEntities(List<EntityEntry> entries) {
        if (entries.isEmpty()) {
            return 0;
        }

        String entityType = entries.get(0).entityType;
        Set<String> normalizedNames = entries.stream()
            .map(e -> stripAccents(e.displayText.toLowerCase().trim()))
            .collect(Collectors.toSet());

        Set<String> existing = namedEntityRepository
            .findByEntityTypeAndNormalizedNameIn(entityType, normalizedNames)
            .stream()
            .map(NamedEntity::getNormalizedName)
            .collect(Collectors.toSet());

        List<NamedEntity> toInsert = entries.stream()
            .filter(e -> !existing.contains(stripAccents(e.displayText.toLowerCase().trim())))
            .collect(Collectors.groupingBy(
                e -> stripAccents(e.displayText.toLowerCase().trim()),
                Collectors.collectingAndThen(Collectors.toList(), list -> list.get(0))))
            .values().stream()
            .map(e -> NamedEntity.builder()
                .entityType(e.entityType)
                .displayName(e.displayText.trim())
                .normalizedName(stripAccents(e.displayText.toLowerCase().trim()))
                .hint(dedupHint(e.hint))
                .build())
            .collect(Collectors.toList());

        if (!toInsert.isEmpty()) {
            namedEntityRepository.saveAll(toInsert);
        }

        return toInsert.size();
    }

    /**
     * Lightweight record for batch entity registration.
     */
    public record EntityEntry(String displayText, String entityType, String hint) {}

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
     * Backfills the {@code entities} autocomplete table from the {@code players}
     * source table.
     *
     * <p>This is a one-time, idempotent operation that populates autocomplete
     * coverage without having to materialise every question first.  The scraper
     * already loaded all known footballers into {@code players}; this method
     * registers each one as a {@code "footballer"} entity so the autocomplete
     * dropdown has data immediately.
     *
     * <p>Implemented as a single native {@code INSERT … ON CONFLICT DO NOTHING}
     * so the entire backfill runs in one round-trip, eliminating the O(n) query
     * pattern of the former check-then-insert loop and removing the race
     * condition that could produce duplicate inserts under concurrent calls.
     *
     * @return map with keys {@code inserted} (rows written) and {@code skipped}
     *         (rows already present, derived from total player count)
     */
    @Transactional
    public Map<String, Long> backfillFromPlayers() {
        log.info("Starting entity backfill from players table…");
        long total    = playerRepository.count();
        int  inserted = namedEntityRepository.bulkUpsertFootballersFromPlayers();
        long skipped  = total - inserted;
        log.info("Entity backfill complete: {} inserted, {} skipped (already existed).",
                inserted, skipped);
        return Map.of("inserted", (long) inserted, "skipped", skipped);
    }

    // -------------------------------------------------------------------------
    // Package-visible for unit testing
    // -------------------------------------------------------------------------

    // Characters that NFD does not decompose.  Kept in sync with PostgreSQL's
    // unaccent() dictionary so Java-computed keys and SQL LIKE targets match.
    private static final Map<Character, String> NFD_OPAQUE_REPLACEMENTS = Map.ofEntries(
            Map.entry('ø', "o"),   // ø — Norwegian/Danish o-stroke
            Map.entry('Ø', "o"),   // Ø — uppercase
            Map.entry('æ', "ae"),  // æ — Norwegian/Danish ae-ligature
            Map.entry('Æ', "ae"),  // Æ — uppercase
            Map.entry('ł', "l"),   // ł — Polish l-stroke
            Map.entry('Ł', "l"),   // Ł — uppercase
            Map.entry('đ', "d"),   // đ — Croatian/Sami d-stroke
            Map.entry('Đ', "d"),   // Đ — uppercase
            Map.entry('œ', "oe"),  // œ — French oe-ligature
            Map.entry('Œ', "oe"),  // Œ — uppercase
            Map.entry('ð', "d"),   // ð — Icelandic eth
            Map.entry('Ð', "d"),   // Ð — uppercase
            Map.entry('þ', "th"),  // þ — Icelandic thorn
            Map.entry('Þ', "th"),  // Þ — uppercase
            Map.entry('ß', "ss")   // ß — German sharp-s
    );

    /**
     * Deduplicates consecutive identical whitespace-separated tokens in a hint string.
     * e.g. {@code "FRA FRA"} → {@code "FRA"}, {@code "ES ES"} → {@code "ES"}.
     */
    static String dedupHint(String hint) {
        if (hint == null || hint.isBlank()) return hint;
        String[] tokens = hint.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0 && tokens[i].equals(tokens[i - 1])) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    /**
     * Strips combining diacritical marks, e.g. {@code "agüero"} → {@code "aguero"}.
     * Mirrors PostgreSQL {@code unaccent()} so Java-computed keys and SQL query
     * targets are always identical.
     *
     * <p>NFD normalisation handles most accented characters (ü, ö, é, etc.) by
     * decomposing them into base letter + combining mark.  Characters whose
     * diacritic is a stroke or ligature ({@code ø, æ, ł, đ}) do not decompose
     * under NFD, so a second pass replaces them explicitly.
     */
    static String stripAccents(String input) {
        String decomposed = Normalizer
                .normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");

        // Second pass: characters that NFD does not decompose
        StringBuilder sb = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            String replacement = NFD_OPAQUE_REPLACEMENTS.get(c);
            if (replacement != null) {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
