"""
Database CRUD Operations for Football 501 - Version 3 (JSONB)
"""

import logging
from typing import List, Optional, Dict
from datetime import datetime
from sqlalchemy import create_engine, func, text
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.orm.attributes import flag_modified
from sqlalchemy.exc import IntegrityError
from sqlalchemy.dialects.postgresql import insert

from database.models_v3 import (
    Base,
    Player,
    Team,
    Competition,
    Question,
    QuestionValidAnswer,
    ScrapeJob
)
from config import settings

logger = logging.getLogger(__name__)


# Invalid darts scores (cannot be achieved with 3 darts in standard 501)
INVALID_DARTS_SCORES = {163, 166, 169, 172, 173, 175, 176, 178, 179}


class DatabaseManager:
    """
    Manages all database operations for Football 501 V3 (JSONB).
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
        logger.info(f"Database manager initialized (V3 JSONB): {self.db_url}")

    def init_db(self):
        """Create all database tables."""
        # Create extensions first
        with self.engine.connect() as conn:
            conn.execute(text("CREATE EXTENSION IF NOT EXISTS pg_trgm"))
            conn.execute(text("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\""))
            conn.commit()

        # Create tables
        Base.metadata.create_all(bind=self.engine)

        # Create trigram indexes manually (SQLAlchemy doesn't support gin_trgm_ops directly)
        with self.engine.connect() as conn:
            conn.execute(text("""
                CREATE INDEX IF NOT EXISTS idx_players_name_trgm
                ON players USING gin(name gin_trgm_ops)
            """))
            conn.execute(text("""
                CREATE INDEX IF NOT EXISTS idx_qva_normalized_name_trgm
                ON question_valid_answers USING gin(normalized_name gin_trgm_ops)
            """))
            conn.commit()

        logger.info("Database tables created (V3 JSONB)")

    def drop_all(self):
        """Drop all database tables (use with caution!)."""
        Base.metadata.drop_all(bind=self.engine)
        logger.warning("All database tables dropped")

    def get_session(self) -> Session:
        """Get a new database session."""
        return self.SessionLocal()

    # ============================================================================
    # PLAYER OPERATIONS (JSONB)
    # ============================================================================

    def upsert_player(
        self,
        fbref_id: str,
        name: str,
        nationality: Optional[str] = None
    ) -> Player:
        """
        Create or update player.

        Args:
            fbref_id: FBRef player ID (unique)
            name: Player display name
            nationality: Player nationality

        Returns:
            Player instance
        """
        with self.get_session() as session:
            normalized_name = name.lower().strip()

            # Upsert using PostgreSQL ON CONFLICT
            stmt = insert(Player).values(
                fbref_id=fbref_id,
                name=name,
                normalized_name=normalized_name,
                nationality=nationality,
                career_stats=[],
                created_at=datetime.utcnow(),
                updated_at=datetime.utcnow()
            ).on_conflict_do_update(
                index_elements=['fbref_id'],
                set_={
                    'name': name,
                    'normalized_name': normalized_name,
                    'nationality': nationality,
                    'updated_at': datetime.utcnow()
                }
            ).returning(Player)

            result = session.execute(stmt)
            player = result.scalar_one()
            session.commit()
            session.refresh(player)
            return player

    def add_player_season_stats(
        self,
        fbref_id: str,
        season: str,
        team_name: str,
        team_id: str,
        competition_name: str,
        competition_id: str,
        appearances: int = 0,
        goals: int = 0,
        assists: int = 0,
        clean_sheets: int = 0,
        minutes_played: int = 0
    ) -> bool:
        """
        Add or update season statistics for a player (JSONB).

        Args:
            fbref_id: FBRef player ID
            season: Season string (e.g., '2023-2024')
            team_name: Team name
            team_id: Team UUID
            competition_name: Competition name
            competition_id: Competition UUID
            appearances: Matches played
            goals: Goals scored
            assists: Assists
            clean_sheets: Clean sheets (for goalkeepers)
            minutes_played: Minutes played

        Returns:
            True if successful
        """
        with self.get_session() as session:
            player = session.query(Player).filter_by(fbref_id=fbref_id).first()

            if not player:
                logger.error(f"Player not found: {fbref_id}")
                return False

            # Build season data object
            season_data = {
                'season': season,
                'team': team_name,
                'team_id': str(team_id),
                'competition': competition_name,
                'competition_id': str(competition_id),
                'appearances': appearances,
                'goals': goals,
                'assists': assists,
                'clean_sheets': clean_sheets,
                'minutes_played': minutes_played
            }

            # Get current career_stats (make a fresh copy to avoid reference issues)
            current_stats = list(player.career_stats) if player.career_stats else []

            # Check if season already exists
            found = False
            for i, existing_season in enumerate(current_stats):
                if (existing_season.get('season') == season and
                    existing_season.get('team') == team_name and
                    existing_season.get('competition') == competition_name):
                    # Update existing season
                    current_stats[i] = season_data
                    found = True
                    break

            if not found:
                # Add new season
                current_stats.append(season_data)

            # Update player and flag as modified (required for JSONB columns)
            player.career_stats = current_stats
            flag_modified(player, 'career_stats')
            player.updated_at = datetime.utcnow()

            session.commit()
            logger.debug(f"Updated {player.name}: {season} {team_name} ({appearances} apps)")
            return True

    def update_player_last_scraped(self, fbref_id: str):
        """Update last_scraped_at timestamp."""
        with self.get_session() as session:
            session.query(Player).filter_by(fbref_id=fbref_id).update({
                'last_scraped_at': datetime.utcnow()
            })
            session.commit()

    def get_player_by_fbref_id(self, fbref_id: str) -> Optional[Player]:
        """Get player by FBRef ID."""
        with self.get_session() as session:
            return session.query(Player).filter_by(fbref_id=fbref_id).first()

    # ============================================================================
    # TEAM OPERATIONS
    # ============================================================================

    def get_or_create_team(
        self,
        name: str,
        team_type: str,
        country: Optional[str] = None,
        fbref_id: Optional[str] = None
    ) -> Team:
        """
        Get existing team or create new one.

        Args:
            name: Team name
            team_type: 'club' or 'national'
            country: Country name
            fbref_id: FBRef team ID

        Returns:
            Team instance
        """
        with self.get_session() as session:
            normalized_name = name.lower().strip()

            # Try to find existing
            team = session.query(Team).filter_by(
                name=name,
                team_type=team_type
            ).first()

            if not team:
                # Create new team
                team = Team(
                    name=name,
                    normalized_name=normalized_name,
                    team_type=team_type,
                    country=country,
                    fbref_id=fbref_id
                )
                session.add(team)
                session.commit()
                session.refresh(team)
                logger.debug(f"Created team: {name} ({team_type})")

            return team

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
            competition_type: Type (domestic_league, continental, international, cup)
            country: Country (NULL for international)
            fbref_id: FBRef competition ID
            display_name: User-friendly display name

        Returns:
            Competition instance
        """
        with self.get_session() as session:
            normalized_name = name.lower().strip()

            # Try to find existing
            query = session.query(Competition).filter_by(
                name=name,
                competition_type=competition_type
            )

            if country:
                query = query.filter_by(country=country)
            else:
                query = query.filter(Competition.country.is_(None))

            competition = query.first()

            if not competition:
                # Create new competition
                competition = Competition(
                    name=name,
                    normalized_name=normalized_name,
                    competition_type=competition_type,
                    country=country,
                    fbref_id=fbref_id,
                    display_name=display_name or name
                )
                session.add(competition)
                session.commit()
                session.refresh(competition)
                logger.debug(f"Created competition: {name}")

            return competition

    # ============================================================================
    # QUESTION OPERATIONS
    # ============================================================================

    def populate_question_answers(self, question_id: str) -> int:
        """
        Populate valid answers for a question by querying JSONB career_stats.

        Args:
            question_id: Question UUID

        Returns:
            Number of answers populated
        """
        with self.get_session() as session:
            question = session.query(Question).filter_by(id=question_id).first()

            if not question:
                logger.error(f"Question not found: {question_id}")
                return 0

            # Delete existing answers
            session.query(QuestionValidAnswer).filter_by(question_id=question_id).delete()

            # Build JSONB query based on filters
            query_parts = []

            if question.team_id:
                team = session.query(Team).filter_by(id=question.team_id).first()
                if team:
                    query_parts.append(f"season->>'team' = '{team.name}'")

            if question.competition_id:
                comp = session.query(Competition).filter_by(id=question.competition_id).first()
                if comp:
                    query_parts.append(f"season->>'competition' = '{comp.name}'")

            if question.season_filter:
                query_parts.append(f"season->>'season' = '{question.season_filter}'")

            where_clause = " AND ".join(query_parts) if query_parts else "1=1"

            # Determine score calculation based on stat_type
            if question.stat_type == 'appearances':
                score_expr = "(season->>'appearances')::int"
            elif question.stat_type == 'goals':
                score_expr = "(season->>'goals')::int"
            elif question.stat_type == 'combined_apps_goals':
                score_expr = "(season->>'appearances')::int + (season->>'goals')::int"
            elif question.stat_type == 'goalkeeper':
                score_expr = "(season->>'appearances')::int + (season->>'clean_sheets')::int"
            else:
                score_expr = "(season->>'appearances')::int"

            # Raw SQL query to extract answers from JSONB
            sql = text(f"""
                INSERT INTO question_valid_answers (
                    id,
                    question_id,
                    player_id,
                    player_name,
                    normalized_name,
                    score,
                    is_valid_darts_score,
                    is_bust,
                    created_at,
                    last_computed
                )
                SELECT
                    gen_random_uuid(),
                    :question_id,
                    p.id,
                    p.name,
                    p.normalized_name,
                    {score_expr} as score,
                    CASE
                        WHEN {score_expr} BETWEEN 1 AND 180
                            AND {score_expr} NOT IN (163, 166, 169, 172, 173, 175, 176, 178, 179)
                        THEN true
                        ELSE false
                    END as is_valid_darts_score,
                    CASE WHEN {score_expr} > 180 THEN true ELSE false END as is_bust,
                    NOW(),
                    NOW()
                FROM players p,
                    jsonb_array_elements(p.career_stats) as season
                WHERE
                    {where_clause}
                    AND {score_expr} > 0
            """)

            result = session.execute(sql, {'question_id': str(question_id)})
            session.commit()

            count = result.rowcount
            logger.info(f"Populated {count} answers for question {question_id}")
            return count

    # ============================================================================
    # SCRAPE JOB OPERATIONS
    # ============================================================================

    def create_scrape_job(
        self,
        job_type: str,
        season: Optional[str] = None,
        competition_id: Optional[str] = None
    ) -> ScrapeJob:
        """Create a new scrape job."""
        with self.get_session() as session:
            job = ScrapeJob(
                job_type=job_type,
                season=season,
                competition_id=competition_id,
                status='running',
                started_at=datetime.utcnow()
            )
            session.add(job)
            session.commit()
            session.refresh(job)
            logger.info(f"Created scrape job: {job.id} ({job_type})")
            return job

    def update_scrape_job(
        self,
        job_id: str,
        status: str,
        players_scraped: int = 0,
        players_failed: int = 0,
        error_message: Optional[str] = None
    ):
        """Update scrape job status."""
        with self.get_session() as session:
            session.query(ScrapeJob).filter_by(id=job_id).update({
                'status': status,
                'players_scraped': players_scraped,
                'players_failed': players_failed,
                'error_message': error_message,
                'completed_at': datetime.utcnow() if status in ['success', 'failed', 'partial'] else None
            })
            session.commit()
            logger.info(f"Updated scrape job {job_id}: {status}")

    # ============================================================================
    # UTILITY FUNCTIONS
    # ============================================================================

    @staticmethod
    def is_valid_darts_score(score: int) -> bool:
        """Check if score is valid in standard 501 darts."""
        if score < 1 or score > 180:
            return False
        return score not in INVALID_DARTS_SCORES

    @staticmethod
    def is_bust(score: int) -> bool:
        """Check if score is a bust (> 180)."""
        return score > 180
