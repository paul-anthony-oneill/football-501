"""
Job Scheduler for Football 501 Scraping Service

Handles automated scraping jobs (weekly, monthly).
"""

import logging
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

from config import settings
from .populate_questions import QuestionPopulator

logger = logging.getLogger(__name__)


class JobScheduler:
    """
    Manages scheduled scraping jobs.
    """

    def __init__(self):
        """Initialize job scheduler."""
        self.scheduler = BackgroundScheduler()
        self.populator = QuestionPopulator()

        logger.info("Job scheduler initialized")

    def start(self):
        """Start the scheduler."""
        if not settings.enable_scheduler:
            logger.info("Scheduler disabled in configuration")
            return

        # Add weekly update job
        self.scheduler.add_job(
            func=self._weekly_update,
            trigger=CronTrigger.from_crontab(settings.weekly_update_cron),
            id='weekly_update',
            name='Weekly Stats Update',
            replace_existing=True
        )

        logger.info(
            f"Scheduled weekly update: {settings.weekly_update_cron}"
        )

        # Start scheduler
        self.scheduler.start()
        logger.info("Scheduler started")

    def stop(self):
        """Stop the scheduler."""
        if self.scheduler.running:
            self.scheduler.shutdown()
            logger.info("Scheduler stopped")

    def _weekly_update(self):
        """
        Weekly update job - runs every Sunday at 3 AM UTC.

        Updates current season statistics.
        """
        logger.info("Starting weekly update job...")

        try:
            result = self.populator.update_current_season()

            logger.info(
                f"Weekly update complete: "
                f"{result['questions_updated']} questions updated, "
                f"{result['rows_updated']} rows updated, "
                f"duration: {result['duration']:.1f}s"
            )

        except Exception as e:
            logger.error(f"Weekly update failed: {str(e)}", exc_info=True)

    def trigger_weekly_update(self):
        """
        Manually trigger weekly update (for admin API).

        Returns:
            Result dict from update operation
        """
        logger.info("Manually triggering weekly update...")
        return self.populator.update_current_season()

    def get_scheduler_status(self) -> dict:
        """
        Get scheduler status.

        Returns:
            Dict with scheduler info

        Example:
            >>> scheduler = JobScheduler()
            >>> status = scheduler.get_scheduler_status()
            >>> print(status)
            {
                'running': True,
                'jobs': [
                    {
                        'id': 'weekly_update',
                        'name': 'Weekly Stats Update',
                        'next_run': '2026-01-26 03:00:00 UTC'
                    }
                ]
            }
        """
        if not self.scheduler.running:
            return {'running': False, 'jobs': []}

        jobs = []
        for job in self.scheduler.get_jobs():
            jobs.append({
                'id': job.id,
                'name': job.name,
                'next_run': str(job.next_run_time) if job.next_run_time else None
            })

        return {
            'running': True,
            'jobs': jobs
        }
