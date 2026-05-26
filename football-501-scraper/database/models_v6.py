"""
SQLAlchemy Database Models for Football 501 — Version 6 (V6 + V7 Schema)

Reflects Flyway migrations V1–V7:
  V6: seasons, player_external_ids, team_external_ids, player_season_stints
  V7: question_templates, questions.status, answers.materialized_at, scrape_run_logs

Use these models for all new scraper code. models_v4.py is kept for backward
compatibility only and will be removed after V9.
"""

from datetime import datetime
import uuid

from sqlalchemy import (
    Boolean,
    Column,
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    SmallInteger,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship

Base = declarative_base()

# ==============================================================================
# FOOTBALL SOURCE LAYER (V6)
# ==============================================================================


class Season(Base):
    """
    One row per season cycle, e.g. '2023-24'.
    Cups map to the league season they overlap (FA Cup 2023-24 → '2023-24').
    """

    __tablename__ = "seasons"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    label = Column(String(10), nullable=False, unique=True)        # '2023-24'
    start_year = Column(SmallInteger, nullable=False)              # 2023
    end_year = Column(SmallInteger, nullable=False)                # 2024
    start_date = Column(Date)
    end_date = Column(Date)
    is_current = Column(Boolean, nullable=False, default=False)

    stints = relationship("PlayerSeasonStint", back_populates="season")

    __table_args__ = (
        Index("idx_seasons_start_year", "start_year"),
    )

    def __repr__(self):
        return f"<Season(label={self.label!r}, current={self.is_current})>"


class Team(Base):
    """Team entity — clubs and national teams."""

    __tablename__ = "teams"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False)
    team_type = Column(String(50), nullable=False)   # 'club', 'national'
    country = Column(String(100))
    # fbref_id dropped in V9 — external IDs live in team_external_ids.
    popularity_rank = Column(Integer, default=10)
    created_at = Column(DateTime, default=datetime.utcnow)

    external_ids = relationship("TeamExternalId", back_populates="team", cascade="all, delete-orphan")
    stints = relationship("PlayerSeasonStint", back_populates="team")

    __table_args__ = (
        UniqueConstraint("name", "team_type", name="uq_team_name_type"),
        Index("idx_teams_normalized_name", "normalized_name"),
    )

    def __repr__(self):
        return f"<Team(name={self.name!r}, type={self.team_type!r})>"


class Competition(Base):
    """Competition entity — leagues, cups, UEFA tournaments."""

    __tablename__ = "competitions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False)
    competition_type = Column(String(50), nullable=False)   # 'domestic_league', 'domestic_cup', ...
    country = Column(String(100))
    fbref_id = Column(String(100), unique=True)
    display_name = Column(String(255))
    tier = Column(SmallInteger)                              # 1 for top-flight; NULL otherwise (V6)
    created_at = Column(DateTime, default=datetime.utcnow)

    stints = relationship("PlayerSeasonStint", back_populates="competition")

    __table_args__ = (
        Index("idx_competitions_normalized_name", "normalized_name"),
    )

    def __repr__(self):
        return f"<Competition(name={self.name!r}, type={self.competition_type!r})>"


class Player(Base):
    """
    Player identity record (post-V9 schema).

    fbref_id and career_stats were dropped in V9:
      - External IDs now live in player_external_ids (keyed by source + external_id).
      - Per-season stats now live in player_season_stints.

    To look up a player by FBref ID, join through player_external_ids:
        session.query(Player)
               .join(PlayerExternalId, PlayerExternalId.player_id == Player.id)
               .filter(PlayerExternalId.source == 'fbref',
                       PlayerExternalId.external_id == fbref_id)
               .first()
    """

    __tablename__ = "players"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)
    normalized_name = Column(String(255), nullable=False, index=True)
    nationality = Column(String(100))
    last_scraped_at = Column(DateTime)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    external_ids = relationship("PlayerExternalId", back_populates="player", cascade="all, delete-orphan")
    stints = relationship("PlayerSeasonStint", back_populates="player")

    def __repr__(self):
        return f"<Player(name={self.name!r})>"


class PlayerExternalId(Base):
    """Multi-source external IDs for players (FBref, Transfermarkt, …)."""

    __tablename__ = "player_external_ids"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    player_id = Column(UUID(as_uuid=True), ForeignKey("players.id", ondelete="CASCADE"), nullable=False)
    source = Column(String(32), nullable=False)          # 'fbref', 'transfermarkt', ...
    external_id = Column(String(64), nullable=False)
    source_url = Column(Text)
    confidence = Column(SmallInteger, nullable=False, default=100)
    created_at = Column(DateTime, default=datetime.utcnow)

    player = relationship("Player", back_populates="external_ids")

    __table_args__ = (
        UniqueConstraint("source", "external_id"),
        Index("idx_player_ext_player", "player_id"),
    )

    def __repr__(self):
        return f"<PlayerExternalId(source={self.source!r}, external_id={self.external_id!r})>"


class TeamExternalId(Base):
    """Multi-source external IDs for teams. Same shape as PlayerExternalId."""

    __tablename__ = "team_external_ids"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    team_id = Column(UUID(as_uuid=True), ForeignKey("teams.id", ondelete="CASCADE"), nullable=False)
    source = Column(String(32), nullable=False)
    external_id = Column(String(64), nullable=False)
    source_url = Column(Text)
    confidence = Column(SmallInteger, nullable=False, default=100)
    created_at = Column(DateTime, default=datetime.utcnow)

    team = relationship("Team", back_populates="external_ids")

    __table_args__ = (
        UniqueConstraint("source", "external_id"),
        Index("idx_team_ext_team", "team_id"),
    )

    def __repr__(self):
        return f"<TeamExternalId(source={self.source!r}, external_id={self.external_id!r})>"


class PlayerSeasonStint(Base):
    """
    One row per (player, season, team, competition) — the V6 keystone table.

    Replaces players.career_stats JSONB after V8 backfill is verified (V9).
    """

    __tablename__ = "player_season_stints"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    player_id = Column(UUID(as_uuid=True), ForeignKey("players.id", ondelete="CASCADE"), nullable=False)
    season_id = Column(UUID(as_uuid=True), ForeignKey("seasons.id", ondelete="RESTRICT"), nullable=False)
    team_id = Column(UUID(as_uuid=True), ForeignKey("teams.id", ondelete="RESTRICT"), nullable=False)
    competition_id = Column(UUID(as_uuid=True), ForeignKey("competitions.id", ondelete="RESTRICT"), nullable=False)

    # Appearance breakdown
    appearances = Column(SmallInteger, nullable=False, default=0)
    starts = Column(SmallInteger, nullable=False, default=0)
    sub_appearances = Column(SmallInteger, nullable=False, default=0)
    minutes = Column(Integer, nullable=False, default=0)

    # Outfield stats
    goals            = Column(SmallInteger, nullable=False, default=0)
    penalty_goals    = Column(SmallInteger, nullable=False, default=0)   # V8: Performance_PK
    penalty_attempts = Column(SmallInteger, nullable=False, default=0)   # V8: Performance_PKatt
    assists          = Column(SmallInteger, nullable=False, default=0)

    # Discipline
    yellow_cards = Column(SmallInteger, nullable=False, default=0)
    red_cards = Column(SmallInteger, nullable=False, default=0)

    # Goalkeeper stats (0 for outfield players)
    clean_sheets = Column(SmallInteger, nullable=False, default=0)
    goals_conceded = Column(SmallInteger, nullable=False, default=0)
    is_goalkeeper = Column(Boolean, nullable=False, default=False)

    # Ingest provenance
    source = Column(String(32), nullable=False)               # 'fbref'
    source_scraped_at = Column(DateTime, nullable=False)

    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    player = relationship("Player", back_populates="stints")
    season = relationship("Season", back_populates="stints")
    team = relationship("Team", back_populates="stints")
    competition = relationship("Competition", back_populates="stints")

    __table_args__ = (
        UniqueConstraint(
            "player_id", "season_id", "team_id", "competition_id",
            name="player_season_stints_player_season_team_comp_key",
        ),
        Index("idx_stints_team_comp_season", "team_id", "competition_id", "season_id"),
        Index("idx_stints_player_comp", "player_id", "competition_id"),
        Index("idx_stints_comp_season", "competition_id", "season_id"),
    )

    def __repr__(self):
        return (
            f"<PlayerSeasonStint(player_id={self.player_id}, "
            f"team_id={self.team_id}, season_id={self.season_id})>"
        )


# ==============================================================================
# GAME ENGINE LAYER (V1–V7)
# ==============================================================================


class ScrapeJob(Base):
    """Scrape job audit log."""

    __tablename__ = "scrape_jobs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    job_type = Column(String(50), nullable=False)       # 'initial', 'weekly_update', 'manual', 'backfill'
    season = Column(String(20))
    competition_id = Column(UUID(as_uuid=True), ForeignKey("competitions.id", ondelete="SET NULL"))
    status = Column(String(20), nullable=False)         # 'running', 'success', 'failed', 'partial'
    players_scraped = Column(Integer, default=0)
    players_failed = Column(Integer, default=0)
    error_message = Column(Text)
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime)

    run_logs = relationship("ScrapeRunLog", back_populates="job", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<ScrapeJob(type={self.job_type!r}, status={self.status!r})>"


class ScrapeRunLog(Base):
    """Per-message audit log for scrape jobs (V7). Written by the Python microservice."""

    __tablename__ = "scrape_run_logs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    job_id = Column(UUID(as_uuid=True), ForeignKey("scrape_jobs.id", ondelete="CASCADE"), nullable=False)
    level = Column(String(10), nullable=False)     # 'INFO', 'WARN', 'ERROR'
    message = Column(Text, nullable=False)
    context = Column(JSONB, nullable=False, default=dict)
    logged_at = Column(DateTime, default=datetime.utcnow)

    job = relationship("ScrapeJob", back_populates="run_logs")

    __table_args__ = (
        Index("idx_scrape_run_logs_job", "job_id"),
        Index("idx_scrape_run_logs_level", "level", "logged_at"),
    )

    def __repr__(self):
        return f"<ScrapeRunLog(level={self.level!r}, message={self.message[:50]!r})>"


class Category(Base):
    __tablename__ = "categories"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False, unique=True)
    slug = Column(String(255), nullable=False, unique=True)
    description = Column(Text)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    templates = relationship("QuestionTemplate", back_populates="category")
    questions = relationship("Question", back_populates="category")


class QuestionTemplate(Base):
    """
    Template definition for auto-generated questions (V7).
    Metadata in DB; materialiser logic in Java (QuestionMaterializer).
    """

    __tablename__ = "question_templates"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    category_id = Column(UUID(as_uuid=True), ForeignKey("categories.id", ondelete="CASCADE"), nullable=False)
    slug = Column(String(64), nullable=False, unique=True)
    display_name = Column(String(255), nullable=False)
    text_template = Column(Text, nullable=False)
    param_schema = Column(JSONB, nullable=False)
    materializer_key = Column(String(64), nullable=False)
    metric_key = Column(String(50), nullable=False)
    default_min_score = Column(Integer)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    category = relationship("Category", back_populates="templates")
    questions = relationship("Question", back_populates="template")

    def __repr__(self):
        return f"<QuestionTemplate(slug={self.slug!r}, materializer={self.materializer_key!r})>"


class Question(Base):
    """Domain-agnostic question entity with lifecycle status (V7)."""

    __tablename__ = "questions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    category_id = Column(UUID(as_uuid=True), ForeignKey("categories.id", ondelete="CASCADE"), nullable=False)
    question_text = Column(Text, nullable=False)
    metric_key = Column(String(50), nullable=False)
    config = Column(JSONB, nullable=False, default=dict)
    min_score = Column(Integer)
    difficulty = Column(Integer, default=2)
    status = Column(String(20), nullable=False, default="draft")   # draft | active | retired
    template_id = Column(UUID(as_uuid=True), ForeignKey("question_templates.id", ondelete="SET NULL"))
    template_params = Column(JSONB, nullable=False, default=dict)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    category = relationship("Category", back_populates="questions")
    template = relationship("QuestionTemplate", back_populates="questions")
    answers = relationship("Answer", back_populates="question", cascade="all, delete-orphan")

    __table_args__ = (
        Index("idx_questions_status", "status"),
        Index("idx_questions_category", "category_id"),
        Index("idx_questions_template", "template_id"),
    )

    def __repr__(self):
        return f"<Question(text={self.question_text[:40]!r}, status={self.status!r})>"


class Answer(Base):
    """Pre-materialised answer for a question (V7: adds materialized_at)."""

    __tablename__ = "answers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    question_id = Column(UUID(as_uuid=True), ForeignKey("questions.id", ondelete="CASCADE"), nullable=False)
    answer_key = Column(String(255), nullable=False)
    display_text = Column(String(255), nullable=False)
    score = Column(Integer, nullable=False)
    is_valid_darts = Column(Boolean, nullable=False)
    is_bust = Column(Boolean, nullable=False)
    answer_metadata = Column("metadata", JSONB)     # 'metadata' is reserved by SQLAlchemy declarative base
    materialized_at = Column(DateTime, default=datetime.utcnow)    # V7
    created_at = Column(DateTime, default=datetime.utcnow)

    question = relationship("Question", back_populates="answers")

    __table_args__ = (
        UniqueConstraint("question_id", "answer_key", name="idx_answers_question_key"),
        Index("idx_answers_question_score", "question_id", "score"),
    )

    def __repr__(self):
        return f"<Answer(key={self.answer_key!r}, score={self.score})>"
