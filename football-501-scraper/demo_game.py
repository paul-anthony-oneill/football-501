#!/usr/bin/env python3
"""
Football 501 Game Demo - Simulating actual gameplay

Demonstrates the complete game flow:
1. Load question
2. Players take turns answering
3. Validate answers against database
4. Update scores
5. Check for winner
"""

from test_answer_validation import TestAnswerValidation, safe_print
from database import DatabaseManager

class GameSimulator:
    """Simulate a Football 501 game."""

    def __init__(self):
        self.validator = TestAnswerValidation()
        self.db = DatabaseManager()

    def display_question(self, question_id):
        """Display the current question."""
        from database import Question
        with self.db.get_session() as session:
            question = session.query(Question).filter(Question.id == question_id).first()
            if question:
                safe_print(f"\nQuestion: {question.text}")
                safe_print(f"League: {question.league}")
                safe_print(f"Season: {question.season}")
                safe_print(f"Team: {question.team}")
            else:
                safe_print(f"Question ID {question_id} not found!")

    def take_turn(self, player_name, current_score, player_input):
        """Simulate a player's turn."""
        safe_print(f"\n--- {player_name}'s Turn ---")
        safe_print(f"Current score: {current_score}")
        safe_print(f"Player answers: '{player_input}'")

        # Validate answer
        result = self.validator.validate_answer(self.validator.question_id, player_input)

        if not result['valid']:
            safe_print(f"[INVALID] {result['error']}")
            safe_print(f"Score remains: {current_score}")
            return current_score, False

        # Valid answer
        safe_print(f"[VALID] {result['player_name']} - {result['statistic_value']} appearances")

        # Check if it's a valid darts score
        if not result['is_valid_darts_score']:
            safe_print(f"[BUST] {result['statistic_value']} is not a valid darts score!")
            safe_print(f"Score remains: {current_score}")
            return current_score, False

        # Check if it would cause a bust
        new_score = current_score - result['statistic_value']

        if result['is_bust'] or new_score < -10:
            safe_print(f"[BUST] Would go below -10 (new score: {new_score})")
            safe_print(f"Score remains: {current_score}")
            return current_score, False

        # Valid move - update score
        safe_print(f"Score: {current_score} - {result['statistic_value']} = {new_score}")

        # Check for win
        if -10 <= new_score <= 0:
            safe_print(f"[WINNER!] {player_name} checked out with {result['player_name']}!")
            return new_score, True

        return new_score, False

    def play_game(self):
        """Simulate a complete game."""
        safe_print("\n" + "=" * 80)
        safe_print("FOOTBALL 501 - GAME SIMULATION")
        safe_print("=" * 80)

        # Display question
        self.display_question(self.validator.question_id)

        # Starting scores
        player1_score = 501
        player2_score = 501

        # Game turns (pre-scripted for demo)
        turns = [
            ("Player 1", "Haaland"),       # 31 -> 470
            ("Player 2", "Foden"),         # 35 -> 466
            ("Player 1", "Rodri"),         # 34 -> 436
            ("Player 2", "silva"),         # 33 -> 433
            ("Player 1", "ederson"),       # 33 -> 403
            ("Player 2", "walker"),        # 32 -> 401
            ("Player 1", "alvarez"),       # 36 -> 367
            ("Player 2", "de bruyne"),     # 18 -> 383
            ("Player 1", "dias"),          # 30 -> 337
            ("Player 2", "kovacic"),       # 30 -> 353
            ("Player 1", "akanji"),        # 30 -> 307
            ("Player 2", "ake"),           # 26 -> 327
            ("Player 1", "grealish"),      # 20 -> 287
            ("Player 2", "gvardiol"),      # 26 -> 301
            ("Player 1", "doku"),          # 26 -> 261
            ("Player 2", "stones"),        # 14 -> 287
            ("Player 1", "nunes"),         # 20 -> 241
            ("Player 2", "bobb"),          # 5 -> 282
        ]

        turn_count = 0
        winner = None

        for player_name, player_input in turns:
            turn_count += 1
            safe_print(f"\n{'=' * 80}")
            safe_print(f"Turn {turn_count}")

            # Determine current score based on player
            if "Player 1" in player_name:
                player1_score, won = self.take_turn(player_name, player1_score, player_input)
                if won:
                    winner = player_name
                    break
            else:
                player2_score, won = self.take_turn(player_name, player2_score, player_input)
                if won:
                    winner = player_name
                    break

        # Final scores
        safe_print("\n" + "=" * 80)
        safe_print("GAME OVER")
        safe_print("=" * 80)
        safe_print(f"\nPlayer 1 Final Score: {player1_score}")
        safe_print(f"Player 2 Final Score: {player2_score}")

        if winner:
            safe_print(f"\n{winner} WINS!")
        else:
            safe_print("\nGame still in progress...")

        safe_print("\n" + "=" * 80)


def main():
    """Run the game simulation."""
    game = GameSimulator()
    game.play_game()


if __name__ == "__main__":
    main()
