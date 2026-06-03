package com.trivia501.materializer;

import com.trivia501.model.Question;
import com.trivia501.model.QuestionTemplate;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for converting a {@link QuestionTemplate} + param bindings
 * into a set of pre-computed answer rows.
 *
 * <h3>Registration</h3>
 * <p>Each implementation must be a Spring {@code @Component} (or {@code @Service})
 * and must declare a static {@code MATERIALIZER_KEY} constant that matches the
 * value stored in {@link QuestionTemplate#getMaterializerKey()}.
 *
 * <p>{@link com.trivia501.service.QuestionMaterializerService} auto-discovers all
 * implementations and dispatches to the correct one by key.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li><b>enumerate</b> — given an active template, return every valid set of
 *       concrete param bindings (e.g. every (team, competition, start_year) combo
 *       where real stint data exists).  The generator job calls this to produce
 *       draft {@link Question} rows without duplicates.</li>
 *   <li><b>materialize</b> — given a concrete question + its params, query the
 *       football source layer and return the answer rows to upsert.  Called when
 *       an admin promotes a draft to active, and by the stale-answer detector.</li>
 * </ol>
 *
 * <h3>Idempotency contract</h3>
 * <p>Both methods must be idempotent. {@code enumerate} must not insert anything;
 * {@code materialize} must be safe to call multiple times (upsert semantics).
 */
public interface QuestionMaterializer {

    /**
     * The unique key that links this materializer to a {@code question_templates} row.
     * Example: {@code "football.team_competition_metric_since"}
     */
    String getMaterializerKey();

    /**
     * Enumerate all valid concrete parameter sets for the given template.
     *
     * <p>Each returned map becomes the {@code template_params} of one draft question.
     * The generator job calls this and skips any param set where a draft or active
     * question with those params already exists.
     *
     * @param template the active template row
     * @return list of param maps; each map must contain every key declared in
     *         the template's {@code param_schema}
     */
    List<Map<String, Object>> enumerateParams(QuestionTemplate template);

    /**
     * Materialise answer rows for a single question.
     *
     * <p>The implementation must read only from the football source layer
     * ({@code player_season_stints}, {@code players}, {@code seasons}, …) and
     * never from the {@code answers} table.  The caller
     * ({@link com.trivia501.service.QuestionMaterializerService}) is responsible
     * for upserting the returned rows into {@code answers} and {@code entities}.
     *
     * @param context carries the question, template, and resolved params
     * @return the computed answer rows; may be empty if no data exists yet
     */
    List<MaterializedAnswer> materialize(MaterializationContext context);
}
