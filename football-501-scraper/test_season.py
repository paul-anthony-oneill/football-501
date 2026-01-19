#!/usr/bin/env python3
"""Quick test to see why a specific season fails."""

from scrapers import FBrefScraper

def safe_print(text):
    try:
        print(text)
    except UnicodeEncodeError:
        print(text.encode('ascii', errors='replace').decode('ascii'))

# Test one of the "failed" seasons
season = "2002-2003"
league = "England Premier League"
team = "Manchester City"

safe_print(f"\nTesting: {team} - {league} - {season}\n")

scraper = FBrefScraper()

try:
    player_df = scraper.scrape_team_stats(season, league, team, "standard")
    safe_print(f"Success! Found {len(player_df)} players")

    if len(player_df) > 0:
        safe_print("\nFirst 5 players:")
        column_mapping = scraper.get_column_names(player_df)
        player_col = column_mapping.get('player_name')
        if player_col:
            for i, row in player_df.head(5).iterrows():
                safe_print(f"  - {row[player_col]}")
    else:
        safe_print("\nNo players found - likely Manchester City was not in Premier League this season")

except Exception as e:
    safe_print(f"Error: {e}")
    import traceback
    traceback.print_exc()
