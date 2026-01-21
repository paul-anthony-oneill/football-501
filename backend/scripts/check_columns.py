from ScraperFC.fbref import FBref
import pandas as pd

def check_columns():
    print("Initializing ScraperFC...")
    fb = FBref(wait_time=7)
    
    print("Scraping Premier League 2023-2024 stats (minimal)...")
    try:
        # Scrape a small amount if possible, but scrape_stats gets the whole league
        result = fb.scrape_stats("2023-2024", "England Premier League", "standard")
        
        if 'player' in result:
            df = result['player']
            print(f"\nDataFrame Columns ({len(df.columns)}):")
            for col in df.columns:
                print(col)
                
            print("\nFirst row sample:")
            print(df.iloc[0])
            
            # Check for hidden attributes or ID-like columns
            # Sometimes ScraperFC puts the ID in a specific column or the index
            print("\nDataFrame Index:")
            print(df.index[:5])
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_columns()

