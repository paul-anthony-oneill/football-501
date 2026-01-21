"""
Scrape Complete Premier League History (1992-2026)

Scrapes all 34 Premier League seasons and stores stats in JSONB.
"""

import logging
from datetime import datetime
from scrapers.league_seeder_v3 import LeagueSeederV3
from database.crud_v3 import DatabaseManager
from sqlalchemy import text

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def scrape_all_seasons():
    """Scrape all Premier League seasons from 1992-1993 to 2025-2026."""

    # Generate all seasons
    seasons = [f'{year}-{year+1}' for year in range(1992, 2026)]

    logger.info('='*60)
    logger.info('PREMIER LEAGUE COMPLETE HISTORY SCRAPE')
    logger.info('='*60)
    logger.info(f'Total seasons: {len(seasons)}')
    logger.info(f'Range: {seasons[0]} to {seasons[-1]}')
    logger.info(f'Estimated time: {len(seasons)} seasons × 60s ≈ {len(seasons)} minutes')
    logger.info('='*60)

    start_time = datetime.now()

    seeder = LeagueSeederV3()
    db = DatabaseManager()

    results = {
        'seasons_processed': 0,
        'total_players': 0,
        'total_stats_stored': 0,
        'errors': []
    }

    for i, season in enumerate(seasons, 1):
        logger.info(f'\n[{i}/{len(seasons)}] Processing {season}...')

        try:
            # Scrape season
            season_start = datetime.now()
            players = seeder.seed_premier_league_season(season)
            season_duration = (datetime.now() - season_start).total_seconds()

            results['seasons_processed'] += 1
            results['total_players'] += len(players)
            results['total_stats_stored'] += len(players)

            logger.info(f'✓ {season}: {len(players)} players in {season_duration:.1f}s')

        except Exception as e:
            logger.error(f'✗ {season}: {str(e)}')
            results['errors'].append({'season': season, 'error': str(e)})

        # Progress update every 5 seasons
        if i % 5 == 0:
            elapsed = (datetime.now() - start_time).total_seconds()
            avg_per_season = elapsed / i
            remaining = (len(seasons) - i) * avg_per_season

            logger.info(f'\n--- Progress: {i}/{len(seasons)} seasons ---')
            logger.info(f'Elapsed: {elapsed/60:.1f} min')
            logger.info(f'Estimated remaining: {remaining/60:.1f} min')
            logger.info(f'Stats stored so far: {results["total_stats_stored"]}')

    # Final summary
    total_duration = (datetime.now() - start_time).total_seconds()

    logger.info('\n' + '='*60)
    logger.info('SCRAPING COMPLETE!')
    logger.info('='*60)
    logger.info(f'Seasons processed: {results["seasons_processed"]}/{len(seasons)}')
    logger.info(f'Total stats stored: {results["total_stats_stored"]}')
    logger.info(f'Total time: {total_duration/60:.1f} minutes')
    logger.info(f'Average per season: {total_duration/len(seasons):.1f} seconds')

    if results['errors']:
        logger.warning(f'\nErrors: {len(results["errors"])}')
        for err in results['errors']:
            logger.warning(f'  - {err["season"]}: {err["error"]}')

    # Check final database state
    logger.info('\n' + '='*60)
    logger.info('FINAL DATABASE STATE')
    logger.info('='*60)

    with db.get_session() as session:
        # Total players
        from database.models_v3 import Player
        total_players = session.query(Player).count()
        logger.info(f'Total unique players: {total_players}')

        # Players with career data
        result = session.execute(text('''
            SELECT COUNT(*) FROM players
            WHERE jsonb_array_length(career_stats) > 0
        '''))
        with_stats = result.scalar()
        logger.info(f'Players with career data: {with_stats}')

        # Total season records
        result = session.execute(text('''
            SELECT SUM(jsonb_array_length(career_stats)) as total
            FROM players
        '''))
        total_seasons = result.scalar()
        logger.info(f'Total season records: {total_seasons}')

        # Seasons breakdown
        result = session.execute(text('''
            SELECT season->>'season' as season_name, COUNT(*) as player_count
            FROM players, jsonb_array_elements(career_stats) as season
            GROUP BY season->>'season'
            ORDER BY season_name
        '''))

        logger.info('\nSeason coverage:')
        for row in result:
            logger.info(f'  {row[0]}: {row[1]} players')

    return results


if __name__ == "__main__":
    scrape_all_seasons()
