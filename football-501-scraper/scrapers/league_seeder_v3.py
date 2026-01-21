"""
League Seeder V3 - Discovers players and their FBRef IDs for JSONB architecture.

Scrapes league season pages to find player names and FBRef IDs, then inserts
them into the players table. This is Stage 1 before scraping full career data.
"""

import logging
import time
import re
from typing import List, Dict, Optional
from bs4 import BeautifulSoup
from botasaurus.browser import browser, Driver

from database.crud_v3 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class LeagueSeederV3:
    """
    Scrapes league pages to seed the database with player entities and FBRef IDs.
    """

    def __init__(self):
        self.db = DatabaseManager()
        logger.info("League Seeder V3 initialized")

    def seed_premier_league_season(self, season: str = "2023-2024") -> List[Dict]:
        """
        Seed Premier League players for a specific season.

        Args:
            season: Season string (e.g., "2023-2024")

        Returns:
            List of player dicts with keys: name, fbref_id, nationality
        """
        # Premier League comp ID is 9 on FBRef
        url = f"https://fbref.com/en/comps/9/{season}/stats/{season}-Premier-League-Stats"

        logger.info(f"Seeding Premier League {season}")
        return self.seed_league(url, season, "Premier League")

    def seed_premier_league_multiple_seasons(
        self,
        seasons: List[str] = None
    ) -> Dict[str, List[Dict]]:
        """
        Seed Premier League players for multiple seasons.

        Args:
            seasons: List of season strings (defaults to last 5 seasons)

        Returns:
            Dict mapping season to list of players
        """
        if seasons is None:
            seasons = ["2019-2020", "2020-2021", "2021-2022", "2022-2023", "2023-2024"]

        results = {}
        for season in seasons:
            logger.info(f"\n{'='*60}")
            logger.info(f"Processing {season}")
            logger.info('='*60)

            try:
                players = self.seed_premier_league_season(season)
                results[season] = players
                logger.info(f"✅ {season}: {len(players)} players seeded")
            except Exception as e:
                logger.error(f"❌ {season} failed: {str(e)}")
                results[season] = []

            # Wait between seasons to avoid rate limiting
            if season != seasons[-1]:
                logger.info("Waiting 10 seconds before next season...")
                time.sleep(10)

        return results

    def seed_league(self, url: str, season: str, league_name: str) -> List[Dict]:
        """
        Scrape a league season page to discover players and extract their season stats.

        Args:
            url: FBRef URL for the league season stats
            season: Season string (e.g., "2023-2024")
            league_name: League name for logging

        Returns:
            List of player dicts with keys: name, fbref_id, nationality, stats
        """
        logger.info(f"Scraping {league_name} {season} from {url}")

        try:
            soup = self._get_soup_with_browser(url)

            # Debug: List all tables found
            tables = soup.find_all('table')
            table_ids = [t.get('id') for t in tables if t.get('id')]
            logger.info(f"Found {len(tables)} tables. IDs: {table_ids[:5]}")

            # Find the standard stats table
            table = soup.find('table', id=re.compile(r'stats_standard'))

            if not table:
                # Fallback: search by content
                logger.warning("Standard stats table ID not found, searching by content...")
                for t in tables:
                    if t.find('th', text='Player') or t.find('th', {'data-stat': 'player'}):
                        table = t
                        logger.info(f"Found table by content: {t.get('id')}")
                        break

            if not table:
                logger.error(f"Could not find player stats table on {url}")
                return []

            # Parse rows
            tbody = table.find('tbody')
            if not tbody:
                logger.error("Table has no tbody")
                return []

            players_batch = []

            for row in tbody.find_all('tr'):
                # Skip header rows
                if row.get('class') and 'thead' in row.get('class'):
                    continue

                player_data = self._extract_player_info_with_stats(row, season, league_name)
                if player_data:
                    players_batch.append(player_data)

            logger.info(f"Found {len(players_batch)} players on page")

            # Process database updates (now includes season stats)
            stats = self._process_players_with_stats(players_batch, season, league_name)

            logger.info(
                f"Seeding complete: {stats['new_players']} new, "
                f"{stats['existing_players']} existing, "
                f"{stats['stats_stored']} season stats stored, "
                f"{stats['errors']} errors"
            )

            return players_batch

        except Exception as e:
            logger.error(f"Seeding failed: {str(e)}")
            raise

    def _extract_player_info_with_stats(self, row, season: str, league_name: str) -> Optional[Dict]:
        """Extract player name, ID, nationality, and season stats from a row."""
        try:
            # Find player cell (data-stat="player")
            player_cell = row.find(['th', 'td'], {'data-stat': 'player'})

            if not player_cell:
                return None

            link = player_cell.find('a')
            if not link:
                return None

            href = link.get('href')  # e.g., /en/players/d5dd5f1f/Adama-Traore
            full_name = link.get_text(strip=True)

            # Extract FBRef ID from href
            # Pattern: /en/players/{id}/{name}
            match = re.search(r'/en/players/([a-zA-Z0-9]+)/', href)
            if not match:
                return None

            fbref_id = match.group(1)

            # Extract nationality if available
            nation_cell = row.find('td', {'data-stat': 'nationality'})
            nation = None
            if nation_cell:
                # Text format: "eng ENG" - take the last part
                nation_text = nation_cell.get_text(strip=True)
                if nation_text:
                    # Extract country code (last word)
                    parts = nation_text.split()
                    nation = parts[-1] if parts else None

            # Extract team
            team_cell = row.find('td', {'data-stat': 'team'})
            team_name = team_cell.get_text(strip=True) if team_cell else "Unknown"

            # Extract stats from the row
            def safe_int(cell_name):
                cell = row.find('td', {'data-stat': cell_name})
                if cell:
                    text = cell.get_text(strip=True)
                    try:
                        return int(text) if text else 0
                    except ValueError:
                        return 0
                return 0

            appearances = safe_int('games')
            goals = safe_int('goals')
            assists = safe_int('assists')
            minutes = safe_int('minutes')

            return {
                'name': full_name,
                'fbref_id': fbref_id,
                'nationality': nation,
                'team': team_name,
                'season': season,
                'league': league_name,
                'appearances': appearances,
                'goals': goals,
                'assists': assists,
                'minutes_played': minutes
            }

        except Exception as e:
            logger.debug(f"Row parse error: {e}")
            return None

    def _extract_player_info(self, row) -> Optional[Dict]:
        """Extract player name, ID, and nationality from a row (legacy method)."""
        try:
            # Find player cell (data-stat="player")
            player_cell = row.find(['th', 'td'], {'data-stat': 'player'})

            if not player_cell:
                return None

            link = player_cell.find('a')
            if not link:
                return None

            href = link.get('href')  # e.g., /en/players/d5dd5f1f/Adama-Traore
            full_name = link.get_text(strip=True)

            # Extract FBRef ID from href
            # Pattern: /en/players/{id}/{name}
            match = re.search(r'/en/players/([a-zA-Z0-9]+)/', href)
            if not match:
                return None

            fbref_id = match.group(1)

            # Extract nationality if available
            nation_cell = row.find('td', {'data-stat': 'nationality'})
            nation = None
            if nation_cell:
                # Text format: "eng ENG" - take the last part
                nation_text = nation_cell.get_text(strip=True)
                if nation_text:
                    # Extract country code (last word)
                    parts = nation_text.split()
                    nation = parts[-1] if parts else None

            return {
                'name': full_name,
                'fbref_id': fbref_id,
                'nationality': nation
            }

        except Exception as e:
            logger.debug(f"Row parse error: {e}")
            return None

    def _process_players_with_stats(self, players: List[Dict], season: str, league_name: str) -> Dict[str, int]:
        """
        Insert/update players in database and store their season stats in JSONB.

        Returns:
            Statistics dict
        """
        stats = {
            'new_players': 0,
            'existing_players': 0,
            'stats_stored': 0,
            'errors': 0
        }

        # Get or create competition
        competition = self.db.get_or_create_competition(
            name=league_name,
            competition_type='domestic_league',
            country='England',  # For Premier League
            display_name=league_name
        )

        for p_data in players:
            try:
                # Upsert player
                player = self.db.upsert_player(
                    fbref_id=p_data['fbref_id'],
                    name=p_data['name'],
                    nationality=p_data.get('nationality')
                )

                # Check if this is a new player
                if player.created_at == player.updated_at:
                    stats['new_players'] += 1
                else:
                    stats['existing_players'] += 1

                # Get or create team
                team = self.db.get_or_create_team(
                    name=p_data['team'],
                    team_type='club',
                    country='England'
                )

                # Add season stats to JSONB
                success = self.db.add_player_season_stats(
                    fbref_id=p_data['fbref_id'],
                    season=season,
                    team_name=team.name,
                    team_id=str(team.id),
                    competition_name=competition.name,
                    competition_id=str(competition.id),
                    appearances=p_data.get('appearances', 0),
                    goals=p_data.get('goals', 0),
                    assists=p_data.get('assists', 0),
                    clean_sheets=0,  # Not in league table
                    minutes_played=p_data.get('minutes_played', 0)
                )

                if success:
                    stats['stats_stored'] += 1
                    logger.debug(f"Stored {season} stats for {p_data['name']}: {p_data.get('appearances', 0)} apps")

            except Exception as e:
                logger.error(f"Error saving player/stats {p_data['name']}: {e}")
                stats['errors'] += 1

        return stats

    def _process_players(self, players: List[Dict]) -> Dict[str, int]:
        """
        Insert/update players in database (legacy method without stats).

        Returns:
            Statistics dict
        """
        stats = {
            'new_players': 0,
            'existing_players': 0,
            'errors': 0
        }

        for p_data in players:
            try:
                # Check if player exists
                existing = self.db.get_player_by_fbref_id(p_data['fbref_id'])

                if existing:
                    stats['existing_players'] += 1
                    logger.debug(f"Player exists: {p_data['name']}")
                else:
                    # Create new player
                    player = self.db.upsert_player(
                        fbref_id=p_data['fbref_id'],
                        name=p_data['name'],
                        nationality=p_data['nationality']
                    )
                    stats['new_players'] += 1
                    logger.debug(f"Created player: {p_data['name']} ({p_data['fbref_id']})")

            except Exception as e:
                logger.error(f"Error saving player {p_data['name']}: {e}")
                stats['errors'] += 1

        return stats

    @staticmethod
    def _get_soup_with_browser(url: str) -> BeautifulSoup:
        """
        Fetch page using botasaurus browser automation.
        Bypasses CloudFlare protection.
        """
        @browser(
            headless=False,
            block_images_and_css=False,
            wait_for_complete_page_load=True,
            output=None,
            create_error_logs=False,
            reuse_driver=True,
        )
        def fetch_page(driver: Driver, url: str):
            driver.google_get(url)
            time.sleep(8)  # Wait for CloudFlare

            # Wait for stats table
            try:
                driver.wait_for_element("table[id*='stats_standard']", wait=15)
                logger.debug("Stats table loaded")
            except Exception:
                logger.warning("Stats table wait timeout (may still be OK)")

            html = driver.page_html

            if not html or len(html) < 1000:
                logger.error(f"Page HTML too short ({len(html) if html else 0} bytes)")

            return BeautifulSoup(html, "html.parser")

        return fetch_page(url)


def main():
    """Demo: Seed Premier League 2023-24."""
    seeder = LeagueSeederV3()

    logger.info("="*60)
    logger.info("Premier League Seeder Demo")
    logger.info("="*60)

    # Seed single season
    players = seeder.seed_premier_league_season("2023-2024")

    logger.info("\n" + "="*60)
    logger.info(f"✅ Seeded {len(players)} players from Premier League 2023-24")
    logger.info("="*60)

    # Show sample players
    if players:
        logger.info("\nSample players:")
        for player in players[:5]:
            logger.info(f"  - {player['name']} ({player['fbref_id']}) - {player['nationality']}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='Seed Premier League players into database')
    parser.add_argument('--season', type=str, default='2023-2024', help='Season to seed (e.g., 2023-2024)')
    parser.add_argument('--multiple', action='store_true', help='Seed multiple seasons (last 5 years)')

    args = parser.parse_args()

    seeder = LeagueSeederV3()

    if args.multiple:
        results = seeder.seed_premier_league_multiple_seasons()

        total = sum(len(players) for players in results.values())
        logger.info(f"\n✅ Total players seeded across all seasons: {total}")

        for season, players in results.items():
            logger.info(f"  {season}: {len(players)} players")
    else:
        players = seeder.seed_premier_league_season(args.season)
        logger.info(f"\n✅ Seeded {len(players)} players from {args.season}")
