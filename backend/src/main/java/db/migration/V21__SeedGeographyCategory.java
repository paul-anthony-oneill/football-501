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

public class V21__SeedGeographyCategory extends BaseJavaMigration {

    private static final String CATEGORY_ID = "3d06f275-d476-6edf-132c-9cf74559a1a9";
    private static final String[] QUESTION_IDS = {
        "4bde928a-c9ed-f5f3-e8a6-8447eb858d85", // global
        "a10296e6-6526-47a6-db85-a6fc81a0d51b", // Africa
        "a64937b9-1e46-4d43-c599-00e623fd5e24", // Americas
        "8a2b9539-5c83-ed99-181d-41fdb49365a7", // Asia
        "6406745b-9110-bac2-f207-1c8f84100558", // Europe
    };

    private static final String[] QUESTION_TEXTS = {
        "Name a country — its population in millions is your score",
        "Name a country in Africa — its population in millions is your score",
        "Name a country in Americas — its population in millions is your score",
        "Name a country in Asia — its population in millions is your score",
        "Name a country in Europe — its population in millions is your score",
    };

    private static final String[] QUESTION_REGIONS = {
        null, "Africa", "Americas", "Asia", "Europe"
    };

    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();

        // 1. Insert category
        try (var stmt = conn.prepareStatement(
                "INSERT INTO categories (id, name, slug, description, created_at, updated_at) " +
                "VALUES (?, 'Geography', 'geography', " +
                "'Country population trivia — name a country and score its population in millions', " +
                "NOW(), NOW()) ON CONFLICT (slug) DO UPDATE SET id = EXCLUDED.id")) {
            stmt.setObject(1, UUID.fromString(CATEGORY_ID));
            stmt.executeUpdate();
        }

        // 2. Insert questions
        for (int i = 0; i < QUESTION_IDS.length; i++) {
            String config = QUESTION_REGIONS[i] == null
                ? "{\"entity_type\": \"country\"}"
                : "{\"entity_type\": \"country\", \"region\": \"" + QUESTION_REGIONS[i] + "\"}";
            try (var stmt = conn.prepareStatement(
                    "INSERT INTO questions (id, category_id, question_text, metric_key, config, " +
                    "min_score, difficulty, status, template_id, template_params, " +
                    "high_value_count, mid_range_count, checkout_count, total_valid_count, " +
                    "total_score_pool, single_question_viable, difficulty_score, difficulty_locked, " +
                    "suitable_for_daily, created_at, updated_at) VALUES (" +
                    "?, ?, ?, 'population_millions', ?::jsonb, " +
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
            "VALUES (gen_random_uuid(), 'country', ?, ?, NOW()) " +
            "ON CONFLICT (entity_type, normalized_name) DO NOTHING");

        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/db/data/geography_answers.csv"),
                StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length < 6) continue;

                String answerKey = fields[0];
                String displayText = fields[1];
                int score = Integer.parseInt(fields[2]);
                String region = fields[3];
                String subregion = fields[4];
                String flag = fields[5];

                boolean isBust = score > 180 || score == 163 || score == 166 || score == 169
                    || score == 172 || score == 173 || score == 175 || score == 176
                    || score == 178 || score == 179;
                boolean isValidDarts = score >= 1 && score <= 180 && !isBust;

                String metadata = String.format(
                    "{\"region\": \"%s\", \"subregion\": \"%s\", \"flag\": \"%s\"}",
                    escapeJson(region), escapeJson(subregion), escapeJson(flag));

                // Determine which questions this answer belongs to
                List<Integer> questionIndexes = new ArrayList<>();
                questionIndexes.add(0); // always global
                for (int qi = 1; qi < QUESTION_REGIONS.length; qi++) {
                    if (region.equals(QUESTION_REGIONS[qi])) {
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

    /** Parse a CSV line, handling quoted fields (e.g. "Virgin Islands, British"). */
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
