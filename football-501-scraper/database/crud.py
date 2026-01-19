"""
Database CRUD Operations for Football 501 Scraping Service
"""

import logging
from typing import List, Dict, Optional
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.exc import SQLAlchemyError

from config import settings
from .models import Base, Question, Answer, ScrapeJob

logger = logging.getLogger(__name__)


class DatabaseManager:
    """
    Manages database connections and CRUD operations.
    """

    def __init__(self, database_url: Optional[str] = None):
        """
        Initialize database manager.

        Args:
            database_url: PostgreSQL connection string (default from config)
        """
        self.database_url = database_url or settings.database_url
        self.engine = create_engine(self.database_url, pool_pre_ping=True)
        self.SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)

        logger.info(f"Database connection initialized: {self.database_url.split('@')[1]}")

    def create_tables(self):
        """Create all tables in the database."""
        Base.metadata.create_all(bind=self.engine)
        logger.info("Database tables created")

    def get_session(self) -> Session:
        """
        Get a database session.

        Returns:
            SQLAlchemy Session

        Example:
            >>> db = DatabaseManager()
            >>> with db.get_session() as session:
            ...     questions = session.query(Question).all()
        """
        return self.SessionLocal()

    # ========== Question Operations ==========

    def get_questions(
        self,
        status: Optional[str] = None,
        season: Optional[str] = None,
        league: Optional[str] = None
    ) -> List[Question]:
        """
        Get questions with optional filters.

        Args:
            status: Filter by status ('active', 'inactive')
            season: Filter by season (e.g., "2023-2024")
            league: Filter by league

        Returns:
            List of Question objects
        """
        with self.get_session() as session:
            query = session.query(Question)

            if status:
                query = query.filter(Question.status == status)
            if season:
                query = query.filter(Question.season == season)
            if league:
                query = query.filter(Question.league == league)

            questions = query.all()
            logger.info(f"Retrieved {len(questions)} questions")
            return questions

    def get_question_by_id(self, question_id: int) -> Optional[Question]:
        """
        Get question by ID.

        Args:
            question_id: Question ID

        Returns:
            Question object or None
        """
        with self.get_session() as session:
            question = session.query(Question).filter(Question.id == question_id).first()
            return question

    # ========== Answer Operations ==========

    def insert_answers_batch(self, answers: List[Dict]) -> int:
        """
        Bulk insert answers into database.

        Args:
            answers: List of answer dicts from DataTransformer

        Returns:
            Number of rows inserted

        Example:
            >>> answers = [
            ...     {
            ...         'question_id': 1234,
            ...         'player_name': 'Erling Haaland',
            ...         'statistic_value': 31,
            ...         'is_valid_darts_score': True,
            ...         'is_bust': False
            ...     },
            ...     ...
            ... ]
            >>> count = db.insert_answers_batch(answers)
            >>> print(f"Inserted {count} answers")
        """
        if not answers:
            logger.warning("No answers to insert")
            return 0

        with self.get_session() as session:
            try:
                # Convert dicts to Answer objects
                answer_objects = [Answer(**answer) for answer in answers]

                # Bulk insert
                session.bulk_save_objects(answer_objects)
                session.commit()

                logger.info(f"Inserted {len(answers)} answers")
                return len(answers)

            except SQLAlchemyError as e:
                session.rollback()
                logger.error(f"Failed to insert answers: {str(e)}")
                raise

    def update_answers(
        self,
        question_id: int,
        new_answers: List[Dict]
    ) -> Dict[str, int]:
        """
        Update answers for a question.

        Compares new answers with existing:
        - Inserts new players
        - Updates changed statistics
        - Deletes removed players

        Args:
            question_id: Question ID
            new_answers: List of new answer dicts

        Returns:
            Dict with counts: {'inserted': N, 'updated': M, 'deleted': K}
        """
        with self.get_session() as session:
            try:
                # Get existing answers
                existing = session.query(Answer).filter(
                    Answer.question_id == question_id
                ).all()

                existing_map = {ans.player_name: ans for ans in existing}
                new_map = {ans['player_name']: ans for ans in new_answers}

                inserted = 0
                updated = 0
                deleted = 0

                # Update or insert
                for player_name, new_data in new_map.items():
                    if player_name in existing_map:
                        # Update if stat value changed
                        existing_ans = existing_map[player_name]
                        if existing_ans.statistic_value != new_data['statistic_value']:
                            existing_ans.statistic_value = new_data['statistic_value']
                            existing_ans.is_valid_darts_score = new_data['is_valid_darts_score']
                            existing_ans.is_bust = new_data['is_bust']
                            existing_ans.updated_at = datetime.utcnow()
                            updated += 1
                    else:
                        # Insert new player
                        session.add(Answer(**new_data))
                        inserted += 1

                # Delete removed players
                for player_name, existing_ans in existing_map.items():
                    if player_name not in new_map:
                        session.delete(existing_ans)
                        deleted += 1

                session.commit()

                logger.info(
                    f"Updated answers for question {question_id}: "
                    f"{inserted} inserted, {updated} updated, {deleted} deleted"
                )

                return {
                    'inserted': inserted,
                    'updated': updated,
                    'deleted': deleted
                }

            except SQLAlchemyError as e:
                session.rollback()
                logger.error(f"Failed to update answers: {str(e)}")
                raise

    def delete_answers_by_question(self, question_id: int) -> int:
        """
        Delete all answers for a question.

        Args:
            question_id: Question ID

        Returns:
            Number of rows deleted
        """
        with self.get_session() as session:
            try:
                count = session.query(Answer).filter(
                    Answer.question_id == question_id
                ).delete()

                session.commit()
                logger.info(f"Deleted {count} answers for question {question_id}")
                return count

            except SQLAlchemyError as e:
                session.rollback()
                logger.error(f"Failed to delete answers: {str(e)}")
                raise

    def get_answer_count_by_question(self, question_id: int) -> int:
        """
        Get count of answers for a question.

        Args:
            question_id: Question ID

        Returns:
            Number of answers
        """
        with self.get_session() as session:
            count = session.query(Answer).filter(
                Answer.question_id == question_id
            ).count()
            return count

    # ========== Scrape Job Operations ==========

    def create_scrape_job(
        self,
        job_type: str,
        season: Optional[str] = None,
        league: Optional[str] = None,
        question_id: Optional[int] = None
    ) -> ScrapeJob:
        """
        Create a new scrape job record.

        Args:
            job_type: Type of job ('initial', 'weekly', 'manual')
            season: Season (optional)
            league: League (optional)
            question_id: Question ID (optional)

        Returns:
            ScrapeJob object
        """
        with self.get_session() as session:
            try:
                job = ScrapeJob(
                    job_type=job_type,
                    season=season,
                    league=league,
                    question_id=question_id,
                    status='running',
                    started_at=datetime.utcnow()
                )

                session.add(job)
                session.commit()
                session.refresh(job)

                logger.info(f"Created scrape job {job.id} (type={job_type})")
                return job

            except SQLAlchemyError as e:
                session.rollback()
                logger.error(f"Failed to create scrape job: {str(e)}")
                raise

    def update_scrape_job(
        self,
        job_id: int,
        status: str,
        rows_inserted: int = 0,
        rows_updated: int = 0,
        rows_deleted: int = 0,
        error_message: Optional[str] = None
    ):
        """
        Update scrape job status.

        Args:
            job_id: Job ID
            status: New status ('success', 'failed')
            rows_inserted: Number of rows inserted
            rows_updated: Number of rows updated
            rows_deleted: Number of rows deleted
            error_message: Error message (if failed)
        """
        with self.get_session() as session:
            try:
                job = session.query(ScrapeJob).filter(ScrapeJob.id == job_id).first()

                if not job:
                    raise ValueError(f"Job {job_id} not found")

                job.status = status
                job.rows_inserted = rows_inserted
                job.rows_updated = rows_updated
                job.rows_deleted = rows_deleted
                job.error_message = error_message
                job.completed_at = datetime.utcnow()

                session.commit()

                logger.info(
                    f"Updated scrape job {job_id}: status={status}, "
                    f"duration={job.duration_seconds:.1f}s"
                )

            except SQLAlchemyError as e:
                session.rollback()
                logger.error(f"Failed to update scrape job: {str(e)}")
                raise

    def get_scrape_jobs(
        self,
        job_type: Optional[str] = None,
        status: Optional[str] = None,
        limit: int = 20
    ) -> List[ScrapeJob]:
        """
        Get scrape job history.

        Args:
            job_type: Filter by job type
            status: Filter by status
            limit: Maximum number of jobs to return

        Returns:
            List of ScrapeJob objects
        """
        with self.get_session() as session:
            query = session.query(ScrapeJob).order_by(ScrapeJob.started_at.desc())

            if job_type:
                query = query.filter(ScrapeJob.job_type == job_type)
            if status:
                query = query.filter(ScrapeJob.status == status)

            jobs = query.limit(limit).all()
            return jobs

    def get_scrape_job_by_id(self, job_id: int) -> Optional[ScrapeJob]:
        """
        Get scrape job by ID.

        Args:
            job_id: Job ID

        Returns:
            ScrapeJob object or None
        """
        with self.get_session() as session:
            job = session.query(ScrapeJob).filter(ScrapeJob.id == job_id).first()
            return job
