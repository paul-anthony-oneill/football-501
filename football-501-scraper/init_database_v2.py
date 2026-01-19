"""
Database Initialization Script - Version 2

Sets up the new normalized database schema from scratch.
"""

import logging
import sys
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from database.crud_v2 import DatabaseManager
from database.models_v2 import Base
from config import settings

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def init_database(drop_existing: bool = False):
    """
    Initialize database with new schema.

    Args:
        drop_existing: If True, drop all existing tables first

    Warning:
        Setting drop_existing=True will DELETE ALL DATA!
    """
    logger.info("Initializing database...")
    logger.info(f"Database URL: {settings.database_url}")

    db = DatabaseManager()

    if drop_existing:
        logger.warning("⚠️  DROPPING ALL EXISTING TABLES!")
        confirm = input("Are you sure? This will delete all data. Type 'yes' to confirm: ")
        if confirm.lower() != 'yes':
            logger.info("Aborted.")
            return False

        db.drop_all()
        logger.info("✅ All tables dropped")

    # Create new tables
    db.init_db()
    logger.info("✅ Database tables created")

    # Create trigram index for fuzzy search (PostgreSQL only)
    try:
        from sqlalchemy import text
        with db.get_session() as session:
            # Enable pg_trgm extension
            session.execute(text("CREATE EXTENSION IF NOT EXISTS pg_trgm;"))

            # Create trigram index on normalized_name
            session.execute(text(
                "CREATE INDEX IF NOT EXISTS idx_qva_normalized_name_trgm "
                "ON question_valid_answers USING gin(normalized_name gin_trgm_ops);"
            ))
            session.commit()
            logger.info("✅ Fuzzy search index created (pg_trgm)")
    except Exception as e:
        logger.warning(f"Could not create trigram index (requires PostgreSQL): {e}")

    logger.info("✅ Database initialization complete!")
    return True


def populate_initial_data():
    """
    Populate database with initial reference data.
    """
    logger.info("Populating initial reference data...")

    db = DatabaseManager()

    # Create Premier League competition
    premier_league = db.get_or_create_competition(
        name="Premier League",
        competition_type="domestic_league",
        country="England",
        display_name="Premier League"
    )
    logger.info(f"✅ Created competition: {premier_league.name} (id={premier_league.id})")

    # Create a sample team (Manchester City)
    man_city = db.get_or_create_team(
        name="Manchester City",
        team_type="club",
        country="England"
    )
    logger.info(f"✅ Created team: {man_city.name} (id={man_city.id})")

    # Create a sample player (Erling Haaland)
    haaland = db.get_or_create_player(
        name="Erling Haaland",
        normalized_name="erling haaland",
        nationality="Norway"
    )
    logger.info(f"✅ Created player: {haaland.name} (id={haaland.id})")

    # Add sample stats
    stats = db.upsert_player_career_stats(
        player_id=haaland.id,
        team_id=man_city.id,
        competition_id=premier_league.id,
        season="2023-2024",
        appearances=35,
        goals=36,
        assists=8
    )
    logger.info(f"✅ Added sample stats: {haaland.name} - {stats.goals} goals")

    # Create a sample question
    question = db.create_question(
        question_text="Total Premier League goals for Manchester City",
        stat_type="goals",
        team_id=man_city.id,
        competition_id=premier_league.id,
        aggregation="sum",
        min_score=10
    )
    logger.info(f"✅ Created sample question: {question.question_text} (id={question.id})")

    logger.info("✅ Initial data populated!")


def verify_setup():
    """
    Verify database setup by running test queries.
    """
    logger.info("Verifying database setup...")

    db = DatabaseManager()

    # Count entities
    with db.get_session() as session:
        from database.models_v2 import Player, Team, Competition, Question

        player_count = session.query(Player).count()
        team_count = session.query(Team).count()
        comp_count = session.query(Competition).count()
        question_count = session.query(Question).count()

        logger.info(f"Players: {player_count}")
        logger.info(f"Teams: {team_count}")
        logger.info(f"Competitions: {competition_count}")
        logger.info(f"Questions: {question_count}")

        if player_count > 0 and team_count > 0 and comp_count > 0:
            logger.info("✅ Database verification passed!")
            return True
        else:
            logger.warning("⚠️  Database appears empty")
            return False


def main():
    """
    Main CLI for database initialization.
    """
    import argparse

    parser = argparse.ArgumentParser(description='Initialize Football 501 database (v2)')
    parser.add_argument(
        '--drop',
        action='store_true',
        help='Drop existing tables (DANGEROUS - deletes all data!)'
    )
    parser.add_argument(
        '--populate',
        action='store_true',
        help='Populate initial reference data'
    )
    parser.add_argument(
        '--verify',
        action='store_true',
        help='Verify database setup'
    )

    args = parser.parse_args()

    print("=" * 60)
    print("Football 501 - Database Initialization (Version 2)")
    print("=" * 60)
    print()

    # Default: init + populate + verify
    if not any([args.drop, args.populate, args.verify]):
        args.populate = True
        args.verify = True

    # Initialize database
    if not init_database(drop_existing=args.drop):
        return

    print()

    # Populate initial data
    if args.populate:
        populate_initial_data()
        print()

    # Verify setup
    if args.verify:
        verify_setup()
        print()

    print("=" * 60)
    print("Setup complete! Next steps:")
    print("=" * 60)
    print()
    print("1. Scrape player data:")
    print("   python -m scrapers.player_career_scraper --league 'Premier League' --season '2023-2024'")
    print()
    print("2. Create questions via database or API")
    print()
    print("3. Populate question answers:")
    print("   python -m jobs.populate_questions_v2 --all")
    print()


if __name__ == '__main__':
    main()
