"""
Parallel Player Career Scraper - Scrapes multiple players concurrently.

Uses ThreadPoolExecutor to speed up batch scraping while respecting rate limits.
Uses botasaurus browser automation to bypass CloudFlare protection.
"""

import logging
import time
from typing import List, Dict, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock
from bs4 import BeautifulSoup
from botasaurus.browser import browser, Driver

from database.crud_v2 import DatabaseManager
from config import settings

logger = logging.getLogger(__name__)


class ParallelPlayerScraper:
    """
    Scrapes player careers in parallel using thread pools.
    """

    def __init__(self, max_workers: int = 5, wait_time: int = None):
        """
        Initialize parallel scraper.

        Args:
            max_workers: Number of concurrent threads (default: 5)
            wait_time: Seconds between requests (default from config)
        """
        self.db = DatabaseManager()
        self.max_workers = max_workers
        self.wait_time = wait_time or settings.fbref_wait_time
        self.rate_limiter = RateLimiter(self.wait_time)
        logger.info(f"Parallel scraper initialized: {max_workers} workers, {self.wait_time}s wait")

    def scrape_players_parallel(
        self,
        player_ids: List[int],
        force_rescrape: bool = False
    ) -> Dict[str, int]:
        """
        Scrape multiple players in parallel.

        Args:
            player_ids: List of player IDs to scrape
            force_rescrape: Force rescrape even if recently done

        Returns:
            Dict with statistics
        """
        logger.info(f"Starting parallel scrape: {len(player_ids)} players, {self.max_workers} workers")

        overall_stats = {
            'players_processed': 0,
            'players_skipped': 0,
            'total_stats_stored': 0,
            'errors': 0
        }

        # Filter out players without FBref IDs upfront
        valid_jobs = []
        for player_id in player_ids:
            player = self.db.get_player_by_id(player_id)
            if player and player.fbref_id:
                valid_jobs.append((player_id, player.fbref_id, player.name))
            else:
                overall_stats['players_skipped'] += 1

        logger.info(f"Valid players to scrape: {len(valid_jobs)}")

        # Scrape in parallel
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            # Submit all jobs
            future_to_player = {
                executor.submit(
                    self._scrape_single_player,
                    player_id,
                    fbref_id,
                    force_rescrape
                ): (player_id, name)
                for player_id, fbref_id, name in valid_jobs
            }

            # Process results as they complete
            for idx, future in enumerate(as_completed(future_to_player), 1):
                player_id, name = future_to_player[future]

                try:
                    result = future.result()

                    if result.get('skipped'):
                        overall_stats['players_skipped'] += 1
                    else:
                        overall_stats['players_processed'] += 1
                        overall_stats['total_stats_stored'] += result.get('stats_stored', 0)

                    logger.info(
                        f"[{idx}/{len(valid_jobs)}] {name}: "
                        f"{result.get('stats_stored', 0)} stats stored"
                    )

                except Exception as e:
                    logger.error(f"Failed to scrape {name}: {str(e)}")
                    overall_stats['errors'] += 1

        logger.info(
            f"Parallel scrape complete: {overall_stats['players_processed']} processed, "
            f"{overall_stats['total_stats_stored']} stats stored, "
            f"{overall_stats['errors']} errors"
        )

        return overall_stats

    def _scrape_single_player(
        self,
        player_id: int,
        fbref_id: str,
        force_rescrape: bool
    ) -> Dict[str, int]:
        """
        Scrape a single player (called by thread pool).

        Args:
            player_id: Database player ID
            fbref_id: FBref player ID
            force_rescrape: Force rescrape

        Returns:
            Dict with stats
        """
        # Check if recently scraped
        player = self.db.get_player_by_id(player_id)
        if not force_rescrape and self._recently_scraped(player, days=30):
            return {'stats_stored': 0, 'skipped': True}

        # Rate limiting (thread-safe)
        self.rate_limiter.wait()

        # Fetch page using botasaurus (bypasses CloudFlare)
        player_url = f"https://fbref.com/en/players/{fbref_id}/"

        try:
            soup = self._get_soup_with_browser(player_url)

            # Parse career stats
            career_stats = self._parse_player_career_table(soup, player_id)

            # Store in database
            stats_stored = 0
            for stat_entry in career_stats:
                try:
                    self.db.upsert_player_career_stats(**stat_entry)
                    stats_stored += 1
                except Exception as e:
                    logger.debug(f"Failed to store stat: {str(e)}")

            # Update timestamp
            self.db.update_player_last_scraped(player_id)

            return {
                'stats_stored': stats_stored,
                'skipped': False,
                'player_name': player.name
            }

        except Exception as e:
            logger.error(f"Error scraping player {player_id}: {str(e)}")
            raise

    def _parse_player_career_table(self, soup: BeautifulSoup, player_id: int) -> List[Dict]:
        """Parse career statistics from player page."""
        career_stats = []

        table_ids = ['stats_standard_dom_lg', 'stats_standard', 'stats_standard_intl_club']

        for table_id in table_ids:
            table = soup.find('table', {'id': table_id})
            if not table:
                continue

            tbody = table.find('tbody')
            if not tbody:
                continue

            for row in tbody.find_all('tr'):
                if row.get('class') and any(c in ['thead', 'spacer', 'partial_table'] for c in row.get('class')):
                    continue

                try:
                    stat_entry = self._parse_career_row(row, player_id)
                    if stat_entry:
                        career_stats.append(stat_entry)
                except Exception as e:
                    logger.debug(f"Failed to parse row: {str(e)}")

        return career_stats

    def _parse_career_row(self, row, player_id: int) -> Optional[Dict]:
        """Parse a single career stats row."""
        cells = row.find_all(['th', 'td'])
        if len(cells) < 10:
            return None

        try:
            season = cells[0].get_text(strip=True)
            squad = cells[1].get_text(strip=True)
            country = cells[2].get_text(strip=True) if len(cells) > 2 else None
            comp = cells[3].get_text(strip=True) if len(cells) > 3 else None

            if not season or not squad or 'Season' in season or 'Total' in season:
                return None

            mp = self._extract_stat(row, 'matches')
            goals = self._extract_stat(row, 'goals')
            assists = self._extract_stat(row, 'assists')

            team_type = 'national' if 'National Team' in squad else 'club'

            team = self.db.get_or_create_team(
                name=squad,
                team_type=team_type,
                country=country if country else None
            )

            competition = self._get_or_create_competition(comp, country)

            return {
                'player_id': player_id,
                'team_id': team.id,
                'competition_id': competition.id,
                'season': season,
                'appearances': mp,
                'goals': goals,
                'assists': assists,
                'clean_sheets': 0
            }

        except Exception as e:
            return None

    def _extract_stat(self, row, data_stat_name: str) -> int:
        """Extract stat value from row."""
        cell = row.find('td', {'data-stat': data_stat_name})
        if cell:
            value = cell.get_text(strip=True)
            return self._safe_int(value)
        return 0

    def _get_or_create_competition(self, comp_name: str, country: Optional[str]) -> 'Competition':
        """Get or create competition."""
        comp_map = {
            'Premier League': ('domestic_league', 'England'),
            'La Liga': ('domestic_league', 'Spain'),
            'Bundesliga': ('domestic_league', 'Germany'),
            'Serie A': ('domestic_league', 'Italy'),
            'Ligue 1': ('domestic_league', 'France'),
            'Champions Lg': ('continental', None),
            'Europa Lg': ('continental', None),
            'World Cup': ('international', None),
        }

        for key, (comp_type, comp_country) in comp_map.items():
            if key in comp_name:
                return self.db.get_or_create_competition(
                    name=key,
                    competition_type=comp_type,
                    country=comp_country,
                    display_name=comp_name
                )

        return self.db.get_or_create_competition(
            name=comp_name,
            competition_type='domestic_league',
            country=country,
            display_name=comp_name
        )

    @staticmethod
    def _get_soup_with_browser(url: str) -> BeautifulSoup:
        """
        Fetch page using botasaurus browser automation.
        Bypasses CloudFlare protection (same technique as ScraperFC).

        Args:
            url: URL to fetch

        Returns:
            BeautifulSoup object
        """
        @browser(
            headless=False,  # Must use visible browser (CloudFlare detects headless)
            block_images_and_css=False,  # Load all content for parsing
            wait_for_complete_page_load=True,  # Wait for page load
            output=None,
            create_error_logs=False,
            reuse_driver=True,  # Reuse browser instance for speed
        )
        def fetch_page(driver: Driver, url: str):
            driver.google_get(url)

            # Wait longer for CloudFlare challenge to complete
            time.sleep(8)

            # Check if we got player data (look for career stats table)
            try:
                # FBref uses "stats_standard_dom_lg" table ID for career stats
                driver.wait_for_element("#stats_standard_dom_lg", wait=15)
                logger.debug("Found career stats table")
            except Exception as e:
                # Try alternative table IDs
                try:
                    driver.wait_for_element("#stats_standard", wait=5)
                    logger.debug("Found alternative stats table")
                except Exception:
                    logger.warning(f"No career stats tables found on: {url}")

            # Get final HTML after all loading
            html = driver.page_html

            if not html or len(html) < 1000:
                logger.error(f"Page HTML too short ({len(html) if html else 0} bytes), likely blocked")

            return BeautifulSoup(html, "html.parser")

        return fetch_page(url)

    def _recently_scraped(self, player, days: int = 30) -> bool:
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
    """
    Thread-safe rate limiter.
    """

    def __init__(self, wait_time: int):
        """
        Initialize rate limiter.

        Args:
            wait_time: Seconds to wait between requests
        """
        self.wait_time = wait_time
        self.last_request_time = 0
        self.lock = Lock()

    def wait(self):
        """
        Wait if necessary to respect rate limit.
        """
        with self.lock:
            current_time = time.time()
            time_since_last = current_time - self.last_request_time

            if time_since_last < self.wait_time:
                sleep_time = self.wait_time - time_since_last
                time.sleep(sleep_time)

            self.last_request_time = time.time()
