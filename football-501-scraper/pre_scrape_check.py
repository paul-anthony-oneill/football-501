"""
Pre-Scrape Check - Verify everything is ready before starting the big scrape.

Run this before scrape_premier_league_complete.py to ensure:
- Database is migrated
- Dependencies are installed
- Configuration is correct
"""

import logging
import sys

logging.basicConfig(level=logging.INFO, format='%(message)s')
logger = logging.getLogger(__name__)


def check_dependencies():
    """Check required Python packages."""
    logger.info("Checking dependencies...")

    required = [
        'requests',
        'beautifulsoup4',
        'sqlalchemy',
        'pandas',
        'ScraperFC'
    ]

    missing = []

    for package in required:
        try:
            if package == 'beautifulsoup4':
                __import__('bs4')
            elif package == 'ScraperFC':
                __import__('ScraperFC')
            else:
                __import__(package)
            logger.info(f"  ✅ {package}")
        except ImportError:
            logger.error(f"  ❌ {package} - MISSING")
            missing.append(package)

    if missing:
        logger.error(f"\n❌ Missing packages: {', '.join(missing)}")
        logger.info("\nInstall with:")
        logger.info(f"  pip install {' '.join(missing)}")
        return False

    return True


def check_database():
    """Check database connection and tables."""
    logger.info("\nChecking database...")

    try:
        from database.crud_v2 import DatabaseManager
        db = DatabaseManager()

        with db.get_session() as session:
            from database.models_v2 import Player, PlayerScrapeLog

            player_count = session.query(Player).count()
            logger.info(f"  ✅ Database connected")
            logger.info(f"  ✅ Players table: {player_count} rows")

            # Check if PlayerScrapeLog table exists
            try:
                log_count = session.query(PlayerScrapeLog).count()
                logger.info(f"  ✅ PlayerScrapeLog table: {log_count} rows")
            except Exception:
                logger.error(f"  ❌ PlayerScrapeLog table missing")
                logger.info("\nRun migration:")
                logger.info("  python migrate_database_v3.py")
                return False

        return True

    except Exception as e:
        logger.error(f"  ❌ Database error: {str(e)}")
        return False


def check_fbref_ids():
    """Check how many players have FBref IDs."""
    logger.info("\nChecking FBref IDs...")

    try:
        from database.crud_v2 import DatabaseManager
        db = DatabaseManager()

        with db.get_session() as session:
            from database.models_v2 import Player

            total_players = session.query(Player).count()
            with_fbref = session.query(Player).filter(Player.fbref_id.isnot(None)).count()

            percentage = (with_fbref / total_players * 100) if total_players > 0 else 0

            logger.info(f"  Players with FBref IDs: {with_fbref}/{total_players} ({percentage:.1f}%)")

            if with_fbref == 0:
                logger.error("  ❌ No players have FBref IDs")
                logger.info("\nRun:")
                logger.info("  python update_fbref_ids.py")
                return False
            elif percentage < 50:
                logger.warning(f"  ⚠️  Only {percentage:.1f}% have FBref IDs")
                logger.info("\nRecommend running:")
                logger.info("  python update_fbref_ids.py")
                return False
            else:
                logger.info("  ✅ Sufficient FBref IDs")

        return True

    except Exception as e:
        logger.error(f"  ❌ Error: {str(e)}")
        return False


def check_configuration():
    """Check configuration settings."""
    logger.info("\nChecking configuration...")

    try:
        from config import settings

        logger.info(f"  Database URL: {settings.database_url[:50]}...")
        logger.info(f"  FBref wait time: {settings.fbref_wait_time}s")
        logger.info(f"  ✅ Configuration loaded")

        return True

    except Exception as e:
        logger.error(f"  ❌ Configuration error: {str(e)}")
        return False


def estimate_scrape_time():
    """Estimate how long the scrape will take."""
    logger.info("\nEstimating scrape time...")

    try:
        from database.crud_v2 import DatabaseManager
        db = DatabaseManager()

        with db.get_session() as session:
            from database.models_v2 import Player

            players_to_scrape = session.query(Player).filter(
                Player.fbref_id.isnot(None)
            ).count()

            if players_to_scrape == 0:
                logger.info("  No players to scrape")
                return

            # 7 seconds per player, with 15% speedup for parallel
            sequential_time = players_to_scrape * 7 / 3600  # hours
            parallel_time = sequential_time * 0.85  # 15% faster

            logger.info(f"  Players to scrape: {players_to_scrape}")
            logger.info(f"  Estimated time (sequential): ~{sequential_time:.1f} hours")
            logger.info(f"  Estimated time (parallel): ~{parallel_time:.1f} hours")

    except Exception as e:
        logger.error(f"  ❌ Error: {str(e)}")


def main():
    """Run all checks."""
    logger.info("="*80)
    logger.info("PRE-SCRAPE READINESS CHECK")
    logger.info("="*80)

    checks = [
        ("Dependencies", check_dependencies),
        ("Database", check_database),
        ("FBref IDs", check_fbref_ids),
        ("Configuration", check_configuration),
    ]

    results = []

    for name, check_func in checks:
        try:
            result = check_func()
            results.append((name, result))
        except Exception as e:
            logger.error(f"\n❌ {name} check failed: {str(e)}")
            results.append((name, False))

    # Estimate time
    estimate_scrape_time()

    # Summary
    logger.info("\n" + "="*80)
    logger.info("SUMMARY")
    logger.info("="*80)

    all_passed = all(result for _, result in results)

    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        logger.info(f"  {status}: {name}")

    if all_passed:
        logger.info("\n✅ All checks passed! Ready to scrape.")
        logger.info("\nRun:")
        logger.info("  python scrape_premier_league_complete.py")
    else:
        logger.error("\n❌ Some checks failed. Fix issues before scraping.")
        sys.exit(1)


if __name__ == "__main__":
    main()
