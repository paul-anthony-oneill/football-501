"""
Question Population Jobs - Version 2

Populates questions from CACHED player career data.
No direct scraping - queries player_career_stats table.
"""

import logging
from typing import List, Dict, Optional
from datetime import datetime
from collections import defaultdict

from database.crud_v2 import DatabaseManager
from database.models_v2 import Question
from scrapers.data_transformer import DataTransformer

logger = logging.getLogger(__name__)


class QuestionPopulator:
    """
    Populates questions from cached player_career_stats.
    Fast and efficient - no external scraping during question creation.
    """

    def __init__(self):
        self.db = DatabaseManager()
        self.transformer = DataTransformer()
        logger.info("Question populator initialized (v2)")

    def populate_single_question(self, question_id: int) -> Dict[str, any]:
        """
        Populate answers for a question from cached data.

        Args:
            question_id: Question ID

        Returns:
            Dict with population results
        """
        start_time = datetime.utcnow()

        try:
            # Get question
            question = self.db.get_question_by_id(question_id)
            if not question:
                raise ValueError(f"Question {question_id} not found")

            logger.info(f"Populating question {question_id}: {question.question_text}")

            # Query cached stats
            raw_stats = self._query_cached_stats(question)

            if not raw_stats:
                logger.warning(f"No data found for question {question_id}")
                return {
                    'question_id': question_id,
                    'status': 'no_data',
                    'players_added': 0
                }

            # Aggregate stats (if needed)
            aggregated = self._aggregate_stats(raw_stats, question)

            # Transform to answers
            answers = self._transform_to_answers(aggregated, question)

            # Delete old answers
            self.db.delete_answers_for_question(question_id)

            # Insert new answers
            rows_inserted = self.db.insert_valid_answers(answers)

            duration = (datetime.utcnow() - start_time).total_seconds()

            logger.info(
                f"Successfully populated question {question_id}: "
                f"{rows_inserted} answers in {duration:.1f}s"
            )

            return {
                'question_id': question_id,
                'status': 'success',
                'players_added': rows_inserted,
                'duration': duration
            }

        except Exception as e:
            logger.error(f"Failed to populate question {question_id}: {str(e)}")
            return {
                'question_id': question_id,
                'status': 'failed',
                'error': str(e)
            }

    def populate_all_active_questions(self) -> Dict[str, any]:
        """
        Populate all active questions.

        Returns:
            Dict with summary results
        """
        start_time = datetime.utcnow()

        questions = self.db.get_active_questions()

        logger.info(f"Populating {len(questions)} active questions")

        results = {
            'total_questions': len(questions),
            'successful': 0,
            'failed': 0,
            'total_players': 0
        }

        for question in questions:
            result = self.populate_single_question(question.id)

            if result['status'] == 'success':
                results['successful'] += 1
                results['total_players'] += result['players_added']
            else:
                results['failed'] += 1

        duration = (datetime.utcnow() - start_time).total_seconds()
        results['duration'] = duration

        logger.info(
            f"Populated {results['successful']}/{results['total_questions']} questions, "
            f"{results['total_players']} total answers in {duration:.1f}s"
        )

        return results

    # ============================================================================
    # PRIVATE METHODS
    # ============================================================================

    def _query_cached_stats(self, question: Question) -> List[Dict]:
        """
        Query cached player_career_stats based on question filters.

        Args:
            question: Question instance

        Returns:
            List of stat dicts
        """
        # Build query filters
        team_name = None
        competition_name = None

        if question.team_id:
            team = self.db.get_team_by_id(question.team_id)
            team_name = team.name if team else None

        if question.competition_id:
            comp = self.db.get_competition_by_id(question.competition_id)
            competition_name = comp.name if comp else None

        # Query cached stats
        stats = self.db.query_player_stats(
            team_name=team_name,
            competition_name=competition_name,
            season=question.season_filter,
            nationality=question.nationality_filter,
            min_appearances=0
        )

        logger.debug(f"Found {len(stats)} raw stats for question {question.id}")

        return stats

    def _aggregate_stats(self, raw_stats: List[Dict], question: Question) -> Dict[int, Dict]:
        """
        Aggregate stats by player based on question's aggregation strategy.

        Args:
            raw_stats: List of raw stat dicts
            question: Question instance

        Returns:
            Dict mapping player_id to aggregated stats
        """
        aggregation = question.aggregation

        if aggregation == 'sum':
            # Sum all seasons for each player
            player_totals = defaultdict(lambda: {
                'player_id': 0,
                'player_name': '',
                'normalized_name': '',
                'appearances': 0,
                'goals': 0,
                'assists': 0,
                'clean_sheets': 0
            })

            for stat in raw_stats:
                pid = stat['player_id']
                player_totals[pid]['player_id'] = pid
                player_totals[pid]['player_name'] = stat['player_name']
                player_totals[pid]['normalized_name'] = stat['normalized_name']
                player_totals[pid]['appearances'] += stat.get('appearances', 0)
                player_totals[pid]['goals'] += stat.get('goals', 0)
                player_totals[pid]['assists'] += stat.get('assists', 0)
                player_totals[pid]['clean_sheets'] += stat.get('clean_sheets', 0)

            return dict(player_totals)

        elif aggregation == 'single_season':
            # Return stats as-is (one row per player/season)
            # Use most recent season if multiple
            player_latest = {}
            for stat in raw_stats:
                pid = stat['player_id']
                season = stat['season']

                if pid not in player_latest or season > player_latest[pid]['season']:
                    player_latest[pid] = stat

            return player_latest

        elif aggregation == 'latest_season':
            # Get latest season only
            latest_season = max((s['season'] for s in raw_stats), default=None)
            if not latest_season:
                return {}

            player_stats = {}
            for stat in raw_stats:
                if stat['season'] == latest_season:
                    player_stats[stat['player_id']] = stat

            return player_stats

        else:
            logger.warning(f"Unknown aggregation '{aggregation}', defaulting to sum")
            return self._aggregate_stats(raw_stats, question)

    def _transform_to_answers(self, aggregated: Dict[int, Dict], question: Question) -> List[Dict]:
        """
        Transform aggregated stats to answer format.

        Args:
            aggregated: Dict mapping player_id to stats
            question: Question instance

        Returns:
            List of answer dicts ready for insertion
        """
        answers = []

        for player_id, stats in aggregated.items():
            # Calculate score based on stat_type
            score = self._calculate_score(stats, question.stat_type)

            # Apply min_score filter
            if question.min_score and score < question.min_score:
                continue

            # Validate darts score
            is_valid_darts = self._is_valid_darts_score(score)
            is_bust = self._is_bust(score)

            answers.append({
                'question_id': question.id,
                'player_id': player_id,
                'player_name': stats['player_name'],
                'normalized_name': stats['normalized_name'],
                'score': score,
                'is_valid_darts_score': is_valid_darts,
                'is_bust': is_bust
            })

        logger.debug(f"Transformed {len(answers)} answers for question {question.id}")

        return answers

    def _calculate_score(self, stats: Dict, stat_type: str) -> int:
        """
        Calculate score based on stat type.

        Args:
            stats: Player stats dict
            stat_type: Type of statistic

        Returns:
            Score value
        """
        if stat_type == 'appearances':
            return stats.get('appearances', 0)

        elif stat_type == 'goals':
            return stats.get('goals', 0)

        elif stat_type == 'assists':
            return stats.get('assists', 0)

        elif stat_type == 'combined_apps_goals':
            return stats.get('appearances', 0) + stats.get('goals', 0)

        elif stat_type == 'combined_apps_assists':
            return stats.get('appearances', 0) + stats.get('assists', 0)

        elif stat_type == 'goalkeeper':
            # Appearances + clean sheets
            return stats.get('appearances', 0) + stats.get('clean_sheets', 0)

        else:
            logger.warning(f"Unknown stat_type '{stat_type}', returning appearances")
            return stats.get('appearances', 0)

    def _is_valid_darts_score(self, score: int) -> bool:
        """
        Check if score is a valid darts checkout.

        Invalid scores in 501 darts: 163, 166, 169, 172, 173, 175, 176, 178, 179

        Args:
            score: Score value

        Returns:
            True if valid darts score
        """
        invalid_scores = {163, 166, 169, 172, 173, 175, 176, 178, 179}
        return score not in invalid_scores

    def _is_bust(self, score: int) -> bool:
        """
        Check if score results in a bust.

        Bust conditions:
        - Score > 180 (max possible in 3 darts)
        - Score < -10 (outside checkout range)

        Args:
            score: Score value

        Returns:
            True if bust
        """
        return score > 180 or score < -10

    # ============================================================================
    # HELPER METHODS FOR DATABASE LOOKUPS
    # ============================================================================

    def _get_team_by_id(self, team_id: int):
        """Get team by ID (for readability)."""
        return self.db.get_team_by_id(team_id)

    def _get_competition_by_id(self, competition_id: int):
        """Get competition by ID (for readability)."""
        return self.db.get_competition_by_id(competition_id)


def main():
    """
    CLI for question population.
    """
    import argparse

    parser = argparse.ArgumentParser(description='Populate questions from cached data')
    parser.add_argument('--question-id', type=int, help='Populate single question')
    parser.add_argument('--all', action='store_true', help='Populate all active questions')

    args = parser.parse_args()

    populator = QuestionPopulator()

    if args.question_id:
        result = populator.populate_single_question(args.question_id)
        print(f"Result: {result}")

    elif args.all:
        result = populator.populate_all_active_questions()
        print(f"Result: {result}")

    else:
        parser.print_help()


if __name__ == '__main__':
    main()
