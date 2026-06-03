package com.trivia501.materializer;

import com.trivia501.model.Question;
import com.trivia501.model.QuestionTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable carrier passed to {@link QuestionMaterializer#materialize}.
 *
 * <p>Bundles the question being materialised, its parent template, and the
 * concrete param bindings so the materializer implementation does not need to
 * re-join against the template table.
 *
 * @param question       the {@code Question} being materialised (never {@code null})
 * @param template       the parent template; may be {@code null} for hand-curated
 *                       questions triggered via a manual re-materialise
 * @param templateParams concrete param bindings resolved from the question's
 *                       {@code template_params} JSONB column; an empty map for
 *                       hand-curated questions
 */
public record MaterializationContext(
        Question       question,
        QuestionTemplate template,
        Map<String, Object> templateParams
) {

    /** Convenience — extract a String param by key (never null). */
    public String param(String key) {
        Object val = templateParams.get(key);
        if (val == null) {
            throw new IllegalArgumentException(
                "Missing required template param: " + key);
        }
        return val.toString();
    }

    /** Convenience — extract a UUID param by key (never null). */
    public UUID uuidParam(String key) {
        return UUID.fromString(param(key));
    }

    /** Convenience — extract an int param by key (never null). */
    public int intParam(String key) {
        return Integer.parseInt(param(key));
    }

    /** True when the question was generated from a template. */
    public boolean isTemplateGenerated() {
        return template != null && !templateParams.isEmpty();
    }
}
