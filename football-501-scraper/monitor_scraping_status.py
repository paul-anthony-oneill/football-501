"""
Monitor Scraping Status - View detailed statistics about scraping progress.

This script provides comprehensive reporting on:
- Overall scraping statistics
- Failed players with error details
- Pending players
- Recent scrape jobs
"""

import logging
from datetime import datetime
from database.crud_v2 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(message)s'
)

logger = logging.getLogger(__name__)


def monitor_scraping_status():
    """Display comprehensive scraping status."""
    db = DatabaseManager()

    logger.info("="*80)
    logger.info("SCRAPING STATUS MONITOR")
    logger.info("="*80)
    logger.info(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    # Overall statistics
    logger.info("\n" + "-"*80)
    logger.info("OVERALL STATISTICS")
    logger.info("-"*80)

    scrape_stats = db.get_scrape_statistics()

    if not scrape_stats:
        logger.info("No scraping logs found.")
        return

    total_players = sum(scrape_stats.values())

    logger.info(f"Total players tracked: {total_players}")
    logger.info("")

    for status, count in sorted(scrape_stats.items()):
        percentage = (count / total_players * 100) if total_players > 0 else 0
        status_icon = {
            'success': '✅',
            'failed': '❌',
            'pending': '⏳',
            'skipped': '⏭️'
        }.get(status, '•')

        logger.info(f"  {status_icon} {status:10s}: {count:5d} ({percentage:5.1f}%)")

    # Database contents
    logger.info("\n" + "-"*80)
    logger.info("DATABASE CONTENTS")
    logger.info("-"*80)

    with db.get_session() as session:
        from database.models_v2 import Player, PlayerCareerStats, Team, Competition
        from sqlalchemy import func

        total_players_db = session.query(Player).count()
        players_with_fbref = session.query(Player).filter(Player.fbref_id.isnot(None)).count()
        total_stats = session.query(PlayerCareerStats).count()
        total_teams = session.query(Team).count()
        total_comps = session.query(Competition).count()

        # Players with most stats
        top_players = session.query(
            Player.name,
            func.count(PlayerCareerStats.id).label('stat_count')
        ).join(PlayerCareerStats).group_by(
            Player.id, Player.name
        ).order_by(
            func.count(PlayerCareerStats.id).desc()
        ).limit(5).all()

        logger.info(f"Total players: {total_players_db}")
        logger.info(f"Players with FBref IDs: {players_with_fbref}")
        logger.info(f"Career stat records: {total_stats}")
        logger.info(f"Teams: {total_teams}")
        logger.info(f"Competitions: {total_comps}")

        if top_players:
            logger.info("\nTop 5 players by career records:")
            for name, count in top_players:
                logger.info(f"  - {name}: {count} records")

    # Failed players details
    failed_ids = db.get_failed_players()

    if failed_ids:
        logger.info("\n" + "-"*80)
        logger.info(f"FAILED PLAYERS ({len(failed_ids)} total)")
        logger.info("-"*80)

        with db.get_session() as session:
            from database.models_v2 import Player, PlayerScrapeLog

            # Get failed with details
            failed_details = session.query(
                Player.id,
                Player.name,
                Player.fbref_id,
                PlayerScrapeLog.attempt_count,
                PlayerScrapeLog.error_type,
                PlayerScrapeLog.error_message,
                PlayerScrapeLog.last_attempt_at
            ).join(
                PlayerScrapeLog, Player.id == PlayerScrapeLog.player_id
            ).filter(
                PlayerScrapeLog.status == 'failed'
            ).order_by(
                PlayerScrapeLog.last_attempt_at.desc()
            ).limit(20).all()

            # Group by error type
            error_types = {}
            for _, _, _, _, error_type, _, _ in failed_details:
                if error_type:
                    error_types[error_type] = error_types.get(error_type, 0) + 1

            logger.info("\nError breakdown:")
            for error_type, count in sorted(error_types.items(), key=lambda x: -x[1]):
                logger.info(f"  {error_type}: {count}")

            logger.info(f"\nRecent failures (showing up to 20):")
            for pid, name, fbref_id, attempts, err_type, err_msg, last_attempt in failed_details[:20]:
                logger.info(f"\n  Player: {name} (ID: {pid})")
                logger.info(f"    FBref ID: {fbref_id or 'MISSING'}")
                logger.info(f"    Attempts: {attempts}")
                logger.info(f"    Error type: {err_type}")
                logger.info(f"    Last attempt: {last_attempt}")
                if err_msg:
                    short_msg = err_msg[:100] + '...' if len(err_msg) > 100 else err_msg
                    logger.info(f"    Error: {short_msg}")

    # Pending players
    pending_ids = db.get_pending_players()

    if pending_ids:
        logger.info("\n" + "-"*80)
        logger.info(f"PENDING PLAYERS ({len(pending_ids)} total)")
        logger.info("-"*80)

        logger.info("These players have not been scraped yet.")

        if len(pending_ids) <= 20:
            with db.get_session() as session:
                from database.models_v2 import Player

                for player_id in pending_ids:
                    player = session.query(Player).filter_by(id=player_id).first()
                    if player:
                        logger.info(f"  - {player.name} (ID: {player_id}, FBref: {player.fbref_id or 'MISSING'})")
        else:
            logger.info(f"  (Too many to display - showing count only)")

    # Recent scrape jobs
    logger.info("\n" + "-"*80)
    logger.info("RECENT SCRAPE JOBS")
    logger.info("-"*80)

    with db.get_session() as session:
        from database.models_v2 import ScrapeJob

        recent_jobs = session.query(ScrapeJob).order_by(
            ScrapeJob.started_at.desc()
        ).limit(5).all()

        if recent_jobs:
            for job in recent_jobs:
                logger.info(f"\nJob #{job.id} - {job.job_type}")
                logger.info(f"  Status: {job.status}")
                logger.info(f"  Started: {job.started_at}")
                if job.completed_at:
                    logger.info(f"  Completed: {job.completed_at}")
                    logger.info(f"  Duration: {job.duration_seconds:.1f}s")
                logger.info(f"  Players scraped: {job.players_scraped}")
                logger.info(f"  Rows inserted: {job.rows_inserted}")
                if job.error_message:
                    logger.info(f"  Error: {job.error_message[:100]}")
        else:
            logger.info("No recent jobs found.")

    # Recommendations
    logger.info("\n" + "="*80)
    logger.info("RECOMMENDATIONS")
    logger.info("="*80)

    if failed_ids:
        logger.info(f"⚠️  {len(failed_ids)} failed players detected")
        logger.info("   Run: python retry_failed_players.py")

    if pending_ids:
        logger.info(f"⏳ {len(pending_ids)} players still pending")
        logger.info("   Continue scraping to complete")

    if scrape_stats.get('success', 0) == total_players:
        logger.info("✅ All players successfully scraped!")

    logger.info("\n" + "="*80)


def export_failed_players_csv():
    """Export failed players to CSV for manual review."""
    db = DatabaseManager()

    with db.get_session() as session:
        from database.models_v2 import Player, PlayerScrapeLog

        failed = session.query(
            Player.id,
            Player.name,
            Player.fbref_id,
            Player.nationality,
            PlayerScrapeLog.attempt_count,
            PlayerScrapeLog.error_type,
            PlayerScrapeLog.error_message
        ).join(
            PlayerScrapeLog, Player.id == PlayerScrapeLog.player_id
        ).filter(
            PlayerScrapeLog.status == 'failed'
        ).all()

        if not failed:
            logger.info("No failed players to export.")
            return

        # Write CSV
        import csv

        filename = f"failed_players_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"

        with open(filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow([
                'Player ID', 'Name', 'FBref ID', 'Nationality',
                'Attempts', 'Error Type', 'Error Message'
            ])

            for row in failed:
                writer.writerow(row)

        logger.info(f"✅ Exported {len(failed)} failed players to {filename}")


def main():
    """Main entry point."""
    import argparse

    parser = argparse.ArgumentParser(description="Monitor Scraping Status")
    parser.add_argument(
        '--export-failed',
        action='store_true',
        help='Export failed players to CSV'
    )

    args = parser.parse_args()

    if args.export_failed:
        export_failed_players_csv()
    else:
        monitor_scraping_status()


if __name__ == "__main__":
    main()
