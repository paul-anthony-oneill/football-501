package com.football501.repository;

import com.football501.model.NamedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NamedEntityRepository extends JpaRepository<NamedEntity, UUID> {

    /**
     * Accent-insensitive substring search scoped to a specific entity type,
     * using the PostgreSQL {@code unaccent()} function and the GIN trigram
     * index on {@code unaccent(normalized_name)}.
     * <p>
     * Results are ranked so that names beginning with the query come first,
     * with alphabetical ordering as a tiebreaker.
     * <p>
     * Example: {@code searchByType("footballer", "aguero", 10)} returns
     * "Sergio Agüero" even though the query contains no accent.
     *
     * @param entityType the pool to search within (e.g. "footballer", "city")
     * @param query      the raw, possibly accent-free search term
     * @param limit      maximum number of results to return
     */
    @Query(value = """
            SELECT id, entity_type, display_name, normalized_name, hint, created_at
            FROM   entities
            WHERE  entity_type = :entityType
            AND    unaccent(normalized_name) LIKE '%' || unaccent(lower(:query)) || '%'
            ORDER BY
                CASE WHEN unaccent(normalized_name) LIKE unaccent(lower(:query)) || '%'
                     THEN 0 ELSE 1 END,
                normalized_name
            LIMIT  :limit
            """, nativeQuery = true)
    List<NamedEntity> searchByType(
            @Param("entityType") String entityType,
            @Param("query") String query,
            @Param("limit") int limit
    );

    /** Exact lookup by type + normalized key — used for upsert deduplication. */
    Optional<NamedEntity> findByEntityTypeAndNormalizedName(String entityType, String normalizedName);
}
