package db.migration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V22__SeedFilmCategory extends BaseJavaMigration {

    private static final String CATEGORY_ID = "14100a53-812a-4064-974f-fc270509742d";
    private static final String[] QUESTION_IDS = {
        "361ff97a-7354-4dc6-94be-96c91fb739f3", // global
        "cbd35280-6deb-465f-a700-08d080d6734e", // 1990s
        "6046cb99-4f91-4768-8459-b308a93c04a4", // 2000s
        "41636d5d-26a3-4515-8100-c646d675c9bb", // 2010s
        "112a7961-e24a-4de1-87d0-006820137ea5", // 2020s
    };

    private static final String[] QUESTION_TEXTS = {
        "Name a movie — its worldwide box office per $10M is your score",
        "Name a movie from the 1990s — its worldwide box office per $10M is your score",
        "Name a movie from the 2000s — its worldwide box office per $10M is your score",
        "Name a movie from the 2010s — its worldwide box office per $10M is your score",
        "Name a movie from the 2020s — its worldwide box office per $10M is your score",
    };

    private static final String[] QUESTION_DECADES = {
        null, "1990s", "2000s", "2010s", "2020s"
    };

    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();

        // 1. Insert category
        try (var stmt = conn.prepareStatement(
                "INSERT INTO categories (id, name, slug, description, created_at, updated_at) " +
                "VALUES (?, 'Film', 'film', " +
                "'Movie box office trivia — name a movie and score its worldwide revenue per $10M', " +
                "NOW(), NOW()) ON CONFLICT (slug) DO UPDATE SET id = EXCLUDED.id")) {
            stmt.setObject(1, UUID.fromString(CATEGORY_ID));
            stmt.executeUpdate();
        }

        // 2. Insert questions
        for (int i = 0; i < QUESTION_IDS.length; i++) {
            String config = QUESTION_DECADES[i] == null
                ? "{\"entity_type\": \"film\"}"
                : "{\"entity_type\": \"film\", \"decade\": \"" + QUESTION_DECADES[i] + "\"}";
            try (var stmt = conn.prepareStatement(
                    "INSERT INTO questions (id, category_id, question_text, metric_key, config, " +
                    "min_score, difficulty, status, template_id, template_params, " +
                    "high_value_count, mid_range_count, checkout_count, total_valid_count, " +
                    "total_score_pool, single_question_viable, difficulty_score, difficulty_locked, " +
                    "suitable_for_daily, created_at, updated_at) VALUES (" +
                    "?, ?, ?, 'box_office_millions', ?::jsonb, " +
                    "1, 2, 'draft', NULL, '{}'::jsonb, " +
                    "0, 0, 0, 0, 0, true, 0.00, false, false, NOW(), NOW()) " +
                    "ON CONFLICT (id) DO NOTHING")) {
                stmt.setObject(1, UUID.fromString(QUESTION_IDS[i]));
                stmt.setObject(2, UUID.fromString(CATEGORY_ID));
                stmt.setString(3, QUESTION_TEXTS[i]);
                stmt.setString(4, config);
                stmt.executeUpdate();
            }
        }

        // 3. Read CSV and batch-insert answers + entities
        var answerInsert = conn.prepareStatement(
            "INSERT INTO answers (id, question_id, answer_key, display_text, score, " +
            "is_valid_darts, is_bust, metadata, materialized_at) VALUES (" +
            "gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?::jsonb, NOW()) " +
            "ON CONFLICT (question_id, answer_key) DO NOTHING");

        var entityInsert = conn.prepareStatement(
            "INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at) " +
            "VALUES (gen_random_uuid(), 'film', ?, ?, NOW()) " +
            "ON CONFLICT (entity_type, normalized_name) DO NOTHING");

        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/db/data/film_answers.csv"),
                StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length < 7) continue;

                String answerKey = fields[0];
                String displayText = fields[1];
                int score = Integer.parseInt(fields[2]);
                String releaseYear = fields[3];
                String decade = fields[4];
                String tmdbId = fields[5];
                String posterPath = fields[6];

                boolean isBust = score > 180 || score == 163 || score == 166 || score == 169
                    || score == 172 || score == 173 || score == 175 || score == 176
                    || score == 178 || score == 179;
                boolean isValidDarts = score >= 1 && score <= 180 && !isBust;

                String metadata = String.format(
                    "{\"release_year\":\"%s\",\"tmdb_id\":%s,\"poster_path\":\"%s\"}",
                    releaseYear, tmdbId, escapeJson(posterPath));

                // Determine which questions this answer belongs to
                List<Integer> questionIndexes = new ArrayList<>();
                questionIndexes.add(0); // always global
                for (int qi = 1; qi < QUESTION_DECADES.length; qi++) {
                    if (decade.equals(QUESTION_DECADES[qi])) {
                        questionIndexes.add(qi);
                        break;
                    }
                }

                for (int qi : questionIndexes) {
                    answerInsert.setObject(1, UUID.fromString(QUESTION_IDS[qi]));
                    answerInsert.setString(2, answerKey);
                    answerInsert.setString(3, displayText);
                    answerInsert.setInt(4, score);
                    answerInsert.setBoolean(5, isValidDarts);
                    answerInsert.setBoolean(6, isBust);
                    answerInsert.setString(7, metadata);
                    answerInsert.addBatch();
                }

                // Entity: canonical display name
                entityInsert.setString(1, displayText);
                entityInsert.setString(2, answerKey);
                entityInsert.addBatch();
            }
        }

        answerInsert.executeBatch();
        entityInsert.executeBatch();
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
