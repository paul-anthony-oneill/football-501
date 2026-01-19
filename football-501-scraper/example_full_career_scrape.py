"""
Example: Complete workflow for scraping player full career data.

This demonstrates the recommended workflow:
1. Scrape league stats (gets player list with FBref IDs)
2. Extract and store FBref IDs
3. Scrape full career histories for players
4. Query comprehensive player data
"""

import logging
from scrapers.player_career_scraper import PlayerCareerScraper
from database.crud_v2 import DatabaseManager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def demonstrate_full_workflow():
    """
    Complete workflow example.
    """
    scraper = PlayerCareerScraper()
    db = DatabaseManager()

    logger.info("="*80)
    logger.info("WORKFLOW: Scraping Complete Player Career Data")
    logger.info("="*80)

    # ========================================================================
    # STEP 1: Check current database state
    # ========================================================================
    logger.info("\n[STEP 1] Current Database State")
    logger.info("-"*80)

    with db.get_session() as session:
        from database.models_v2 import Player, PlayerCareerStats, Team, Competition

        total_players = session.query(Player).count()
        players_with_fbref = session.query(Player).filter(Player.fbref_id.isnot(None)).count()
        total_stats = session.query(PlayerCareerStats).count()
        total_teams = session.query(Team).count()
        total_comps = session.query(Competition).count()

        logger.info(f"Players in database: {total_players}")
        logger.info(f"Players with FBref IDs: {players_with_fbref}")
        logger.info(f"Career stat records: {total_stats}")
        logger.info(f"Teams: {total_teams}")
        logger.info(f"Competitions: {total_comps}")

        if players_with_fbref == 0:
            logger.warning("\n⚠️  No players have FBref IDs yet!")
            logger.info("\nYou need to:")
            logger.info("1. Run: python update_fbref_ids.py")
            logger.info("   (This extracts FBref IDs from scraped league data)")
            logger.info("2. OR manually set fbref_id for a player in the database")
            logger.info("\nExample FBref IDs:")
            logger.info("  - Erling Haaland: 1f44ac21")
            logger.info("  - Kevin De Bruyne: b8a3ad0c")
            logger.info("  - Phil Foden: ed1e53f3")
            return

    # ========================================================================
    # STEP 2: Demonstrate single player career scrape
    # ========================================================================
    logger.info("\n[STEP 2] Scraping Single Player Career")
    logger.info("-"*80)

    with db.get_session() as session:
        from database.models_v2 import Player

        # Get first player with FBref ID
        player = session.query(Player).filter(Player.fbref_id.isnot(None)).first()

        if not player:
            logger.error("No players with FBref IDs found")
            return

        logger.info(f"Selected player: {player.name}")
        logger.info(f"FBref ID: {player.fbref_id}")

        # Check existing stats for this player
        existing_stats = session.query(PlayerCareerStats).filter_by(player_id=player.id).count()
        logger.info(f"Existing career records: {existing_stats}")

    # Scrape full career
    logger.info(f"\nScraping full career for {player.name}...")
    logger.info("(This will take ~7 seconds due to rate limiting)")

    try:
        result = scraper.scrape_full_player_career(
            player_id=player.id,
            fbref_id=player.fbref_id,
            force_rescrape=True
        )

        logger.info(f"\n✅ Career scrape complete!")
        logger.info(f"  New stats stored: {result['stats_stored']}")

        # Show the data
        logger.info(f"\nCareer breakdown for {player.name}:")

        with db.get_session() as session:
            stats = session.query(PlayerCareerStats).filter_by(player_id=player.id).all()

            for stat in stats:
                team = db.get_team_by_id(stat.team_id)
                comp = db.get_competition_by_id(stat.competition_id)
                logger.info(
                    f"  {stat.season} | {team.name:25} | {comp.name:20} | "
                    f"{stat.appearances:3} apps | {stat.goals:3} goals | {stat.assists:3} assists"
                )

    except Exception as e:
        logger.error(f"❌ Failed to scrape: {str(e)}")
        import traceback
        traceback.print_exc()
        return

    # ========================================================================
    # STEP 3: Demonstrate batch scraping (optional)
    # ========================================================================
    logger.info("\n[STEP 3] Batch Scraping (Optional)")
    logger.info("-"*80)
    logger.info("To scrape all players with FBref IDs, run:")
    logger.info("  result = scraper.scrape_all_stored_players(force_rescrape=False)")
    logger.info("\nNote: This will take ~7 seconds per player due to rate limiting")

    with db.get_session() as session:
        from database.models_v2 import Player
        total_with_ids = session.query(Player).filter(Player.fbref_id.isnot(None)).count()
        estimated_time = total_with_ids * 7 / 60
        logger.info(f"  Players to process: {total_with_ids}")
        logger.info(f"  Estimated time: ~{estimated_time:.1f} minutes")

    # ========================================================================
    # STEP 4: Query comprehensive data
    # ========================================================================
    logger.info("\n[STEP 4] Querying Comprehensive Player Data")
    logger.info("-"*80)

    # Example queries
    with db.get_session() as session:
        from database.models_v2 import Player, PlayerCareerStats, Team, Competition

        # Find players who played in multiple competitions
        from sqlalchemy import func
        multi_comp_players = session.query(
            Player.name,
            func.count(func.distinct(PlayerCareerStats.competition_id)).label('comp_count')
        ).join(PlayerCareerStats).group_by(Player.id, Player.name).having(
            func.count(func.distinct(PlayerCareerStats.competition_id)) > 1
        ).all()

        if multi_comp_players:
            logger.info("\nPlayers who played in multiple competitions:")
            for name, count in multi_comp_players[:5]:
                logger.info(f"  {name}: {count} competitions")

        # Find players who played for national teams
        national_stats = session.query(PlayerCareerStats).join(Team).filter(
            Team.team_type == 'national'
        ).count()

        logger.info(f"\nNational team records: {national_stats}")

    logger.info("\n" + "="*80)
    logger.info("WORKFLOW COMPLETE")
    logger.info("="*80)


if __name__ == "__main__":
    demonstrate_full_workflow()
