"""
Football 501 Scraping Service - Configuration
"""

import os
from typing import List
from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Application
    app_name: str = "Football 501 Scraping Service"
    app_version: str = "1.0.0"
    debug: bool = Field(default=False, env="DEBUG")

    # Server
    host: str = Field(default="0.0.0.0", env="HOST")
    port: int = Field(default=8001, env="PORT")

    # Database
    database_url: str = Field(
        default="postgresql://football501:dev_password@localhost:5432/football501",
        env="DATABASE_URL"
    )

    # FBref Scraping
    fbref_wait_time: int = Field(
        default=7,
        env="FBREF_WAIT_TIME",
        description="Seconds to wait between FBref requests (rate limit)"
    )
    max_retries: int = Field(default=3, env="MAX_RETRIES")
    request_timeout: int = Field(default=30, env="REQUEST_TIMEOUT")

    # Scheduling
    weekly_update_cron: str = Field(
        default="0 3 * * SUN",
        env="WEEKLY_UPDATE_CRON",
        description="Cron expression for weekly updates (default: Sunday 3 AM UTC)"
    )
    monthly_update_cron: str = Field(
        default="0 2 1 * *",
        env="MONTHLY_UPDATE_CRON",
        description="Cron expression for monthly updates (default: 1st at 2 AM UTC)"
    )
    enable_scheduler: bool = Field(default=True, env="ENABLE_SCHEDULER")

    # Leagues Configuration
    mvp_leagues: List[str] = [
        "England Premier League",
        "Spain La Liga",
        "Italy Serie A"
    ]
    expansion_leagues: List[str] = [
        "Germany Bundesliga",
        "France Ligue 1",
        "UEFA Champions League"
    ]
    current_season: str = Field(default="2024-2025", env="CURRENT_SEASON")
    historical_seasons: List[str] = [
        "2023-2024",
        "2022-2023",
        "2021-2022"
    ]

    # Admin API
    admin_api_enabled: bool = Field(default=True, env="ADMIN_API_ENABLED")
    admin_jwt_secret: str = Field(
        default="your-secret-key-change-in-production",
        env="ADMIN_JWT_SECRET"
    )
    admin_jwt_algorithm: str = "HS256"
    admin_jwt_expire_minutes: int = 60

    # Logging
    log_level: str = Field(default="INFO", env="LOG_LEVEL")
    log_file: str = Field(
        default="/var/log/football501-scraper.log",
        env="LOG_FILE"
    )

    # Monitoring
    enable_metrics: bool = Field(default=True, env="ENABLE_METRICS")
    metrics_port: int = Field(default=9090, env="METRICS_PORT")

    # Invalid Darts Scores (from Football 501 game rules)
    invalid_darts_scores: set = {163, 166, 169, 172, 173, 175, 176, 178, 179}

    class Config:
        env_file = ".env"
        case_sensitive = False


# Global settings instance
settings = Settings()


# League name mapping (ScraperFC full names)
LEAGUE_MAPPING = {
    "EPL": "England Premier League",
    "Premier League": "England Premier League",
    "La Liga": "Spain La Liga",
    "Serie A": "Italy Serie A",
    "Bundesliga": "Germany Bundesliga",
    "Ligue 1": "France Ligue 1",
    "Champions League": "UEFA Champions League",
    "Europa League": "UEFA Europa League",
    "World Cup": "FIFA World Cup"
}


def get_league_name(league: str) -> str:
    """
    Convert league abbreviation to ScraperFC full name.

    Args:
        league: League name or abbreviation (e.g., "EPL", "La Liga")

    Returns:
        Full league name for ScraperFC (e.g., "England Premier League")
    """
    return LEAGUE_MAPPING.get(league, league)
