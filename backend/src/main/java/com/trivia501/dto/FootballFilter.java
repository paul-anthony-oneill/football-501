package com.trivia501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional filter for football question selection in {@link StartFreePlayRequest}.
 *
 * <p>Scope values:
 * <ul>
 *   <li>{@code random_any}          — random from all football template questions</li>
 *   <li>{@code random_league_level} — random from league-scope questions (any league)</li>
 *   <li>{@code random_club_level}   — random from club-scope questions; {@code league} narrows to one league</li>
 *   <li>{@code league}              — exact league-scope question; requires {@code league} + {@code statType}</li>
 *   <li>{@code club}                — exact club-scope question; requires {@code league}, {@code club} + {@code statType}</li>
 * </ul>
 *
 * <p>When {@code statType} is {@code null} and scope is {@code league} or {@code club},
 * the backend picks randomly among all stat types for that scope target.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FootballFilter {

    /** One of: random_any | random_league_level | random_club_level | league | club */
    private String scope;

    /** League slug, e.g. "premier-league". Required for scope = league / club / random_club_level. */
    private String league;

    /** Club slug, e.g. "arsenal". Required for scope = club. */
    private String club;

    /**
     * Stat type slug. One of: goals | assists | appearances |
     * goals_assists | goals_appearances | assists_appearances | goals_assists_appearances.
     * Null means "any stat" (random selection within the given scope).
     */
    private String statType;
}
