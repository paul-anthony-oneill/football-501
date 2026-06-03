package com.trivia501.materializer;

import com.trivia501.model.*;
import com.trivia501.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Materializes league-wide player questions of the form:
 * <pre>
 *   "Goals in the Premier League since 2000"
 *   "Goals + Assists in La Liga since 2000"
 *   "Substitute appearances in the Bundesliga since 2000"
 * </pre>
 *
 * <p>Unlike {@link FootballTeamCompetitionMetricSinceMaterializer}, which scopes
 * answers to a single club, this materializer covers <em>every player</em> who
 * appeared in the given competition since the start year.  This produces a much
 * larger answer pool and is suited to harder, open-ended questions.
 *
 * <h3>Template params consumed</h3>
 * <ul>
 *   <li>{@code competition_id}   — UUID of the competition</li>
 *   <li>{@code competition_name} — denormalised display name (set during enumeration)</li>
 *   <li>{@code start_year}       — inclusive lower bound on season start year</li>
 * </ul>
 *
 * <h3>Supported metric keys</h3>
 * <ul>
 *   <li>{@code goals}           — total goals in the competition</li>
 *   <li>{@code appearances}     — total appearances in the competition</li>
 *   <li>{@code assists}         — total assists in the competition</li>
 *   <li>{@code goals_assists}   — goals + assists combined</li>
 *   <li>{@code clean_sheets}    — goalkeeper clean sheets (use with care — filters to ~5–10 answers)</li>
 *   <li>{@code sub_appearances} — substitute appearances only</li>
 * </ul>
 *
 * <h3>Enumeration</h3>
 * <p>Returns one param set per competition that has at least one stint row with a
 * season starting on or after {@code start_year}.  Competitions are filtered by
 * type and optionally restricted to top-flight only, both controlled via the
 * template's {@code param_schema}.
 */
@Component
@Slf4j
public class FootballPlayerCompetitionMetricSinceMaterializer implements QuestionMaterializer {

    public static final String KEY = "football.player_competition_metric_since";

    private static final Map<String, String> METRIC_LABELS = Map.of(
        "goals",           "Goals",
        "appearances",     "Appearances",
        "assists",         "Assists",
        "goals_assists",   "Goals + Assists",
        "clean_sheets",    "Clean sheets",
        "sub_appearances", "Substitute appearances"
    );

    private static final int DEFAULT_START_YEAR = 2000;

    private final PlayerSeasonStintRepository stintRepository;
    private final PlayerRepository            playerRepository;
    private final CompetitionRepository       competitionRepository;

    public FootballPlayerCompetitionMetricSinceMaterializer(
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
     * Returns one param set per competition that both matches the configured type
     * filter <em>and</em> has real stint data since the configured start year.
     *
     * <p>Each param map includes a denormalised {@code competition_name} so the
     * question generator can render the question text without an extra DB round-trip.
     */
    @Override
    public List<Map<String, Object>> enumerateParams(QuestionTemplate template) {
        int startYear              = extractStartYear(template);
        List<String> compTypes     = extractCompetitionTypes(template);
        boolean topFlightOnly      = shouldRestrictToTopFlight(template);

        // Competitions that match the type filter
        List<Competition> competitions = compTypes.stream()
            .flatMap(type -> competitionRepository.findByCompetitionType(type).stream())
            .distinct()
            .collect(Collectors.toList());

        if (topFlightOnly) {
            competitions = competitions.stream()
                .filter(c -> Short.valueOf((short) 1).equals(c.getTier()))
                .collect(Collectors.toList());
        }

        if (competitions.isEmpty()) {
            log.warn("Template {} ({}): no competitions found for types {} — empty enumeration.",
                template.getId(), template.getSlug(), compTypes);
            return List.of();
        }

        // Only keep competitions where stint data actually exists
        Set<UUID> competitionsWithData = new HashSet<>(
            stintRepository.findDistinctCompetitionIdsSince(startYear)
        );

        List<Map<String, Object>> results = new ArrayList<>();

        for (Competition comp : competitions) {
            if (!competitionsWithData.contains(comp.getId())) {
                log.debug("Skipping competition {} — no stint data since {}.", comp.getName(), startYear);
                continue;
            }

            String compName = comp.getDisplayName() != null ? comp.getDisplayName() : comp.getName();

            results.add(Map.of(
                "competition_id",   comp.getId().toString(),
                "competition_name", compName,
                "start_year",       String.valueOf(startYear)
            ));
        }

        log.info("Template {} ({}): enumerated {} param sets.",
            template.getId(), template.getSlug(), results.size());
        return results;
    }

    // ── Materialization ──────────────────────────────────────────────────────

    /**
     * Aggregates {@code player_season_stints} across all teams in the competition
     * since {@code start_year} and returns one {@link MaterializedAnswer} per player
     * with a positive score for the configured metric.
     */
    @Override
    public List<MaterializedAnswer> materialize(MaterializationContext ctx) {
        UUID competitionId = ctx.uuidParam("competition_id");
        int  startYear     = ctx.intParam("start_year");

        String metricKey = ctx.template() != null
            ? ctx.template().getMetricKey()
            : ctx.question().getMetricKey();

        if (!METRIC_LABELS.containsKey(metricKey)) {
            throw new IllegalArgumentException(
                "Unknown metric_key: '" + metricKey + "'. Valid: " + METRIC_LABELS.keySet());
        }

        log.debug("Materializing: comp={}, since={}, metric={}", competitionId, startYear, metricKey);

        List<PlayerSeasonStintRepository.StintAggregate> aggregates =
            stintRepository.aggregateByCompetitionSince(competitionId, startYear);

        if (aggregates.isEmpty()) {
            log.warn("No stint data found for comp={}, since={}", competitionId, startYear);
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
                    "player_id",      player.getId().toString(),
                    "competition_id", competitionId.toString(),
                    "start_year",     startYear,
                    "metric_key",     metricKey
                )
            ));
        }

        log.info("Materialised {} answers for question {} (metric={}, comp={}, since={})",
            answers.size(), ctx.question().getId(), metricKey, competitionId, startYear);
        return answers;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private int resolveMetric(PlayerSeasonStintRepository.StintAggregate agg, String metricKey) {
        return switch (metricKey) {
            case "goals"           -> (int) agg.getTotalGoals();
            case "appearances"     -> (int) agg.getTotalAppearances();
            case "assists"         -> (int) agg.getTotalAssists();
            case "goals_assists"   -> (int) (agg.getTotalGoals() + agg.getTotalAssists());
            case "clean_sheets"    -> (int) agg.getTotalCleanSheets();
            case "sub_appearances" -> (int) agg.getTotalSubAppearances();
            default -> throw new IllegalArgumentException("Unknown metric_key: " + metricKey);
        };
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
                Map<String, Object> compDef = (Map<String, Object>) params.get("competition_id");
                if (compDef != null) {
                    List<Object> types = (List<Object>) compDef.get("competition_types");
                    if (types != null && !types.isEmpty()) {
                        return types.stream().map(Object::toString).collect(Collectors.toList());
                    }
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
                Map<String, Object> compDef = (Map<String, Object>) params.get("competition_id");
                if (compDef != null) {
                    Object topFlightOnly = compDef.get("top_flight_only");
                    if (topFlightOnly != null) {
                        return Boolean.parseBoolean(topFlightOnly.toString());
                    }
                }
            }
        } catch (Exception ignored) { }
        return true;
    }
}
