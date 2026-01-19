"""
SQLAlchemy Database Models for Football 501
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
    ForeignKey
)
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship

Base = declarative_base()


class Question(Base):
    """
    Question model - matches Spring Boot backend schema.
    """
    __tablename__ = "questions"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    text = Column(Text, nullable=False)
    league = Column(String(100))
    season = Column(String(20))
    team = Column(String(100))
    stat_type = Column(String(50))
    status = Column(String(20), default="active")
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationship to answers
    answers = relationship("Answer", back_populates="question", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Question(id={self.id}, text='{self.text[:50]}...')>"


class Answer(Base):
    """
    Answer model - stores player statistics for questions.
    """
    __tablename__ = "answers"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    question_id = Column(BigInteger, ForeignKey("questions.id"), nullable=False)
    player_name = Column(String(255), nullable=False)
    player_api_id = Column(Integer, nullable=True)
    statistic_value = Column(Integer, nullable=False)
    is_valid_darts_score = Column(Boolean, nullable=False)
    is_bust = Column(Boolean, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationship to question
    question = relationship("Question", back_populates="answers")

    def __repr__(self):
        return (
            f"<Answer(id={self.id}, player='{self.player_name}', "
            f"value={self.statistic_value})>"
        )


class ScrapeJob(Base):
    """
    Scrape job audit log - tracks all scraping operations.
    """
    __tablename__ = "scrape_jobs"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    job_type = Column(String(50), nullable=False)  # 'initial', 'weekly', 'manual'
    season = Column(String(20))
    league = Column(String(100))
    question_id = Column(BigInteger, ForeignKey("questions.id"), nullable=True)
    status = Column(String(20), nullable=False)  # 'running', 'success', 'failed'
    rows_inserted = Column(Integer, default=0)
    rows_updated = Column(Integer, default=0)
    rows_deleted = Column(Integer, default=0)
    error_message = Column(Text, nullable=True)
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime, nullable=True)

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
