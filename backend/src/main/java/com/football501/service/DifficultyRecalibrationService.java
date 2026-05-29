package com.football501.service;

import com.football501.engine.DifficultyCalculator;
import com.football501.engine.DifficultyConstants;
import com.football501.model.Question;
import com.football501.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulk recalculates {@code difficulty_score} and re-evaluates viability for all
 * unlocked questions using their stored zone counts.
 *
 * <h3>When to use</h3>
 * <ul>
 *   <li>After adjusting any constant in {@link DifficultyConstants} (formula weights,
 *       saturation thresholds, or viability minimums).</li>
 *   <li>An admin manually requests a global re-score via
 *       {@code POST /api/admin/questions/recalculate-difficulty}.</li>
 * </ul>
 *
 * <h3>What it does NOT do</h3>
 * <p>This service reads only the stored zone counts ({@code high_value_count},
 * {@code mid_range_count}, {@code checkout_count}, {@code total_valid_count},
 * {@code total_score_pool}). It never touches the {@code answers} table.
 * Changing zone <em>boundaries</em> (e.g. extending checkout from 1–19 to 1–25)
 * requires re-materialisation or a SQL backfill before recalibration is meaningful.
 *
 * <h3>Locked questions</h3>
 * <p>Questions with {@code difficulty_locked = true} are skipped entirely. Their
 * {@code difficulty_score} is preserved as-is.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DifficultyRecalibrationService {

    private final QuestionRepository questionRepository;

    /**
     * Recalculates difficulty and viability for every unlocked question.
     *
     * @return a summary of what changed
     */
    @Transactional
    public RecalibrationResult recalculateAll() {
        List<Question> questions = questionRepository.findByDifficultyLockedFalse();
        int updated    = 0;
        int reExcluded = 0;

        for (Question q : questions) {
            double newScore = DifficultyCalculator.calculate(
                q.getHighValueCount(),
                q.getMidRangeCount(),
                q.getCheckoutCount(),
                q.getTotalValidCount()
            );

            boolean nowViable = q.getTotalScorePool()  >= DifficultyConstants.MIN_SCORE_POOL
                             && q.getTotalValidCount() >= DifficultyConstants.MIN_ANSWER_COUNT;

            boolean scoreChanged    = Math.abs(newScore - q.getDifficultyScore()) > 0.005;
            boolean viabilityChanged = nowViable != q.isSingleQuestionViable();

            if (scoreChanged || viabilityChanged) {
                q.setDifficultyScore(newScore);
                q.setSingleQuestionViable(nowViable);

                if (!nowViable) {
                    String reason = buildViabilityReason(q.getTotalScorePool(), q.getTotalValidCount());
                    q.setViabilityExclusionReason(reason);
                    if (!Question.STATUS_EXCLUDED.equals(q.getStatus())) {
                        q.setStatus(Question.STATUS_EXCLUDED);
                        reExcluded++;
                        log.warn("Question {} newly excluded during recalibration: {}", q.getId(), reason);
                    }
                } else {
                    q.setViabilityExclusionReason(null);
                }

                questionRepository.save(q);
                updated++;
            }
        }

        log.info("Recalibration complete — total={} updated={} reExcluded={}",
            questions.size(), updated, reExcluded);
        return new RecalibrationResult(questions.size(), updated, reExcluded);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildViabilityReason(int scorePool, int validCount) {
        List<String> reasons = new ArrayList<>();
        if (scorePool < DifficultyConstants.MIN_SCORE_POOL) {
            reasons.add("insufficient_score_pool: " + scorePool
                      + " < " + DifficultyConstants.MIN_SCORE_POOL);
        }
        if (validCount < DifficultyConstants.MIN_ANSWER_COUNT) {
            reasons.add("insufficient_answer_count: " + validCount
                      + " < " + DifficultyConstants.MIN_ANSWER_COUNT);
        }
        return String.join("; ", reasons);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * Summary returned from {@link #recalculateAll()}.
     *
     * @param total      total questions processed (unlocked)
     * @param updated    questions whose difficulty score or viability changed
     * @param reExcluded questions newly set to {@code status = 'excluded'} because
     *                   they no longer pass viability checks after threshold changes
     */
    public record RecalibrationResult(int total, int updated, int reExcluded) {}
}
