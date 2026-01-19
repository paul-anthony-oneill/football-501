"""
Tests for Football 501 Scraping Service
"""

import pytest
from unittest.mock import Mock, patch
import pandas as pd

from scrapers import FBrefScraper, DataTransformer
from config import settings


class TestFBrefScraper:
    """Test FBref scraper wrapper."""

    def test_initialization(self):
        """Test scraper initializes correctly."""
        scraper = FBrefScraper(wait_time=5)
        assert scraper.wait_time == 5

    def test_flatten_columns(self):
        """Test column flattening."""
        scraper = FBrefScraper()

        # Create multi-level columns
        df = pd.DataFrame(
            [[1, 2, 3]],
            columns=pd.MultiIndex.from_tuples([
                ('Playing Time', 'MP'),
                ('Performance', 'Gls'),
                ('Unnamed', 'Squad')
            ])
        )

        flattened = scraper._flatten_columns(df)

        assert 'Playing Time_MP' in flattened.columns
        assert 'Performance_Gls' in flattened.columns

    def test_get_column_names(self):
        """Test column name identification."""
        scraper = FBrefScraper()

        df = pd.DataFrame({
            'Unnamed: 1_level_0_Player': ['Test Player'],
            'Unnamed: 4_level_0_Squad': ['Test Team'],
            'Playing Time_MP': [30],
            'Performance_Gls': [10]
        })

        columns = scraper.get_column_names(df)

        assert 'player_name' in columns
        assert 'squad' in columns
        assert 'appearances' in columns
        assert 'goals' in columns

    def test_validate_data_empty(self):
        """Test validation rejects empty DataFrame."""
        scraper = FBrefScraper()
        df = pd.DataFrame()

        is_valid, msg = scraper.validate_data(df)

        assert not is_valid
        assert "empty" in msg.lower()

    def test_validate_data_too_few_rows(self):
        """Test validation rejects DataFrames with too few rows."""
        scraper = FBrefScraper()
        df = pd.DataFrame({
            'Player': ['A', 'B'],
            'Squad': ['X', 'Y'],
            'MP': [1, 2]
        })

        is_valid, msg = scraper.validate_data(df)

        assert not is_valid
        assert "Too few rows" in msg


class TestDataTransformer:
    """Test data transformer."""

    def test_initialization(self):
        """Test transformer initializes correctly."""
        transformer = DataTransformer()
        assert transformer.invalid_scores == settings.invalid_darts_scores

    def test_is_valid_darts_score(self):
        """Test darts score validation."""
        transformer = DataTransformer()

        # Valid scores
        assert transformer._is_valid_darts_score(31)
        assert transformer._is_valid_darts_score(180)
        assert transformer._is_valid_darts_score(1)

        # Invalid scores
        assert not transformer._is_valid_darts_score(163)
        assert not transformer._is_valid_darts_score(169)
        assert not transformer._is_valid_darts_score(179)

        # Out of range
        assert not transformer._is_valid_darts_score(0)
        assert not transformer._is_valid_darts_score(181)

    def test_is_bust(self):
        """Test bust detection."""
        transformer = DataTransformer()

        assert not transformer._is_bust(180)
        assert transformer._is_bust(181)
        assert transformer._is_bust(200)

    def test_transform_to_answers(self):
        """Test transforming DataFrame to answers."""
        transformer = DataTransformer()

        df = pd.DataFrame({
            'Player': ['Erling Haaland', 'Phil Foden'],
            'Squad': ['Manchester City', 'Manchester City'],
            'MP': ['31', '35'],
            'Gls': ['27', '19']
        })

        column_mapping = {
            'player_name': 'Player',
            'squad': 'Squad',
            'appearances': 'MP',
            'goals': 'Gls'
        }

        answers = transformer.transform_to_answers(
            df, question_id=1234, stat_column='appearances', column_mapping=column_mapping
        )

        assert len(answers) == 2
        assert answers[0]['player_name'] == 'Erling Haaland'
        assert answers[0]['statistic_value'] == 31
        assert answers[0]['is_valid_darts_score'] is True
        assert answers[0]['is_bust'] is False

    def test_transform_combined_stats(self):
        """Test combined statistics transformation."""
        transformer = DataTransformer()

        df = pd.DataFrame({
            'Player': ['Erling Haaland'],
            'MP': ['31'],
            'Gls': ['27']
        })

        column_mapping = {
            'player_name': 'Player',
            'appearances': 'MP',
            'goals': 'Gls'
        }

        answers = transformer.transform_combined_stats(
            df,
            question_id=1235,
            stat_columns=['appearances', 'goals'],
            column_mapping=column_mapping
        )

        assert len(answers) == 1
        assert answers[0]['statistic_value'] == 58  # 31 + 27
        assert answers[0]['is_valid_darts_score'] is True

    def test_filter_by_nationality(self):
        """Test nationality filtering."""
        transformer = DataTransformer()

        df = pd.DataFrame({
            'Player': ['Player A', 'Player B', 'Player C'],
            'Nation': ['BRA Brazil', 'ENG England', 'BRA Brazil']
        })

        column_mapping = {'nation': 'Nation'}

        filtered = transformer.filter_by_nationality(df, 'BRA', column_mapping)

        assert len(filtered) == 2
        assert all('BRA' in nat for nat in filtered['Nation'])

    def test_validate_answers(self):
        """Test answer validation."""
        transformer = DataTransformer()

        answers = [
            {
                'player_name': 'Player A',
                'statistic_value': 31,
                'is_valid_darts_score': True,
                'is_bust': False
            },
            {
                'player_name': 'Player B',
                'statistic_value': 163,
                'is_valid_darts_score': False,
                'is_bust': False
            },
            {
                'player_name': 'Player C',
                'statistic_value': 181,
                'is_valid_darts_score': False,
                'is_bust': True
            }
        ]

        stats = transformer.validate_answers(answers)

        assert stats['total'] == 3
        assert stats['valid_scores'] == 1
        assert stats['invalid_scores'] == 1
        assert stats['busts'] == 1
        assert stats['null_names'] == 0


# Integration tests would go here (require actual database/FBref)
@pytest.mark.integration
class TestIntegration:
    """Integration tests (require external services)."""

    @pytest.mark.skip(reason="Requires FBref access")
    def test_scrape_real_data(self):
        """Test scraping real data from FBref."""
        scraper = FBrefScraper()
        df = scraper.scrape_player_stats("2023-2024", "England Premier League")
        assert len(df) > 500  # Should have 600+ players

    @pytest.mark.skip(reason="Requires database")
    def test_database_operations(self):
        """Test database CRUD operations."""
        # Would test actual database operations
        pass
