package com.football501.materializer;

import java.util.Map;
import java.util.UUID;

/**
 * A single pre-computed answer row produced by a {@link QuestionMaterializer}.
 *
 * <p>The caller ({@link com.football501.service.QuestionMaterializerService})
 * converts these into {@link com.football501.model.Answer} entities and upserts
 * them, so the materializer implementation does not need to touch the
 * {@code answers} table directly.
 *
 * @param answerKey   normalised match key, e.g. {@code "erling haaland"}
 * @param displayText human-readable text, e.g. {@code "Erling Haaland"}
 * @param score       computed stat value (e.g. 36 goals)
 * @param metadata    optional structured context for the UI (player_id, etc.)
 */
public record MaterializedAnswer(
        String              answerKey,
        String              displayText,
        int                 score,
        Map<String, Object> metadata
) {

    /** Convenience constructor with no metadata. */
    public MaterializedAnswer(String answerKey, String displayText, int score) {
        this(answerKey, displayText, score, Map.of());
    }
}
