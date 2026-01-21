"""
Populate Answers (V2)
---------------------
Reads generic Questions (with JSONB config) and Players (with JSONB stats).
Calculates scores based on metric_key and filters.
Populates the generic 'answers' table.

Usage:
    python populate_answers_v2.py
"""

import sys
import os
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine, text

# Add current directory to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config import settings
from database.models_v4 import Base, Player, Question, Answer
from utils.darts import is_valid_darts_score

def get_db_session():
    engine = create_engine(settings.database_url)
    Session = sessionmaker(bind=engine)
    return Session()

def clean_value(val):
    """Handle string numbers like '1,200' -> 1200"""
    if isinstance(val, str):
        val = val.replace(',', '')
        if val.isdigit():
            return int(val)
    if isinstance(val, (int, float)):
        return int(val)
    return 0

def run():
    session = get_db_session()
    
    print("Fetching active questions...")
    questions = session.query(Question).filter(Question.is_active == True).all()
    print(f"Found {len(questions)} active questions.")

    if not questions:
        print("No questions found. Exiting.")
        return

    print("Fetching all players...")
    players = session.query(Player).all()
    print(f"Found {len(players)} players.")

    print("Clearing existing answers...")
    session.query(Answer).delete()
    session.commit()

    answers_batch = []
    
    print("Processing answers...")
    
    # Track seen keys per question to avoid UniqueConstraint violations
    # Map: question_id -> set(answer_keys)
    seen_keys = {q.id: set() for q in questions}

    for p in players:
        if not p.career_stats:
            continue

        for q in questions:
            # 1. Apply Filters from q.config
            # Config example: {"team_id": "...", "competition_id": "..."}
            # Stats example: {"team_id": "...", "goals": 10, ...}
            
            relevant_stats = []
            
            for season_stat in p.career_stats:
                match = True
                
                # Iterate through all filters in the question config
                # If config is empty {}, it matches ALL stats (Global stats)
                for filter_key, filter_val in q.config.items():
                    # Handle special keys or mismatching types if necessary
                    # Assuming exact match for IDs (strings)
                    stat_val = season_stat.get(filter_key)
                    
                    if str(stat_val) != str(filter_val):
                        match = False
                        break
                
                if match:
                    relevant_stats.append(season_stat)
            
            if not relevant_stats:
                continue

            # 2. Calculate Metric (Summation)
            # e.g. metric_key = "goals"
            total_score = 0
            for stat in relevant_stats:
                val = clean_value(stat.get(q.metric_key, 0))
                total_score += val
            
            # 3. Validate Answer
            # Only store if score is positive (optional rule, but logical for 501)
            # Or should we store 0 scores? Usually 0 is a waste of a turn but valid answer.
            # Let's store > 0 for now to keep table clean, unless specifically requested.
            if total_score > 0:
                answer_key = p.normalized_name
                
                # Check for duplicates
                if answer_key in seen_keys[q.id]:
                    # Collision: Same name, different player (or same player processed twice?)
                    # For now, skip the second one to respect DB constraint.
                    # Ideally we would append (dob) or something.
                    continue
                
                seen_keys[q.id].add(answer_key)

                is_valid = is_valid_darts_score(total_score)
                
                # Check min_score constraint
                if q.min_score and total_score < q.min_score:
                    continue

                # Bust Logic (Standard 501 rules: > 180 is impossible in one turn)
                # Actually, in this game, if the Answer Score is 200, it's a "Bust" for that turn.
                is_bust = total_score > 180 or total_score <= 0
                
                answers_batch.append(Answer(
                    question_id=q.id,
                    answer_key=p.normalized_name,
                    display_text=p.name,
                    score=total_score,
                    is_valid_darts=is_valid,
                    is_bust=is_bust,
                    answer_metadata={"player_id": str(p.id)}
                ))

        # Batch Insert check (every 1000 players?)
        # Actually doing bulk insert at end is risky for memory if 5000 * 50 = 250k objects.
        # Let's commit every 100 players.
        if len(answers_batch) > 10000:
            print(f"Persisting batch of {len(answers_batch)} answers...")
            session.bulk_save_objects(answers_batch)
            answers_batch = []

    # Final commit
    if answers_batch:
        print(f"Persisting final batch of {len(answers_batch)} answers...")
        session.bulk_save_objects(answers_batch)
    
    session.commit()
    print("Done!")

if __name__ == "__main__":
    run()
