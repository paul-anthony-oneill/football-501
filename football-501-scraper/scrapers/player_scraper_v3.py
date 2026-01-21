"""
Player Career Scraper V3 - JSONB Storage
Scrapes player careers from FBRef and stores in JSONB format.
"""

import logging
import time
from typing import List, Dict, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock
from bs4 import BeautifulSoup
from botasaurus.browser import browser, Driver

from database.crud_v3 import DatabaseManager
from config import settings

logger = logging.getLogger(__name__)


class PlayerScraperV3:
    """
    Scrapes player careers from FBRef and stores in JSONB format.
    """

    def __init__(self, max_workers: int = 5, wait_time: int = None):
        """
        Initialize player scraper.

        Args:
            max_workers: Number of concurrent threads
            wait_time: Seconds between requests
        """
        self.db = DatabaseManager()
        self.max_workers = max_workers
        self.wait_time = wait_time or settings.fbref_wait_time
        self.rate_limiter = RateLimiter(self.wait_time)
        logger.info(f"Player scraper V3 initialized: {max_workers} workers, {self.wait_time}s wait")

    def scrape_player(
        self,
        fbref_id: str,
        player_name: str,
        nationality: Optional[str] = None,
        force_rescrape: bool = False
    ) -> Dict[str, int]:
        """
        Scrape single player and store in JSONB.

        Args:
            fbref_id: FBRef player ID
            player_name: Player display name
            nationality: Player nationality
            force_rescrape: Force rescrape even if recently done

        Returns:
            Dict with stats
        """
        # Check if recently scraped
        player = self.db.get_player_by_fbref_id(fbref_id)
        if player and not force_rescrape and self._recently_scraped(player, days=7):
            logger.info(f"Skipping {player_name} (recently scraped)")
            return {'stats_stored': 0, 'skipped': True}

        # Upsert player
        player = self.db.upsert_player(
            fbref_id=fbref_id,
            name=player_name,
            nationality=nationality
        )

        # Rate limiting
        self.rate_limiter.wait()

        # Fetch page
        player_url = f"https://fbref.com/en/players/{fbref_id}/"

        try:
            soup = self._get_soup_with_browser(player_url)

            # Parse career stats
            career_stats = self._parse_player_career_table(soup)

            # Store in database (JSONB)
            stats_stored = 0
            for stat_entry in career_stats:
                try:
                    # Get or create team
                    team = self.db.get_or_create_team(
                        name=stat_entry['team_name'],
                        team_type=stat_entry['team_type'],
                        country=stat_entry.get('country')
                    )

                    # Get or create competition
                    competition = self.db.get_or_create_competition(
                        name=stat_entry['competition_name'],
                        competition_type=stat_entry['competition_type'],
                        country=stat_entry.get('competition_country')
                    )

                    # Add season stats to JSONB
                    self.db.add_player_season_stats(
                        fbref_id=fbref_id,
                        season=stat_entry['season'],
                        team_name=team.name,
                        team_id=str(team.id),
                        competition_name=competition.name,
                        competition_id=str(competition.id),
                        appearances=stat_entry['appearances'],
                        goals=stat_entry['goals'],
                        assists=stat_entry['assists'],
                        clean_sheets=stat_entry.get('clean_sheets', 0),
                        minutes_played=stat_entry.get('minutes_played', 0)
                    )
                    stats_stored += 1
                except Exception as e:
                    logger.debug(f"Failed to store stat: {str(e)}")

            # Update timestamp
            self.db.update_player_last_scraped(fbref_id)

            logger.info(f"Scraped {player_name}: {stats_stored} seasons stored")

            return {
                'stats_stored': stats_stored,
                'skipped': False,
                'player_name': player_name
            }

        except Exception as e:
            logger.error(f"Error scraping player {player_name}: {str(e)}")
            raise

    def scrape_players_parallel(
        self,
        players: List[Dict[str, str]],
        force_rescrape: bool = False
    ) -> Dict[str, int]:
        """
        Scrape multiple players in parallel.

        Args:
            players: List of dicts with keys: fbref_id, name, nationality
            force_rescrape: Force rescrape

        Returns:
            Overall statistics
        """
        logger.info(f"Starting parallel scrape: {len(players)} players, {self.max_workers} workers")

        overall_stats = {
            'players_processed': 0,
            'players_skipped': 0,
            'total_stats_stored': 0,
            'errors': 0
        }

        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            # Submit all jobs
            future_to_player = {
                executor.submit(
                    self.scrape_player,
                    player['fbref_id'],
                    player['name'],
                    player.get('nationality'),
                    force_rescrape
                ): player['name']
                for player in players
            }

            # Process results
            for idx, future in enumerate(as_completed(future_to_player), 1):
                player_name = future_to_player[future]

                try:
                    result = future.result()

                    if result.get('skipped'):
                        overall_stats['players_skipped'] += 1
                    else:
                        overall_stats['players_processed'] += 1
                        overall_stats['total_stats_stored'] += result.get('stats_stored', 0)

                    logger.info(
                        f"[{idx}/{len(players)}] {player_name}: "
                        f"{result.get('stats_stored', 0)} stats stored"
                    )

                except Exception as e:
                    logger.error(f"Failed to scrape {player_name}: {str(e)}")
                    overall_stats['errors'] += 1

        logger.info(
            f"Parallel scrape complete: {overall_stats['players_processed']} processed, "
            f"{overall_stats['total_stats_stored']} stats stored, "
            f"{overall_stats['errors']} errors"
        )

        return overall_stats

    def _parse_player_career_table(self, soup: BeautifulSoup) -> List[Dict]:
        """Parse career statistics from player page."""
        career_stats = []

        # FBRef has multiple stats tables (domestic, international, etc.)
        table_ids = ['stats_standard_dom_lg', 'stats_standard', 'stats_standard_intl_club']

        for table_id in table_ids:
            table = soup.find('table', {'id': table_id})
            if not table:
                continue

            tbody = table.find('tbody')
            if not tbody:
                continue

            for row in tbody.find_all('tr'):
                # Skip header/spacer rows
                if row.get('class') and any(c in ['thead', 'spacer', 'partial_table'] for c in row.get('class')):
                    continue

                try:
                    stat_entry = self._parse_career_row(row)
                    if stat_entry:
                        career_stats.append(stat_entry)
                except Exception as e:
                    logger.debug(f"Failed to parse row: {str(e)}")

        return career_stats

    def _parse_career_row(self, row) -> Optional[Dict]:
        """Parse a single career stats row."""
        try:
            # Extract using data-stat attributes
            season = self._get_text_by_stat(row, 'season')
            squad = self._get_text_by_stat(row, 'team') or self._get_text_by_stat(row, 'squad')
            country = self._get_text_by_stat(row, 'country')
            comp = self._get_text_by_stat(row, 'comp_level')

            # Skip invalid rows
            if not season or not squad or 'Season' in season or 'Total' in season:
                return None

            # Extract stats
            appearances = self._extract_stat(row, 'matches')
            goals = self._extract_stat(row, 'goals')
            assists = self._extract_stat(row, 'assists')
            minutes = self._extract_stat(row, 'minutes')

            # Determine team type
            team_type = 'national' if 'National Team' in squad else 'club'

            # Determine competition type
            competition_info = self._classify_competition(comp, country)

            return {
                'season': season,
                'team_name': squad,
                'team_type': team_type,
                'country': country if country else None,
                'competition_name': competition_info['name'],
                'competition_type': competition_info['type'],
                'competition_country': competition_info.get('country'),
                'appearances': appearances,
                'goals': goals,
                'assists': assists,
                'clean_sheets': 0,  # Not available in standard table
                'minutes_played': minutes
            }

        except Exception as e:
            logger.debug(f"Row parse error: {e}")
            return None

    def _classify_competition(self, comp_name: str, country: Optional[str]) -> Dict:
        """
        Classify competition type.

        Returns:
            Dict with keys: name, type, country
        """
        comp_map = {
            'Premier League': {'name': 'Premier League', 'type': 'domestic_league', 'country': 'England'},
            'La Liga': {'name': 'La Liga', 'type': 'domestic_league', 'country': 'Spain'},
            'Bundesliga': {'name': 'Bundesliga', 'type': 'domestic_league', 'country': 'Germany'},
            'Serie A': {'name': 'Serie A', 'type': 'domestic_league', 'country': 'Italy'},
            'Ligue 1': {'name': 'Ligue 1', 'type': 'domestic_league', 'country': 'France'},
            'Champions Lg': {'name': 'Champions League', 'type': 'continental', 'country': None},
            'Europa Lg': {'name': 'Europa League', 'type': 'continental', 'country': None},
            'World Cup': {'name': 'World Cup', 'type': 'international', 'country': None},
        }

        for key, info in comp_map.items():
            if key in comp_name:
                return info

        # Default: domestic league
        return {
            'name': comp_name,
            'type': 'domestic_league',
            'country': country
        }

    def _get_text_by_stat(self, row, stat_name: str) -> Optional[str]:
        """Get text content of a cell by data-stat attribute."""
        cell = row.find(['th', 'td'], {'data-stat': stat_name})
        if cell:
            return cell.get_text(strip=True)
        return None

    def _extract_stat(self, row, data_stat_name: str) -> int:
        """Extract stat value from row."""
        cell = row.find('td', {'data-stat': data_stat_name})
        if cell:
            value = cell.get_text(strip=True)
            return self._safe_int(value)
        return 0

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

            # Wait for career stats table
            try:
                driver.wait_for_element("#stats_standard_dom_lg", wait=15)
            except Exception:
                try:
                    driver.wait_for_element("#stats_standard", wait=5)
                except Exception:
                    logger.warning(f"No career stats tables found on: {url}")

            html = driver.page_html
            return BeautifulSoup(html, "html.parser")

        return fetch_page(url)

    def _recently_scraped(self, player, days: int = 7) -> bool:
        """Check if player was recently scraped."""
        if not player.last_scraped_at:
            return False
        from datetime import datetime, timedelta
        threshold = datetime.utcnow() - timedelta(days=days)
        return player.last_scraped_at > threshold

    def _safe_int(self, value, default: int = 0) -> int:
        """Safely convert to int."""
        try:
            import pandas as pd
            if pd.isna(value):
                return default
            return int(float(value))
        except (ValueError, TypeError):
            return default


class RateLimiter:
    """Thread-safe rate limiter."""

    def __init__(self, wait_time: int):
        self.wait_time = wait_time
        self.last_request_time = 0
        self.lock = Lock()

    def wait(self):
        """Wait if necessary to respect rate limit."""
        with self.lock:
            current_time = time.time()
            time_since_last = current_time - self.last_request_time

            if time_since_last < self.wait_time:
                sleep_time = self.wait_time - time_since_last
                time.sleep(sleep_time)

            self.last_request_time = time.time()
