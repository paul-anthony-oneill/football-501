"""
Example: Parallel player career scraping for maximum speed.

This demonstrates how to scrape multiple players concurrently.
"""

import logging
from scrapers.parallel_player_scraper import ParallelPlayerScraper
from database.crud_v2 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def compare_sequential_vs_parallel():
    """
    Compare sequential vs parallel scraping performance.
    """
    logger.info("="*80)
    logger.info("PARALLEL SCRAPING PERFORMANCE COMPARISON")
    logger.info("="*80)

    db = DatabaseManager()

    # Get players with FBref IDs
    with db.get_session() as session:
        from database.models_v2 import Player
        player_ids = [p.id for p in session.query(Player).filter(
            Player.fbref_id.isnot(None)
        ).limit(10).all()]

    if not player_ids:
        logger.error("No players with FBref IDs found. Run update_fbref_ids.py first.")
        return

    total_players = len(player_ids)
    logger.info(f"\nTest set: {total_players} players")

    # Calculate time estimates
    sequential_time = total_players * 7  # 7 seconds per player
    parallel_time = sequential_time * 0.85  # ~15% speedup from overlapping work

    logger.info("\nEstimated times:")
    logger.info(f"  Sequential (1 at a time): {sequential_time}s (~{sequential_time/60:.1f} min)")
    logger.info(f"  Pipeline Parallel (5 workers): {parallel_time}s (~{parallel_time/60:.1f} min)")
    logger.info(f"\n  Expected speedup: ~15% (from overlapping CPU work with waiting)")
    logger.info(f"  Note: Still makes only 1 request per 7 seconds (rate limit safe)")

    # Ask user to confirm
    logger.info("\n" + "-"*80)
    response = input(f"Scrape {total_players} players in parallel (5 workers)? [y/N]: ")

    if response.lower() != 'y':
        logger.info("Cancelled.")
        return

    # Run parallel scraping
    logger.info("\n" + "="*80)
    logger.info("STARTING PARALLEL SCRAPE")
    logger.info("="*80)

    import time
    start_time = time.time()

    scraper = ParallelPlayerScraper(max_workers=5)
    result = scraper.scrape_players_parallel(
        player_ids=player_ids,
        force_rescrape=True
    )

    elapsed_time = time.time() - start_time

    logger.info("\n" + "="*80)
    logger.info("RESULTS")
    logger.info("="*80)
    logger.info(f"Players processed: {result['players_processed']}")
    logger.info(f"Players skipped:   {result['players_skipped']}")
    logger.info(f"Stats stored:      {result['total_stats_stored']}")
    logger.info(f"Errors:            {result['errors']}")
    logger.info(f"\nActual time:       {elapsed_time:.1f}s (~{elapsed_time/60:.1f} min)")
    logger.info(f"Expected time:     {parallel_time_5:.1f}s (~{parallel_time_5/60:.1f} min)")
    logger.info(f"Efficiency:        {(parallel_time_5/elapsed_time)*100:.0f}%")


def scrape_all_players_parallel():
    """
    Scrape ALL players in database using parallel approach.
    """
    logger.info("="*80)
    logger.info("SCRAPE ALL PLAYERS (PARALLEL)")
    logger.info("="*80)

    db = DatabaseManager()

    # Get all players with FBref IDs
    with db.get_session() as session:
        from database.models_v2 import Player
        all_player_ids = [p.id for p in session.query(Player).filter(
            Player.fbref_id.isnot(None)
        ).all()]

    total_players = len(all_player_ids)

    if total_players == 0:
        logger.error("No players with FBref IDs found. Run update_fbref_ids.py first.")
        return

    logger.info(f"\nTotal players: {total_players}")

    # Time estimates
    workers = 5
    base_time = total_players * 7
    estimated_time = base_time * 0.85  # ~15% speedup from pipeline
    logger.info(f"Estimated time: {estimated_time:.0f}s (~{estimated_time/60:.1f} minutes)")
    logger.info(f"(Pipeline parallel: ~15% faster than sequential due to overlapping work)")

    logger.info("\n" + "-"*80)
    response = input(f"Scrape all {total_players} players? [y/N]: ")

    if response.lower() != 'y':
        logger.info("Cancelled.")
        return

    # Run scraping
    import time
    start_time = time.time()

    scraper = ParallelPlayerScraper(max_workers=workers)
    result = scraper.scrape_players_parallel(
        player_ids=all_player_ids,
        force_rescrape=False  # Skip recently scraped
    )

    elapsed_time = time.time() - start_time

    logger.info("\n" + "="*80)
    logger.info("COMPLETE")
    logger.info("="*80)
    logger.info(f"Players processed: {result['players_processed']}")
    logger.info(f"Players skipped:   {result['players_skipped']}")
    logger.info(f"Stats stored:      {result['total_stats_stored']}")
    logger.info(f"Errors:            {result['errors']}")
    logger.info(f"\nTotal time: {elapsed_time:.1f}s ({elapsed_time/60:.1f} minutes)")


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "--all":
        scrape_all_players_parallel()
    else:
        compare_sequential_vs_parallel()

        logger.info("\n" + "="*80)
        logger.info("TIP: To scrape ALL players, run:")
        logger.info("  python example_parallel_scraping.py --all")
        logger.info("="*80)
