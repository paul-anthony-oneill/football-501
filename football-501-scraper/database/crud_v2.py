"""
Database CRUD Operations for Football 501 - Version 2
"""

import logging
from typing import List, Optional, Dict, Tuple
from datetime import datetime
from sqlalchemy import create_engine, or_, func
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.exc import IntegrityError

from database.models_v2 import (
    Base,
    Player,
    Team,
    Competition,
    PlayerCareerStats,
    Question,
    QuestionValidAnswer,
    ScrapeJob,
    PlayerScrapeLog
)
from config import settings

logger = logging.getLogger(__name__)


class DatabaseManager:
    """
    Manages all database operations for Football 501.
    """

    def __init__(self, db_url: Optional[str] = None):
        """
        Initialize database manager.

        Args:
            db_url: Database URL (defaults to settings)
        """
        self.db_url = db_url or settings.database_url
        self.engine = create_engine(self.db_url, echo=settings.sql_echo)
        self.SessionLocal = sessionmaker(bind=self.engine, autocommit=False, autoflush=False)
        logger.info(f"Database manager initialized: {self.db_url}")

    def init_db(self):
        """Create all database tables."""
        Base.metadata.create_all(bind=self.engine)
        logger.info("Database tables created")

    def drop_all(self):
        """Drop all database tables (use with caution!)."""
        Base.metadata.drop_all(bind=self.engine)
        logger.warning("All database tables dropped")

    def get_session(self) -> Session:
        """Get a new database session."""
        return self.SessionLocal()

    # ============================================================================
    # PLAYER OPERATIONS
    # ============================================================================

    def get_or_create_player(
        self,
        name: str,
        normalized_name: str,
        nationality: Optional[str] = None,
        fbref_id: Optional[str] = None
    ) -> Player:
        """
        Get existing player or create new one.

        Args:
            name: Player display name
            normalized_name: Lowercase name for matching
            nationality: Player nationality
            fbref_id: FBref player ID

        Returns:
            Player instance
        """
        with self.get_session() as session:
            # Try to find by fbref_id first
            if fbref_id:
                player = session.query(Player).filter_by(fbref_id=fbref_id).first()
                if player:
                    return player

            # Try to find by normalized name
            player = session.query(Player).filter_by(normalized_name=normalized_name).first()

            if not player:
                # Create new player
                player = Player(
                    name=name,
                    normalized_name=normalized_name,
                    nationality=nationality,
                    fbref_id=fbref_id
                )
                session.add(player)
                session.commit()
                session.refresh(player)
                logger.info(f"Created new player: {name}")

            return player

    def get_player_by_id(self, player_id: int) -> Optional[Player]:
        """Get player by ID."""
        with self.get_session() as session:
            return session.query(Player).filter_by(id=player_id).first()

    def get_player_by_name(self, name: str) -> Optional[Player]:
        """Get player by name (case-insensitive)."""
        normalized = name.lower().strip()
        with self.get_session() as session:
            return session.query(Player).filter_by(normalized_name=normalized).first()

    def update_player_last_scraped(self, player_id: int):
        """Update player's last_scraped_at timestamp."""
        with self.get_session() as session:
            player = session.query(Player).filter_by(id=player_id).first()
            if player:
                player.last_scraped_at = datetime.utcnow()
                session.commit()

    # ============================================================================
    # TEAM OPERATIONS
    # ============================================================================

    def get_or_create_team(
        self,
        name: str,
        team_type: str = 'club',
        country: Optional[str] = None,
        fbref_id: Optional[str] = None
    ) -> Team:
        """
        Get existing team or create new one.

        Args:
            name: Team name
            team_type: 'club' or 'national'
            country: Country
            fbref_id: FBref team ID

        Returns:
            Team instance
        """
        with self.get_session() as session:
            # Try to find by fbref_id first
            if fbref_id:
                team = session.query(Team).filter_by(fbref_id=fbref_id).first()
                if team:
                    return team

            # Try to find by name and type
            team = session.query(Team).filter_by(name=name, team_type=team_type).first()

            if not team:
                # Create new team
                team = Team(
                    name=name,
                    team_type=team_type,
                    country=country,
                    fbref_id=fbref_id
                )
                session.add(team)
                session.commit()
                session.refresh(team)
                logger.info(f"Created new team: {name} ({team_type})")

            return team

    def get_team_by_id(self, team_id: int) -> Optional[Team]:
        """Get team by ID."""
        with self.get_session() as session:
            return session.query(Team).filter_by(id=team_id).first()

    def get_team_by_name(self, name: str) -> Optional[Team]:
        """Get team by name."""
        with self.get_session() as session:
            return session.query(Team).filter_by(name=name).first()

    # ============================================================================
    # COMPETITION OPERATIONS
    # ============================================================================

    def get_or_create_competition(
        self,
        name: str,
        competition_type: str,
        country: Optional[str] = None,
        fbref_id: Optional[str] = None,
        display_name: Optional[str] = None
    ) -> Competition:
        """
        Get existing competition or create new one.

        Args:
            name: Competition name
            competition_type: 'domestic_league', 'continental', 'international', 'cup'
            country: Country (NULL for international)
            fbref_id: FBref competition ID
            display_name: User-friendly name

        Returns:
            Competition instance
        """
        with self.get_session() as session:
            # Try to find by fbref_id first
            if fbref_id:
                comp = session.query(Competition).filter_by(fbref_id=fbref_id).first()
                if comp:
                    return comp

            # Try to find by name, type, country
            comp = session.query(Competition).filter_by(
                name=name,
                competition_type=competition_type,
                country=country
            ).first()

            if not comp:
                # Create new competition
                comp = Competition(
                    name=name,
                    competition_type=competition_type,
                    country=country,
                    fbref_id=fbref_id,
                    display_name=display_name or name
                )
                session.add(comp)
                session.commit()
                session.refresh(comp)
                logger.info(f"Created new competition: {name} ({competition_type})")

            return comp

    def get_competition_by_id(self, competition_id: int) -> Optional[Competition]:
        """Get competition by ID."""
        with self.get_session() as session:
            return session.query(Competition).filter_by(id=competition_id).first()

    def get_competition_by_name(self, name: str) -> Optional[Competition]:
        """Get competition by name."""
        with self.get_session() as session:
            return session.query(Competition).filter_by(name=name).first()

    # ============================================================================
    # PLAYER CAREER STATS OPERATIONS
    # ============================================================================

    def upsert_player_career_stats(
        self,
        player_id: int,
        team_id: int,
        competition_id: int,
        season: str,
        appearances: int = 0,
        goals: int = 0,
        assists: int = 0,
        clean_sheets: int = 0,
        minutes_played: int = 0
    ) -> PlayerCareerStats:
        """
        Insert or update player career stats.

        Args:
            player_id: Player ID
            team_id: Team ID
            competition_id: Competition ID
            season: Season string
            appearances: Number of appearances
            goals: Number of goals
            assists: Number of assists
            clean_sheets: Number of clean sheets
            minutes_played: Minutes played

        Returns:
            PlayerCareerStats instance
        """
        with self.get_session() as session:
            # Try to find existing stat
            stat = session.query(PlayerCareerStats).filter_by(
                player_id=player_id,
                team_id=team_id,
                competition_id=competition_id,
                season=season
            ).first()

            if stat:
                # Update existing
                stat.appearances = appearances
                stat.goals = goals
                stat.assists = assists
                stat.clean_sheets = clean_sheets
                stat.minutes_played = minutes_played
                stat.last_updated = datetime.utcnow()
                logger.debug(f"Updated stats: player={player_id}, team={team_id}, season={season}")
            else:
                # Create new
                stat = PlayerCareerStats(
                    player_id=player_id,
                    team_id=team_id,
                    competition_id=competition_id,
                    season=season,
                    appearances=appearances,
                    goals=goals,
                    assists=assists,
                    clean_sheets=clean_sheets,
                    minutes_played=minutes_played
                )
                session.add(stat)
                logger.debug(f"Created stats: player={player_id}, team={team_id}, season={season}")

            session.commit()
            session.refresh(stat)
            return stat

    def query_player_stats(
        self,
        team_name: Optional[str] = None,
        competition_name: Optional[str] = None,
        season: Optional[str] = None,
        nationality: Optional[str] = None,
        min_appearances: int = 0
    ) -> List[Dict]:
        """
        Query player career stats with filters.

        Args:
            team_name: Team name filter
            competition_name: Competition name filter
            season: Season filter
            nationality: Player nationality filter
            min_appearances: Minimum appearances

        Returns:
            List of dicts with player stats
        """
        with self.get_session() as session:
            query = session.query(
                Player.id.label('player_id'),
                Player.name.label('player_name'),
                Player.normalized_name,
                Player.nationality,
                Team.name.label('team_name'),
                Competition.name.label('competition_name'),
                PlayerCareerStats.season,
                PlayerCareerStats.appearances,
                PlayerCareerStats.goals,
                PlayerCareerStats.assists,
                PlayerCareerStats.clean_sheets
            ).join(
                PlayerCareerStats, Player.id == PlayerCareerStats.player_id
            ).join(
                Team, Team.id == PlayerCareerStats.team_id
            ).join(
                Competition, Competition.id == PlayerCareerStats.competition_id
            )

            # Apply filters
            if team_name:
                query = query.filter(Team.name == team_name)
            if competition_name:
                query = query.filter(Competition.name == competition_name)
            if season:
                query = query.filter(PlayerCareerStats.season == season)
            if nationality:
                query = query.filter(Player.nationality == nationality)
            if min_appearances > 0:
                query = query.filter(PlayerCareerStats.appearances >= min_appearances)

            results = query.all()

            # Convert to dicts
            return [
                {
                    'player_id': r.player_id,
                    'player_name': r.player_name,
                    'normalized_name': r.normalized_name,
                    'nationality': r.nationality,
                    'team_name': r.team_name,
                    'competition_name': r.competition_name,
                    'season': r.season,
                    'appearances': r.appearances,
                    'goals': r.goals,
                    'assists': r.assists,
                    'clean_sheets': r.clean_sheets
                }
                for r in results
            ]

    # ============================================================================
    # QUESTION OPERATIONS
    # ============================================================================

    def create_question(
        self,
        question_text: str,
        stat_type: str,
        team_id: Optional[int] = None,
        competition_id: Optional[int] = None,
        nationality_filter: Optional[str] = None,
        season_filter: Optional[str] = None,
        aggregation: str = 'sum',
        min_score: Optional[int] = None
    ) -> Question:
        """
        Create a new question.

        Args:
            question_text: Question text
            stat_type: 'appearances', 'goals', 'combined_apps_goals', etc.
            team_id: Team ID filter
            competition_id: Competition ID filter
            nationality_filter: Nationality filter
            season_filter: Season filter
            aggregation: 'sum', 'single_season', 'latest_season'
            min_score: Minimum score filter

        Returns:
            Question instance
        """
        with self.get_session() as session:
            question = Question(
                question_text=question_text,
                stat_type=stat_type,
                team_id=team_id,
                competition_id=competition_id,
                nationality_filter=nationality_filter,
                season_filter=season_filter,
                aggregation=aggregation,
                min_score=min_score
            )
            session.add(question)
            session.commit()
            session.refresh(question)
            logger.info(f"Created question: {question_text}")
            return question

    def get_question_by_id(self, question_id: int) -> Optional[Question]:
        """Get question by ID."""
        with self.get_session() as session:
            return session.query(Question).filter_by(id=question_id).first()

    def get_active_questions(self) -> List[Question]:
        """Get all active questions."""
        with self.get_session() as session:
            return session.query(Question).filter_by(is_active=True).all()

    # ============================================================================
    # QUESTION VALID ANSWER OPERATIONS
    # ============================================================================

    def insert_valid_answers(self, answers: List[Dict]) -> int:
        """
        Insert valid answers for a question.

        Args:
            answers: List of answer dicts

        Returns:
            Number of answers inserted
        """
        with self.get_session() as session:
            inserted = 0
            for answer_data in answers:
                try:
                    answer = QuestionValidAnswer(**answer_data)
                    session.add(answer)
                    inserted += 1
                except IntegrityError:
                    session.rollback()
                    logger.warning(f"Duplicate answer skipped: {answer_data}")
                    continue

            session.commit()
            logger.info(f"Inserted {inserted} valid answers")
            return inserted

    def delete_answers_for_question(self, question_id: int) -> int:
        """
        Delete all answers for a question.

        Args:
            question_id: Question ID

        Returns:
            Number of answers deleted
        """
        with self.get_session() as session:
            deleted = session.query(QuestionValidAnswer).filter_by(
                question_id=question_id
            ).delete()
            session.commit()
            logger.info(f"Deleted {deleted} answers for question {question_id}")
            return deleted

    def get_answers_for_question(self, question_id: int) -> List[QuestionValidAnswer]:
        """Get all valid answers for a question."""
        with self.get_session() as session:
            return session.query(QuestionValidAnswer).filter_by(
                question_id=question_id
            ).all()

    # ============================================================================
    # SCRAPE JOB OPERATIONS
    # ============================================================================

    def create_scrape_job(
        self,
        job_type: str,
        season: Optional[str] = None,
        league: Optional[str] = None,
        competition_id: Optional[int] = None,
        question_id: Optional[int] = None
    ) -> ScrapeJob:
        """
        Create a new scrape job record.

        Args:
            job_type: 'initial', 'weekly', 'manual', 'career_scrape'
            season: Season
            league: League name
            competition_id: Competition ID
            question_id: Question ID

        Returns:
            ScrapeJob instance
        """
        with self.get_session() as session:
            job = ScrapeJob(
                job_type=job_type,
                season=season,
                league=league,
                competition_id=competition_id,
                question_id=question_id,
                status='running'
            )
            session.add(job)
            session.commit()
            session.refresh(job)
            logger.info(f"Created scrape job: {job_type} (id={job.id})")
            return job

    def update_scrape_job(
        self,
        job_id: int,
        status: Optional[str] = None,
        rows_inserted: Optional[int] = None,
        rows_updated: Optional[int] = None,
        rows_deleted: Optional[int] = None,
        players_scraped: Optional[int] = None,
        error_message: Optional[str] = None
    ):
        """
        Update scrape job status.

        Args:
            job_id: Job ID
            status: Job status
            rows_inserted: Rows inserted count
            rows_updated: Rows updated count
            rows_deleted: Rows deleted count
            players_scraped: Players scraped count
            error_message: Error message
        """
        with self.get_session() as session:
            job = session.query(ScrapeJob).filter_by(id=job_id).first()
            if not job:
                logger.error(f"Scrape job {job_id} not found")
                return

            if status:
                job.status = status
            if rows_inserted is not None:
                job.rows_inserted = rows_inserted
            if rows_updated is not None:
                job.rows_updated = rows_updated
            if rows_deleted is not None:
                job.rows_deleted = rows_deleted
            if players_scraped is not None:
                job.players_scraped = players_scraped
            if error_message:
                job.error_message = error_message

            if status in ('success', 'failed', 'partial'):
                job.completed_at = datetime.utcnow()

            session.commit()
            logger.info(f"Updated scrape job {job_id}: status={status}")

    # ============================================================================
    # PLAYER SCRAPE LOG OPERATIONS
    # ============================================================================

    def create_player_scrape_log(
        self,
        player_id: int,
        scrape_job_id: Optional[int] = None,
        status: str = 'pending'
    ) -> PlayerScrapeLog:
        """
        Create a scrape log entry for a player.

        Args:
            player_id: Player ID
            scrape_job_id: Parent scrape job ID
            status: Initial status

        Returns:
            PlayerScrapeLog instance
        """
        with self.get_session() as session:
            log = PlayerScrapeLog(
                player_id=player_id,
                scrape_job_id=scrape_job_id,
                status=status,
                attempt_count=0
            )
            session.add(log)
            session.commit()
            session.refresh(log)
            return log

    def update_player_scrape_log(
        self,
        player_id: int,
        status: str,
        stats_stored: Optional[int] = None,
        error_message: Optional[str] = None,
        error_type: Optional[str] = None
    ):
        """
        Update player scrape log status.

        Args:
            player_id: Player ID
            status: New status ('success', 'failed', 'skipped')
            stats_stored: Number of stats stored
            error_message: Error message if failed
            error_type: Error type category
        """
        with self.get_session() as session:
            # Get most recent log for this player
            log = session.query(PlayerScrapeLog).filter_by(
                player_id=player_id
            ).order_by(PlayerScrapeLog.created_at.desc()).first()

            if not log:
                # Create new log if doesn't exist
                log = PlayerScrapeLog(
                    player_id=player_id,
                    status=status,
                    attempt_count=1
                )
                session.add(log)
            else:
                # Update existing log
                log.status = status
                log.attempt_count += 1
                log.last_attempt_at = datetime.utcnow()

            if stats_stored is not None:
                log.stats_stored = stats_stored
            if error_message:
                log.error_message = error_message
            if error_type:
                log.error_type = error_type

            session.commit()
            logger.debug(f"Updated scrape log for player {player_id}: {status}")

    def get_failed_players(self) -> List[int]:
        """
        Get list of player IDs that failed scraping.

        Returns:
            List of player IDs
        """
        with self.get_session() as session:
            failed = session.query(PlayerScrapeLog.player_id).filter_by(
                status='failed'
            ).all()
            return [p.player_id for p in failed]

    def get_pending_players(self) -> List[int]:
        """
        Get list of player IDs pending scraping.

        Returns:
            List of player IDs
        """
        with self.get_session() as session:
            pending = session.query(PlayerScrapeLog.player_id).filter_by(
                status='pending'
            ).all()
            return [p.player_id for p in pending]

    def get_scrape_statistics(self) -> Dict[str, int]:
        """
        Get scraping statistics summary.

        Returns:
            Dict with counts by status
        """
        with self.get_session() as session:
            stats = session.query(
                PlayerScrapeLog.status,
                func.count(PlayerScrapeLog.id)
            ).group_by(PlayerScrapeLog.status).all()

            return {status: count for status, count in stats}
