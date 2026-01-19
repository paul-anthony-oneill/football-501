"""
Utility script to extract and store FBref player IDs.

This updates existing players in the database with their FBref IDs
by re-scraping league data and extracting player URLs.
"""

import logging
import re
from scrapers.fbref_scraper import FBrefScraper
from database.crud_v2 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def extract_fbref_id_from_url(url: str) -> str:
    """
    Extract FBref player ID from URL.

    Args:
        url: FBref player URL (e.g., "/en/players/1f44ac21/Erling-Haaland")

    Returns:
        FBref ID (e.g., "1f44ac21")
    """
    match = re.search(r'/players/([a-f0-9]+)/', url)
    if match:
        return match.group(1)
    return None


def update_player_fbref_ids(league: str = "Premier League", season: str = "2023-2024"):
    """
    Update FBref IDs for players by scraping league data.

    Args:
        league: League name
        season: Season string
    """
    scraper = FBrefScraper()
    db = DatabaseManager()

    logger.info(f"Fetching player data from {league} {season}...")

    try:
        # Scrape player stats
        player_df = scraper.scrape_player_stats(season, league, "standard")

        logger.info(f"Found {len(player_df)} players")

        # Check if we have player URL column
        url_cols = [c for c in player_df.columns if 'player' in c.lower() and c != 'Player']

        if not url_cols:
            logger.error("No player URL column found in data")
            logger.info(f"Available columns: {list(player_df.columns)}")
            return

        logger.info(f"Using column: {url_cols[0]}")

        # Get column mapping
        col_map = scraper.get_column_names(player_df)

        updated = 0
        not_found = 0

        for idx, row in player_df.iterrows():
            try:
                player_name = row[col_map['player_name']]

                # Try to find player URL/ID
                # ScraperFC might store it in different columns
                fbref_id = None

                for col in url_cols:
                    if col in row.index and row[col]:
                        fbref_id = extract_fbref_id_from_url(str(row[col]))
                        if fbref_id:
                            break

                if not fbref_id:
                    logger.debug(f"No FBref ID found for {player_name}")
                    not_found += 1
                    continue

                # Update player in database
                player = db.get_player_by_name(player_name)

                if player:
                    with db.get_session() as session:
                        from database.models_v2 import Player
                        db_player = session.query(Player).filter_by(id=player.id).first()
                        if db_player:
                            db_player.fbref_id = fbref_id
                            session.commit()
                            updated += 1
                            logger.info(f"Updated {player_name} with FBref ID: {fbref_id}")
                else:
                    logger.debug(f"Player {player_name} not found in database")

            except Exception as e:
                logger.error(f"Error processing row {idx}: {str(e)}")

        logger.info(f"✅ Update complete!")
        logger.info(f"  Updated: {updated} players")
        logger.info(f"  Not found: {not_found} players")

    except Exception as e:
        logger.error(f"Failed to update FBref IDs: {str(e)}")
        raise


if __name__ == "__main__":
    logger.info("="*80)
    logger.info("UPDATE FBREF PLAYER IDS")
    logger.info("="*80)

    # Update for 2023-2024 season
    update_player_fbref_ids(league="Premier League", season="2023-2024")
