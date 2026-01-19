"""
Add FBref IDs for test players manually.

This allows us to test the scraping system with known players.
"""

from database.crud_v2 import DatabaseManager
from database.models_v2 import Player

# Known Premier League players with their FBref IDs
TEST_PLAYERS = {
    "Erling Haaland": "1f44ac21",
    "Kevin De Bruyne": "b8a3ad0c",
    "Phil Foden": "ed1e53f3",
    "Mohamed Salah": "e342ad68",
    "Harry Kane": "21a66f6a",
    "Bukayo Saka": "bc7dc64d",
    "Bruno Fernandes": "507c7bdf",
    "Son Heung-min": "92e7e919",
    "Martin Ødegaard": "07b4348d",
    "Cole Palmer": "e3ca5c0c"
}

def add_test_fbref_ids():
    """Add FBref IDs to test players."""
    db = DatabaseManager()

    print("Adding FBref IDs to test players...")
    print("="*60)

    added = 0
    not_found = []

    with db.get_session() as session:
        for player_name, fbref_id in TEST_PLAYERS.items():
            # Try to find player (case-insensitive)
            player = session.query(Player).filter(
                Player.normalized_name == player_name.lower()
            ).first()

            if player:
                player.fbref_id = fbref_id
                added += 1
                print(f"[OK] {player_name} -> {fbref_id}")
            else:
                not_found.append(player_name)
                print(f"[NOT FOUND] {player_name}")

        session.commit()

    print("="*60)
    print(f"Added FBref IDs: {added}/{len(TEST_PLAYERS)}")

    if not_found:
        print(f"\nNot found in database:")
        for name in not_found:
            print(f"  - {name}")

if __name__ == "__main__":
    add_test_fbref_ids()
