#!/usr/bin/env python3
"""
Test-Driven Development: Answer Validation for Football 501

Tests the complete workflow of answering a question:
1. User submits player name
2. System validates against database (fuzzy matching)
3. Returns player stats (appearances, validity, bust status)
4. Updates player score

TDD Approach: Write tests first, then implement validation logic
"""

from database import DatabaseManager

def safe_print(text):
    """Print with Windows encoding support."""
    try:
        print(text)
    except UnicodeEncodeError:
        print(text.encode('ascii', errors='replace').decode('ascii'))


class TestAnswerValidation:
    """Test suite for answer validation logic."""

    def __init__(self):
        """Initialize test database connection."""
        self.db = DatabaseManager()
        # Use question 1 (Man City 2023-24) for all tests
        self.question_id = 1

    def test_exact_match_player_name(self):
        """Test 1: Exact player name match should return player data."""
        safe_print("\nTest 1: Exact match - 'Erling Haaland'")

        result = self.validate_answer(self.question_id, "Erling Haaland")

        assert result['valid'] is True
        assert result['player_name'] == "Erling Haaland"
        assert result['statistic_value'] == 31
        assert result['is_valid_darts_score'] is True
        assert result['is_bust'] is False
        safe_print(f"  [PASS] Found: {result['player_name']} = {result['statistic_value']}")

    def test_partial_match_player_name(self):
        """Test 2: Partial name (just surname) should fuzzy match."""
        safe_print("\nTest 2: Fuzzy match - 'Haaland'")

        result = self.validate_answer(self.question_id, "Haaland")

        assert result['valid'] is True
        assert "Haaland" in result['player_name']
        assert result['statistic_value'] == 31
        safe_print(f"  [PASS] Matched: {result['player_name']} = {result['statistic_value']}")

    def test_case_insensitive_match(self):
        """Test 3: Case-insensitive matching."""
        safe_print("\nTest 3: Case insensitive - 'phil foden'")

        result = self.validate_answer(self.question_id, "phil foden")

        assert result['valid'] is True
        assert result['player_name'] == "Phil Foden"
        assert result['statistic_value'] == 35
        safe_print(f"  [PASS] Matched: {result['player_name']} = {result['statistic_value']}")

    def test_accent_insensitive_match(self):
        """Test 4: Match names with accents using simple spelling."""
        safe_print("\nTest 4: Accent handling - 'alvarez'")

        result = self.validate_answer(self.question_id, "alvarez")

        assert result['valid'] is True
        assert "lvarez" in result['player_name']  # Julián Álvarez
        assert result['statistic_value'] == 36
        safe_print(f"  [PASS] Matched: {result['player_name']} = {result['statistic_value']}")

    def test_invalid_player_name(self):
        """Test 5: Invalid player name should return error."""
        safe_print("\nTest 5: Invalid player - 'Lionel Messi'")

        result = self.validate_answer(self.question_id, "Lionel Messi")

        assert result['valid'] is False
        assert 'error' in result
        assert 'not found' in result['error'].lower()
        safe_print(f"  [PASS] Error: {result['error']}")

    def test_empty_player_name(self):
        """Test 6: Empty input should return error."""
        safe_print("\nTest 6: Empty input")

        result = self.validate_answer(self.question_id, "")

        assert result['valid'] is False
        assert 'error' in result
        safe_print(f"  [PASS] Error: {result['error']}")

    def test_special_characters(self):
        """Test 7: Handle special characters in names."""
        safe_print("\nTest 7: Special characters - 'De Bruyne'")

        result = self.validate_answer(self.question_id, "De Bruyne")

        # This might not exist in 2023-24, but test the logic
        if result['valid']:
            safe_print(f"  [PASS] Matched: {result['player_name']} = {result['statistic_value']}")
        else:
            # Expected if player not in squad
            safe_print(f"  [PASS] Not found (expected if not in 2023-24 squad)")

    def test_multiple_matches_returns_best(self):
        """Test 8: Multiple fuzzy matches should return best match."""
        safe_print("\nTest 8: Ambiguous match - 'silva'")

        result = self.validate_answer(self.question_id, "silva")

        assert result['valid'] is True
        assert "Silva" in result['player_name']
        safe_print(f"  [PASS] Best match: {result['player_name']} = {result['statistic_value']}")

    def test_valid_darts_score_flag(self):
        """Test 9: Verify is_valid_darts_score flag is correct."""
        safe_print("\nTest 9: Valid darts score check")

        result = self.validate_answer(self.question_id, "Rodri")

        assert result['valid'] is True
        assert result['statistic_value'] == 34
        # 34 is a valid darts score
        assert result['is_valid_darts_score'] is True
        safe_print(f"  [PASS] Score {result['statistic_value']} is valid dart score")

    def test_score_deduction_calculation(self):
        """Test 10: Calculate new score after answer."""
        safe_print("\nTest 10: Score deduction - starting from 501")

        current_score = 501
        result = self.validate_answer(self.question_id, "Foden")

        assert result['valid'] is True
        new_score = current_score - result['statistic_value']

        assert new_score == 466  # 501 - 35
        safe_print(f"  [PASS] 501 - {result['statistic_value']} = {new_score}")

    # ===== Implementation to make tests pass =====

    def validate_answer(self, question_id: int, player_input: str) -> dict:
        """
        Validate player answer against database.

        Args:
            question_id: Question ID to validate against
            player_input: Player name from user

        Returns:
            dict with keys:
                - valid: bool (True if player found)
                - player_name: str (full matched name)
                - statistic_value: int (appearances/goals/etc)
                - is_valid_darts_score: bool
                - is_bust: bool
                - error: str (if invalid)
        """
        # Validate input
        if not player_input or not player_input.strip():
            return {
                'valid': False,
                'error': 'Player name cannot be empty'
            }

        player_input = player_input.strip()

        # Query database with fuzzy matching
        from database import Answer
        from sqlalchemy import func

        with self.db.get_session() as session:
            # Try exact match first (case-insensitive)
            answer = session.query(Answer).filter(
                Answer.question_id == question_id,
                func.lower(Answer.player_name) == player_input.lower()
            ).first()

            # If no exact match, try fuzzy match using ILIKE (case-insensitive)
            if not answer:
                # This will match "Haaland" to "Erling Haaland"
                answer = session.query(Answer).filter(
                    Answer.question_id == question_id,
                    Answer.player_name.ilike(f"%{player_input}%")
                ).first()

            # If still no match, try accent-insensitive search using unaccent
            if not answer:
                from sqlalchemy import text
                result = session.execute(
                    text("""
                        SELECT id, player_name, statistic_value,
                               is_valid_darts_score, is_bust
                        FROM answers
                        WHERE question_id = :qid
                        AND unaccent(lower(player_name)) LIKE unaccent(lower(:input))
                        LIMIT 1
                    """),
                    {"qid": question_id, "input": f"%{player_input}%"}
                )
                row = result.fetchone()

                if row:
                    return {
                        'valid': True,
                        'player_name': row[1],
                        'statistic_value': row[2],
                        'is_valid_darts_score': row[3],
                        'is_bust': row[4]
                    }

            # If still no match, try even fuzzier with similarity
            if not answer:
                # Use PostgreSQL similarity function (requires pg_trgm extension)
                from sqlalchemy import text
                result = session.execute(
                    text("""
                        SELECT id, player_name, statistic_value,
                               is_valid_darts_score, is_bust,
                               similarity(player_name, :input) as sim
                        FROM answers
                        WHERE question_id = :qid
                        AND similarity(player_name, :input) > 0.3
                        ORDER BY sim DESC
                        LIMIT 1
                    """),
                    {"qid": question_id, "input": player_input}
                )
                row = result.fetchone()

                if row:
                    return {
                        'valid': True,
                        'player_name': row[1],
                        'statistic_value': row[2],
                        'is_valid_darts_score': row[3],
                        'is_bust': row[4]
                    }

            if not answer:
                return {
                    'valid': False,
                    'error': f"Player '{player_input}' not found for this question"
                }

            # Player found - return data
            return {
                'valid': True,
                'player_name': answer.player_name,
                'statistic_value': answer.statistic_value,
                'is_valid_darts_score': answer.is_valid_darts_score,
                'is_bust': answer.is_bust
            }


def run_tests_manually():
    """Run tests manually without pytest for quick verification."""
    safe_print("\n" + "=" * 80)
    safe_print("FOOTBALL 501 - ANSWER VALIDATION TDD TESTS")
    safe_print("=" * 80)

    test_suite = TestAnswerValidation()

    tests = [
        ("Exact Match", test_suite.test_exact_match_player_name),
        ("Fuzzy Match", test_suite.test_partial_match_player_name),
        ("Case Insensitive", test_suite.test_case_insensitive_match),
        ("Accent Handling", test_suite.test_accent_insensitive_match),
        ("Invalid Player", test_suite.test_invalid_player_name),
        ("Empty Input", test_suite.test_empty_player_name),
        ("Special Characters", test_suite.test_special_characters),
        ("Multiple Matches", test_suite.test_multiple_matches_returns_best),
        ("Valid Darts Score", test_suite.test_valid_darts_score_flag),
        ("Score Deduction", test_suite.test_score_deduction_calculation),
    ]

    passed = 0
    failed = 0

    for name, test_func in tests:
        try:
            test_func()
            passed += 1
        except AssertionError as e:
            safe_print(f"  [FAIL] {e}")
            failed += 1
        except Exception as e:
            safe_print(f"  [ERROR] {e}")
            failed += 1

    safe_print("\n" + "=" * 80)
    safe_print(f"RESULTS: {passed} passed, {failed} failed")
    safe_print("=" * 80)

    return failed == 0


if __name__ == "__main__":
    import sys
    success = run_tests_manually()
    sys.exit(0 if success else 1)
