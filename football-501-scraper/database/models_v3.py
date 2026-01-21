"""
SQLAlchemy Database Models for Football 501 - Version 3 (JSONB)
Clean slate design with JSONB storage for player career statistics.
"""

from datetime import datetime
from sqlalchemy import (
    Column,
    String,
    Text,
    Boolean,
    Integer,
    DateTime,
    ForeignKey,
    UniqueConstraint,
    Index,
    func
)
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
import uuid

Base = declarative_base()


class Player(Base):
    """
    Player entity with JSONB career statistics.
    """
    __tablename__ = "players"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    fbref_id = Column(String(50), unique=True, nullable=False)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False, index=True)
    nationality = Column(String(100))
    career_stats = Column(JSONB, nullable=False, default=list)
    last_scraped_at = Column(DateTime)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    answers = relationship("QuestionValidAnswer", back_populates="player", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Player(id={self.id}, name='{self.name}', fbref_id='{self.fbref_id}')>"

    def add_season_stats(self, season_data: dict):
        """
        Add or update season statistics in JSONB array.

        Args:
            season_data: Dict with keys: season, team, team_id, competition, competition_id,
                        appearances, goals, assists, clean_sheets, minutes_played
        """
        if not self.career_stats:
            self.career_stats = []

        # Check if season already exists
        for i, season in enumerate(self.career_stats):
            if (season.get('season') == season_data.get('season') and
                season.get('team') == season_data.get('team') and
                season.get('competition') == season_data.get('competition')):
                # Update existing season
                self.career_stats[i] = season_data
                return

        # Add new season
        self.career_stats.append(season_data)


class Team(Base):
    """
    Team entity - both clubs and national teams.
    """
    __tablename__ = "teams"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False)
    team_type = Column(String(50), nullable=False)  # 'club', 'national'
    country = Column(String(100))
    fbref_id = Column(String(100), unique=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    questions = relationship("Question", back_populates="team")

    __table_args__ = (
        UniqueConstraint('name', 'team_type', name='uq_team_name_type'),
        Index('idx_teams_normalized_name', 'normalized_name'),
    )

    def __repr__(self):
        return f"<Team(id={self.id}, name='{self.name}', type='{self.team_type}')>"


class Competition(Base):
    """
    Competition entity - leagues, cups, international tournaments.
    """
    __tablename__ = "competitions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False)
    competition_type = Column(String(50), nullable=False)  # 'domestic_league', 'continental', 'international', 'cup'
    country = Column(String(100))  # NULL for international
    fbref_id = Column(String(100), unique=True)
    display_name = Column(String(255))
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    questions = relationship("Question", back_populates="competition")
    scrape_jobs = relationship("ScrapeJob", back_populates="competition")

    __table_args__ = (
        # Use COALESCE in application logic, not in database constraint
        Index('idx_competitions_name_type_country', 'name', 'competition_type', 'country'),
        Index('idx_competitions_normalized_name', 'normalized_name'),
    )

    def __repr__(self):
        return f"<Competition(id={self.id}, name='{self.name}')>"


class Question(Base):
    """
    Question definition with filter criteria.
    """
    __tablename__ = "questions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    question_text = Column(Text, nullable=False)
    stat_type = Column(String(50), nullable=False)  # 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'

    # Filters (NULL = no filter)
    team_id = Column(UUID(as_uuid=True), ForeignKey("teams.id", ondelete="SET NULL"))
    competition_id = Column(UUID(as_uuid=True), ForeignKey("competitions.id", ondelete="SET NULL"))
    season_filter = Column(String(20))  # '2023-2024', 'career', NULL for all
    nationality_filter = Column(String(100))

    # Configuration
    min_score = Column(Integer)
    is_active = Column(Boolean, default=True)

    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    team = relationship("Team", back_populates="questions")
    competition = relationship("Competition", back_populates="questions")
    answers = relationship("QuestionValidAnswer", back_populates="question", cascade="all, delete-orphan")

    __table_args__ = (
        Index('idx_questions_active', 'is_active'),
        Index('idx_questions_filters', 'team_id', 'competition_id', 'season_filter'),
    )

    def __repr__(self):
        return f"<Question(id={self.id}, text='{self.question_text[:50]}...')>"


class QuestionValidAnswer(Base):
    """
    Pre-computed valid answers for questions.
    Populated by querying JSONB career_stats.
    """
    __tablename__ = "question_valid_answers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    question_id = Column(UUID(as_uuid=True), ForeignKey("questions.id", ondelete="CASCADE"), nullable=False)
    player_id = Column(UUID(as_uuid=True), ForeignKey("players.id", ondelete="CASCADE"), nullable=False)
    player_name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False)
    score = Column(Integer, nullable=False)
    is_valid_darts_score = Column(Boolean, nullable=False)
    is_bust = Column(Boolean, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    last_computed = Column(DateTime, default=datetime.utcnow)

    # Relationships
    question = relationship("Question", back_populates="answers")
    player = relationship("Player", back_populates="answers")

    __table_args__ = (
        UniqueConstraint('question_id', 'player_id', name='uq_question_player'),
        Index('idx_qva_question', 'question_id'),
        # Trigram index created manually via raw SQL
    )

    def __repr__(self):
        return (
            f"<QuestionValidAnswer(question_id={self.question_id}, "
            f"player='{self.player_name}', score={self.score})>"
        )


class ScrapeJob(Base):
    """
    Scrape job audit log.
    """
    __tablename__ = "scrape_jobs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    job_type = Column(String(50), nullable=False)  # 'initial', 'weekly_update', 'manual'
    season = Column(String(20))
    competition_id = Column(UUID(as_uuid=True), ForeignKey("competitions.id", ondelete="SET NULL"))
    status = Column(String(20), nullable=False)  # 'running', 'success', 'failed', 'partial'
    players_scraped = Column(Integer, default=0)
    players_failed = Column(Integer, default=0)
    error_message = Column(Text)
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime)

    # Relationships
    competition = relationship("Competition", back_populates="scrape_jobs")

    __table_args__ = (
        Index('idx_scrape_jobs_started', 'started_at'),
        Index('idx_scrape_jobs_status', 'status'),
    )

    def __repr__(self):
        return f"<ScrapeJob(id={self.id}, type='{self.job_type}', status='{self.status}')>"

    @property
    def duration_seconds(self) -> float:
        """Calculate job duration in seconds."""
        if self.completed_at and self.started_at:
            delta = self.completed_at - self.started_at
            return delta.total_seconds()
        return 0.0
