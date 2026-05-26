package com.football501.materializer;

import com.football501.model.*;
import com.football501.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Materializes career-total questions of the form:
 * <pre>
 *   "Career goals in top-flight football since 2000"
 *   "Career appearances in top-flight football since 2000"
 * </pre>
 *
 * <p>Unlike the team- or competition-scoped materializers, this one aggregates
 * across <em>all</em> qualifying competitions (e.g. all top-flight domestic leagues)
 * for a player's entire career since a start year.  It produces the largest possible
 * answer pool and is best suited to a single global question per metric.
 *
 * <h3>Template params consumed</h3>
 * <ul>
 *   <li>{@code start_year} — inclusive lower bound on season start year (default 2000)</li>
 * </ul>
 *
 * <p>Competition filtering (type, top-flight-only) is read from the template's
 * {@code param_schema} rather than from the question params, because the competition
 * set is a property of the template, not of an individual question instance.
 *
 * <h3>Supported metric keys</h3>
 * goals, appearances, assists, goals_assists, sub_appearances
 *
 * <h3>Enumeration</h3>
 * <p>Returns exactly one param set per template (since the competition set and
 * start year are fixed by the template).  For example, there is only one
 * "Career goals in top-flight football since 2000" question — it does not vary
 * by team or competition.
 */
@Component
@Slf4j
public class FootballPlayerCareerMetricMaterializer implements QuestionMaterializer {

    public static final String KEY = "football.player_career_metric";

    private static final Map<String, String> METRIC_LABELS = Map.of(
        "goals",           "Goals",
        "appearances",     "Appearances",
        "assists",         "Assists",
        "goals_assists",   "Goals + Assists",
        "sub_appearances", "Substitute appearances"
    );

    private static final int DEFAULT_START_YEAR = 2000;

    private final PlayerSeasonStintRepository stintRepository;
    private final PlayerRepository            playerRepository;
    private final CompetitionRepository       competitionRepository;

    public FootballPlayerCareerMetricMaterializer(
            PlayerSeasonStintRepository stintRepository,
            PlayerRepository            playerRepository,
            CompetitionRepository       competitionRepository
    ) {
        this.stintRepository       = stintRepository;
        this.playerRepository      = playerRepository;
        this.competitionRepository = competitionRepository;
    }

    @Override
    public String getMaterializerKey() {
        return KEY;
    }

    // ── Enumeration ──────────────────────────────────────────────────────────

    /**
     * Returns a single param set for the template — career questions are global,
     * not parameterized per competition or team.
     *
     * <p>The single param set contains only {@code start_year}.  Competition
     * filtering happens inside {@link #materialize} using the template's
     * {@code param_schema}.
     */
    @Override
    public List<Map<String, Object>> enumerateParams(QuestionTemplate template) {
        int startYear = extractStartYear(template);
        return List.of(Map.of("start_year", String.valueOf(startYear)));
    }

    // ── Materialization ──────────────────────────────────────────────────────

    /**
     * Aggregates career totals across all qualifying competitions for every player
     * and returns one {@link MaterializedAnswer} per player with a positive score.
     */
    @Override
    public List<MaterializedAnswer> materialize(MaterializationContext ctx) {
        int    startYear = ctx.intParam("start_year");
        String metricKey = ctx.template() != null
            ? ctx.template().getMetricKey()
            : ctx.question().getMetricKey();

        if (!METRIC_LABELS.containsKey(metricKey)) {
            throw new IllegalArgumentException(
                "Unknown metric_key: '" + metricKey + "'. Valid: " + METRIC_LABELS.keySet());
        }

        // Resolve the set of competitions to aggregate over from the template schema.
        List<UUID> competitionIds = resolveCompetitionIds(ctx.template());
        if (competitionIds.isEmpty()) {
            log.warn("No matching competitions found for question {} — returning empty.", ctx.question().getId());
            return List.of();
        }

        log.debug("Materializing career {}: since={}, across {} competitions",
            metricKey, startYear, competitionIds.size());

        List<PlayerSeasonStintRepository.StintAggregate> aggregates =
            stintRepository.aggregateCareerTotalsSince(startYear, competitionIds);

        if (aggregates.isEmpty()) {
            log.warn("No stint data found for career {} since {}", metricKey, startYear);
            return List.of();
        }

        List<MaterializedAnswer> answers = new ArrayList<>();

        for (PlayerSeasonStintRepository.StintAggregate agg : aggregates) {
            int score = resolveMetric(agg, metricKey);
            if (score <= 0) {
                continue;
            }

            Optional<Player> playerOpt = playerRepository.findById(agg.getPlayerId());
            if (playerOpt.isEmpty()) {
                log.warn("Player not found: {}", agg.getPlayerId());
                continue;
            }
            Player player = playerOpt.get();

            answers.add(new MaterializedAnswer(
                player.getNormalizedName(),
                player.getName(),
                score,
                Map.of(
                    "player_id",   player.getId().toString(),
                    "start_year",  startYear,
                    "metric_key",  metricKey
                )
            ));
        }

        log.info("Materialised {} career answers for question {} (metric={}, since={})",
            answers.size(), ctx.question().getId(), metricKey, startYear);
        return answers;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private int resolveMetric(PlayerSeasonStintRepository.StintAggregate agg, String metricKey) {
        return switch (metricKey) {
            case "goals"           -> (int) agg.getTotalGoals();
            case "appearances"     -> (int) agg.getTotalAppearances();
            case "assists"         -> (int) agg.getTotalAssists();
            case "goals_assists"   -> (int) (agg.getTotalGoals() + agg.getTotalAssists());
            case "sub_appearances" -> (int) agg.getTotalSubAppearances();
            default -> throw new IllegalArgumentException("Unknown metric_key: " + metricKey);
        };
    }

    /**
     * Resolves the set of competition IDs to aggregate over, based on the template's
     * {@code param_schema}.  Reads {@code competition_types} (default: domestic_league)
     * and {@code top_flight_only} (default: true).
     */
    private List<UUID> resolveCompetitionIds(QuestionTemplate template) {
        List<String> compTypes    = extractCompetitionTypes(template);
        boolean topFlightOnly     = shouldRestrictToTopFlight(template);

        List<Competition> competitions = compTypes.stream()
            .flatMap(type -> competitionRepository.findByCompetitionType(type).stream())
            .distinct()
            .collect(Collectors.toList());

        if (topFlightOnly) {
            competitions = competitions.stream()
                .filter(c -> Short.valueOf((short) 1).equals(c.getTier()))
                .collect(Collectors.toList());
        }

        return competitions.stream().map(Competition::getId).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private int extractStartYear(QuestionTemplate template) {
        try {
            Map<String, Object> schema = template.getParamSchema();
            Map<String, Object> params = (Map<String, Object>) schema.get("params");
            if (params != null) {
                Map<String, Object> def = (Map<String, Object>) params.get("start_year");
                if (def != null) {
                    List<Object> values = (List<Object>) def.get("values");
                    if (values != null && !values.isEmpty()) {
                        return Integer.parseInt(values.get(0).toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract start_year from param_schema, using default {}: {}",
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
                List<Object> types = (List<Object>) params.get("competition_types");
                if (types != null && !types.isEmpty()) {
                    return types.stream().map(Object::toString).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract competition_types from param_schema: {}", e.getMessage());
        }
        return List.of("domestic_league");
    }

    @SuppressWarnings("unchecked")
    private boolean shouldRestrictToTopFlight(QuestionTemplate template) {
        try {
            Map<String, Object> schema = template.getParamSchema();
            Map<String, Object> params = (Map<String, Object>) schema.get("params");
            if (params != null) {
                Object topFlightOnly = params.get("top_flight_only");
                if (topFlightOnly != null) {
                    return Boolean.parseBoolean(topFlightOnly.toString());
                }
            }
        } catch (Exception ignored) { }
        return true;
    }
}
