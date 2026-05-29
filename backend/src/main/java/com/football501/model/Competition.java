package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Football competition — domestic leagues, cups, and UEFA tournaments.
 *
 * <h3>Canonical {@code competition_type} values (V6):</h3>
 * <ul>
 *   <li>{@code "domestic_league"} — EPL, La Liga, Serie A, Bundesliga, Ligue 1</li>
 *   <li>{@code "domestic_cup"}   — FA Cup, Copa del Rey, DFB-Pokal, etc.</li>
 *   <li>{@code "continental_club"} — UCL, UEL, UECL</li>
 * </ul>
 *
 * <p>The {@code tier} column (added in V6) equals {@code 1} for top-flight domestic
 * leagues; it is {@code NULL} for cups and continental competitions.
 */
@Entity
@Table(
    name = "competitions",
    indexes = {
        @Index(name = "idx_competitions_normalized_name", columnList = "normalized_name"),
        @Index(name = "idx_competitions_fbref",           columnList = "fbref_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    /**
     * Canonical type slug.
     * @see Competition class javadoc for valid values.
     */
    @Column(name = "competition_type", nullable = false, length = 50)
    private String competitionType;

    /** {@code null} for international / multi-country competitions. */
    @Column(length = 100)
    private String country;

    @Column(name = "fbref_id", length = 100)
    private String fbrefId;

    /** UI display name, e.g. {@code "UEFA Champions League"}. */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * League tier — {@code 1} for top-flight domestic leagues; {@code null}
     * for cups and continental competitions.  Added in V6.
     */
    @Column
    private Short tier;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
