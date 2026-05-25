package com.football501.service;

import com.football501.materializer.QuestionMaterializer;
import com.football501.model.*;
import com.football501.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates draft {@link Question} rows from active {@link QuestionTemplate}s.
 *
 * <h3>Pipeline</h3>
 * <ol>
 *   <li>Read all active templates.</li>
 *   <li>For each template, find the registered {@link QuestionMaterializer} and
 *       call {@link QuestionMaterializer#enumerateParams} to get the full set of
 *       valid param combinations.</li>
 *   <li>For each param set, build the question text from the template's
 *       {@code text_template} and insert a draft {@link Question} — skipping any
 *       combo that already has a draft or active question.</li>
 * </ol>
 *
 * <p>This service is idempotent: running it multiple times only creates missing
 * combinations; it never duplicates or modifies existing questions.
 *
 * <h3>Triggering</h3>
 * <ul>
 *   <li>Admin triggers manually via {@code POST /api/admin/templates/generate}.</li>
 *   <li>Nightly scheduled job (future: add a {@code @Scheduled} annotation once
 *       templates are fully seeded and verified).</li>
 * </ul>
 */
@Service
@Slf4j
public class QuestionGeneratorService {

    private final QuestionTemplateRepository templateRepository;
    private final QuestionRepository         questionRepository;
    private final CompetitionRepository      competitionRepository;
    private final TeamRepository             teamRepository;
    private final Map<String, QuestionMaterializer> materializersByKey;

    public QuestionGeneratorService(
            QuestionTemplateRepository    templateRepository,
            QuestionRepository            questionRepository,
            CompetitionRepository         competitionRepository,
            TeamRepository                teamRepository,
            List<QuestionMaterializer>    materializers
    ) {
        this.templateRepository    = templateRepository;
        this.questionRepository    = questionRepository;
        this.competitionRepository = competitionRepository;
        this.teamRepository        = teamRepository;
        this.materializersByKey    = materializers.stream()
            .collect(Collectors.toMap(QuestionMaterializer::getMaterializerKey, Function.identity()));
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Runs the generator for all active templates and returns the total number
     * of new draft questions created.
     */
    @Transactional
    public GeneratorResult generateAll() {
        List<QuestionTemplate> templates = templateRepository.findByIsActiveTrue();
        log.info("Generator: {} active template(s) found.", templates.size());

        int totalCreated = 0;
        int totalSkipped = 0;

        for (QuestionTemplate template : templates) {
            GeneratorResult r = generateForTemplate(template);
            totalCreated += r.created();
            totalSkipped += r.skipped();
        }

        log.info("Generator complete: {} created, {} skipped (already exist).",
            totalCreated, totalSkipped);
        return new GeneratorResult(totalCreated, totalSkipped);
    }

    /**
     * Runs the generator for a single template by ID.
     */
    @Transactional
    public GeneratorResult generateForTemplate(UUID templateId) {
        QuestionTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        return generateForTemplate(template);
    }

    // ── Internal generation logic ────────────────────────────────────────────

    private GeneratorResult generateForTemplate(QuestionTemplate template) {
        QuestionMaterializer materializer = materializersByKey.get(template.getMaterializerKey());
        if (materializer == null) {
            log.warn("Template {} ({}): no materializer registered for key '{}' — skipping.",
                template.getId(), template.getSlug(), template.getMaterializerKey());
            return new GeneratorResult(0, 0);
        }

        List<Map<String, Object>> paramSets = materializer.enumerateParams(template);
        log.info("Template {} ({}): {} param set(s) enumerated.",
            template.getId(), template.getSlug(), paramSets.size());

        int created = 0;
        int skipped = 0;

        for (Map<String, Object> params : paramSets) {
            try {
                boolean wasCreated = createDraftIfAbsent(template, params);
                if (wasCreated) created++;
                else            skipped++;
            } catch (Exception ex) {
                log.error("Error creating draft for template {} with params {}: {}",
                    template.getSlug(), params, ex.getMessage(), ex);
            }
        }

        log.info("Template {} ({}): {} new draft(s), {} already exist.",
            template.getId(), template.getSlug(), created, skipped);
        return new GeneratorResult(created, skipped);
    }

    /**
     * Creates a draft question for the given param set if none exists yet.
     *
     * @return {@code true} if a new row was inserted; {@code false} if skipped
     */
    private boolean createDraftIfAbsent(QuestionTemplate template, Map<String, Object> params) {
        // Check for an existing question with the same (template_id, template_params)
        // by checking if any non-retired question has the same template + params combo.
        boolean exists = questionRepository
            .existsByTemplateIdAndTemplateParamsAndStatusNot(
                template.getId(), params, Question.STATUS_RETIRED);

        if (exists) {
            return false;
        }

        String questionText = renderQuestionText(template, params);

        Question draft = Question.builder()
            .categoryId(template.getCategoryId())
            .questionText(questionText)
            .metricKey(template.getMetricKey())
            .config(buildConfig(template, params))
            .minScore(template.getDefaultMinScore())
            .difficulty(2)
            .status(Question.STATUS_DRAFT)
            .templateId(template.getId())
            .templateParams(params)
            .build();

        questionRepository.save(draft);
        log.debug("Created draft: {}", questionText);
        return true;
    }

    // ── Question-text rendering ──────────────────────────────────────────────

    /**
     * Fills in the template's {@code text_template} placeholders with resolved
     * display names from the params map.
     *
     * <p>Placeholder values are resolved in this order:
     * <ol>
     *   <li>Params already contain a display value (e.g. {@code "competition_name"}
     *       added by the enumerator for convenience).</li>
     *   <li>Look up the entity by UUID in the corresponding repository.</li>
     *   <li>Fall back to the raw param value.</li>
     * </ol>
     */
    private String renderQuestionText(QuestionTemplate template, Map<String, Object> params) {
        String text = template.getTextTemplate();

        // Resolve team name
        if (text.contains("{team_name}")) {
            String teamName = resolveTeamName(params);
            text = text.replace("{team_name}", teamName);
        }

        // Resolve competition name
        if (text.contains("{competition_name}")) {
            String compName = resolveCompetitionName(params);
            text = text.replace("{competition_name}", compName);
        }

        // Resolve start year
        if (text.contains("{start_year}")) {
            text = text.replace("{start_year}", params.getOrDefault("start_year", "2000").toString());
        }

        return text;
    }

    private String resolveTeamName(Map<String, Object> params) {
        // If the enumerator already put a display name in
        Object teamName = params.get("team_name");
        if (teamName != null) return teamName.toString();

        // Look up by UUID
        Object teamId = params.get("team_id");
        if (teamId != null) {
            try {
                return teamRepository.findById(UUID.fromString(teamId.toString()))
                    .map(Team::getName)
                    .orElse(teamId.toString());
            } catch (Exception e) {
                return teamId.toString();
            }
        }
        return "(unknown team)";
    }

    private String resolveCompetitionName(Map<String, Object> params) {
        Object compName = params.get("competition_name");
        if (compName != null) return compName.toString();

        Object compId = params.get("competition_id");
        if (compId != null) {
            try {
                return competitionRepository.findById(UUID.fromString(compId.toString()))
                    .map(c -> c.getDisplayName() != null ? c.getDisplayName() : c.getName())
                    .orElse(compId.toString());
            } catch (Exception e) {
                return compId.toString();
            }
        }
        return "(unknown competition)";
    }

    /**
     * Builds the {@code config} JSONB from the template + params.
     * This is a merged snapshot used by the game engine and old manual endpoints.
     */
    private Map<String, Object> buildConfig(QuestionTemplate template, Map<String, Object> params) {
        Map<String, Object> config = new HashMap<>(params);
        config.put("entity_type", "footballer");   // autocomplete pool
        config.put("materializer_key", template.getMaterializerKey());
        config.put("metric_key", template.getMetricKey());
        return config;
    }

    // ── Result record ────────────────────────────────────────────────────────

    /**
     * Summary of a generator run.
     *
     * @param created number of new draft questions created
     * @param skipped number of param sets skipped (question already exists)
     */
    public record GeneratorResult(int created, int skipped) {
        public int total() { return created + skipped; }
    }
}
