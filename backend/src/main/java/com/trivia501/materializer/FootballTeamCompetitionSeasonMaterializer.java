package com.trivia501.materializer;

import com.trivia501.model.*;
import com.trivia501.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Materializes questions of the form:
 * <pre>
 *   "Goals for Manchester United in the Premier League 2023-24"
 *   "Appearances for Arsenal in the Champions League 2018-19"
 * </pre>
 *
 * <p>Unlike {@link FootballTeamCompetitionMetricSinceMaterializer}, which aggregates
 * stats across all seasons since a start year, this materializer is scoped to a
 * <em>single season</em>.  This produces sharper, harder questions and supports
 * all competition types (domestic leagues, cups, continental) wherever scrape data
 * exists in {@code player_season_stints}.
 *
 * <h3>Template params consumed</h3>
 * <ul>
 *   <li>{@code team_id}        — UUID of the team</li>
 *   <li>{@code competition_id} — UUID of the competition</li>
 *   <li>{@code season_id}      — UUID of the specific season</li>
 * </ul>
 *
 * <p>The params map also carries denormalized display values
 * ({@code team_name}, {@code competition_name}, {@code season_label}) which the
 * {@link com.trivia501.service.QuestionGeneratorService} uses when rendering
 * the question text.
 *
 * <h3>Enumeration</h3>
 * <p>For each competition of the configured types, every distinct
 * (team, season) pair that has at least one {@code player_season_stints} row
 * is returned as a separate param set.  For the default Top-5 European
 * domestic-league dataset this yields roughly 2,600 combinations
 * (5 leagues × ~20 teams × 26 seasons).  All are created as draft questions;
 * admins activate selectively.
 *
 * <h3>Supported metrics</h3>
 * goals, appearances, assists, clean_sheets
 *
 * <h3>Adding new competition types</h3>
 * <p>When Champions League or cup data is scraped, simply add the relevant rows
 * to {@code competitions} and {@code player_season_stints}.  No code changes are
 * needed — the enumerator picks up new (team, competition, season) triplets
 * automatically on the next generator run.
 */
@Component
@Slf4j
public class FootballTeamCompetitionSeasonMaterializer implements QuestionMaterializer {

    public static final String KEY = "football.team_competition_season_metric";

    private static final Map<String, String> METRIC_LABELS = Map.of(
        "goals",           "Goals",
        "appearances",     "Appearances",
        "assists",         "Assists",
        "clean_sheets",    "Clean sheets",
        "sub_appearances", "Substitute appearances"
    );

    /**
     * Competition types enumerated by default when the template param_schema does
     * not override {@code competition_types}.  Deliberately broader than the
     * "since" materializer (which defaults to domestic_league only) so that
     * Champions League and cup questions are generated automatically once data
     * is available.
     */
    private static final List<String> DEFAULT_COMPETITION_TYPES =
        List.of("domestic_league", "domestic_cup", "continental_club");

    private final PlayerSeasonStintRepository stintRepository;
    private final PlayerRepository            playerRepository;
    private final TeamRepository              teamRepository;
    private final CompetitionRepository       competitionRepository;
    private final SeasonRepository            seasonRepository;

    public FootballTeamCompetitionSeasonMaterializer(
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

    // ── Enumeration ──────────────────────────────────────────────────────────

    /**
     * Returns one param set per (team, competition, season) triplet that has
     * actual stint data.
     *
     * <p>All seasons in the database are included.  Seasons with no data for a
     * given (team, competition) are naturally absent because
     * {@link PlayerSeasonStintRepository#findDistinctTeamSeasonByCompetition}
     * only returns rows that exist in {@code player_season_stints}.
     *
     * <p>Each returned map includes the denormalized display values
     * {@code team_name}, {@code competition_name}, and {@code season_label} so
     * {@link com.trivia501.service.QuestionGeneratorService} can render the
     * question text without additional lookups.
     */
    @Override
    public List<Map<String, Object>> enumerateParams(QuestionTemplate template) {
        List<String> competitionTypes = extractCompetitionTypes(template);

        List<Competition> competitions = competitionTypes.stream()
            .flatMap(type -> competitionRepository.findByCompetitionType(type).stream())
            .distinct()
            .collect(Collectors.toList());

        if (competitions.isEmpty()) {
            log.warn("Template {} ({}): no competitions found for types {} — returning empty enumeration.",
                template.getId(), template.getSlug(), competitionTypes);
            return List.of();
        }

        // Pre-load all seasons into a map to avoid N+1 lookups per pair.
        // There are typically ~26 seasons so this is cheap.
        Map<UUID, Season> seasonMap = seasonRepository.findAll().stream()
            .collect(Collectors.toMap(Season::getId, s -> s));

        List<Map<String, Object>> results = new ArrayList<>();

        for (Competition comp : competitions) {
            String compName = comp.getDisplayName() != null ? comp.getDisplayName() : comp.getName();

            List<PlayerSeasonStintRepository.TeamSeasonPair> pairs =
                stintRepository.findDistinctTeamSeasonByCompetition(comp.getId());

            for (PlayerSeasonStintRepository.TeamSeasonPair pair : pairs) {
                Season season = seasonMap.get(pair.getSeasonId());
                if (season == null) {
                    log.warn("Season {} not found in season map — skipping.", pair.getSeasonId());
                    continue;
                }

                Optional<Team> teamOpt = teamRepository.findById(pair.getTeamId());
                if (teamOpt.isEmpty()) {
                    log.warn("Team {} not found — skipping.", pair.getTeamId());
                    continue;
                }

                results.add(Map.of(
                    "team_id",          pair.getTeamId().toString(),
                    "competition_id",   comp.getId().toString(),
                    "season_id",        pair.getSeasonId().toString(),
                    "team_name",        teamOpt.get().getName(),
                    "competition_name", compName,
                    "season_label",     season.getLabel()
                ));
            }
        }

        log.info("Template {} ({}): enumerated {} param sets.",
            template.getId(), template.getSlug(), results.size());
        return results;
    }

    // ── Materialization ──────────────────────────────────────────────────────

    /**
     * Aggregates {@code player_season_stints} for the exact (team, competition,
     * season) triplet and returns one {@link MaterializedAnswer} per player with
     * a positive score for the configured metric.
     */
    @Override
    public List<MaterializedAnswer> materialize(MaterializationContext ctx) {
        UUID teamId        = ctx.uuidParam("team_id");
        UUID competitionId = ctx.uuidParam("competition_id");
        UUID seasonId      = ctx.uuidParam("season_id");

        String metricKey = ctx.template() != null
            ? ctx.template().getMetricKey()
            : ctx.question().getMetricKey();

        if (!METRIC_LABELS.containsKey(metricKey)) {
            throw new IllegalArgumentException(
                "Unknown metric_key: '" + metricKey + "'. Valid: " + METRIC_LABELS.keySet());
        }

        // Resolve the season label for logging
        String seasonLabel = ctx.templateParams().getOrDefault("season_label", seasonId.toString()).toString();

        log.debug("Materializing: team={}, comp={}, season={}, metric={}",
            teamId, competitionId, seasonLabel, metricKey);

        List<PlayerSeasonStintRepository.StintAggregate> aggregates =
            stintRepository.aggregateByTeamCompetitionSeason(teamId, competitionId, seasonId);

        if (aggregates.isEmpty()) {
            log.warn("No stint data found for team={}, comp={}, season={}",
                teamId, competitionId, seasonLabel);
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
                    "team_id",        teamId.toString(),
                    "competition_id", competitionId.toString(),
                    "season_id",      seasonId.toString(),
                    "season_label",   seasonLabel,
                    "metric_key",     metricKey
                )
            ));
        }

        log.info("Materialised {} answers for question {} (metric={}, team={}, comp={}, season={})",
            answers.size(), ctx.question().getId(), metricKey, teamId, competitionId, seasonLabel);
        return answers;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private int resolveMetric(PlayerSeasonStintRepository.StintAggregate agg, String metricKey) {
        return switch (metricKey) {
            case "goals"           -> (int) agg.getTotalGoals();
            case "appearances"     -> (int) agg.getTotalAppearances();
            case "assists"         -> (int) agg.getTotalAssists();
            case "clean_sheets"    -> (int) agg.getTotalCleanSheets();
            case "sub_appearances" -> (int) agg.getTotalSubAppearances();
            default -> throw new IllegalArgumentException("Unknown metric_key: " + metricKey);
        };
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
        return DEFAULT_COMPETITION_TYPES;
    }
}
