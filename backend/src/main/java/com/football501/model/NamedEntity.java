package com.football501.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A named thing that can be entered as an answer during gameplay.
 * <p>
 * Examples by entity type:
 * <ul>
 *   <li>{@code footballer} — "Sergio Agüero", "Erling Haaland"</li>
 *   <li>{@code city}       — "New York City", "São Paulo"</li>
 *   <li>{@code country}    — "Brazil", "France"</li>
 *   <li>{@code director}   — "Christopher Nolan", "Bong Joon-ho"</li>
 * </ul>
 * <p>
 * Stored in the {@code entities} table.  Intentionally decoupled from the
 * {@code answers} table so that appearing in autocomplete search results does
 * <em>not</em> reveal whether a name is a valid answer to the current question.
 * <p>
 * The {@code entityType} field matches the {@code entity_type} key inside a
 * {@code Question}'s {@code config} JSONB, so the frontend can request the
 * right autocomplete pool for the active question.
 */
@Entity
@Table(name = "entities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    /**
     * Groups entities into pools used by specific question types.
     * Examples: {@code "footballer"}, {@code "city"}, {@code "country"}, {@code "director"}.
     * Matches {@code config.entity_type} on the {@link Question} that triggered this search.
     */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    /** Display form shown in the autocomplete dropdown, e.g. "Sergio Agüero". */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Lowercase, accent-stripped form used as the unique key and the search
     * target, e.g. "sergio aguero".  Mirrors what PostgreSQL {@code unaccent()}
     * produces so Java normalization and SQL normalization stay in sync.
     */
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    /**
     * Optional short label shown next to the name in the dropdown
     * (e.g. a country code so the frontend can render a flag emoji).
     * Kept as a plain string to stay domain-agnostic.
     */
    @Column
    private String hint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
