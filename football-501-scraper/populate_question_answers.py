"""
Populate Question Answers from JSONB Data

Queries player JSONB career_stats and populates question_valid_answers table.
"""

import logging
from typing import Optional
import sys

from database.crud_v3 import DatabaseManager
from database.models_v3 import Question, Team, Competition

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def populate_question_answers(question_id: str):
    """
    Populate answers for a specific question.

    Args:
        question_id: Question UUID
    """
    db = DatabaseManager()

    with db.get_session() as session:
        # Get question details
        question = session.query(Question).filter_by(id=question_id).first()

        if not question:
            logger.error(f"Question not found: {question_id}")
            return

        logger.info(f"Populating answers for: {question.question_text}")
        logger.info(f"  Stat type: {question.stat_type}")

        # Show filters
        if question.team_id:
            team = session.query(Team).filter_by(id=question.team_id).first()
            logger.info(f"  Team filter: {team.name if team else 'None'}")

        if question.competition_id:
            comp = session.query(Competition).filter_by(id=question.competition_id).first()
            logger.info(f"  Competition filter: {comp.name if comp else 'None'}")

        if question.season_filter:
            logger.info(f"  Season filter: {question.season_filter}")

    # Populate answers
    count = db.populate_question_answers(question_id)
    logger.info(f"✅ Populated {count} valid answers")


def populate_all_active_questions():
    """Populate answers for all active questions."""
    db = DatabaseManager()

    with db.get_session() as session:
        questions = session.query(Question).filter_by(is_active=True).all()

        logger.info(f"Found {len(questions)} active questions")

        for question in questions:
            logger.info(f"\n{'='*60}")
            logger.info(f"Question: {question.question_text}")

            try:
                count = db.populate_question_answers(str(question.id))
                logger.info(f"✅ Populated {count} answers")
            except Exception as e:
                logger.error(f"❌ Failed to populate: {str(e)}")


def create_sample_question():
    """Create a sample question for testing."""
    db = DatabaseManager()

    with db.get_session() as session:
        # Get Premier League
        premier_league = session.query(Competition).filter_by(
            name='Premier League'
        ).first()

        if not premier_league:
            logger.error("Premier League not found in database")
            return

        # Get Manchester City
        man_city = db.get_or_create_team(
            name='Manchester City',
            team_type='club',
            country='England'
        )

        # Create question
        from database.models_v3 import Question

        question = Question(
            question_text='Appearances for Manchester City in Premier League 2023-24',
            stat_type='appearances',
            team_id=man_city.id,
            competition_id=premier_league.id,
            season_filter='2023-2024',
            is_active=True
        )

        session.add(question)
        session.commit()
        session.refresh(question)

        logger.info(f"✅ Created sample question: {question.id}")
        logger.info(f"   Text: {question.question_text}")

        return str(question.id)


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='Populate question answers from JSONB data')
    parser.add_argument('--question-id', type=str, help='Specific question UUID to populate')
    parser.add_argument('--all', action='store_true', help='Populate all active questions')
    parser.add_argument('--create-sample', action='store_true', help='Create sample question')

    args = parser.parse_args()

    if args.create_sample:
        logger.info("Creating sample question...")
        question_id = create_sample_question()
        if question_id:
            logger.info(f"\nTo populate answers, run:")
            logger.info(f"  python populate_question_answers.py --question-id {question_id}")

    elif args.question_id:
        populate_question_answers(args.question_id)

    elif args.all:
        populate_all_active_questions()

    else:
        parser.print_help()
        sys.exit(1)
