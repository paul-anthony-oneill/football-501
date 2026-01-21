"""
Initialize Database V3 (JSONB Schema)

Drops all existing tables and creates fresh V3 schema.
"""

import logging
from database.crud_v3 import DatabaseManager
from database.models_v3 import Base, Competition

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def init_database(drop_existing: bool = True):
    """
    Initialize database with V3 schema.

    Args:
        drop_existing: If True, drop all existing tables first
    """
    db = DatabaseManager()

    if drop_existing:
        logger.warning("⚠️  Dropping all existing tables...")
        response = input("Are you sure? This will DELETE ALL DATA (yes/no): ")
        if response.lower() != 'yes':
            logger.info("Aborted")
            return

        db.drop_all()
        logger.info("✅ Dropped all tables")

    # Create tables
    logger.info("Creating V3 tables...")
    db.init_db()
    logger.info("✅ Created all tables")

    # Seed competitions
    logger.info("Seeding competitions...")
    seed_competitions(db)
    logger.info("✅ Seeded competitions")

    logger.info("\n" + "="*60)
    logger.info("Database V3 initialized successfully!")
    logger.info("="*60)


def seed_competitions(db: DatabaseManager):
    """Seed initial competitions."""
    competitions = [
        {
            'name': 'Premier League',
            'competition_type': 'domestic_league',
            'country': 'England',
            'display_name': 'Premier League'
        },
        {
            'name': 'Champions League',
            'competition_type': 'continental',
            'country': None,
            'display_name': 'UEFA Champions League'
        },
        {
            'name': 'Europa League',
            'competition_type': 'continental',
            'country': None,
            'display_name': 'UEFA Europa League'
        },
        {
            'name': 'FA Cup',
            'competition_type': 'cup',
            'country': 'England',
            'display_name': 'FA Cup'
        },
        {
            'name': 'EFL Cup',
            'competition_type': 'cup',
            'country': 'England',
            'display_name': 'EFL Cup (Carabao Cup)'
        },
        {
            'name': 'La Liga',
            'competition_type': 'domestic_league',
            'country': 'Spain',
            'display_name': 'La Liga'
        },
        {
            'name': 'Bundesliga',
            'competition_type': 'domestic_league',
            'country': 'Germany',
            'display_name': 'Bundesliga'
        },
        {
            'name': 'Serie A',
            'competition_type': 'domestic_league',
            'country': 'Italy',
            'display_name': 'Serie A'
        },
        {
            'name': 'Ligue 1',
            'competition_type': 'domestic_league',
            'country': 'France',
            'display_name': 'Ligue 1'
        }
    ]

    for comp_data in competitions:
        comp = db.get_or_create_competition(**comp_data)
        logger.info(f"  - {comp.display_name}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='Initialize Football 501 Database V3')
    parser.add_argument('--no-drop', action='store_true', help='Do not drop existing tables')

    args = parser.parse_args()

    init_database(drop_existing=not args.no_drop)
