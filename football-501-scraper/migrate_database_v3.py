"""
Database Migration - Add PlayerScrapeLog table.

Run this to add the new error tracking table to your existing database.
"""

import logging
from database.crud_v2 import DatabaseManager

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def migrate_database():
    """Add PlayerScrapeLog table to database."""
    logger.info("="*80)
    logger.info("DATABASE MIGRATION - Adding PlayerScrapeLog Table")
    logger.info("="*80)

    db = DatabaseManager()

    try:
        # This will create all tables that don't exist
        # Existing tables are not modified
        db.init_db()

        logger.info("✅ Migration complete!")
        logger.info("\nNew table added:")
        logger.info("  - player_scrape_logs: Tracks scraping success/failure per player")

        # Verify table was created
        with db.get_session() as session:
            from database.models_v2 import PlayerScrapeLog

            count = session.query(PlayerScrapeLog).count()
            logger.info(f"\nVerification: player_scrape_logs table exists (current rows: {count})")

    except Exception as e:
        logger.error(f"❌ Migration failed: {str(e)}")
        raise


if __name__ == "__main__":
    migrate_database()
