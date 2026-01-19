#!/usr/bin/env python3
"""
Scrape data from multiple competitions to build a complete career profile.

This script demonstrates how to populate the database with data from:
1. Top 5 European Leagues (Premier League, La Liga, Bundesliga, Serie A, Ligue 1)
2. Continental Competitions (Champions League, Europa League)
3. Recent history (last 3 seasons)

Usage:
    python scrape_all_competitions.py --seasons 1 --leagues "Premier League,Champions League"
"""

import sys
import os
import argparse
import time
from typing import List

# Add current directory to path
sys.path.append(os.getcwd())

from scrapers.player_career_scraper import PlayerCareerScraper
from database.crud_v2 import DatabaseManager
from config import LEAGUE_MAPPING

def safe_print(text):
    try:
        print(text)
    except UnicodeEncodeError:
        print(text.encode('ascii', errors='replace').decode('ascii'))

# Define standard competition sets
COMPETITION_SETS = {
    "top5": [
        "England Premier League",
        "Spain La Liga", 
        "Germany Bundesliga", 
        "Italy Serie A", 
        "France Ligue 1"
    ],
    "europe": [
        "UEFA Champions League",
        "UEFA Europa League"
    ],
    "english_tiers": [
        "England Premier League",
        "England Championship"
    ]
}

def generate_seasons(count: int = 3) -> List[str]:
    """Generate list of recent seasons."""
    current_year = 2024
    seasons = []
    for i in range(count):
        start_year = current_year - i
        seasons.append(f"{start_year}-{start_year + 1}")
    return seasons

def main():
    parser = argparse.ArgumentParser(description="Scrape multiple competitions")
    parser.add_argument("--seasons", type=int, default=1, help="Number of recent seasons to scrape (default: 1)")
    parser.add_argument("--set", choices=COMPETITION_SETS.keys(), help="Predefined set of leagues")
    parser.add_argument("--leagues", type=str, help="Comma-separated list of specific leagues (e.g., 'Premier League,La Liga')")
    parser.add_argument("--min-apps", type=int, default=5, help="Minimum appearances to store (default: 5)")
    
    args = parser.parse_args()

    # Determine leagues to scrape
    target_leagues = []
    
    if args.leagues:
        # Use provided list, mapping to full names where possible
        raw_list = [l.strip() for l in args.leagues.split(",")]
        for l in raw_list:
            # Check if it's a key in LEAGUE_MAPPING (e.g. "EPL" -> "England Premier League")
            # Or if it's already a full name
            full_name = LEAGUE_MAPPING.get(l, l)
            target_leagues.append(full_name)
    elif args.set:
        target_leagues = COMPETITION_SETS[args.set]
    else:
        # Default to just Premier League + Champions League for demo
        target_leagues = ["England Premier League", "UEFA Champions League"]

    seasons = generate_seasons(args.seasons)
    
    scraper = PlayerCareerScraper()
    
    safe_print("\n" + "=" * 80)
    safe_print("MULTI-COMPETITION SCRAPER")
    safe_print("=" * 80)
    safe_print(f"Leagues: {', '.join(target_leagues)}")
    safe_print(f"Seasons: {', '.join(seasons)}")
    safe_print(f"Min Appearances: {args.min_apps}")
    safe_print("-" * 80)

    total_stats = 0
    errors = 0

    for season in seasons:
        safe_print(f"\nProcessing Season: {season}")
        
        for league in target_leagues:
            safe_print(f"  Scraping {league}...")
            
            try:
                # Scrape entire league
                result = scraper.scrape_league_players(
                    league=league,
                    season=season,
                    min_appearances=args.min_apps,
                    rescrape_recent=False 
                )
                
                stored = result.get('stats_stored', 0)
                processed = result.get('players_processed', 0)
                
                safe_print(f"    [OK] Processed {processed} players, stored {stored} stats")
                total_stats += stored
                
                # Polite wait to avoid rate limits between leagues
                time.sleep(5)
                
            except Exception as e:
                safe_print(f"    [ERROR] Failed to scrape {league}: {e}")
                errors += 1
                time.sleep(5) # Wait even on error

    safe_print("\n" + "=" * 80)
    safe_print("SUMMARY")
    safe_print("=" * 80)
    safe_print(f"Total stats records stored: {total_stats}")
    safe_print(f"Total errors: {errors}")
    safe_print("\nData is now available in 'player_career_stats' table.")
    safe_print("Questions created without competition filters will now aggregate this data.")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
