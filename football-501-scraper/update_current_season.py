"""
Update Current Premier League Season

Rescrapes the current season (2025-2026) and updates JSONB career_stats
for all players. Run this weekly to keep current season data fresh.

Usage:
    python update_current_season.py
"""

import logging
from datetime import datetime
from scrapers.league_seeder_v3 import LeagueSeederV3
from database.crud_v3 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def update_current_season(season: str = "2025-2026"):
    """
    Update the current Premier League season.

    Args:
        season: Season string (default: 2025-2026)
    """
    logger.info('='*60)
    logger.info(f'UPDATING CURRENT SEASON: {season}')
    logger.info('='*60)

    start_time = datetime.now()
    seeder = LeagueSeederV3()
    db = DatabaseManager()

    try:
        # Scrape current season (will update existing player records)
        logger.info(f'Scraping {season}...')
        players = seeder.seed_premier_league_season(season)

        duration = (datetime.now() - start_time).total_seconds()

        # Get statistics
        with db.get_session() as session:
            from sqlalchemy import text

            # Count total players with current season data
            result = session.execute(text("""
                SELECT COUNT(DISTINCT p.id) as player_count,
                       SUM((season->>'appearances')::int) as total_appearances
                FROM players p,
                     jsonb_array_elements(p.career_stats) as season
                WHERE season->>'season' = :season
            """), {'season': season})

            row = result.fetchone()
            player_count = row[0] if row else 0
            total_appearances = row[1] if row else 0

        logger.info('='*60)
        logger.info('UPDATE COMPLETE')
        logger.info('='*60)
        logger.info(f'Season: {season}')
        logger.info(f'Players updated: {player_count}')
        logger.info(f'Total appearances: {total_appearances}')
        logger.info(f'Duration: {duration:.1f}s')
        logger.info('='*60)

        return {
            'success': True,
            'season': season,
            'players_updated': player_count,
            'duration': duration
        }

    except Exception as e:
        logger.error(f'❌ Error updating {season}: {e}')
        return {
            'success': False,
            'season': season,
            'error': str(e)
        }


if __name__ == "__main__":
    # Update current season (2025-2026)
    result = update_current_season()

    if result['success']:
        logger.info(f"✅ Successfully updated {result['season']}")
    else:
        logger.error(f"❌ Failed to update {result['season']}: {result['error']}")
