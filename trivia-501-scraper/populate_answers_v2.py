"""
populate_answers_v2.py — DEPRECATED
-------------------------------------
This script is no longer used.

It read player stats from the ``players.career_stats`` JSONB column and wrote
rows directly to the ``answers`` table.  Both of those approaches have been
superseded:

  * ``players.career_stats`` was dropped in V9 (Flyway migration).
    Per-season stats now live in ``player_season_stints``.

  * Direct Python writes to ``answers`` have been replaced by the Java
    materializer pipeline:
      1. Admin triggers   POST /api/admin/templates/generate
         → creates draft Question rows from question_templates
      2. Admin promotes a draft to active via the admin UI or
         PATCH /api/admin/questions/{id}/status  {"status": "active"}
         → QuestionMaterializerService.materialize() runs automatically,
           reading from player_season_stints and writing to answers + entities

See:
  backend/.../materializer/FootballTeamCompetitionMetricSinceMaterializer.java
  backend/.../materializer/FootballTeamCompetitionSeasonMaterializer.java
  backend/.../service/QuestionMaterializerService.java
"""

raise SystemExit(
    "\n[populate_answers_v2.py] This script is deprecated and must not be run.\n"
    "Use the Java materializer pipeline instead — see the docstring for details.\n"
)
