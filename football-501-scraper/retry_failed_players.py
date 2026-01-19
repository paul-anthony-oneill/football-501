"""
Retry Failed Players - Automatically retry players that failed during scraping.

This script identifies all players marked as 'failed' in the scrape logs
and attempts to scrape them again.
"""

import logging
import sys
from datetime import datetime

from scrapers.parallel_player_scraper import ParallelPlayerScraper
from database.crud_v2 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('retry_failed.log'),
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)


def retry_failed_players(use_parallel: bool = True, max_attempts: int = 3):
    """
    Retry all failed players.

    Args:
        use_parallel: Use parallel scraping
        max_attempts: Max retry attempts per player

    Returns:
        Statistics dict
    """
    db = DatabaseManager()
    scraper = ParallelPlayerScraper(max_workers=5) if use_parallel else None

    logger.info("="*80)
    logger.info("RETRY FAILED PLAYERS")
    logger.info("="*80)

    # Get failed players
    failed_player_ids = db.get_failed_players()

    if not failed_player_ids:
        logger.info("✅ No failed players found!")
        return {'success': True, 'retried': 0}

    logger.info(f"Found {len(failed_player_ids)} failed players")

    # Filter by attempt count
    retry_list = []

    with db.get_session() as session:
        from database.models_v2 import PlayerScrapeLog, Player

        for player_id in failed_player_ids:
            log = session.query(PlayerScrapeLog).filter_by(
                player_id=player_id
            ).order_by(PlayerScrapeLog.created_at.desc()).first()

            if log and log.attempt_count < max_attempts:
                player = session.query(Player).filter_by(id=player_id).first()
                if player:
                    retry_list.append((player_id, player.name, log.attempt_count))

    if not retry_list:
        logger.info("All failed players have exceeded max attempts.")
        return {'success': True, 'retried': 0}

    logger.info(f"\nRetrying {len(retry_list)} players:")
    for player_id, name, attempts in retry_list[:10]:
        logger.info(f"  - {name} (attempt {attempts + 1}/{max_attempts})")
    if len(retry_list) > 10:
        logger.info(f"  ... and {len(retry_list) - 10} more")

    logger.info(f"\nEstimated time: ~{len(retry_list) * 7 / 60:.1f} minutes")
    logger.info("Starting retry in 5 seconds... (Ctrl+C to cancel)")

    import time
    time.sleep(5)

    # Retry scraping
    player_ids_to_retry = [p[0] for p in retry_list]

    if use_parallel and scraper:
        result = scraper.scrape_players_parallel(
            player_ids=player_ids_to_retry,
            force_rescrape=True
        )
    else:
        # Sequential retry
        from scrapers.player_career_scraper import PlayerCareerScraper
        seq_scraper = PlayerCareerScraper()

        result = {
            'players_processed': 0,
            'players_skipped': 0,
            'total_stats_stored': 0,
            'errors': 0
        }

        for idx, player_id in enumerate(player_ids_to_retry, 1):
            player = db.get_player_by_id(player_id)
            logger.info(f"[{idx}/{len(player_ids_to_retry)}] Retrying {player.name}...")

            try:
                player_result = seq_scraper.scrape_full_player_career(
                    player_id=player_id,
                    fbref_id=player.fbref_id,
                    force_rescrape=True
                )

                if player_result.get('skipped'):
                    result['players_skipped'] += 1
                    db.update_player_scrape_log(player_id, 'skipped')
                else:
                    result['players_processed'] += 1
                    result['total_stats_stored'] += player_result['stats_stored']
                    db.update_player_scrape_log(
                        player_id, 'success',
                        stats_stored=player_result['stats_stored']
                    )

            except Exception as e:
                logger.error(f"  ❌ Failed again: {str(e)}")
                result['errors'] += 1
                db.update_player_scrape_log(
                    player_id, 'failed',
                    error_message=str(e)[:500],
                    error_type=type(e).__name__
                )

    # Summary
    logger.info("\n" + "="*80)
    logger.info("RETRY COMPLETE")
    logger.info("="*80)
    logger.info(f"Players processed: {result['players_processed']}")
    logger.info(f"Players skipped: {result['players_skipped']}")
    logger.info(f"Stats stored: {result['total_stats_stored']}")
    logger.info(f"Still failed: {result['errors']}")

    # Show remaining failed
    remaining_failed = db.get_failed_players()
    logger.info(f"\nRemaining failed players: {len(remaining_failed)}")

    if remaining_failed and len(remaining_failed) <= 10:
        logger.info("\nFailed players:")
        with db.get_session() as session:
            from database.models_v2 import Player
            for player_id in remaining_failed:
                player = session.query(Player).filter_by(id=player_id).first()
                if player:
                    logger.info(f"  - {player.name} (ID: {player_id})")

    return result


def main():
    """Main entry point."""
    import argparse

    parser = argparse.ArgumentParser(description="Retry Failed Player Scrapes")
    parser.add_argument(
        '--sequential',
        action='store_true',
        help='Use sequential scraping (slower but safer)'
    )
    parser.add_argument(
        '--max-attempts',
        type=int,
        default=3,
        help='Maximum retry attempts per player (default: 3)'
    )

    args = parser.parse_args()

    try:
        result = retry_failed_players(
            use_parallel=not args.sequential,
            max_attempts=args.max_attempts
        )

        if result['errors'] == 0:
            logger.info("\n✅ All retries successful!")
        else:
            logger.warning(f"\n⚠️  {result['errors']} players still failing")

    except KeyboardInterrupt:
        logger.warning("\n\n⚠️  Retry interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"\n\n❌ Retry failed: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    main()
