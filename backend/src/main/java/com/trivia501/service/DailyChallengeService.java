package com.trivia501.service;

import com.trivia501.engine.DifficultyConstants;
import com.trivia501.model.Category;
import com.trivia501.model.DailyChallenge;
import com.trivia501.model.Game;
import com.trivia501.model.Match;
import com.trivia501.model.Question;
import com.trivia501.repository.CategoryRepository;
import com.trivia501.repository.DailyChallengeRepository;
import com.trivia501.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class DailyChallengeService {

    private static final int[] STARTING_SCORES = {501, 401, 351, 301, 251, 201, 167, 125, 101};

    private final DailyChallengeRepository challengeRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final MatchService matchService;
    private final GameService gameService;

    public DailyChallengeService(
            DailyChallengeRepository challengeRepository,
            QuestionRepository questionRepository,
            CategoryRepository categoryRepository,
            MatchService matchService,
            GameService gameService
    ) {
        this.challengeRepository = challengeRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.matchService = matchService;
        this.gameService = gameService;
    }

    /**
     * Returns all of today's daily challenges across all active categories.
     */
    @Transactional(readOnly = true)
    public List<DailyChallenge> getTodaysChallenges() {
        return challengeRepository.findByChallengeDate(LocalDate.now());
    }

    /**
     * Returns today's challenge for a specific category, creating it lazily if
     * the scheduler has not run yet (or if no question was pre-selected).
     */
    @Transactional
    public DailyChallenge getTodaysChallenge(UUID categoryId) {
        LocalDate today = LocalDate.now();
        return challengeRepository.findByChallengeDateAndCategoryId(today, categoryId)
                .orElseGet(() -> createChallenge(categoryId));
    }

    /**
     * Resolves a category slug to today's challenge.
     *
     * @throws IllegalArgumentException if the category slug is unknown
     */
    @Transactional
    public DailyChallenge getTodaysChallenge(String categorySlug) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));
        return getTodaysChallenge(category.getId());
    }

    /**
     * Starts a daily challenge game for the given player and category.
     *
     * @param playerId     the authenticated player UUID
     * @param categorySlug the category slug (e.g. "football")
     * @return a result bundle with the created match, game, and question
     * @throws IllegalStateException if no viable question is available for today
     */
    @Transactional
    public GameStartRecord startDailyChallenge(UUID playerId, String categorySlug) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));

        DailyChallenge challenge = getTodaysChallenge(category.getId());

        // Abandon any existing in-progress games for this player
        gameService.abandonActiveGamesForPlayer(playerId);

        Match match = matchService.createMatch(
                playerId,
                null, // solo — no opponent
                category.getId(),
                Match.MatchType.DAILY_CHALLENGE,
                Match.MatchFormat.BEST_OF_1,
                null // difficulty — inferred from the selected question
        );
        match.setGameMode("DAILY_CHALLENGE");

        Game game = gameService.createGame(match.getId(), challenge.getQuestionId(), 1, challenge.getStartingScore());

        Question question = questionRepository.findById(challenge.getQuestionId())
                .orElseThrow(() -> new IllegalStateException("Question not found for daily challenge"));

        log.info("Daily challenge started: gameId={}, playerId={}, category={}, startingScore={}",
                game.getId(), playerId, categorySlug, challenge.getStartingScore());

        return new GameStartRecord(match, game, question, challenge);
    }

    /**
     * Picks a random valid starting score from the curated set.
     */
    public int pickStartingScore() {
        return STARTING_SCORES[ThreadLocalRandom.current().nextInt(STARTING_SCORES.length)];
    }

    /**
     * Creates a daily challenge for the given category by selecting a random
     * starting score and a viable question.
     *
     * <p>If no question is viable for the first starting score, retries with
     * progressively lower scores until one works or the list is exhausted.
     */
    private DailyChallenge createChallenge(UUID categoryId) {
        int score = pickStartingScore();
        Optional<Question> questionOpt = questionRepository.findRandomDailyQuestion(
                categoryId, score, DifficultyConstants.DAILY_MIN_DIFFICULTY, DifficultyConstants.DAILY_MAX_DIFFICULTY);

        // Fallback: if no question found for the chosen score, try all scores
        if (questionOpt.isEmpty()) {
            for (int fallbackScore : STARTING_SCORES) {
                questionOpt = questionRepository.findRandomDailyQuestion(
                        categoryId, fallbackScore, DifficultyConstants.DAILY_MIN_DIFFICULTY, DifficultyConstants.DAILY_MAX_DIFFICULTY);
                if (questionOpt.isPresent()) {
                    score = fallbackScore;
                    break;
                }
            }
        }

        Question question = questionOpt.orElseThrow(() ->
                new IllegalStateException(
                        "No suitable_for_daily question found for category " + categoryId +
                        ". Mark at least one question with suitable_for_daily = true."));

        DailyChallenge challenge = DailyChallenge.builder()
                .challengeDate(LocalDate.now())
                .categoryId(categoryId)
                .questionId(question.getId())
                .startingScore(score)
                .status("active")
                .build();

        challenge = challengeRepository.save(challenge);
        log.info("Daily challenge created for {}: questionId={}, startingScore={}",
                LocalDate.now(), question.getId(), score);
        return challenge;
    }

    /**
     * Result bundle for starting a daily challenge game.
     */
    public record GameStartRecord(Match match, Game game, Question question, DailyChallenge challenge) {}
}
