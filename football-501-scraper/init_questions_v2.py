"""
Initialize Questions (V2)
-------------------------
Creates generic Categories and Questions based on existing Teams.
"""

import sys
import os
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine

# Add current directory to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config import settings
from database.models_v4 import Base, Category, Question, Team

def get_db_session():
    engine = create_engine(settings.database_url)
    Session = sessionmaker(bind=engine)
    return Session()

def run():
    session = get_db_session()
    
    # 1. Create Category
    print("Creating Category 'Premier League'...")
    category = session.query(Category).filter_by(slug="premier-league").first()
    if not category:
        category = Category(
            name="Premier League",
            slug="premier-league",
            description="English Premier League Stats"
        )
        session.add(category)
        session.commit()
    
    print(f"Category ID: {category.id}")

    # 2. Fetch Teams
    # We want teams that actually have stats. 
    # For now, let's just fetch all 'club' teams.
    teams = session.query(Team).filter_by(team_type='club').all()
    print(f"Found {len(teams)} clubs.")

    # 3. Create Questions
    # We will create 2 questions per team: Goals and Appearances
    
    questions_created = 0
    
    for team in teams:
        # Check if question exists
        # Goals
        q_text_goals = f"{team.name} - Premier League Goals"
        exists = session.query(Question).filter_by(question_text=q_text_goals).first()
        
        if not exists:
            q_goals = Question(
                category_id=category.id,
                question_text=q_text_goals,
                metric_key="goals",
                config={
                    "team": team.name,
                    "competition": "Premier League"
                },
                min_score=1, # Ignore 0 goals
                is_active=True
            )
            session.add(q_goals)
            questions_created += 1

        # Appearances
        q_text_apps = f"{team.name} - Premier League Appearances"
        exists = session.query(Question).filter_by(question_text=q_text_apps).first()
        
        if not exists:
            q_apps = Question(
                category_id=category.id,
                question_text=q_text_apps,
                metric_key="appearances",
                config={
                    "team": team.name,
                    "competition": "Premier League"
                },
                min_score=1,
                is_active=True
            )
            session.add(q_apps)
            questions_created += 1
            
    session.commit()
    print(f"Created {questions_created} new questions.")

if __name__ == "__main__":
    run()
