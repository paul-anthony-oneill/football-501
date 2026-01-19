"""
Test script for the enhanced player career scraper.

This demonstrates how to scrape complete career histories for players.
"""

import logging
from scrapers.player_career_scraper import PlayerCareerScraper
from database.crud_v2 import DatabaseManager

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def test_single_player():
    """
    Test scraping a single player's career.

    Note: You need to manually set a player's fbref_id in the database first.
    """
    scraper = PlayerCareerScraper()
    db = DatabaseManager()

    # Example: Erling Haaland's FBref ID is "1f44ac21"
    # You would need to update a player in your database first

    # Find a player to test with
    with db.get_session() as session:
        from database.models_v2 import Player
        player = session.query(Player).first()

        if not player:
            logger.error("No players found in database")
            return

        # Manually set FBref ID for testing (example: Haaland)
        # player.fbref_id = "1f44ac21"
        # session.commit()

        if not player.fbref_id:
            logger.warning(f"Player {player.name} has no FBref ID. Please set one first.")
            logger.info("Example FBref IDs:")
            logger.info("  - Erling Haaland: 1f44ac21")
            logger.info("  - Kevin De Bruyne: b8a3ad0c")
            logger.info("  - Phil Foden: ed1e53f3")
            return

        logger.info(f"Testing with player: {player.name} (ID: {player.id})")
        logger.info(f"FBref ID: {player.fbref_id}")

    # Scrape their full career
    try:
        result = scraper.scrape_full_player_career(
            player_id=player.id,
            fbref_id=player.fbref_id,
            force_rescrape=True
        )

        logger.info(f"✅ Scrape complete!")
        logger.info(f"  Stats stored: {result['stats_stored']}")
        logger.info(f"  Player: {result.get('player_name', 'Unknown')}")

        # Show what was stored
        logger.info("\nQuerying stored career stats...")
        stats = db.query_player_stats()

        for stat in stats[:10]:  # Show first 10
            logger.info(
                f"  {stat['player_name']} - {stat['team_name']} - "
                f"{stat['competition_name']} - {stat['season']}: "
                f"{stat['appearances']} apps, {stat['goals']} goals"
            )

    except Exception as e:
        logger.error(f"❌ Failed to scrape player career: {str(e)}")
        raise


def test_batch_scraping():
    """
    Test batch scraping multiple players.
    """
    scraper = PlayerCareerScraper()

    # Scrape first 3 players with FBref IDs (for testing)
    logger.info("Starting batch scrape (max 3 players)")

    try:
        result = scraper.scrape_all_stored_players(
            force_rescrape=True,
            max_players=3
        )

        logger.info(f"✅ Batch scrape complete!")
        logger.info(f"  Players processed: {result['players_processed']}")
        logger.info(f"  Players skipped: {result['players_skipped']}")
        logger.info(f"  Total stats stored: {result['total_stats_stored']}")
        logger.info(f"  Errors: {result['errors']}")

    except Exception as e:
        logger.error(f"❌ Batch scrape failed: {str(e)}")
        raise


if __name__ == "__main__":
    logger.info("="*80)
    logger.info("PLAYER CAREER SCRAPER TEST")
    logger.info("="*80)

    # Test single player first
    test_single_player()

    # Uncomment to test batch scraping
    # test_batch_scraping()
