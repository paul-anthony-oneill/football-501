"""
Player Career Scraper - Fetches comprehensive career data for players.

This scraper stores ALL career statistics (all teams, all leagues, all seasons)
for reuse across multiple questions. No need to re-scrape for different questions.
"""

import logging
from typing import List, Dict, Optional
from datetime import datetime, timedelta
import time
import pandas as pd
import requests
from bs4 import BeautifulSoup

from scrapers.fbref_scraper import FBrefScraper
from database.crud_v2 import DatabaseManager
from config import settings

logger = logging.getLogger(__name__)


class PlayerCareerScraper:
    """
    Scrapes complete career history for players and stores for reuse.
    """

    def __init__(self):
        self.scraper = FBrefScraper()
        self.db = DatabaseManager()
        logger.info("Player career scraper initialized")

    def scrape_league_players(
        self,
        league: str,
        season: str,
        min_appearances: int = 5,
        rescrape_recent: bool = False
    ) -> Dict[str, int]:
        """
        Scrape ALL career data for players in a league/season.

        This is the main entry point for initial data population.

        Strategy:
        1. Get player list from league/season (e.g., Premier League 2023-2024)
        2. For each player, store their stats for that season
        3. Mark player as "scraped" for future reference

        Future enhancement: Scrape full career history from player pages.

        Args:
            league: League name (e.g., "Premier League")
            season: Season (e.g., "2023-2024")
            min_appearances: Only process players with >= N appearances
            rescrape_recent: If True, rescrape players scraped in last 7 days

        Returns:
            Dict with scraping statistics
        """
        logger.info(f"Starting career scrape: {league} {season}")

        # Create scrape job
        job = self.db.create_scrape_job(
            job_type='career_scrape',
            season=season,
            league=league
        )

        try:
            # Get competition
            competition = self.db.get_or_create_competition(
                name=league,
                competition_type='domestic_league',
                country=self._get_country_from_league(league),
                display_name=league
            )

            # Step 1: Scrape player stats for this league/season
            logger.info(f"Fetching player data from FBref...")
            player_df = self.scraper.scrape_player_stats(season, league, "standard")

            # Get column mapping
            col_map = self.scraper.get_column_names(player_df)

            logger.info(f"Found {len(player_df)} players in {league} {season}")

            # Filter by appearances
            if 'appearances' in col_map:
                apps_col = col_map['appearances']
                player_df[apps_col] = pd.to_numeric(player_df[apps_col], errors='coerce').fillna(0)
                active_players = player_df[player_df[apps_col] >= min_appearances].copy()
                logger.info(f"Filtered to {len(active_players)} players with >= {min_appearances} appearances")
            else:
                active_players = player_df
                logger.warning("No appearances column found, processing all players")

            # Statistics
            stats = {
                'players_processed': 0,
                'players_skipped': 0,
                'stats_stored': 0,
                'errors': 0
            }

            # Step 2: Process each player
            for idx, row in active_players.iterrows():
                try:
                    player_name = row[col_map['player_name']]

                    # Get or create player
                    player = self.db.get_or_create_player(
                        name=player_name,
                        normalized_name=player_name.lower().strip(),
                        nationality=row.get(col_map.get('nation', 'Nation'), None)
                    )

                    # Check if recently scraped
                    if not rescrape_recent and self._recently_scraped(player):
                        logger.debug(f"Skipping {player_name} (recently scraped)")
                        stats['players_skipped'] += 1
                        continue

                    # Store career data for this season
                    stored = self._store_player_season_data(
                        player=player,
                        row=row,
                        col_map=col_map,
                        competition=competition,
                        season=season
                    )

                    if stored:
                        stats['stats_stored'] += stored
                        stats['players_processed'] += 1

                        # Update player's last_scraped timestamp
                        self.db.update_player_last_scraped(player.id)

                        logger.info(
                            f"[{stats['players_processed']}/{len(active_players)}] "
                            f"Stored {stored} stats for {player_name}"
                        )

                except Exception as e:
                    logger.error(f"Error processing player {idx}: {str(e)}")
                    stats['errors'] += 1

            # Update job status
            self.db.update_scrape_job(
                job.id,
                status='success' if stats['errors'] == 0 else 'partial',
                rows_inserted=stats['stats_stored'],
                players_scraped=stats['players_processed']
            )

            logger.info(
                f"Career scrape complete: {stats['players_processed']} players, "
                f"{stats['stats_stored']} stats stored, {stats['errors']} errors"
            )

            return stats

        except Exception as e:
            logger.error(f"Career scrape failed: {str(e)}")
            self.db.update_scrape_job(
                job.id,
                status='failed',
                error_message=str(e)
            )
            raise

    def _store_player_season_data(
        self,
        player,
        row: pd.Series,
        col_map: Dict[str, str],
        competition,
        season: str
    ) -> int:
        """
        Store player's statistics for a season.

        Args:
            player: Player instance
            row: DataFrame row with player data
            col_map: Column name mapping
            competition: Competition instance
            season: Season string

        Returns:
            Number of stats records created
        """
        # Extract team name
        squad_col = col_map.get('squad', 'Squad')
        team_name = row.get(squad_col, 'Unknown')

        # Get or create team
        team = self.db.get_or_create_team(
            name=team_name,
            team_type='club',
            country=self._get_country_from_league(competition.name)
        )

        # Extract statistics
        appearances = self._safe_int(row.get(col_map.get('appearances', 'MP'), 0))
        goals = self._safe_int(row.get(col_map.get('goals', 'Gls'), 0))
        assists = self._safe_int(row.get(col_map.get('assists', 'Ast'), 0))
        clean_sheets = self._safe_int(row.get(col_map.get('clean_sheets', 'CS'), 0))

        # Store in database
        self.db.upsert_player_career_stats(
            player_id=player.id,
            team_id=team.id,
            competition_id=competition.id,
            season=season,
            appearances=appearances,
            goals=goals,
            assists=assists,
            clean_sheets=clean_sheets
        )

        return 1

    def update_current_season(
        self,
        league: str,
        season: str
    ) -> Dict[str, int]:
        """
        Update stats for current season (weekly job).

        This re-scrapes the current season and updates existing players.

        Args:
            league: League name
            season: Current season

        Returns:
            Dict with update statistics
        """
        logger.info(f"Updating current season: {league} {season}")

        # Create scrape job
        job = self.db.create_scrape_job(
            job_type='weekly',
            season=season,
            league=league
        )

        try:
            # Re-scrape with rescrape_recent=True
            stats = self.scrape_league_players(
                league=league,
                season=season,
                min_appearances=1,
                rescrape_recent=True
            )

            self.db.update_scrape_job(
                job.id,
                status='success',
                rows_updated=stats['stats_stored'],
                players_scraped=stats['players_processed']
            )

            logger.info(f"Weekly update complete: {stats['players_processed']} players updated")

            return stats

        except Exception as e:
            logger.error(f"Weekly update failed: {str(e)}")
            self.db.update_scrape_job(
                job.id,
                status='failed',
                error_message=str(e)
            )
            raise

    def scrape_team_players(
        self,
        team_name: str,
        league: str,
        season: str
    ) -> Dict[str, int]:
        """
        Scrape career data for all players on a specific team.

        Args:
            team_name: Team name
            league: League name
            season: Season

        Returns:
            Dict with statistics
        """
        logger.info(f"Scraping team: {team_name} ({league} {season})")

        # Scrape team stats
        team_df = self.scraper.scrape_team_stats(season, league, team_name, "standard")

        # Get competition
        competition = self.db.get_or_create_competition(
            name=league,
            competition_type='domestic_league',
            country=self._get_country_from_league(league)
        )

        # Get column mapping
        col_map = self.scraper.get_column_names(team_df)

        stats = {
            'players_processed': 0,
            'stats_stored': 0
        }

        for idx, row in team_df.iterrows():
            try:
                player_name = row[col_map['player_name']]

                # Get or create player
                player = self.db.get_or_create_player(
                    name=player_name,
                    normalized_name=player_name.lower().strip(),
                    nationality=row.get(col_map.get('nation', 'Nation'), None)
                )

                # Store data
                stored = self._store_player_season_data(
                    player=player,
                    row=row,
                    col_map=col_map,
                    competition=competition,
                    season=season
                )

                stats['stats_stored'] += stored
                stats['players_processed'] += 1

                # Update timestamp
                self.db.update_player_last_scraped(player.id)

            except Exception as e:
                logger.error(f"Error processing player {idx}: {str(e)}")

        logger.info(
            f"Team scrape complete: {stats['players_processed']} players, "
            f"{stats['stats_stored']} stats stored"
        )

        return stats

    def scrape_full_player_career(
        self,
        player_id: int,
        fbref_id: str,
        force_rescrape: bool = False
    ) -> Dict[str, int]:
        """
        Scrape complete career history from a player's FBref page.

        This fetches ALL career data including:
        - All clubs played for
        - All competitions (domestic leagues, Champions League, etc.)
        - National team appearances
        - Complete season-by-season breakdown

        Args:
            player_id: Database player ID
            fbref_id: FBref player ID (from player URL)
            force_rescrape: If True, rescrape even if recently done

        Returns:
            Dict with scraping statistics
        """
        logger.info(f"Scraping full career for player ID {player_id} (FBref: {fbref_id})")

        # Check if recently scraped
        player = self.db.get_player_by_id(player_id)
        if not force_rescrape and self._recently_scraped(player, days=30):
            logger.info(f"Player {player.name} was recently scraped, skipping")
            return {'stats_stored': 0, 'skipped': True}

        # Build player URL
        player_url = f"https://fbref.com/en/players/{fbref_id}/"

        # Fetch page with rate limiting
        time.sleep(self.scraper.wait_time)

        try:
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
            response = requests.get(player_url, headers=headers, timeout=30)
            response.raise_for_status()

            soup = BeautifulSoup(response.content, 'html.parser')

            # Parse career stats table
            career_stats = self._parse_player_career_table(soup, player_id)

            # Store in database
            stats_stored = 0
            for stat_entry in career_stats:
                try:
                    self.db.upsert_player_career_stats(**stat_entry)
                    stats_stored += 1
                except Exception as e:
                    logger.error(f"Failed to store stat entry: {str(e)}")

            # Update player's last_scraped timestamp
            self.db.update_player_last_scraped(player_id)

            logger.info(f"Stored {stats_stored} career stat entries for player {player.name}")

            return {
                'stats_stored': stats_stored,
                'skipped': False,
                'player_name': player.name
            }

        except requests.RequestException as e:
            logger.error(f"Failed to fetch player page {player_url}: {str(e)}")
            raise
        except Exception as e:
            logger.error(f"Error parsing player career: {str(e)}")
            raise

    def scrape_multiple_player_careers(
        self,
        player_ids: List[int],
        force_rescrape: bool = False
    ) -> Dict[str, int]:
        """
        Batch scrape full careers for multiple players.

        Args:
            player_ids: List of database player IDs
            force_rescrape: If True, rescrape even if recently done

        Returns:
            Dict with overall statistics
        """
        logger.info(f"Starting batch career scrape for {len(player_ids)} players")

        overall_stats = {
            'players_processed': 0,
            'players_skipped': 0,
            'total_stats_stored': 0,
            'errors': 0
        }

        for idx, player_id in enumerate(player_ids, 1):
            try:
                player = self.db.get_player_by_id(player_id)

                if not player:
                    logger.warning(f"Player ID {player_id} not found, skipping")
                    overall_stats['players_skipped'] += 1
                    continue

                if not player.fbref_id:
                    logger.warning(f"Player {player.name} has no FBref ID, skipping")
                    overall_stats['players_skipped'] += 1
                    continue

                logger.info(f"[{idx}/{len(player_ids)}] Processing {player.name}")

                result = self.scrape_full_player_career(
                    player_id=player_id,
                    fbref_id=player.fbref_id,
                    force_rescrape=force_rescrape
                )

                if result.get('skipped'):
                    overall_stats['players_skipped'] += 1
                else:
                    overall_stats['players_processed'] += 1
                    overall_stats['total_stats_stored'] += result.get('stats_stored', 0)

            except Exception as e:
                logger.error(f"Error processing player ID {player_id}: {str(e)}")
                overall_stats['errors'] += 1

        logger.info(
            f"Batch scrape complete: {overall_stats['players_processed']} players, "
            f"{overall_stats['total_stats_stored']} stats stored, "
            f"{overall_stats['errors']} errors"
        )

        return overall_stats

    def scrape_all_stored_players(
        self,
        force_rescrape: bool = False,
        max_players: Optional[int] = None
    ) -> Dict[str, int]:
        """
        Scrape full careers for all players in database with FBref IDs.

        Args:
            force_rescrape: If True, rescrape even if recently done
            max_players: Limit number of players to process (for testing)

        Returns:
            Dict with overall statistics
        """
        logger.info("Fetching all players with FBref IDs from database")

        # Get all players with FBref IDs
        with self.db.get_session() as session:
            from database.models_v2 import Player
            query = session.query(Player.id).filter(Player.fbref_id.isnot(None))

            if max_players:
                query = query.limit(max_players)

            player_ids = [p.id for p in query.all()]

        logger.info(f"Found {len(player_ids)} players with FBref IDs")

        if not player_ids:
            logger.warning("No players with FBref IDs found")
            return {
                'players_processed': 0,
                'players_skipped': 0,
                'total_stats_stored': 0,
                'errors': 0
            }

        return self.scrape_multiple_player_careers(
            player_ids=player_ids,
            force_rescrape=force_rescrape
        )

    def _parse_player_career_table(
        self,
        soup: BeautifulSoup,
        player_id: int
    ) -> List[Dict]:
        """
        Parse career statistics table from player page.

        FBref player pages have a table called "stats_standard_dom_lg" for
        domestic leagues and "stats_standard_intl_club" for international.

        Args:
            soup: BeautifulSoup object of player page
            player_id: Database player ID

        Returns:
            List of stat entry dicts ready for database insertion
        """
        career_stats = []

        # Table IDs to check (in priority order)
        table_ids = [
            'stats_standard_dom_lg',      # Domestic leagues only
            'stats_standard',             # All club stats
            'stats_standard_intl_club'    # International club comps
        ]

        for table_id in table_ids:
            table = soup.find('table', {'id': table_id})

            if not table:
                continue

            logger.debug(f"Found table: {table_id}")

            # Find table body
            tbody = table.find('tbody')
            if not tbody:
                continue

            # Parse each row
            for row in tbody.find_all('tr'):
                # Skip header rows and spacer rows
                if row.get('class') and any(c in ['thead', 'spacer', 'partial_table'] for c in row.get('class')):
                    continue

                try:
                    stat_entry = self._parse_career_row(row, player_id)
                    if stat_entry:
                        career_stats.append(stat_entry)
                except Exception as e:
                    logger.warning(f"Failed to parse row: {str(e)}")
                    continue

        logger.info(f"Parsed {len(career_stats)} career stat entries")
        return career_stats

    def _parse_career_row(
        self,
        row,
        player_id: int
    ) -> Optional[Dict]:
        """
        Parse a single row from career stats table.

        Args:
            row: BeautifulSoup row element
            player_id: Database player ID

        Returns:
            Dict with stat entry data, or None if invalid
        """
        cells = row.find_all(['th', 'td'])

        if len(cells) < 10:  # Need at least: season, squad, comp, MP, Gls, Ast, etc.
            return None

        try:
            # Extract data from cells (column positions may vary)
            season = cells[0].get_text(strip=True)
            squad = cells[1].get_text(strip=True)
            country = cells[2].get_text(strip=True) if len(cells) > 2 else None
            comp = cells[3].get_text(strip=True) if len(cells) > 3 else None

            # Find stats columns (positions vary by table type)
            # Look for data-stat attributes
            mp = self._extract_stat(row, 'matches')
            goals = self._extract_stat(row, 'goals')
            assists = self._extract_stat(row, 'assists')

            # Skip invalid rows
            if not season or not squad or season == 'Season':
                return None

            # Skip summary rows (usually have season like "X Seasons")
            if 'Season' in season or 'Total' in season:
                return None

            # Determine team type (club vs national)
            team_type = 'national' if 'National Team' in squad else 'club'

            # Get or create team
            team = self.db.get_or_create_team(
                name=squad,
                team_type=team_type,
                country=country if country else None
            )

            # Get or create competition
            competition = self._get_or_create_competition_from_name(comp, country)

            # Build stat entry
            return {
                'player_id': player_id,
                'team_id': team.id,
                'competition_id': competition.id,
                'season': season,
                'appearances': mp,
                'goals': goals,
                'assists': assists,
                'clean_sheets': 0  # Not available in standard table
            }

        except Exception as e:
            logger.debug(f"Failed to parse row: {str(e)}")
            return None

    def _extract_stat(self, row, data_stat_name: str) -> int:
        """
        Extract a stat value from a row by data-stat attribute.

        Args:
            row: BeautifulSoup row element
            data_stat_name: Value of data-stat attribute

        Returns:
            Integer stat value (0 if not found)
        """
        cell = row.find('td', {'data-stat': data_stat_name})
        if cell:
            value = cell.get_text(strip=True)
            return self._safe_int(value)
        return 0

    def _get_or_create_competition_from_name(
        self,
        comp_name: str,
        country: Optional[str] = None
    ) -> 'Competition':
        """
        Get or create competition from name.

        Args:
            comp_name: Competition name from FBref
            country: Country (if applicable)

        Returns:
            Competition instance
        """
        # Map common competition names
        comp_type_map = {
            'Premier League': ('domestic_league', 'England'),
            'La Liga': ('domestic_league', 'Spain'),
            'Bundesliga': ('domestic_league', 'Germany'),
            'Serie A': ('domestic_league', 'Italy'),
            'Ligue 1': ('domestic_league', 'France'),
            'Champions Lg': ('continental', None),
            'Europa Lg': ('continental', None),
            'World Cup': ('international', None),
            'UEFA Euro': ('international', None),
            'Copa América': ('international', None),
        }

        # Try to find match
        for key, (comp_type, comp_country) in comp_type_map.items():
            if key in comp_name:
                return self.db.get_or_create_competition(
                    name=key,
                    competition_type=comp_type,
                    country=comp_country,
                    display_name=comp_name
                )

        # Default to domestic league
        return self.db.get_or_create_competition(
            name=comp_name,
            competition_type='domestic_league',
            country=country,
            display_name=comp_name
        )

    # ============================================================================
    # HELPER METHODS
    # ============================================================================

    def _recently_scraped(self, player, days: int = 7) -> bool:
        """
        Check if player was scraped recently.

        Args:
            player: Player instance
            days: Number of days threshold

        Returns:
            True if scraped within threshold
        """
        if not player.last_scraped_at:
            return False

        threshold = datetime.utcnow() - timedelta(days=days)
        return player.last_scraped_at > threshold

    def _safe_int(self, value, default: int = 0) -> int:
        """
        Safely convert value to int.

        Args:
            value: Value to convert
            default: Default if conversion fails

        Returns:
            Integer value
        """
        try:
            if pd.isna(value):
                return default
            return int(float(value))
        except (ValueError, TypeError):
            return default

    def _get_country_from_league(self, league: str) -> str:
        """
        Get country from league name.

        Args:
            league: League name

        Returns:
            Country name
        """
        league_countries = {
            'Premier League': 'England',
            'England Premier League': 'England',
            'La Liga': 'Spain',
            'Bundesliga': 'Germany',
            'Serie A': 'Italy',
            'Ligue 1': 'France',
            'Champions League': None,  # Continental
            'Europa League': None,  # Continental
        }

        return league_countries.get(league, None)
