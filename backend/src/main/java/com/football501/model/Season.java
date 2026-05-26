package com.football501.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A season cycle, e.g. "2023-24".
 *
 * <p>Cup competitions (FA Cup, UEFA CL, etc.) that span the calendar year are mapped
 * to the league season they fall within — FA Cup 2023-24 → {@code label = "2023-24"}.
 */
@Entity
@Table(
    name = "seasons",
    indexes = {
        @Index(name = "idx_seasons_start_year", columnList = "start_year")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable key, e.g. {@code "2023-24"}. Unique. */
    @Column(nullable = false, unique = true, length = 10)
    private String label;

    /** Calendar year the season begins in (e.g. {@code 2023}). */
    @Column(name = "start_year", nullable = false)
    private Short startYear;

    /** Calendar year the season ends in (e.g. {@code 2024}). */
    @Column(name = "end_year", nullable = false)
    private Short endYear;

    /** Approximate start date; used for cup-overlap resolution. Nullable. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Approximate end date. Nullable. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** {@code true} for the single currently active season; updated nightly by the scraper. */
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = false;
}
