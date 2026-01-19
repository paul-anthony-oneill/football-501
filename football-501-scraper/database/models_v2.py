"""
SQLAlchemy Database Models for Football 501 - Version 2
Normalized schema with career statistics storage.
"""

from datetime import datetime
from sqlalchemy import (
    Column,
    BigInteger,
    Integer,
    String,
    Text,
    Boolean,
    DateTime,
    ForeignKey,
    UniqueConstraint,
    Index
)
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship

Base = declarative_base()


class Player(Base):
    """
    Player entity - stores once, reused across all questions.
    """
    __tablename__ = "players"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False, index=True)  # lowercase for fuzzy matching
    nationality = Column(String(100))
    fbref_id = Column(String(100), unique=True)  # FBref player ID
    created_at = Column(DateTime, default=datetime.utcnow)
    last_scraped_at = Column(DateTime)

    # Relationships
    career_stats = relationship("PlayerCareerStats", back_populates="player", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Player(id={self.id}, name='{self.name}')>"


class Team(Base):
    """
    Team entity - both clubs and national teams.
    """
    __tablename__ = "teams"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(255), nullable=False)
    team_type = Column(String(50), nullable=False)  # 'club', 'national'
    country = Column(String(100))
    fbref_id = Column(String(100), unique=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    career_stats = relationship("PlayerCareerStats", back_populates="team")

    __table_args__ = (
        UniqueConstraint('name', 'team_type', name='uq_team_name_type'),
    )

    def __repr__(self):
        return f"<Team(id={self.id}, name='{self.name}', type='{self.team_type}')>"


class Competition(Base):
    """
    Competition entity - leagues, cups, international tournaments.
    """
    __tablename__ = "competitions"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(255), nullable=False)
    competition_type = Column(String(50), nullable=False)  # 'domestic_league', 'continental', 'international', 'cup'
    country = Column(String(100))  # NULL for international competitions
    fbref_id = Column(String(100), unique=True)
    display_name = Column(String(255))  # User-friendly name
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    career_stats = relationship("PlayerCareerStats", back_populates="competition")

    __table_args__ = (
        UniqueConstraint('name', 'competition_type', 'country', name='uq_comp_name_type_country'),
    )

    def __repr__(self):
        return f"<Competition(id={self.id}, name='{self.name}')>"


class PlayerCareerStats(Base):
    """
    Player career statistics - comprehensive storage for all teams/leagues/seasons.
    This is the central data store that questions will query against.
    """
    __tablename__ = "player_career_stats"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    player_id = Column(BigInteger, ForeignKey("players.id", ondelete="CASCADE"), nullable=False)
    team_id = Column(BigInteger, ForeignKey("teams.id", ondelete="CASCADE"), nullable=False)
    competition_id = Column(BigInteger, ForeignKey("competitions.id", ondelete="CASCADE"), nullable=False)
    season = Column(String(20), nullable=False)  # "2023-2024" or "career" for all-time

    # Statistics
    appearances = Column(Integer, default=0)
    goals = Column(Integer, default=0)
    assists = Column(Integer, default=0)
    clean_sheets = Column(Integer, default=0)
    minutes_played = Column(Integer, default=0)

    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    last_updated = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    player = relationship("Player", back_populates="career_stats")
    team = relationship("Team", back_populates="career_stats")
    competition = relationship("Competition", back_populates="career_stats")

    __table_args__ = (
        UniqueConstraint('player_id', 'team_id', 'competition_id', 'season', name='uq_player_team_comp_season'),
        Index('idx_pcs_player', 'player_id'),
        Index('idx_pcs_team_comp', 'team_id', 'competition_id'),
        Index('idx_pcs_season', 'season'),
    )

    def __repr__(self):
        return (
            f"<PlayerCareerStats(player_id={self.player_id}, team_id={self.team_id}, "
            f"competition_id={self.competition_id}, season='{self.season}', "
            f"apps={self.appearances}, goals={self.goals})>"
        )


class Question(Base):
    """
    Question definition with filters (not hardcoded strings).
    """
    __tablename__ = "questions"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    question_text = Column(Text, nullable=False)
    stat_type = Column(String(50), nullable=False)  # 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'

    # Filters (NULL = no filter)
    team_id = Column(BigInteger, ForeignKey("teams.id", ondelete="SET NULL"))
    competition_id = Column(BigInteger, ForeignKey("competitions.id", ondelete="SET NULL"))
    nationality_filter = Column(String(100))
    season_filter = Column(String(20))  # '2023-2024', 'career', NULL for all

    # Aggregation strategy
    aggregation = Column(String(50), default='sum')  # 'sum', 'single_season', 'latest_season'

    # Configuration
    min_score = Column(Integer)
    is_active = Column(Boolean, default=True)

    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    answers = relationship("QuestionValidAnswer", back_populates="question", cascade="all, delete-orphan")

    __table_args__ = (
        Index('idx_questions_filters', 'team_id', 'competition_id', 'season_filter'),
    )

    def __repr__(self):
        return f"<Question(id={self.id}, text='{self.question_text[:50]}...')>"


class QuestionValidAnswer(Base):
    """
    Pre-computed valid answers for questions.
    Cached for fast gameplay validation.
    """
    __tablename__ = "question_valid_answers"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    question_id = Column(BigInteger, ForeignKey("questions.id", ondelete="CASCADE"), nullable=False)
    player_id = Column(BigInteger, ForeignKey("players.id", ondelete="CASCADE"), nullable=False)
    player_name = Column(String(255), nullable=False)  # Denormalized for display
    normalized_name = Column(String(255), nullable=False)  # For fuzzy search
    score = Column(Integer, nullable=False)
    is_valid_darts_score = Column(Boolean, nullable=False)
    is_bust = Column(Boolean, nullable=False)

    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    last_computed = Column(DateTime, default=datetime.utcnow)

    # Relationships
    question = relationship("Question", back_populates="answers")
    player = relationship("Player")

    __table_args__ = (
        UniqueConstraint('question_id', 'player_id', name='uq_question_player'),
        Index('idx_qva_question', 'question_id'),
        # Trigram index for fuzzy matching (requires PostgreSQL pg_trgm extension)
        # Created via raw SQL: CREATE INDEX idx_qva_normalized_name ON question_valid_answers USING gin(normalized_name gin_trgm_ops);
    )

    def __repr__(self):
        return (
            f"<QuestionValidAnswer(id={self.id}, question_id={self.question_id}, "
            f"player='{self.player_name}', score={self.score})>"
        )


class ScrapeJob(Base):
    """
    Scrape job audit log - tracks all scraping operations.
    """
    __tablename__ = "scrape_jobs"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    job_type = Column(String(50), nullable=False)  # 'initial', 'weekly', 'manual', 'career_scrape'
    season = Column(String(20))
    league = Column(String(100))
    competition_id = Column(BigInteger, ForeignKey("competitions.id", ondelete="SET NULL"))
    question_id = Column(BigInteger, ForeignKey("questions.id", ondelete="SET NULL"))
    status = Column(String(20), nullable=False)  # 'running', 'success', 'failed', 'partial'
    rows_inserted = Column(Integer, default=0)
    rows_updated = Column(Integer, default=0)
    rows_deleted = Column(Integer, default=0)
    players_scraped = Column(Integer, default=0)
    error_message = Column(Text)
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime)

    def __repr__(self):
        return (
            f"<ScrapeJob(id={self.id}, type='{self.job_type}', "
            f"status='{self.status}')>"
        )

    @property
    def duration_seconds(self) -> float:
        """Calculate job duration in seconds."""
        if self.completed_at and self.started_at:
            delta = self.completed_at - self.started_at
            return delta.total_seconds()
        return 0.0


class PlayerScrapeLog(Base):
    """
    Individual player scraping log - tracks success/failure per player.
    Enables easy retry of failed players and prevents data loss.
    """
    __tablename__ = "player_scrape_logs"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    player_id = Column(BigInteger, ForeignKey("players.id", ondelete="CASCADE"), nullable=False)
    scrape_job_id = Column(BigInteger, ForeignKey("scrape_jobs.id", ondelete="SET NULL"))

    # Scraping status
    status = Column(String(20), nullable=False)  # 'pending', 'success', 'failed', 'skipped'
    attempt_count = Column(Integer, default=0)
    last_attempt_at = Column(DateTime)

    # Results
    stats_stored = Column(Integer, default=0)
    error_message = Column(Text)
    error_type = Column(String(100))  # 'http_403', 'parse_error', 'timeout', etc.

    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    player = relationship("Player")

    __table_args__ = (
        Index('idx_psl_player_status', 'player_id', 'status'),
        Index('idx_psl_status', 'status'),
    )

    def __repr__(self):
        return (
            f"<PlayerScrapeLog(player_id={self.player_id}, "
            f"status='{self.status}', attempts={self.attempt_count})>"
        )
