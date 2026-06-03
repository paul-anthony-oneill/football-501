package com.trivia501.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * External-source identity record for a team.
 *
 * <p>Same shape as {@link PlayerExternalId}; kept as a separate table so that
 * team and player FK relationships stay independent.
 */
@Entity
@Table(
    name = "team_external_ids",
    uniqueConstraints = {
        @UniqueConstraint(name = "team_external_ids_source_external_id_key",
            columnNames = {"source", "external_id"})
    },
    indexes = {
        @Index(name = "idx_team_ext_team", columnList = "team_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /**
     * Source slug: {@code "fbref"}, {@code "transfermarkt"}, {@code "sofascore"},
     * {@code "wikidata"}.
     */
    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(nullable = false)
    @Builder.Default
    private Short confidence = 100;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
