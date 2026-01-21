"""
SQLAlchemy Database Models for Football 501 - Version 4 (Generic Game Engine)
Matches Flyway Migration V2.
Separates 'Source Layer' (Football Data) from 'Game Layer' (Generic 501 Engine).
"""

from datetime import datetime
import uuid
from sqlalchemy import (
    Column,
    String,
    Text,
    Boolean,
    Integer,
    DateTime,
    ForeignKey,
    UniqueConstraint,
    Index
)
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship

Base = declarative_base()

# ==============================================================================
# SOURCE LAYER (Domain Specific: Football)
# ==============================================================================

class Player(Base):
    """
    Player entity with JSONB career statistics.
    Source of truth for answers.
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

    def __repr__(self):
        return f"<Player(id={self.id}, name='{self.name}', fbref_id='{self.fbref_id}')>"


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
    competition_type = Column(String(50), nullable=False)
    country = Column(String(100))
    fbref_id = Column(String(100), unique=True)
    display_name = Column(String(255))
    created_at = Column(DateTime, default=datetime.utcnow)

    scrape_jobs = relationship("ScrapeJob", back_populates="competition")

    __table_args__ = (
        Index('idx_competitions_normalized_name', 'normalized_name'),
    )

    def __repr__(self):
        return f"<Competition(id={self.id}, name='{self.name}')>"


class ScrapeJob(Base):
    """
    Scrape job audit log.
    """
    __tablename__ = "scrape_jobs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    job_type = Column(String(50), nullable=False)
    season = Column(String(20))
    competition_id = Column(UUID(as_uuid=True), ForeignKey("competitions.id", ondelete="SET NULL"))
    status = Column(String(20), nullable=False)
    players_scraped = Column(Integer, default=0)
    players_failed = Column(Integer, default=0)
    error_message = Column(Text)
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime)

    competition = relationship("Competition", back_populates="scrape_jobs")

    def __repr__(self):
        return f"<ScrapeJob(id={self.id}, type='{self.job_type}', status='{self.status}')>"


# ==============================================================================
# GAME LAYER (Generic: Universal 501)
# ==============================================================================

class Category(Base):
    __tablename__ = "categories"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False, unique=True)
    slug = Column(String(255), nullable=False, unique=True)
    description = Column(Text)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    questions = relationship("Question", back_populates="category")


class Question(Base):
    """
    Domain-agnostic Question entity.
    config (JSONB) defines filters for the Source Layer.
    metric_key defines the column/attribute to extract.
    """
    __tablename__ = "questions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    category_id = Column(UUID(as_uuid=True), ForeignKey("categories.id", ondelete="CASCADE"), nullable=False)
    question_text = Column(Text, nullable=False)
    metric_key = Column(String(50), nullable=False)  # e.g., 'goals', 'appearances'
    config = Column(JSONB, nullable=False, default=dict)  # e.g., {'team_id': '...', 'season': '2023'}
    min_score = Column(Integer)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    category = relationship("Category", back_populates="questions")
    answers = relationship("Answer", back_populates="question", cascade="all, delete-orphan")

    __table_args__ = (
        Index('idx_questions_active', 'is_active'),
        Index('idx_questions_category', 'category_id'),
    )


class Answer(Base):
    """
    Pre-computed generic answer.
    Source: Players (Row) + Question.Metric (Column).
    """
    __tablename__ = "answers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    question_id = Column(UUID(as_uuid=True), ForeignKey("questions.id", ondelete="CASCADE"), nullable=False)
    answer_key = Column(String(255), nullable=False)  # Normalized display text (lowercase)
    display_text = Column(String(255), nullable=False) # Original Entity Name
    score = Column(Integer, nullable=False)
    is_valid_darts = Column(Boolean, nullable=False)
    is_bust = Column(Boolean, nullable=False)
    answer_metadata = Column("metadata", JSONB) # Store source metadata (e.g. {'player_id': '...'})
    created_at = Column(DateTime, default=datetime.utcnow)

    question = relationship("Question", back_populates="answers")

    __table_args__ = (
        UniqueConstraint('question_id', 'answer_key', name='idx_answers_question_key'),
        Index('idx_answers_question_score', 'question_id', 'score'),
    )
