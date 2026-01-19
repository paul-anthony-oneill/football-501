"""
Example Usage - Version 2

Demonstrates the complete workflow with the new normalized schema.
"""

import logging
import sys
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from database.crud_v2 import DatabaseManager
from scrapers.player_career_scraper import PlayerCareerScraper
from jobs.populate_questions_v2 import QuestionPopulator

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def example_1_scrape_league():
    """
    Example 1: Scrape all career data for Premier League players.

    This is the initial data population step.
    """
    logger.info("=" * 60)
    logger.info("Example 1: Scrape Premier League Players")
    logger.info("=" * 60)

    scraper = PlayerCareerScraper()

    # Scrape Premier League 2023-2024
    # This will fetch ALL players' stats for that season
    # and store them in player_career_stats table
    result = scraper.scrape_league_players(
        league="Premier League",
        season="2023-2024",
        min_appearances=5  # Only players with 5+ appearances
    )

    logger.info(f"\nResults:")
    logger.info(f"  Players processed: {result['players_processed']}")
    logger.info(f"  Stats stored: {result['stats_stored']}")
    logger.info(f"  Errors: {result['errors']}")

    logger.info("\n✅ Data is now cached in database!")
    logger.info("   You can create multiple questions without re-scraping.")


def example_2_create_questions():
    """
    Example 2: Create questions using cached data.

    No scraping happens here - we just query the database!
    """
    logger.info("=" * 60)
    logger.info("Example 2: Create Questions")
    logger.info("=" * 60)

    db = DatabaseManager()

    # Get team and competition IDs
    man_city = db.get_team_by_name("Manchester City")
    premier_league = db.get_competition_by_name("Premier League")

    if not man_city or not premier_league:
        logger.error("Team or competition not found. Run example_1_scrape_league() first.")
        return

    # Question 1: Total Premier League goals for Man City
    question1 = db.create_question(
        question_text="Total Premier League goals for Manchester City",
        stat_type="goals",
        team_id=man_city.id,
        competition_id=premier_league.id,
        aggregation="sum",  # Sum all seasons
        min_score=10
    )
    logger.info(f"\n✅ Created Question 1: {question1.question_text}")
    logger.info(f"   ID: {question1.id}")

    # Question 2: Premier League appearances for Man City (2023-2024 only)
    question2 = db.create_question(
        question_text="Appearances for Manchester City in Premier League 2023-2024",
        stat_type="appearances",
        team_id=man_city.id,
        competition_id=premier_league.id,
        season_filter="2023-2024",
        aggregation="single_season",
        min_score=5
    )
    logger.info(f"\n✅ Created Question 2: {question2.question_text}")
    logger.info(f"   ID: {question2.id}")

    # Question 3: Combined appearances + goals for Arsenal
    arsenal = db.get_or_create_team("Arsenal", "club", "England")
    question3 = db.create_question(
        question_text="Appearances + Goals for Arsenal in Premier League",
        stat_type="combined_apps_goals",
        team_id=arsenal.id,
        competition_id=premier_league.id,
        aggregation="sum",
        min_score=20
    )
    logger.info(f"\n✅ Created Question 3: {question3.question_text}")
    logger.info(f"   ID: {question3.id}")

    logger.info("\n✅ Questions created! Now populate answers...")


def example_3_populate_answers():
    """
    Example 3: Populate answers from cached data.

    Fast operation - no external scraping!
    """
    logger.info("=" * 60)
    logger.info("Example 3: Populate Answers from Cache")
    logger.info("=" * 60)

    populator = QuestionPopulator()

    # Populate all active questions
    result = populator.populate_all_active_questions()

    logger.info(f"\nResults:")
    logger.info(f"  Questions processed: {result['successful']}/{result['total_questions']}")
    logger.info(f"  Total answers: {result['total_players']}")
    logger.info(f"  Duration: {result['duration']:.1f}s")

    logger.info("\n✅ Answers populated! Questions are ready for gameplay.")


def example_4_query_answers():
    """
    Example 4: Query valid answers for a question.

    This demonstrates what the game engine will do during gameplay.
    """
    logger.info("=" * 60)
    logger.info("Example 4: Query Valid Answers")
    logger.info("=" * 60)

    db = DatabaseManager()

    # Get first active question
    questions = db.get_active_questions()
    if not questions:
        logger.error("No active questions found. Run previous examples first.")
        return

    question = questions[0]
    logger.info(f"\nQuestion: {question.question_text}")

    # Get all valid answers
    answers = db.get_answers_for_question(question.id)

    logger.info(f"\nValid answers: {len(answers)}")

    # Show top 10 answers
    sorted_answers = sorted(answers, key=lambda a: a.score, reverse=True)[:10]

    logger.info("\nTop 10 answers:")
    for i, answer in enumerate(sorted_answers, 1):
        bust_flag = " (BUST)" if answer.is_bust else ""
        valid_flag = "" if answer.is_valid_darts_score else " (INVALID DARTS)"
        logger.info(
            f"  {i:2}. {answer.player_name:<25} - {answer.score:3} points"
            f"{bust_flag}{valid_flag}"
        )

    # Simulate fuzzy search (what happens during gameplay)
    logger.info("\n--- Simulating Fuzzy Search ---")
    test_input = "haaland"
    logger.info(f"Player input: '{test_input}'")

    matches = [a for a in answers if test_input in a.normalized_name]
    if matches:
        match = matches[0]
        logger.info(f"✅ Match found: {match.player_name} ({match.score} points)")
    else:
        logger.info(f"❌ No match found")


def example_5_weekly_update():
    """
    Example 5: Weekly stat update.

    This demonstrates how to update current season stats weekly.
    """
    logger.info("=" * 60)
    logger.info("Example 5: Weekly Stat Update")
    logger.info("=" * 60)

    scraper = PlayerCareerScraper()

    # Re-scrape current season
    result = scraper.update_current_season(
        league="Premier League",
        season="2023-2024"
    )

    logger.info(f"\nUpdate results:")
    logger.info(f"  Players updated: {result['players_processed']}")
    logger.info(f"  Stats updated: {result['stats_stored']}")

    # Re-populate affected questions
    logger.info("\nRe-populating question answers...")
    populator = QuestionPopulator()
    populate_result = populator.populate_all_active_questions()

    logger.info(f"  Questions updated: {populate_result['successful']}")
    logger.info(f"  Total answers: {populate_result['total_players']}")

    logger.info("\n✅ Weekly update complete!")


def example_6_add_new_competition():
    """
    Example 6: Add a new competition without re-scraping players.

    This demonstrates the power of storing all career data upfront.
    If you've already scraped Premier League players, you might already
    have their Champions League stats stored!
    """
    logger.info("=" * 60)
    logger.info("Example 6: Add Champions League (No Re-scraping!)")
    logger.info("=" * 60)

    db = DatabaseManager()

    # Create Champions League competition
    champions_league = db.get_or_create_competition(
        name="Champions League",
        competition_type="continental",
        country=None,  # Continental competition
        display_name="UEFA Champions League"
    )
    logger.info(f"✅ Created competition: {champions_league.name}")

    # Create question for Champions League
    man_city = db.get_team_by_name("Manchester City")
    if not man_city:
        logger.error("Manchester City not found")
        return

    question = db.create_question(
        question_text="Goals in Champions League for Manchester City",
        stat_type="goals",
        team_id=man_city.id,
        competition_id=champions_league.id,
        aggregation="sum",
        min_score=5
    )
    logger.info(f"✅ Created question: {question.question_text}")

    # Try to populate (will only work if we already have CL data)
    populator = QuestionPopulator()
    result = populator.populate_single_question(question.id)

    if result['status'] == 'no_data':
        logger.warning("\n⚠️  No Champions League data found in cache.")
        logger.info("   You'd need to scrape Champions League data separately.")
    else:
        logger.info(f"\n✅ Found {result['players_added']} players!")
        logger.info("   (We already had this data from initial scrape!)")


def main():
    """
    Run all examples in sequence.
    """
    print("\n" + "=" * 60)
    print("Football 501 - Complete Workflow Examples (Version 2)")
    print("=" * 60)
    print()

    try:
        # Example 1: Initial data scraping
        example_1_scrape_league()
        input("\nPress Enter to continue to Example 2...")

        # Example 2: Create questions
        example_2_create_questions()
        input("\nPress Enter to continue to Example 3...")

        # Example 3: Populate answers
        example_3_populate_answers()
        input("\nPress Enter to continue to Example 4...")

        # Example 4: Query answers
        example_4_query_answers()
        input("\nPress Enter to continue to Example 5...")

        # Example 5: Weekly update
        example_5_weekly_update()
        input("\nPress Enter to continue to Example 6...")

        # Example 6: Add new competition
        example_6_add_new_competition()

        print("\n" + "=" * 60)
        print("All examples complete!")
        print("=" * 60)

    except KeyboardInterrupt:
        print("\n\nExamples interrupted by user.")
    except Exception as e:
        logger.error(f"Error running examples: {str(e)}", exc_info=True)


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description='Run Football 501 workflow examples')
    parser.add_argument('--example', type=int, choices=range(1, 7), help='Run specific example (1-6)')

    args = parser.parse_args()

    if args.example:
        examples = {
            1: example_1_scrape_league,
            2: example_2_create_questions,
            3: example_3_populate_answers,
            4: example_4_query_answers,
            5: example_5_weekly_update,
            6: example_6_add_new_competition
        }
        examples[args.example]()
    else:
        main()
