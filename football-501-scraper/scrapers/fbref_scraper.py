"""
FBref Scraper Wrapper for Football 501

Wraps ScraperFC library to provide Football 501-specific functionality.
"""

import logging
from typing import Dict, Optional, Tuple
import pandas as pd
from ScraperFC.fbref import FBref

from config import settings, get_league_name

logger = logging.getLogger(__name__)


class FBrefScraper:
    """
    Wrapper around ScraperFC's FBref scraper.

    Provides simplified interface for Football 501's use cases.
    """

    def __init__(self, wait_time: Optional[int] = None):
        """
        Initialize FBref scraper.

        Args:
            wait_time: Seconds to wait between requests (default from config)
        """
        self.wait_time = wait_time or settings.fbref_wait_time
        self.scraper = FBref(wait_time=self.wait_time)
        logger.info(f"FBref scraper initialized (wait_time={self.wait_time}s)")

    def _flatten_columns(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Flatten multi-level column headers from FBref data.

        Args:
            df: DataFrame with potentially multi-level columns

        Returns:
            DataFrame with flattened column names
        """
        if isinstance(df.columns, pd.MultiIndex):
            df.columns = [
                '_'.join(col).strip() if col[0] != '' else col[1].strip()
                for col in df.columns.values
            ]
        return df

    def scrape_player_stats(
        self,
        season: str,
        league: str,
        stat_category: str = "standard"
    ) -> pd.DataFrame:
        """
        Scrape player statistics for a league/season.

        Args:
            season: Season string (e.g., "2023-2024")
            league: League name (can use abbreviation like "EPL")
            stat_category: Stat type ("standard", "goalkeeping", etc.)

        Returns:
            DataFrame with player statistics

        Raises:
            ValueError: If league/season invalid
            Exception: If scraping fails
        """
        # Convert league name if needed
        full_league_name = get_league_name(league)

        logger.info(
            f"Scraping {stat_category} stats: {full_league_name} {season}"
        )

        try:
            # ScraperFC returns dict with 'player', 'squad', 'opponent' keys
            result = self.scraper.scrape_stats(season, full_league_name, stat_category)

            if not isinstance(result, dict):
                raise ValueError(f"Unexpected result type: {type(result)}")

            if 'player' not in result:
                raise ValueError(f"No player data in result. Keys: {result.keys()}")

            player_stats = result['player']

            if not isinstance(player_stats, pd.DataFrame):
                raise ValueError(
                    f"Player stats is not DataFrame: {type(player_stats)}"
                )

            # Flatten column headers
            player_stats = self._flatten_columns(player_stats)

            logger.info(
                f"Successfully scraped {len(player_stats)} players "
                f"({len(player_stats.columns)} columns)"
            )

            return player_stats

        except Exception as e:
            logger.error(
                f"Failed to scrape {full_league_name} {season}: {str(e)}"
            )
            raise

    def scrape_team_stats(
        self,
        season: str,
        league: str,
        team_name: str,
        stat_category: str = "standard"
    ) -> pd.DataFrame:
        """
        Scrape player statistics for a specific team.

        Args:
            season: Season string (e.g., "2023-2024")
            league: League name
            team_name: Team name (e.g., "Manchester City")
            stat_category: Stat type

        Returns:
            DataFrame with team's player statistics
        """
        # Get all players for league
        all_players = self.scrape_player_stats(season, league, stat_category)

        # Find squad column
        squad_col = [c for c in all_players.columns if 'Squad' in c]
        if not squad_col:
            raise ValueError("No 'Squad' column found in data")

        squad_col = squad_col[0]

        # Filter by team
        team_players = all_players[all_players[squad_col] == team_name].copy()

        logger.info(f"Filtered {len(team_players)} players for {team_name}")

        return team_players

    def scrape_goalkeeper_stats(
        self,
        season: str,
        league: str
    ) -> pd.DataFrame:
        """
        Scrape goalkeeper statistics.

        Args:
            season: Season string
            league: League name

        Returns:
            DataFrame with goalkeeper statistics
        """
        return self.scrape_player_stats(season, league, "goalkeeping")

    def get_column_names(self, df: pd.DataFrame) -> Dict[str, str]:
        """
        Identify key column names in scraped data.

        Args:
            df: Scraped DataFrame

        Returns:
            Dict mapping Football 501 fields to actual column names
        """
        columns = {}

        # Find player name column
        player_cols = [c for c in df.columns if 'Player' in c]
        if player_cols:
            columns['player_name'] = player_cols[0]

        # Find squad column
        squad_cols = [c for c in df.columns if 'Squad' in c]
        if squad_cols:
            columns['squad'] = squad_cols[0]

        # Find matches played (appearances)
        mp_cols = [c for c in df.columns if 'MP' in c and 'Playing' in c]
        if mp_cols:
            columns['appearances'] = mp_cols[0]

        # Find goals
        gls_cols = [c for c in df.columns if 'Gls' in c and 'Performance' in c]
        if gls_cols:
            columns['goals'] = gls_cols[0]

        # Find assists
        ast_cols = [c for c in df.columns if 'Ast' in c and 'Performance' in c]
        if ast_cols:
            columns['assists'] = ast_cols[0]

        # Find nation
        nation_cols = [c for c in df.columns if 'Nation' in c]
        if nation_cols:
            columns['nation'] = nation_cols[0]

        # Find clean sheets (for goalkeepers)
        cs_cols = [c for c in df.columns if 'CS' in c and 'Performance' in c]
        if cs_cols:
            columns['clean_sheets'] = cs_cols[0]

        return columns

    def validate_data(self, df: pd.DataFrame) -> Tuple[bool, str]:
        """
        Validate scraped data quality.

        Args:
            df: Scraped DataFrame

        Returns:
            Tuple of (is_valid, error_message)
        """
        if df.empty:
            return False, "DataFrame is empty"

        if len(df) < 10:
            return False, f"Too few rows: {len(df)} (expected > 10)"

        # Check for key columns
        required_cols = ['Player', 'Squad', 'MP']
        missing_cols = []

        for req_col in required_cols:
            if not any(req_col in col for col in df.columns):
                missing_cols.append(req_col)

        if missing_cols:
            return False, f"Missing columns: {missing_cols}"

        # Check for excessive null values
        null_counts = df.isnull().sum()
        if null_counts.max() > len(df) * 0.5:  # > 50% nulls
            return False, "Excessive null values detected"

        return True, "Data valid"
