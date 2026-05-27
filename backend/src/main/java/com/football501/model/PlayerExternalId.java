package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * External-source identity record for a player.
 *
 * <p>Supports multiple sources (FBref, Transfermarkt, Sofascore, Wikidata) without
 * schema changes.  On day one only {@code source = "fbref"} rows exist; additional
 * sources slot in as {@code (source, external_id)} unique pairs.
 */
@Entity
@Table(
    name = "player_external_ids",
    uniqueConstraints = {
        @UniqueConstraint(name = "player_external_ids_source_external_id_key",
            columnNames = {"source", "external_id"})
    },
    indexes = {
        @Index(name = "idx_player_ext_player", columnList = "player_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    /**
     * Source slug: {@code "fbref"}, {@code "transfermarkt"}, {@code "sofascore"},
     * {@code "wikidata"}.
     */
    @Column(nullable = false, length = 32)
    private String source;

    /** The identifier within the source system, e.g. an FBref player slug. */
    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    /** Canonical URL for this entity on the source site. Nullable. */
    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    /**
     * Match confidence: {@code 100} = exact; lower values indicate a fuzzy
     * cross-source link that should be verified.
     */
    @Column(nullable = false)
    @Builder.Default
    private Short confidence = 100;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
