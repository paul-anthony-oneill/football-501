package com.trivia501.controller;

import com.trivia501.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public metadata endpoints for the Football category.
 *
 * <p>All endpoints are {@code permitAll} — they expose only structural metadata
 * (which leagues/clubs have questions) needed to build the lobby navigation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/football/clubs?league={slug} — distinct clubs with active questions in a league</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/football")
@Slf4j
public class FootballController {

    private final QuestionService questionService;

    public FootballController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * Returns the list of clubs that have at least one active question for the given league.
     *
     * <p>Each club is returned as {@code { "id": "arsenal", "name": "Arsenal" }} where
     * {@code name} is derived by title-casing the slug (hyphens → spaces). This avoids
     * a separate club-name column in the database while the materialiser is still being built.
     *
     * @param league the league slug, e.g. "premier-league"
     * @return list of {@code {id, name}} objects, empty list if no questions exist yet
     */
    @GetMapping("/clubs")
    public ResponseEntity<List<Map<String, String>>> getClubs(@RequestParam String league) {
        log.debug("Fetching clubs for league: {}", league);

        List<Map<String, String>> clubs = questionService.getClubsForLeague(league)
            .stream()
            .map(slug -> Map.of("id", slug, "name", slugToName(slug)))
            .toList();

        return ResponseEntity.ok(clubs);
    }

    /** Converts a slug like "manchester-city" to "Manchester City". */
    private static String slugToName(String slug) {
        if (slug == null || slug.isBlank()) return slug;
        String[] words = slug.split("-");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1));
        }
        return sb.toString();
    }
}
