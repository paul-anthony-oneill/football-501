package com.trivia501.materializer;

import com.trivia501.model.*;
import com.trivia501.repository.*;

// NOTE: PlayerSeasonStint import kept for Javadoc references in private methods
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Materializes questions of the form:
 * <pre>
 *   "Goals for Manchester United in the Premier League since 2000"
 * </pre>
 *
 * <h3>Template params consumed</h3>
 * <ul>
 *   <li>{@code team_id}        — UUID of the team</li>
 *   <li>{@code competition_id} — UUID of the competition</li>
 *   <li>{@code start_year}     — inclusive lower bound on season start year</li>
 * </ul>
 *
 * <p>The metric to compute (goals / appearances / assists / cleanSheets) comes
 * from {@link QuestionTemplate#getMetricKey()} on the parent template.
 *
 * <h3>Enumeration</h3>
 * <p>For each active template that uses this materializer, {@link #enumerateParams}
 * returns one param map per (team, competition) pair where at least one
 * {@code player_season_stints} row exists with {@code season.start_year >= start_year}.
 * The default {@code start_year} value is {@code 2000}.
 */
@Component
@Slf4j
public class FootballTeamCompetitionMetricSinceMaterializer implements QuestionMaterializer {

    public static final String KEY = "football.team_competition_metric_since";

    // Supported metric keys and their labels in question text
    private static final Map<String, String> METRIC_LABELS = Map.of(
        "goals",                    "Goals",
        "appearances",              "Appearances",
        "assists",                  "Assists",
        "clean_sheets",             "Clean sheets",
        "sub_appearances",          "Substitute appearances",
        "goals_assists",            "Goals + Assists",
        "goals_appearances",        "Goals + Appearances",
        "assists_appearances",      "Assists + Appearances",
        "goals_assists_appearances","Goals + Assists + Appearances"
    );

    // Default start-year used when the template param_schema doesn't override it.
    private static final int DEFAULT_START_YEAR = 2000;

    private final PlayerSeasonStintRepository stintRepository;
    private final PlayerRepository            playerRepository;
    private final TeamRepository              teamRepository;
    private final CompetitionRepository       competitionRepository;
    private final SeasonRepository            seasonRepository;

    public FootballTeamCompetitionMetricSinceMaterializer(
            PlayerSeasonStintRepository stintRepository,
            PlayerRepository            playerRepository,
            TeamRepository              teamRepository,
            CompetitionRepository       competitionRepository,
            SeasonRepository            seasonRepository
    ) {
        this.stintRepository       = stintRepository;
        this.playerRepository      = playerRepository;
        this.teamRepository        = teamRepository;
        this.competitionRepository = competitionRepository;
        this.seasonRepository      = seasonRepository;
    }

    @Override
    public String getMaterializerKey() {
        return KEY;
    }

    // ── Enumeration ─────────────────────────────────────────────────────────

    /**
     * Returns one param set per (team, competition) that has actual stint data.
     *
     * <p>The {@code start_year} is read from the template's {@code param_schema}
     * under {@code params.start_year.values[0]}, defaulting to {@value DEFAULT_START_YEAR}.
     *
     * <p>Only competitions listed under the template's
     * {@code params.competition_id.competition_types} (default: {@code ["domestic_league"]})
     * are enumerated.  Only tier-1 leagues are included unless the template schema
     * overrides this.
     */
    @Override
    public List<Map<String, Object>> enumerateParams(QuestionTemplate template) {
        int startYear = extractStartYear(template);
        List<String> competitionTypes = extractCompetitionTypes(template);

        // Collect all competitions of the required types
        List<Competition> competitions = competitionTypes.stream()
            .flatMap(type -> competitionRepository.findByCompetitionType(type).stream())
            .distinct()
            .collect(Collectors.toList());

        // If the schema restricts by tier, filter to tier-1 only
        if (shouldRestrictToTopFlight(template)) {
            competitions = competitions.stream()
                .filter(c -> Short.valueOf((short) 1).equals(c.getTier()))
                .collect(Collectors.toList());
        }

        if (competitions.isEmpty()) {
            log.warn("Template {} ({}): no competitions found for types {} — returning empty enumeration.",
                template.getId(), template.getSlug(), competitionTypes);
            return List.of();
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (Competition comp : competitions) {
            // Find all teams that have at least one stint in this competition
            // with a season starting on or after startYear.
            List<UUID> teamIds = findTeamIdsWithStints(comp.getId(), startYear);

            for (UUID teamId : teamIds) {
                results.add(Map.of(
                    "team_id",        teamId.toString(),
                    "competition_id", comp.getId().toString(),
                    "start_year",     String.valueOf(startYear),
                    "competition_name", comp.getDisplayName() != null ? comp.getDisplayName() : comp.getName()
                ));
            }
        }

        log.info("Template {} ({}): enumerated {} param sets.",
            template.getId(), template.getSlug(), results.size());
        return results;
    }

    // ── Materialization ──────────────────────────────────────────────────────

    /**
     * Queries {@code player_season_stints} for the given (team, competition, since)
     * and returns one {@link MaterializedAnswer} per player with a positive score.
     */
    @Override
    public List<MaterializedAnswer> materialize(MaterializationContext ctx) {
        UUID teamId        = ctx.uuidParam("team_id");
        UUID competitionId = ctx.uuidParam("competition_id");
        int  startYear     = ctx.intParam("start_year");

        String metricKey = ctx.template() != null
            ? ctx.template().getMetricKey()
            : ctx.question().getMetricKey();

        if (!METRIC_LABELS.containsKey(metricKey)) {
            throw new IllegalArgumentException(
                "Unknown metric_key: '" + metricKey + "'. Valid: " + METRIC_LABELS.keySet());
        }

        log.debug("Materializing: team={}, comp={}, since={}, metric={}",
            teamId, competitionId, startYear, metricKey);

        List<PlayerSeasonStintRepository.StintAggregate> aggregates =
            stintRepository.aggregateByTeamCompetitionSince(teamId, competitionId, startYear);

        if (aggregates.isEmpty()) {
            log.warn("No stint data found for team={}, comp={}, since={}",
                teamId, competitionId, startYear);
            return List.of();
        }

        List<MaterializedAnswer> answers = new ArrayList<>();

        for (PlayerSeasonStintRepository.StintAggregate agg : aggregates) {
            int score = resolveMetric(agg, metricKey);
            if (score <= 0) {
                continue;  // skip players with no contribution for this metric
            }

            Optional<Player> playerOpt = playerRepository.findById(agg.getPlayerId());
            if (playerOpt.isEmpty()) {
                log.warn("Player not found: {}", agg.getPlayerId());
                continue;
            }
            Player player = playerOpt.get();

            String answerKey  = player.getNormalizedName();
            String displayText = player.getName();

            answers.add(new MaterializedAnswer(
                answerKey,
                displayText,
                score,
                Map.of(
                    "player_id",    player.getId().toString(),
                    "team_id",      teamId.toString(),
                    "competition_id", competitionId.toString(),
                    "start_year",   startYear,
                    "metric_key",   metricKey
                )
            ));
        }

        log.info("Materialized {} answers for question {} (metric={}, team={}, comp={}, since={})",
            answers.size(), ctx.question().getId(), metricKey, teamId, competitionId, startYear);
        return answers;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private int resolveMetric(PlayerSeasonStintRepository.StintAggregate agg, String metricKey) {
        return switch (metricKey) {
            case "goals"                    -> (int) agg.getTotalGoals();
            case "appearances"              -> (int) agg.getTotalAppearances();
            case "assists"                  -> (int) agg.getTotalAssists();
            case "clean_sheets"             -> (int) agg.getTotalCleanSheets();
            case "sub_appearances"          -> (int) agg.getTotalSubAppearances();
            case "goals_assists"            -> (int)(agg.getTotalGoals()       + agg.getTotalAssists());
            case "goals_appearances"        -> (int)(agg.getTotalGoals()       + agg.getTotalAppearances());
            case "assists_appearances"      -> (int)(agg.getTotalAssists()     + agg.getTotalAppearances());
            case "goals_assists_appearances"-> (int)(agg.getTotalGoals()       + agg.getTotalAssists()
                                                   + agg.getTotalAppearances());
            default -> throw new IllegalArgumentException("Unknown metric_key: " + metricKey);
        };
    }

    /**
     * Returns distinct team UUIDs that have at least one stint in the given
     * competition where the season started on or after {@code startYear}.
     */
    private List<UUID> findTeamIdsWithStints(UUID competitionId, int startYear) {
        return stintRepository.findDistinctTeamIdsByCompetitionSince(competitionId, startYear);
    }

    @SuppressWarnings("unchecked")
    private int extractStartYear(QuestionTemplate template) {
        try {
            Map<String, Object> schema = template.getParamSchema();
            Map<String, Object> params = (Map<String, Object>) schema.get("params");
            if (params != null) {
                Map<String, Object> startYearDef = (Map<String, Object>) params.get("start_year");
                if (startYearDef != null) {
                    List<Object> values = (List<Object>) startYearDef.get("values");
                    if (values != null && !values.isEmpty()) {
                        return Integer.parseInt(values.get(0).toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract start_year from template param_schema, using default {}: {}",
                DEFAULT_START_YEAR, e.getMessage());
        }
        return DEFAULT_START_YEAR;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCompetitionTypes(QuestionTemplate template) {
        try {
            Map<String, Object> schema = template.getParamSchema();
            Map<String, Object> params = (Map<String, Object>) schema.get("params");
            if (params != null) {
                Map<String, Object> compDef = (Map<String, Object>) params.get("competition_id");
                if (compDef != null) {
                    List<Object> types = (List<Object>) compDef.get("competition_types");
                    if (types != null && !types.isEmpty()) {
                        return types.stream().map(Object::toString).collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract competition_types from template param_schema: {}", e.getMessage());
        }
        return List.of("domestic_league");
    }

    @SuppressWarnings("unchecked")
    private boolean shouldRestrictToTopFlight(QuestionTemplate template) {
        try {
            Map<String, Object> schema = template.getParamSchema();
            Map<String, Object> params = (Map<String, Object>) schema.get("params");
            if (params != null) {
                Map<String, Object> compDef = (Map<String, Object>) params.get("competition_id");
                if (compDef != null) {
                    Object topFlightOnly = compDef.get("top_flight_only");
                    if (topFlightOnly != null) {
                        return Boolean.parseBoolean(topFlightOnly.toString());
                    }
                }
            }
        } catch (Exception ignored) { }
        return true;   // default: restrict to top-flight (tier=1)
    }
}
