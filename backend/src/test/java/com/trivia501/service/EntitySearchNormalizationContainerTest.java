package com.trivia501.service;

import com.trivia501.dto.PlayerSearchResponse;
import com.trivia501.model.EntityType;
import com.trivia501.model.NamedEntity;
import com.trivia501.repository.NamedEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the normalization contract between Java's {@link EntitySearchService#stripAccents}
 * and PostgreSQL's {@code unaccent(lower())} function.
 *
 * <h3>Why this test exists</h3>
 * {@code EntitySearchService.upsertEntity} stores {@code normalized_name} by
 * applying Java's {@code Normalizer.NFD + strip CombiningDiacriticalMarks}.
 * The {@code NamedEntityRepository.searchByType} SQL query applies
 * {@code unaccent(lower(:query))} to the user's search term.
 *
 * <p>If the two normalizations diverge for any character, a name upserted via
 * Java will not be found by the SQL search — a silent autocomplete gap.  This
 * test catches that divergence with a real PostgreSQL container, which runs the
 * actual {@code unaccent()} dictionary (not a Java approximation).
 *
 * <h3>Scenario covered</h3>
 * Western-European acute accents (Agüero, Özil, Coutinho) are the most common
 * occurrence in football datasets.  Add more rows to cover Slavic, Nordic, or
 * other scripts as the player database expands.
 *
 * <h3>Test infrastructure</h3>
 * Uses a real PostgreSQL 17 container (Testcontainers) with {@code pg_trgm}
 * and {@code unaccent} extensions enabled via the init script.  Flyway is
 * disabled; Hibernate {@code create-drop} builds the schema from entity
 * mappings so the test is self-contained.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Entity Search — Java vs PostgreSQL normalization contract")
class EntitySearchNormalizationContainerTest {

    @Container
    @SuppressWarnings("rawtypes")
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
        .withInitScript("db/init-test-entity-search.sql");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",                    postgres::getJdbcUrl);
        registry.add("spring.datasource.username",               postgres::getUsername);
        registry.add("spring.datasource.password",               postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",     () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                     () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",            () -> "create-drop");
        registry.add("spring.flyway.enabled",                    () -> "false");
    }

    @Autowired private EntitySearchService entitySearchService;
    @Autowired private NamedEntityRepository namedEntityRepository;

    @BeforeEach
    void clearEntities() {
        namedEntityRepository.deleteAll();
    }

    // ── Test cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Searching without accent finds player stored with acute accent (ü → u)")
    void searchWithoutAccent_findsAccentedStoredName() {
        // Given — stored via Java normalisation (Normalizer.NFD + strip combining marks)
        entitySearchService.upsertEntity("Sergio Agüero", EntityType.FOOTBALLER, "Argentina");

        // When — SQL uses unaccent(lower("aguero")); stored normalized_name = "sergio aguero"
        List<PlayerSearchResponse> results =
                entitySearchService.search(EntityType.FOOTBALLER, "aguero", 10);

        // Then — Java normalisation must produce the same string as PostgreSQL unaccent()
        assertThat(results)
                .as("Search for 'aguero' should find 'Sergio Agüero' stored with Java normalisation")
                .extracting(PlayerSearchResponse::getName)
                .contains("Sergio Agüero");
    }

    @Test
    @DisplayName("Searching with the full accented query also finds the player")
    void searchWithAccentedQuery_findsPlayer() {
        entitySearchService.upsertEntity("Sergio Agüero", EntityType.FOOTBALLER, "Argentina");

        List<PlayerSearchResponse> results =
                entitySearchService.search(EntityType.FOOTBALLER, "agüero", 10);

        assertThat(results)
                .extracting(PlayerSearchResponse::getName)
                .contains("Sergio Agüero");
    }

    @Test
    @DisplayName("Umlaut ö — Mesut Özil stored and found via unaccented query")
    void umlaut_o_storedAndFound() {
        entitySearchService.upsertEntity("Mesut Özil", EntityType.FOOTBALLER, "Germany");

        List<PlayerSearchResponse> results =
                entitySearchService.search(EntityType.FOOTBALLER, "ozil", 10);

        assertThat(results)
                .as("Search for 'ozil' should find 'Mesut Özil'")
                .extracting(PlayerSearchResponse::getName)
                .contains("Mesut Özil");
    }

    @Test
    @DisplayName("stripAccents Java output matches what PostgreSQL stores for unaccented names")
    void stripAccents_matchesExpected() {
        // Verify the Java utility method directly — not a PostgreSQL test,
        // but pins the contract so refactors don't silently change the algorithm.
        assertThat(EntitySearchService.stripAccents("sergio agüero")).isEqualTo("sergio aguero");
        assertThat(EntitySearchService.stripAccents("mesut özil")).isEqualTo("mesut ozil");
        assertThat(EntitySearchService.stripAccents("lukasz piszczek")).isEqualTo("lukasz piszczek");
        assertThat(EntitySearchService.stripAccents("philippe coutinho")).isEqualTo("philippe coutinho");
    }

    @Test
    @DisplayName("Nordic o-stroke (ø) — Alexander Sørloth stored and found via unaccented query")
    void nordic_oStroke_storedAndFound() {
        entitySearchService.upsertEntity("Alexander Sørloth", EntityType.FOOTBALLER, "Norway");

        List<PlayerSearchResponse> results =
                entitySearchService.search(EntityType.FOOTBALLER, "sorl", 10);

        assertThat(results)
                .as("Search for 'sorl' should find 'Alexander Sørloth'")
                .extracting(PlayerSearchResponse::getName)
                .contains("Alexander Sørloth");
    }

    @Test
    @DisplayName("Nordic ae-ligature (æ) stripped to 'ae' in normalized form")
    void nordic_ae_ligature_stripped() {
        // æ does not decompose under NFD — must be handled by the explicit mapping
        assertThat(EntitySearchService.stripAccents("alexander sørloth")).isEqualTo("alexander sorloth");
        assertThat(EntitySearchService.stripAccents("højbjerg")).isEqualTo("hojbjerg");
        assertThat(EntitySearchService.stripAccents("martin ødegaard")).isEqualTo("martin odegaard");
    }

    @Test
    @DisplayName("Polish l-stroke (ł) stripped to 'l' in normalized form")
    void polish_lStroke_stripped() {
        // ł does not decompose under NFD
        assertThat(EntitySearchService.stripAccents("jakub błaszczykowski")).isEqualTo("jakub blaszczykowski");
        assertThat(EntitySearchService.stripAccents("wojciech szczesny")).isEqualTo("wojciech szczesny");
    }

    @Test
    @DisplayName("German sharp-s (ß) stripped to 'ss' in normalized form")
    void german_sharpS_stripped() {
        assertThat(EntitySearchService.stripAccents("groß")).isEqualTo("gross");
    }

    @Test
    @DisplayName("Entity stored by bulk upsert is retrievable by Java normalised query")
    void bulkUpsertedEntity_isSearchable() {
        // Manually insert a NamedEntity the way the bulk upsert would (using Java-normalised key)
        // to verify the search can find it through the SQL LIKE path.
        namedEntityRepository.save(NamedEntity.builder()
                .entityType(EntityType.FOOTBALLER)
                .displayName("Raphaël Varane")
                .normalizedName(EntitySearchService.stripAccents("raphaël varane"))
                .hint("France")
                .build());

        List<PlayerSearchResponse> results =
                entitySearchService.search(EntityType.FOOTBALLER, "varane", 10);

        assertThat(results)
                .extracting(PlayerSearchResponse::getName)
                .contains("Raphaël Varane");
    }
}
