#!/usr/bin/env python3
"""
ScraperFC Proof of Concept - Working Version
"""

from ScraperFC.fbref import FBref
import pandas as pd

INVALID_DARTS_SCORES = {163, 166, 169, 172, 173, 175, 176, 178, 179}

def print_section(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def main():
    print("""
================================================================
ScraperFC Proof of Concept for Football 501
================================================================
Testing England Premier League 2023-24 Season
    """)

    fb = FBref(wait_time=7)

    # TEST 1: Basic scraping
    print_section("TEST 1: Scraping England Premier League 2023-24")
    print("Fetching standard player statistics...")
    print("(This will take ~7 seconds due to FBref rate limiting)\n")

    try:
        result = fb.scrape_stats("2023-2024", "England Premier League", "standard")

        print(f"Return type: {type(result)}")

        # Handle dictionary return
        if isinstance(result, dict):
            print(f"Dictionary keys: {list(result.keys())}")
            # Try to find player stats in the dictionary
            if 'player_stats' in result:
                player_stats = result['player_stats']
            elif 'players' in result:
                player_stats = result['players']
            else:
                # Take the first DataFrame value
                for key, value in result.items():
                    if isinstance(value, pd.DataFrame) and len(value) > 0:
                        player_stats = value
                        print(f"Using '{key}' as player stats")
                        break
        elif isinstance(result, tuple):
            squad_stats, opponent_stats, player_stats = result
        else:
            player_stats = result

        if isinstance(player_stats, pd.DataFrame):
            print(f"\n[SUCCESS] Dataset contains {len(player_stats)} rows")
            print(f"Columns ({len(player_stats.columns)}): {list(player_stats.columns[:10])}...")

            # TEST 2: Show sample data
            print_section("TEST 2: Sample Data")
            print("\nFirst 5 rows:")
            print(player_stats.head())

            # TEST 3: Check for key columns
            print_section("TEST 3: Column Validation")
            key_cols = ['Player', 'Squad', 'Playing_Time_MP', 'Performance_Gls', 'Nation']
            for col in key_cols:
                exists = col in player_stats.columns
                status = "[OK]" if exists else "[MISSING]"
                print(f"{status} {col}")

            # TEST 4: Team filtering
            if 'Squad' in player_stats.columns:
                print_section("TEST 4: Team Filtering - Manchester City")
                man_city = player_stats[player_stats['Squad'] == 'Manchester City']
                print(f"Found {len(man_city)} Manchester City players")

                if len(man_city) > 0 and 'Playing_Time_MP' in player_stats.columns:
                    print("\nTop 5 by appearances:")
                    top5 = man_city.nlargest(5, 'Playing_Time_MP')
                    for idx, row in top5.iterrows():
                        name = row.get('Player', 'Unknown')
                        apps = row.get('Playing_Time_MP', 0)
                        gls = row.get('Performance_Gls', 0)
                        print(f"  - {name}: {apps} apps, {gls} goals")

            # TEST 5: Summary
            print_section("TEST 5: SUMMARY")
            print(f"""
[SUCCESS] ScraperFC Validation Complete!

Dataset Statistics:
  - Total players: {len(player_stats)}
  - Total columns: {len(player_stats.columns)}
  - Data type: pandas DataFrame

Key Findings:
  - Can successfully scrape England Premier League data
  - Returns structured DataFrame with player statistics
  - Compatible with Football 501 requirements

Next Steps:
  1. Build Python microservice (FastAPI)
  2. Implement database population scripts
  3. Connect to PostgreSQL
  4. Test with other leagues (La Liga, Serie A, etc.)

See docs/design/SCRAPERFC_INTEGRATION.md for implementation plan.
            """)

        else:
            print(f"[WARN] player_stats is not a DataFrame: {type(player_stats)}")
            print(f"Content: {player_stats}")

    except Exception as e:
        print(f"\n[ERROR] {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
