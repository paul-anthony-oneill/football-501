"""
Question Population Jobs for Football 501

Handles scraping and populating answers for questions.
"""

import logging
from typing import List, Optional, Dict
from datetime import datetime

from scrapers import FBrefScraper, DataTransformer
from database import DatabaseManager, Question
from config import settings

logger = logging.getLogger(__name__)


class QuestionPopulator:
    """
    Populates questions with player statistics from FBref.
    """

    def __init__(self):
        """Initialize question populator."""
        self.scraper = FBrefScraper()
        self.transformer = DataTransformer()
        self.db = DatabaseManager()

        logger.info("Question populator initialized")

    def populate_single_question(self, question_id: int) -> Dict[str, any]:
        """
        Populate answers for a single question.

        Args:
            question_id: Question ID

        Returns:
            Dict with job results

        Example:
            >>> populator = QuestionPopulator()
            >>> result = populator.populate_single_question(1234)
            >>> print(result)
            {
                'question_id': 1234,
                'status': 'success',
                'players_added': 27,
                'duration': 8.3
            }
        """
        start_time = datetime.utcnow()

        # Create job record
        job = self.db.create_scrape_job(
            job_type='manual',
            question_id=question_id
        )

        try:
            # Get question details
            question = self.db.get_question_by_id(question_id)
            if not question:
                raise ValueError(f"Question {question_id} not found")

            logger.info(f"Populating question {question_id}: {question.text}")

            # Scrape data based on question type
            if question.stat_type == "appearances":
                answers = self._populate_appearances(question)
            elif question.stat_type == "combined_apps_goals":
                answers = self._populate_combined_stats(question)
            elif question.stat_type == "goalkeeper":
                answers = self._populate_goalkeeper_stats(question)
            else:
                raise ValueError(f"Unknown stat_type: {question.stat_type}")

            # Insert answers
            rows_inserted = self.db.insert_answers_batch(answers)

            # Update job status
            duration = (datetime.utcnow() - start_time).total_seconds()
            self.db.update_scrape_job(
                job.id,
                status='success',
                rows_inserted=rows_inserted
            )

            logger.info(
                f"Successfully populated question {question_id}: "
                f"{rows_inserted} players in {duration:.1f}s"
            )

            return {
                'question_id': question_id,
                'status': 'success',
                'players_added': rows_inserted,
                'duration': duration
            }

        except Exception as e:
            # Update job with error
            self.db.update_scrape_job(
                job.id,
                status='failed',
                error_message=str(e)
            )

            logger.error(f"Failed to populate question {question_id}: {str(e)}")

            return {
                'question_id': question_id,
                'status': 'failed',
                'error': str(e)
            }

    def _populate_appearances(self, question: Question) -> List[Dict]:
        """Populate simple appearances question."""
        # Scrape team stats
        player_df = self.scraper.scrape_team_stats(
            question.season,
            question.league,
            question.team,
            "standard"
        )

        # Get column mapping
        column_mapping = self.scraper.get_column_names(player_df)

        # Transform to answers
        answers = self.transformer.transform_to_answers(
            player_df,
            question.id,
            'appearances',
            column_mapping
        )

        return answers

    def _populate_combined_stats(self, question: Question) -> List[Dict]:
        """Populate combined stats (appearances + goals)."""
        # Scrape team stats
        player_df = self.scraper.scrape_team_stats(
            question.season,
            question.league,
            question.team,
            "standard"
        )

        # Get column mapping
        column_mapping = self.scraper.get_column_names(player_df)

        # Transform to combined answers
        answers = self.transformer.transform_combined_stats(
            player_df,
            question.id,
            ['appearances', 'goals'],
            column_mapping
        )

        return answers

    def _populate_goalkeeper_stats(self, question: Question) -> List[Dict]:
        """Populate goalkeeper stats (appearances + clean sheets)."""
        # Scrape goalkeeper stats
        keeper_df = self.scraper.scrape_goalkeeper_stats(
            question.season,
            question.league
        )

        # Filter by team
        squad_col = [c for c in keeper_df.columns if 'Squad' in c][0]
        team_keepers = keeper_df[keeper_df[squad_col] == question.team].copy()

        # Get column mapping
        column_mapping = self.scraper.get_column_names(team_keepers)

        # Transform to answers
        answers = self.transformer.transform_goalkeeper_stats(
            team_keepers,
            question.id,
            column_mapping
        )

        return answers

    def populate_by_season_league(
        self,
        season: str,
        league: str
    ) -> Dict[str, any]:
        """
        Populate all questions for a season/league combination.

        Args:
            season: Season (e.g., "2023-2024")
            league: League name

        Returns:
            Dict with summary results

        Example:
            >>> result = populator.populate_by_season_league(
            ...     "2023-2024",
            ...     "England Premier League"
            ... )
            >>> print(result)
            {
                'season': '2023-2024',
                'league': 'England Premier League',
                'questions_processed': 20,
                'total_players': 603,
                'duration': 145.2
            }
        """
        start_time = datetime.utcnow()

        # Create job record
        job = self.db.create_scrape_job(
            job_type='bulk',
            season=season,
            league=league
        )

        try:
            # Get questions for this season/league
            questions = self.db.get_questions(
                status='active',
                season=season,
                league=league
            )

            logger.info(
                f"Populating {len(questions)} questions for {league} {season}"
            )

            total_inserted = 0
            failed_questions = []

            for question in questions:
                try:
                    result = self.populate_single_question(question.id)
                    if result['status'] == 'success':
                        total_inserted += result['players_added']
                    else:
                        failed_questions.append(question.id)
                except Exception as e:
                    logger.error(
                        f"Failed question {question.id}: {str(e)}"
                    )
                    failed_questions.append(question.id)

            # Update job
            duration = (datetime.utcnow() - start_time).total_seconds()
            self.db.update_scrape_job(
                job.id,
                status='success' if not failed_questions else 'partial',
                rows_inserted=total_inserted
            )

            logger.info(
                f"Completed {league} {season}: "
                f"{len(questions)} questions, {total_inserted} players"
            )

            return {
                'season': season,
                'league': league,
                'questions_processed': len(questions),
                'questions_failed': len(failed_questions),
                'total_players': total_inserted,
                'duration': duration
            }

        except Exception as e:
            self.db.update_scrape_job(
                job.id,
                status='failed',
                error_message=str(e)
            )
            raise

    def update_current_season(self) -> Dict[str, any]:
        """
        Update answers for current season (weekly job).

        Returns:
            Dict with update results
        """
        start_time = datetime.utcnow()

        # Create job record
        job = self.db.create_scrape_job(
            job_type='weekly',
            season=settings.current_season
        )

        try:
            # Get active questions for current season
            questions = self.db.get_questions(
                status='active',
                season=settings.current_season
            )

            logger.info(
                f"Updating {len(questions)} questions for season {settings.current_season}"
            )

            total_inserted = 0
            total_updated = 0
            total_deleted = 0

            for question in questions:
                try:
                    # Scrape fresh data
                    if question.stat_type == "appearances":
                        answers = self._populate_appearances(question)
                    elif question.stat_type == "combined_apps_goals":
                        answers = self._populate_combined_stats(question)
                    elif question.stat_type == "goalkeeper":
                        answers = self._populate_goalkeeper_stats(question)
                    else:
                        continue

                    # Update (not insert)
                    stats = self.db.update_answers(question.id, answers)
                    total_inserted += stats['inserted']
                    total_updated += stats['updated']
                    total_deleted += stats['deleted']

                except Exception as e:
                    logger.error(f"Failed to update question {question.id}: {str(e)}")

            # Update job
            duration = (datetime.utcnow() - start_time).total_seconds()
            self.db.update_scrape_job(
                job.id,
                status='success',
                rows_inserted=total_inserted,
                rows_updated=total_updated,
                rows_deleted=total_deleted
            )

            logger.info(
                f"Weekly update complete: "
                f"{total_inserted} inserted, {total_updated} updated, "
                f"{total_deleted} deleted in {duration:.1f}s"
            )

            return {
                'season': settings.current_season,
                'questions_updated': len(questions),
                'rows_inserted': total_inserted,
                'rows_updated': total_updated,
                'rows_deleted': total_deleted,
                'duration': duration
            }

        except Exception as e:
            self.db.update_scrape_job(
                job.id,
                status='failed',
                error_message=str(e)
            )
            raise
