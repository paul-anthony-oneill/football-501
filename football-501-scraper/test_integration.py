#!/usr/bin/env python3
"""
Football 501 - Integration Test

Tests the complete workflow:
1. Database connection
2. Question retrieval
3. FBref scraping
4. Data transformation
5. Database insertion
6. Result verification

Usage:
    python test_integration.py
"""

import sys
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def safe_print(text):
    """Print text with Windows console encoding fix."""
    try:
        print(text)
    except UnicodeEncodeError:
        # Replace problematic characters with ASCII equivalents
        print(text.encode('ascii', errors='replace').decode('ascii'))

def print_header(title):
    """Print formatted section header."""
    safe_print("\n" + "=" * 70)
    safe_print(f"  {title}")
    safe_print("=" * 70)

def print_success(message):
    """Print success message."""
    safe_print(f"[OK] {message}")

def print_error(message):
    """Print error message."""
    safe_print(f"[ERROR] {message}")

def print_info(message):
    """Print info message."""
    safe_print(f"[INFO] {message}")

def test_imports():
    """Test that all required modules can be imported."""
    print_header("TEST 1: Module Imports")

    try:
        from database import DatabaseManager, Question, Answer, ScrapeJob
        print_success("Database modules imported")

        from scrapers import FBrefScraper, DataTransformer
        print_success("Scraper modules imported")

        from jobs import QuestionPopulator
        print_success("Job modules imported")

        from config import settings
        print_success("Configuration imported")

        return True

    except ImportError as e:
        print_error(f"Import failed: {e}")
        print_info("Run: pip install -r requirements.txt")
        return False

def test_database_connection():
    """Test database connection."""
    print_header("TEST 2: Database Connection")

    try:
        from database import DatabaseManager
        from config import settings
        from sqlalchemy import text

        print_info(f"Connecting to: {settings.database_url.split('@')[1]}")

        db = DatabaseManager()

        # Try a simple query
        with db.get_session() as session:
            result = session.execute(text("SELECT 1 as test"))
            row = result.fetchone()

        print_success("Database connection successful")
        return db

    except Exception as e:
        print_error(f"Database connection failed: {e}")
        print_info("Ensure PostgreSQL is running: docker-compose up -d")
        return None

def test_tables_exist(db):
    """Test that required tables exist."""
    print_header("TEST 3: Database Tables")

    try:
        from sqlalchemy import text

        with db.get_session() as session:
            # Check questions table
            result = session.execute(
                text("SELECT COUNT(*) FROM questions")
            )
            question_count = result.scalar()
            print_success(f"questions table exists ({question_count} rows)")

            # Check answers table
            result = session.execute(
                text("SELECT COUNT(*) FROM answers")
            )
            answer_count = result.scalar()
            print_success(f"answers table exists ({answer_count} rows)")

            # Check scrape_jobs table
            result = session.execute(
                text("SELECT COUNT(*) FROM scrape_jobs")
            )
            job_count = result.scalar()
            print_success(f"scrape_jobs table exists ({job_count} rows)")

            return question_count > 0

    except Exception as e:
        print_error(f"Table check failed: {e}")
        print_info("Run: psql -U football501 -d football501 -f init_db.sql")
        return False

def test_get_test_question(db):
    """Get the test question for Manchester City."""
    print_header("TEST 4: Test Question Retrieval")

    try:
        questions = db.get_questions(
            status='active',
            season='2023-2024',
            league='England Premier League'
        )

        # Filter for Manchester City manually
        man_city_questions = [q for q in questions if q.team == 'Manchester City']

        if not man_city_questions:
            print_error("No test question found")
            print_info("Run: psql -U football501 -d football501 -f init_db.sql")
            return None

        question = man_city_questions[0]
        print_success(f"Found question (ID: {question.id})")
        print_info(f"Text: {question.text}")
        print_info(f"League: {question.league}")
        print_info(f"Season: {question.season}")
        print_info(f"Team: {question.team}")

        return question

    except Exception as e:
        print_error(f"Failed to get question: {e}")
        return None

def test_scraping(question):
    """Test scraping data from FBref."""
    print_header("TEST 5: FBref Scraping")

    try:
        from scrapers import FBrefScraper

        print_info("Initializing scraper...")
        scraper = FBrefScraper()

        print_info(f"Scraping {question.league} {question.season}...")
        print_info("This will take ~7 seconds (FBref rate limit)...")

        player_df = scraper.scrape_team_stats(
            question.season,
            question.league,
            question.team,
            "standard"
        )

        print_success(f"Scraped {len(player_df)} players")

        # Show sample players
        print_info("Sample players:")
        column_mapping = scraper.get_column_names(player_df)
        player_col = column_mapping.get('player_name')
        mp_col = column_mapping.get('appearances')

        if player_col and mp_col:
            import pandas as pd
            # Convert to numeric
            player_df[mp_col] = pd.to_numeric(
                player_df[mp_col],
                errors='coerce'
            ).fillna(0).astype(int)

            # Show top 5
            top5 = player_df.nlargest(5, mp_col)
            for _, row in top5.iterrows():
                name = row[player_col]
                apps = int(row[mp_col])
                print_info(f"  {name}: {apps} appearances")

        return player_df, column_mapping

    except Exception as e:
        print_error(f"Scraping failed: {e}")
        logger.exception("Scraping error details:")
        return None, None

def test_transformation(player_df, column_mapping, question):
    """Test data transformation."""
    print_header("TEST 6: Data Transformation")

    try:
        from scrapers import DataTransformer

        transformer = DataTransformer()

        print_info("Transforming data to Football 501 schema...")

        answers = transformer.transform_to_answers(
            player_df,
            question.id,
            'appearances',
            column_mapping
        )

        print_success(f"Transformed {len(answers)} answers")

        # Validate
        stats = transformer.validate_answers(answers)
        print_info(f"Valid scores: {stats['valid_scores']}")
        print_info(f"Invalid scores: {stats['invalid_scores']}")
        print_info(f"Bust scores: {stats['busts']}")

        # Show sample
        if answers:
            print_info("Sample answer:")
            sample = answers[0]
            print_info(f"  Player: {sample['player_name']}")
            print_info(f"  Value: {sample['statistic_value']}")
            print_info(f"  Valid: {sample['is_valid_darts_score']}")
            print_info(f"  Bust: {sample['is_bust']}")

        return answers

    except Exception as e:
        print_error(f"Transformation failed: {e}")
        logger.exception("Transformation error details:")
        return None

def test_database_insertion(db, answers, question):
    """Test inserting answers into database."""
    print_header("TEST 7: Database Insertion")

    try:
        # Check if question already has answers
        existing_count = db.get_answer_count_by_question(question.id)

        if existing_count > 0:
            print_info(f"Question already has {existing_count} answers")
            print_info("Deleting old answers...")
            db.delete_answers_by_question(question.id)

        print_info(f"Inserting {len(answers)} answers...")

        inserted = db.insert_answers_batch(answers)

        print_success(f"Inserted {inserted} rows")

        # Verify
        new_count = db.get_answer_count_by_question(question.id)
        print_success(f"Verified: {new_count} answers in database")

        return True

    except Exception as e:
        print_error(f"Insertion failed: {e}")
        logger.exception("Insertion error details:")
        return False

def test_query_results(db, question):
    """Test querying the inserted data."""
    print_header("TEST 8: Query Verification")

    try:
        from database import Answer

        with db.get_session() as session:
            # Get all answers for question
            answers = session.query(Answer).filter(
                Answer.question_id == question.id
            ).order_by(Answer.statistic_value.desc()).limit(10).all()

            print_success(f"Retrieved {len(answers)} answers")
            print_info("Top 10 players by appearances:")

            for i, answer in enumerate(answers, 1):
                validity = "[VALID]" if answer.is_valid_darts_score else "[INVALID]"
                print_info(
                    f"  {i}. {answer.player_name}: "
                    f"{answer.statistic_value} {validity}"
                )

            # Test fuzzy matching (like Spring Boot will use)
            print_info("\nTesting fuzzy match for 'Erling Haaland'...")

            haaland = session.query(Answer).filter(
                Answer.question_id == question.id,
                Answer.player_name.op('%')('Haaland')  # Trigram similarity
            ).first()

            if haaland:
                print_success(f"Found: {haaland.player_name} = {haaland.statistic_value}")
            else:
                print_info("Haaland not found (may have different name spelling)")

        return True

    except Exception as e:
        print_error(f"Query failed: {e}")
        logger.exception("Query error details:")
        return False

def test_job_logging(db):
    """Test scrape job logging."""
    print_header("TEST 9: Job Audit Log")

    try:
        jobs = db.get_scrape_jobs(limit=5)

        print_success(f"Found {len(jobs)} recent jobs")

        for job in jobs:
            status_icon = "[OK]" if job.status == 'success' else "[FAIL]"
            print_info(
                f"{status_icon} Job #{job.id}: {job.job_type} - "
                f"{job.rows_inserted} inserted - "
                f"{job.duration_seconds:.1f}s"
            )

        return True

    except Exception as e:
        print_error(f"Job log check failed: {e}")
        return False

def run_full_integration_test():
    """Run complete integration test."""
    print("\n" + "=" * 70)
    print("  FOOTBALL 501 - INTEGRATION TEST")
    print("=" * 70)
    print(f"\nStarted: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    results = {}

    # Test 1: Imports
    results['imports'] = test_imports()
    if not results['imports']:
        print_error("\nTest failed at: Module Imports")
        return False

    # Test 2: Database connection
    db = test_database_connection()
    results['connection'] = db is not None
    if not results['connection']:
        print_error("\nTest failed at: Database Connection")
        return False

    # Test 3: Tables
    results['tables'] = test_tables_exist(db)
    if not results['tables']:
        print_error("\nTest failed at: Database Tables")
        return False

    # Test 4: Get question
    question = test_get_test_question(db)
    results['question'] = question is not None
    if not results['question']:
        print_error("\nTest failed at: Test Question")
        return False

    # Test 5: Scraping
    player_df, column_mapping = test_scraping(question)
    results['scraping'] = player_df is not None
    if not results['scraping']:
        print_error("\nTest failed at: Scraping")
        return False

    # Test 6: Transformation
    answers = test_transformation(player_df, column_mapping, question)
    results['transformation'] = answers is not None
    if not results['transformation']:
        print_error("\nTest failed at: Transformation")
        return False

    # Test 7: Insertion
    results['insertion'] = test_database_insertion(db, answers, question)
    if not results['insertion']:
        print_error("\nTest failed at: Database Insertion")
        return False

    # Test 8: Query
    results['query'] = test_query_results(db, question)

    # Test 9: Job logging
    results['job_log'] = test_job_logging(db)

    # Final summary
    print_header("SUMMARY")

    passed = sum(1 for v in results.values() if v)
    total = len(results)

    for test_name, passed_test in results.items():
        icon = "[OK]" if passed_test else "[FAIL]"
        print(f"{icon} {test_name.replace('_', ' ').title()}")

    print(f"\nTests Passed: {passed}/{total}")

    if passed == total:
        print_success("\n*** ALL TESTS PASSED! ***")
        print_info("The scraping service is working correctly!")
        print_info("You can now integrate with Spring Boot.")
        return True
    else:
        print_error(f"\n[ERROR] {total - passed} test(s) failed")
        return False

if __name__ == "__main__":
    try:
        success = run_full_integration_test()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n[WARN] Test interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n[ERROR] Unexpected error: {e}")
        logger.exception("Test error:")
        sys.exit(1)
