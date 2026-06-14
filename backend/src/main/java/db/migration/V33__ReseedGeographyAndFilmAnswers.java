package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Re-seeds answers for geography and film questions.
 *
 * V21/V22 inserted the questions but the answer batch did not persist on Supabase
 * (likely a resource-stream issue at migration time). This migration is idempotent:
 * all INSERTs use ON CONFLICT (question_id, answer_key) DO NOTHING, so re-running
 * is safe. After inserting, it backfills total_valid_count from the answers table.
 */
public class V33__ReseedGeographyAndFilmAnswers extends BaseJavaMigration {

    // Geography question IDs — must match V21
    private static final String[] GEO_QUESTION_IDS = {
        "4bde928a-c9ed-f5f3-e8a6-8447eb858d85", // global
        "a10296e6-6526-47a6-db85-a6fc81a0d51b", // Africa
        "a64937b9-1e46-4d43-c599-00e623fd5e24", // Americas
        "8a2b9539-5c83-ed99-181d-41fdb49365a7", // Asia
        "6406745b-9110-bac2-f207-1c8f84100558", // Europe
    };
    private static final String[] GEO_REGIONS = { null, "Africa", "Americas", "Asia", "Europe" };

    // Film question IDs — must match V22
    private static final String[] FILM_QUESTION_IDS = {
        "361ff97a-7354-4dc6-94be-96c91fb739f3", // global
        "cbd35280-6deb-465f-a700-08d080d6734e", // 1990s
        "6046cb99-4f91-4768-8459-b308a93c04a4", // 2000s
        "41636d5d-26a3-4515-8100-c646d675c9bb", // 2010s
        "112a7961-e24a-4de1-87d0-006820137ea5", // 2020s
    };
    private static final String[] FILM_DECADES = { null, "1990s", "2000s", "2010s", "2020s" };

    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();

        seedGeography(conn);
        seedFilm(conn);
        backfillValidCounts(conn);
    }

    private void seedGeography(java.sql.Connection conn) throws Exception {
        var answerInsert = conn.prepareStatement(
            "INSERT INTO answers (id, question_id, answer_key, display_text, score, " +
            "is_valid_darts, is_bust, metadata, materialized_at) VALUES (" +
            "gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?::jsonb, NOW()) " +
            "ON CONFLICT (question_id, answer_key) DO NOTHING");

        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/db/data/geography_answers.csv"),
                StandardCharsets.UTF_8))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = parseCsvLine(line);
                if (f.length < 4) continue;

                String answerKey  = f[0];
                String displayText = f[1];
                int    score      = Integer.parseInt(f[2].trim());
                String region     = f[3];

                boolean isBust        = isInvalidDarts(score);
                boolean isValidDarts  = score >= 1 && score <= 180 && !isBust;
                String  metadata      = buildMetadata(f, new String[]{"region","subregion","flag"}, 3);

                List<Integer> targets = questionIndexes(region, GEO_REGIONS);
                for (int qi : targets) {
                    answerInsert.setObject(1, UUID.fromString(GEO_QUESTION_IDS[qi]));
                    answerInsert.setString(2, answerKey);
                    answerInsert.setString(3, displayText);
                    answerInsert.setInt(4, score);
                    answerInsert.setBoolean(5, isValidDarts);
                    answerInsert.setBoolean(6, isBust);
                    answerInsert.setString(7, metadata);
                    answerInsert.addBatch();
                }
            }
        }
        answerInsert.executeBatch();
    }

    private void seedFilm(java.sql.Connection conn) throws Exception {
        var answerInsert = conn.prepareStatement(
            "INSERT INTO answers (id, question_id, answer_key, display_text, score, " +
            "is_valid_darts, is_bust, metadata, materialized_at) VALUES (" +
            "gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?::jsonb, NOW()) " +
            "ON CONFLICT (question_id, answer_key) DO NOTHING");

        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/db/data/film_answers.csv"),
                StandardCharsets.UTF_8))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = parseCsvLine(line);
                if (f.length < 3) continue;

                String answerKey  = f[0];
                String displayText = f[1];
                int    score      = Integer.parseInt(f[2].trim());
                String decade     = f.length > 3 ? f[3] : null;

                boolean isBust       = isInvalidDarts(score);
                boolean isValidDarts = score >= 1 && score <= 180 && !isBust;
                String  metadata     = buildMetadata(f, new String[]{"release_year","decade","tmdb_id","poster_path"}, 3);

                List<Integer> targets = questionIndexes(decade, FILM_DECADES);
                for (int qi : targets) {
                    answerInsert.setObject(1, UUID.fromString(FILM_QUESTION_IDS[qi]));
                    answerInsert.setString(2, answerKey);
                    answerInsert.setString(3, displayText);
                    answerInsert.setInt(4, score);
                    answerInsert.setBoolean(5, isValidDarts);
                    answerInsert.setBoolean(6, isBust);
                    answerInsert.setString(7, metadata);
                    answerInsert.addBatch();
                }
            }
        }
        answerInsert.executeBatch();
    }

    private void backfillValidCounts(java.sql.Connection conn) throws Exception {
        try (var stmt = conn.prepareStatement(
                "UPDATE questions q " +
                "SET total_valid_count = (" +
                "    SELECT COUNT(*) FROM answers a" +
                "    WHERE a.question_id = q.id AND a.is_valid_darts = true" +
                ") " +
                "WHERE q.id = ANY(?)")) {

            List<UUID> allIds = new ArrayList<>();
            for (String id : GEO_QUESTION_IDS)  allIds.add(UUID.fromString(id));
            for (String id : FILM_QUESTION_IDS) allIds.add(UUID.fromString(id));

            stmt.setArray(1, conn.createArrayOf("uuid",
                allIds.stream().map(UUID::toString).toArray()));
            stmt.executeUpdate();
        }
    }

    // Returns index 0 (global) always; also returns the matching regional index if found.
    private static List<Integer> questionIndexes(String value, String[] groups) {
        List<Integer> indexes = new ArrayList<>();
        indexes.add(0);
        if (value != null) {
            for (int i = 1; i < groups.length; i++) {
                if (value.equals(groups[i])) { indexes.add(i); break; }
            }
        }
        return indexes;
    }

    private static boolean isInvalidDarts(int score) {
        return score > 180 || score == 163 || score == 166 || score == 169
            || score == 172 || score == 173 || score == 175 || score == 176
            || score == 178 || score == 179;
    }

    private static String buildMetadata(String[] fields, String[] keys, int startIndex) {
        var sb = new StringBuilder("{");
        for (int i = 0; i < keys.length; i++) {
            int fi = startIndex + i;
            if (fi >= fields.length) break;
            if (i > 0) sb.append(", ");
            sb.append("\"").append(keys[i]).append("\": \"")
              .append(escapeJson(fields[fi])).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
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
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }
}
