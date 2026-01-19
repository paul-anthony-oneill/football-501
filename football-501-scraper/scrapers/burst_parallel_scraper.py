"""
Burst Parallel Scraper - True parallel scraping with burst rate limiting.

WARNING: This makes multiple concurrent requests. Only use if FBref allows it.

Rate limit interpretation: "7 seconds per request" could mean:
- Option A: 1 request per 7 seconds (strict) - parallel_player_scraper.py
- Option B: N requests per (N * 7) seconds (burst allowed) - THIS FILE

Example: 5 requests per 35 seconds = same average rate, but allows bursts.

IMPORTANT: Test with small batches first. Monitor for 403/429 errors.
"""

import logging
import time
from typing import List, Dict, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Semaphore
import requests
from bs4 import BeautifulSoup

from database.crud_v2 import DatabaseManager
from config import settings

logger = logging.getLogger(__name__)


class BurstParallelScraper:
    """
    True parallel scraper that allows burst requests.

    Uses a semaphore to limit concurrent requests, then enforces
    a cooldown period after each batch.
    """

    def __init__(self, max_workers: int = 5, wait_time: int = None):
        """
        Initialize burst scraper.

        Args:
            max_workers: Max concurrent requests (default: 5)
            wait_time: Seconds to wait per worker (default from config)
        """
        self.db = DatabaseManager()
        self.max_workers = max_workers
        self.wait_time = wait_time or settings.fbref_wait_time

        # Semaphore limits concurrent requests
        self.semaphore = Semaphore(max_workers)

        # Track request times for batch cooldown
        self.request_times = []

        logger.warning(
            f"⚠️  Burst scraper initialized: {max_workers} concurrent requests. "
            f"Monitor for 403/429 errors!"
        )

    def scrape_players_burst(
        self,
        player_ids: List[int],
        force_rescrape: bool = False
    ) -> Dict[str, int]:
        """
        Scrape players with burst parallelism.

        Args:
            player_ids: Player IDs to scrape
            force_rescrape: Force rescrape

        Returns:
            Statistics dict
        """
        logger.info(
            f"Starting BURST parallel scrape: {len(player_ids)} players, "
            f"{self.max_workers} concurrent workers"
        )

        overall_stats = {
            'players_processed': 0,
            'players_skipped': 0,
            'total_stats_stored': 0,
            'errors': 0
        }

        # Filter valid jobs
        valid_jobs = []
        for player_id in player_ids:
            player = self.db.get_player_by_id(player_id)
            if player and player.fbref_id:
                valid_jobs.append((player_id, player.fbref_id, player.name))
            else:
                overall_stats['players_skipped'] += 1

        logger.info(f"Valid players: {len(valid_jobs)}")

        # Scrape in bursts
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_player = {
                executor.submit(
                    self._scrape_single_player,
                    player_id,
                    fbref_id,
                    force_rescrape
                ): (player_id, name)
                for player_id, fbref_id, name in valid_jobs
            }

            # Process results
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
                        f"{result.get('stats_stored', 0)} stats"
                    )

                except Exception as e:
                    logger.error(f"Failed {name}: {str(e)}")
                    overall_stats['errors'] += 1

        # Cooldown after batch
        self._enforce_cooldown()

        logger.info(
            f"Burst scrape complete: {overall_stats['players_processed']} processed, "
            f"{overall_stats['total_stats_stored']} stats, "
            f"{overall_stats['errors']} errors"
        )

        return overall_stats

    def _scrape_single_player(
        self,
        player_id: int,
        fbref_id: str,
        force_rescrape: bool
    ) -> Dict[str, int]:
        """Scrape single player (called by thread pool)."""
        player = self.db.get_player_by_id(player_id)

        if not force_rescrape and self._recently_scraped(player, days=30):
            return {'stats_stored': 0, 'skipped': True}

        # Acquire semaphore (blocks if max_workers reached)
        with self.semaphore:
            # Track request time
            request_time = time.time()
            self.request_times.append(request_time)

            # Fetch page
            player_url = f"https://fbref.com/en/players/{fbref_id}/"

            try:
                headers = {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
                }
                response = requests.get(player_url, headers=headers, timeout=30)
                response.raise_for_status()

                soup = BeautifulSoup(response.content, 'html.parser')

                # Parse and store
                career_stats = self._parse_player_career_table(soup, player_id)

                stats_stored = 0
                for stat_entry in career_stats:
                    try:
                        self.db.upsert_player_career_stats(**stat_entry)
                        stats_stored += 1
                    except Exception as e:
                        logger.debug(f"Failed to store stat: {str(e)}")

                self.db.update_player_last_scraped(player_id)

                return {
                    'stats_stored': stats_stored,
                    'skipped': False,
                    'player_name': player.name
                }

            except Exception as e:
                logger.error(f"Error scraping player {player_id}: {str(e)}")
                raise

    def _enforce_cooldown(self):
        """
        Enforce cooldown after burst.

        If we made N requests in X seconds, wait until
        N * wait_time seconds have passed.
        """
        if not self.request_times:
            return

        first_request = min(self.request_times)
        last_request = max(self.request_times)
        num_requests = len(self.request_times)

        elapsed = last_request - first_request
        required_time = num_requests * self.wait_time

        if elapsed < required_time:
            cooldown = required_time - elapsed
            logger.info(
                f"Cooldown: Made {num_requests} requests in {elapsed:.1f}s. "
                f"Waiting {cooldown:.1f}s to maintain average rate."
            )
            time.sleep(cooldown)

        # Clear request times for next batch
        self.request_times.clear()

    def _parse_player_career_table(self, soup: BeautifulSoup, player_id: int) -> List[Dict]:
        """Parse career stats (same as other scrapers)."""
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
                if row.get('class') and any(c in ['thead', 'spacer'] for c in row.get('class')):
                    continue

                try:
                    stat_entry = self._parse_career_row(row, player_id)
                    if stat_entry:
                        career_stats.append(stat_entry)
                except Exception as e:
                    logger.debug(f"Parse error: {str(e)}")

        return career_stats

    def _parse_career_row(self, row, player_id: int) -> Optional[Dict]:
        """Parse single row (same as other scrapers)."""
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
            team = self.db.get_or_create_team(name=squad, team_type=team_type, country=country)
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
        except Exception:
            return None

    def _extract_stat(self, row, data_stat_name: str) -> int:
        """Extract stat from row."""
        cell = row.find('td', {'data-stat': data_stat_name})
        if cell:
            value = cell.get_text(strip=True)
            return self._safe_int(value)
        return 0

    def _get_or_create_competition(self, comp_name: str, country: Optional[str]):
        """Get or create competition."""
        comp_map = {
            'Premier League': ('domestic_league', 'England'),
            'La Liga': ('domestic_league', 'Spain'),
            'Bundesliga': ('domestic_league', 'Germany'),
            'Serie A': ('domestic_league', 'Italy'),
            'Ligue 1': ('domestic_league', 'France'),
            'Champions Lg': ('continental', None),
            'Europa Lg': ('continental', None),
        }

        for key, (comp_type, comp_country) in comp_map.items():
            if key in comp_name:
                return self.db.get_or_create_competition(
                    name=key, competition_type=comp_type,
                    country=comp_country, display_name=comp_name
                )

        return self.db.get_or_create_competition(
            name=comp_name, competition_type='domestic_league',
            country=country, display_name=comp_name
        )

    def _recently_scraped(self, player, days: int = 30) -> bool:
        """Check if recently scraped."""
        if not player.last_scraped_at:
            return False
        from datetime import datetime, timedelta
        threshold = datetime.utcnow() - timedelta(days=days)
        return player.last_scraped_at > threshold

    def _safe_int(self, value, default: int = 0) -> int:
        """Safe int conversion."""
        try:
            import pandas as pd
            if pd.isna(value):
                return default
            return int(float(value))
        except (ValueError, TypeError):
            return default
