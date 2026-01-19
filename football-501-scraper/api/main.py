"""
FastAPI Application for Football 501 Scraping Service

Provides REST API for admin operations and health checks.
"""

import logging
from contextlib import asynccontextmanager
from typing import List, Optional
from pydantic import BaseModel

from fastapi import FastAPI, HTTPException, status
from fastapi.responses import JSONResponse

from config import settings
from jobs import QuestionPopulator, JobScheduler
from database import DatabaseManager

# Configure logging
logging.basicConfig(
    level=settings.log_level,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

# Global instances
populator = None
scheduler = None
db = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager.

    Starts scheduler on startup, stops on shutdown.
    """
    global populator, scheduler, db

    logger.info("Starting Football 501 Scraping Service...")

    # Initialize components
    populator = QuestionPopulator()
    scheduler = JobScheduler()
    db = DatabaseManager()

    # Start scheduler
    scheduler.start()

    logger.info("Service started successfully")

    yield

    # Shutdown
    logger.info("Shutting down service...")
    scheduler.stop()


# Create FastAPI app
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan
)


# ========== Pydantic Models ==========

class PopulateQuestionRequest(BaseModel):
    """Request to populate a single question."""
    question_id: int


class PopulateSeasonRequest(BaseModel):
    """Request to populate a season/league."""
    season: str
    league: str


class PopulateInitialRequest(BaseModel):
    """Request for initial database population."""
    seasons: List[str]
    leagues: List[str]


class JobResponse(BaseModel):
    """Job creation response."""
    job_id: Optional[int]
    status: str
    message: Optional[str]


# ========== Health Check Endpoints ==========

@app.get("/health")
async def health_check():
    """
    Health check endpoint.

    Returns:
        Service health status
    """
    return {
        "status": "healthy",
        "service": settings.app_name,
        "version": settings.app_version
    }


@app.get("/status")
async def service_status():
    """
    Detailed service status.

    Returns:
        Service status with scheduler info
    """
    scheduler_status = scheduler.get_scheduler_status() if scheduler else {'running': False}

    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "scheduler": scheduler_status,
        "database": "connected"  # Could add actual DB health check
    }


# ========== Admin API Endpoints ==========

@app.post("/api/admin/scrape-question", response_model=dict)
async def scrape_question(request: PopulateQuestionRequest):
    """
    Populate answers for a single question.

    Args:
        request: Question ID to populate

    Returns:
        Result dict with status

    Example:
        POST /api/admin/scrape-question
        {
            "question_id": 1234
        }

        Response:
        {
            "question_id": 1234,
            "status": "success",
            "players_added": 27,
            "duration": 8.3
        }
    """
    try:
        result = populator.populate_single_question(request.question_id)
        return result

    except Exception as e:
        logger.error(f"Failed to scrape question: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.post("/api/admin/scrape-season", response_model=dict)
async def scrape_season(request: PopulateSeasonRequest):
    """
    Populate all questions for a season/league.

    Args:
        request: Season and league to populate

    Returns:
        Result dict with summary

    Example:
        POST /api/admin/scrape-season
        {
            "season": "2023-2024",
            "league": "England Premier League"
        }
    """
    try:
        result = populator.populate_by_season_league(
            request.season,
            request.league
        )
        return result

    except Exception as e:
        logger.error(f"Failed to scrape season: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.post("/api/admin/populate-initial", response_model=dict)
async def populate_initial(request: PopulateInitialRequest):
    """
    Initial database population (multiple seasons/leagues).

    This is a long-running operation (20-30 minutes).

    Args:
        request: Seasons and leagues to populate

    Returns:
        Job status

    Example:
        POST /api/admin/populate-initial
        {
            "seasons": ["2023-2024", "2022-2023"],
            "leagues": ["England Premier League", "Spain La Liga"]
        }
    """
    try:
        results = []

        for league in request.leagues:
            for season in request.seasons:
                result = populator.populate_by_season_league(season, league)
                results.append(result)

        return {
            "status": "complete",
            "results": results
        }

    except Exception as e:
        logger.error(f"Failed initial population: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.post("/api/admin/trigger-weekly-update")
async def trigger_weekly_update():
    """
    Manually trigger weekly update.

    Returns:
        Update results
    """
    try:
        result = scheduler.trigger_weekly_update()
        return result

    except Exception as e:
        logger.error(f"Failed weekly update: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.get("/api/admin/jobs", response_model=List[dict])
async def get_jobs(
    job_type: Optional[str] = None,
    status: Optional[str] = None,
    limit: int = 20
):
    """
    Get job history.

    Args:
        job_type: Filter by job type ('weekly', 'manual', etc.)
        status: Filter by status ('success', 'failed')
        limit: Maximum jobs to return (default 20)

    Returns:
        List of job records

    Example:
        GET /api/admin/jobs?limit=10
    """
    try:
        jobs = db.get_scrape_jobs(job_type, status, limit)

        return [
            {
                'job_id': job.id,
                'job_type': job.job_type,
                'season': job.season,
                'league': job.league,
                'status': job.status,
                'rows_inserted': job.rows_inserted,
                'rows_updated': job.rows_updated,
                'rows_deleted': job.rows_deleted,
                'duration': job.duration_seconds,
                'started_at': job.started_at.isoformat() if job.started_at else None,
                'completed_at': job.completed_at.isoformat() if job.completed_at else None,
                'error_message': job.error_message
            }
            for job in jobs
        ]

    except Exception as e:
        logger.error(f"Failed to get jobs: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.get("/api/admin/jobs/{job_id}")
async def get_job(job_id: int):
    """
    Get specific job by ID.

    Args:
        job_id: Job ID

    Returns:
        Job details
    """
    try:
        job = db.get_scrape_job_by_id(job_id)

        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Job {job_id} not found"
            )

        return {
            'job_id': job.id,
            'job_type': job.job_type,
            'season': job.season,
            'league': job.league,
            'status': job.status,
            'rows_inserted': job.rows_inserted,
            'rows_updated': job.rows_updated,
            'rows_deleted': job.rows_deleted,
            'duration': job.duration_seconds,
            'started_at': job.started_at.isoformat() if job.started_at else None,
            'completed_at': job.completed_at.isoformat() if job.completed_at else None,
            'error_message': job.error_message
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get job: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


# ========== Error Handlers ==========

@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """Global exception handler."""
    logger.error(f"Unhandled exception: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "Internal server error"}
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug
    )
