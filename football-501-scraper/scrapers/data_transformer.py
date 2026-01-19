"""
Data Transformer for Football 501

Transforms ScraperFC data to Football 501 database schema.
"""

import logging
from typing import List, Dict, Optional
import pandas as pd

from config import settings

logger = logging.getLogger(__name__)


class DataTransformer:
    """
    Transforms scraped player data to Football 501 answers schema.
    """

    def __init__(self):
        """Initialize data transformer."""
        self.invalid_scores = settings.invalid_darts_scores

    def _is_valid_darts_score(self, score: int) -> bool:
        """
        Check if score is valid in darts 501 rules.

        Args:
            score: Statistic value

        Returns:
            True if valid darts score
        """
        return score not in self.invalid_scores and 1 <= score <= 180

    def _is_bust(self, score: int) -> bool:
        """
        Check if score is a bust (> 180).

        Args:
            score: Statistic value

        Returns:
            True if bust
        """
        return score > 180

    def transform_to_answers(
        self,
        player_df: pd.DataFrame,
        question_id: int,
        stat_column: str,
        column_mapping: Dict[str, str]
    ) -> List[Dict]:
        """
        Transform player DataFrame to Football 501 answers format.

        Args:
            player_df: DataFrame with player statistics
            question_id: ID of the question these answers are for
            stat_column: Which column contains the statistic value
            column_mapping: Dict mapping field names to actual column names

        Returns:
            List of dicts ready for database insertion

        Example:
            >>> transformer = DataTransformer()
            >>> answers = transformer.transform_to_answers(
            ...     player_df=man_city_df,
            ...     question_id=1234,
            ...     stat_column='appearances',
            ...     column_mapping={'player_name': 'Unnamed: 1_level_0_Player', ...}
            ... )
            >>> print(answers[0])
            {
                'question_id': 1234,
                'player_name': 'Erling Haaland',
                'statistic_value': 31,
                'is_valid_darts_score': True,
                'is_bust': False
            }
        """
        answers = []

        player_col = column_mapping.get('player_name')
        if not player_col:
            raise ValueError("player_name not in column_mapping")

        actual_stat_col = column_mapping.get(stat_column)
        if not actual_stat_col:
            raise ValueError(f"{stat_column} not in column_mapping")

        # Convert stat column to numeric
        player_df[actual_stat_col] = pd.to_numeric(
            player_df[actual_stat_col],
            errors='coerce'
        ).fillna(0).astype(int)

        for _, row in player_df.iterrows():
            player_name = row[player_col]
            stat_value = int(row[actual_stat_col])

            # Skip players with 0 appearances (likely unused substitutes)
            if stat_value == 0:
                continue

            answer = {
                'question_id': question_id,
                'player_name': player_name,
                'player_api_id': None,  # FBref doesn't provide IDs
                'statistic_value': stat_value,
                'is_valid_darts_score': self._is_valid_darts_score(stat_value),
                'is_bust': self._is_bust(stat_value)
            }

            answers.append(answer)

        logger.info(
            f"Transformed {len(answers)} answers for question {question_id}"
        )

        return answers

    def transform_combined_stats(
        self,
        player_df: pd.DataFrame,
        question_id: int,
        stat_columns: List[str],
        column_mapping: Dict[str, str]
    ) -> List[Dict]:
        """
        Transform combined statistics (e.g., appearances + goals).

        Args:
            player_df: DataFrame with player statistics
            question_id: Question ID
            stat_columns: List of columns to sum (e.g., ['appearances', 'goals'])
            column_mapping: Column name mapping

        Returns:
            List of answer dicts

        Example:
            >>> answers = transformer.transform_combined_stats(
            ...     player_df=liverpool_df,
            ...     question_id=1235,
            ...     stat_columns=['appearances', 'goals'],
            ...     column_mapping={...}
            ... )
        """
        answers = []

        player_col = column_mapping.get('player_name')
        if not player_col:
            raise ValueError("player_name not in column_mapping")

        # Get actual column names
        actual_cols = []
        for stat_col in stat_columns:
            actual_col = column_mapping.get(stat_col)
            if not actual_col:
                raise ValueError(f"{stat_col} not in column_mapping")
            actual_cols.append(actual_col)

        # Convert all stat columns to numeric
        for actual_col in actual_cols:
            player_df[actual_col] = pd.to_numeric(
                player_df[actual_col],
                errors='coerce'
            ).fillna(0).astype(int)

        # Calculate combined stat
        player_df['combined_stat'] = player_df[actual_cols].sum(axis=1)

        for _, row in player_df.iterrows():
            player_name = row[player_col]
            combined_value = int(row['combined_stat'])

            # Skip players with 0 combined stat
            if combined_value == 0:
                continue

            answer = {
                'question_id': question_id,
                'player_name': player_name,
                'player_api_id': None,
                'statistic_value': combined_value,
                'is_valid_darts_score': self._is_valid_darts_score(combined_value),
                'is_bust': self._is_bust(combined_value)
            }

            answers.append(answer)

        logger.info(
            f"Transformed {len(answers)} combined answers for question {question_id}"
        )

        return answers

    def transform_goalkeeper_stats(
        self,
        keeper_df: pd.DataFrame,
        question_id: int,
        column_mapping: Dict[str, str]
    ) -> List[Dict]:
        """
        Transform goalkeeper statistics (appearances + clean sheets).

        Args:
            keeper_df: DataFrame with goalkeeper statistics
            question_id: Question ID
            column_mapping: Column name mapping

        Returns:
            List of answer dicts
        """
        return self.transform_combined_stats(
            keeper_df,
            question_id,
            ['appearances', 'clean_sheets'],
            column_mapping
        )

    def filter_by_nationality(
        self,
        player_df: pd.DataFrame,
        nationality_code: str,
        column_mapping: Dict[str, str]
    ) -> pd.DataFrame:
        """
        Filter players by nationality.

        Args:
            player_df: DataFrame with player statistics
            nationality_code: 3-letter country code (e.g., "BRA", "ENG")
            column_mapping: Column name mapping

        Returns:
            Filtered DataFrame

        Example:
            >>> brazilian_df = transformer.filter_by_nationality(
            ...     player_df,
            ...     "BRA",
            ...     column_mapping
            ... )
        """
        nation_col = column_mapping.get('nation')
        if not nation_col:
            raise ValueError("nation not in column_mapping")

        filtered_df = player_df[
            player_df[nation_col].str.contains(
                nationality_code,
                case=False,
                na=False
            )
        ].copy()

        logger.info(
            f"Filtered {len(filtered_df)} players by nationality {nationality_code}"
        )

        return filtered_df

    def validate_answers(self, answers: List[Dict]) -> Dict[str, any]:
        """
        Validate transformed answers for quality.

        Args:
            answers: List of answer dicts

        Returns:
            Dict with validation stats

        Example:
            >>> stats = transformer.validate_answers(answers)
            >>> print(stats)
            {
                'total': 603,
                'valid_scores': 590,
                'invalid_scores': 5,
                'busts': 8,
                'null_names': 0
            }
        """
        stats = {
            'total': len(answers),
            'valid_scores': 0,
            'invalid_scores': 0,
            'busts': 0,
            'null_names': 0
        }

        for answer in answers:
            # Check for null names
            if not answer.get('player_name'):
                stats['null_names'] += 1
                continue

            # Count score validity
            if answer.get('is_bust'):
                stats['busts'] += 1
            elif answer.get('is_valid_darts_score'):
                stats['valid_scores'] += 1
            else:
                stats['invalid_scores'] += 1

        return stats
