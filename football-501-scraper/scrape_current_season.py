"""
Scrape Current Season (2024-2025) Statistics
-------------------------------------------
Fetches the latest player statistics from FBref using ScraperFC 
and updates the 'players' table career_stats.
"""

import os
import sys
import pandas as pd
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from ScraperFC.fbref import FBref

# Add current dir to path to import config and models
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings
from database.models_v4 import Player, Team, Competition

def normalize_name(name):
    return "".join(c for c in name.lower() if c.isalnum())

def run_scrape():
    print(f"Starting scrape for {settings.current_season}...")
    
    # Initialize DB
    engine = create_engine(settings.database_url)
    Session = sessionmaker(bind=engine)
    session = Session()
    
    # Initialize Scraper (7s wait per request to respect FBref limits)
    fb = FBref(wait_time=settings.fbref_wait_time)
    
    leagues = [
        ("England Premier League", "EPL")
    ]
    
    season = settings.current_season # "2024-2025"
    
    for league_name, league_slug in leagues:
        print(f"\n--- Scraping {league_name} ({season}) ---")
        try:
            # Scrape standard stats
            result = fb.scrape_stats(season, league_name, "standard")
            
            # Handle return format
            if isinstance(result, dict):
                player_df = result.get('player')
            elif isinstance(result, tuple) and len(result) == 3:
                _, _, player_df = result
            else:
                player_df = result
                
            if not isinstance(player_df, pd.DataFrame):
                player_df = pd.DataFrame(player_df)
            
            # Flatten multi-level columns if they exist
            if isinstance(player_df.columns, pd.MultiIndex):
                # Join the levels with an underscore or just take the last level
                # Usually standard stats have (Playing Time, MP), (Performance, Gls)
                player_df.columns = [
                    f"{col[0]}_{col[1]}" if not col[0].startswith('Unnamed') else col[1]
                    for col in player_df.columns.values
                ]
                
            print(f"Found {len(player_df)} players. Processing...")
            
            players_updated = 0
            for _, row in player_df.iterrows():
                name = row['Player']
                
                # Skip header rows
                if name == 'Player':
                    continue
                    
                squad = row['Squad']
                nation = row.get('Nation', '')
                
                # Metrics for Football 501 - Handle flattened column names
                apps = int(row.get('Playing Time_MP', row.get('Playing_Time_MP', 0)))
                goals = int(row.get('Performance_Gls', 0))
                assists = int(row.get('Performance_Ast', 0))
                minutes = int(row.get('Playing Time_Min', row.get('Playing_Time_Min', 0)))
                
                if apps == 0:
                    continue
                    
                norm_name = normalize_name(name)
                
                # Check if player exists (by FBref ID if possible, otherwise name)
                # Note: ScraperFC doesn't always expose the ID in the dataframe 
                # but we can try to match on normalized name
                player = session.query(Player).filter_by(normalized_name=norm_name).first()
                
                new_stat = {
                    "season": season,
                    "team": squad,
                    "competition": "Premier League" if league_slug == "EPL" else league_name,
                    "appearances": apps,
                    "goals": goals,
                    "assists": assists,
                    "minutes_played": minutes,
                    "last_updated": datetime.utcnow().isoformat()
                }
                
                if player:
                    # Update existing player's career_stats
                    stats = list(player.career_stats)
                    # Remove old entry for this season/team/comp if it exists to avoid duplicates
                    stats = [s for s in stats if not (s.get('season') == season and s.get('team') == squad)]
                    stats.append(new_stat)
                    player.career_stats = stats
                    player.last_scraped_at = datetime.utcnow()
                else:
                    # Create new player
                    # We generate a dummy fbref_id if missing since it's a required field in models_v4
                    fbref_id = f"gen_{norm_name}" 
                    player = Player(
                        fbref_id=fbref_id,
                        name=name,
                        normalized_name=norm_name,
                        nationality=nation,
                        career_stats=[new_stat],
                        last_scraped_at=datetime.utcnow()
                    )
                    session.add(player)
                
                players_updated += 1
                if players_updated % 50 == 0:
                    session.commit()
                    print(f"  Processed {players_updated} players...")
            
            session.commit()
            print(f"Finished {league_name}: {players_updated} players updated.")
            
        except Exception as e:
            print(f"Error scraping {league_name}: {e}")
            session.rollback()

    print("\nScrape complete. Now run 'python populate_answers_v2.py' to refresh the game engine.")

if __name__ == "__main__":
    run_scrape()
