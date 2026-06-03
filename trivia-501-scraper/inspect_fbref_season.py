"""
FBref Season Inspector
======================
Scrapes ONE season of ONE league and dumps everything we get back:
column names, data types, sample rows, squad names, and nullability.

This is a one-off diagnostic — not part of the production pipeline.

Usage
-----
    python inspect_fbref_season.py [--year YYYY-YYYY] [--league LEAGUE]

Defaults: 2024-2025, EPL

Stat categories scraped
-----------------------
  standard    — appearances, goals, assists, cards, minutes
  goalkeeping — clean sheets, goals conceded, saves

Output is written to:
  fbref_inspect_<league>_<year>_standard.txt
  fbref_inspect_<league>_<year>_goalkeeping.txt
  fbref_inspect_<league>_<year>_summary.txt  ← the one to read first
"""

import sys
import os
import time
import argparse
import textwrap
import warnings

import pandas as pd

warnings.filterwarnings("ignore")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from config import settings

# ---------------------------------------------------------------------------
# CLI args
# ---------------------------------------------------------------------------
parser = argparse.ArgumentParser()
parser.add_argument("--year",   default="2024-2025", help="FBref season string, e.g. 2024-2025")
parser.add_argument("--league", default="EPL",       help="FBref league key, e.g. EPL")
args = parser.parse_args()

YEAR   = args.year
LEAGUE = args.league
SLUG   = f"{LEAGUE}_{YEAR}".replace(" ", "_")

CATEGORIES = ["standard", "goalkeeping"]

# ---------------------------------------------------------------------------
# Chrome + FBref setup  (same Cloudflare bypass as scrape_current_season.py)
# ---------------------------------------------------------------------------
print(f"Launching Chrome (undetected) to bypass Cloudflare…")
import undetected_chromedriver as uc
from ScraperFC.fbref import FBref

driver = uc.Chrome(headless=False, version_main=148)
fb     = FBref(wait_time=settings.fbref_wait_time)

def _wait_for_cloudflare(url: str) -> None:
    driver.get(url)
    for _ in range(30):
        if "Just a moment" not in driver.title:
            break
        time.sleep(1)
    time.sleep(fb.wait_time)

def chrome_get(url: str):
    class R:
        pass
    _wait_for_cloudflare(url)
    r = R()
    r.status_code = 200
    r.content = driver.page_source.encode("utf-8")
    return r

fb._get            = chrome_get
fb._driver_init    = lambda: None
fb.driver          = driver
fb._driver_get     = _wait_for_cloudflare
fb._driver_close   = lambda: None

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def flatten_columns(df: pd.DataFrame) -> pd.DataFrame:
    """Collapse MultiIndex columns to 'Group_Stat' strings."""
    if isinstance(df.columns, pd.MultiIndex):
        df.columns = [
            f"{g}_{s}" if not g.startswith("Unnamed") else s
            for g, s in df.columns.values
        ]
    return df


def strip_header_rows(df: pd.DataFrame) -> pd.DataFrame:
    """FBref injects repeated header rows mid-table (Player == 'Player')."""
    if "Player" in df.columns:
        df = df[df["Player"] != "Player"].reset_index(drop=True)
    return df


def col_report(df: pd.DataFrame) -> str:
    lines = []
    lines.append(f"{'Column':<45} {'dtype':<12} {'non-null':>8}  {'null%':>6}  sample_values")
    lines.append("-" * 110)
    for col in df.columns:
        non_null  = df[col].notna().sum()
        null_pct  = 100 * (1 - non_null / len(df)) if len(df) else 0
        samples   = df[col].dropna().astype(str).unique()[:3]
        sample_s  = ", ".join(repr(s) for s in samples)
        lines.append(
            f"{col:<45} {str(df[col].dtype):<12} {non_null:>8}  {null_pct:>5.1f}%  {sample_s}"
        )
    return "\n".join(lines)


def squad_names(df: pd.DataFrame) -> list:
    for col in ("Squad", "squad", "team", "Team"):
        if col in df.columns:
            return sorted(df[col].dropna().unique().tolist())
    return []


# ---------------------------------------------------------------------------
# Scrape each category
# ---------------------------------------------------------------------------
results = {}

for cat in CATEGORIES:
    print(f"\n{'='*60}")
    print(f"  Scraping  {LEAGUE}  {YEAR}  [{cat}]")
    print(f"{'='*60}")

    try:
        raw = fb.scrape_stats(YEAR, LEAGUE, cat)

        # scrape_stats returns (squad_df, opp_df, player_df)
        if isinstance(raw, (tuple, list)) and len(raw) == 3:
            squad_df, opp_df, player_df = raw
        else:
            squad_df, opp_df, player_df = None, None, raw

        cleaned = {}
        for label, df in [("squad", squad_df), ("player", player_df)]:
            if df is None or not isinstance(df, pd.DataFrame) or df.empty:
                print(f"  {label}: no data returned")
                continue
            df = flatten_columns(df)
            df = strip_header_rows(df)
            cleaned[label] = df
            print(f"  {label}: {len(df)} rows × {len(df.columns)} columns")

        results[cat] = cleaned

    except Exception as exc:
        print(f"  ERROR: {exc}")
        results[cat] = {}

driver.quit()
print("\nBrowser closed.")

# ---------------------------------------------------------------------------
# Write per-category detail files
# ---------------------------------------------------------------------------
for cat, cleaned in results.items():
    for label, df in cleaned.items():
        fname = f"fbref_inspect_{SLUG}_{cat}_{label}.txt"
        with open(fname, "w") as f:
            f.write(f"=== {LEAGUE} {YEAR} — {cat} — {label} ===\n\n")
            f.write(f"Shape: {df.shape}\n\n")
            f.write("--- COLUMN REPORT ---\n")
            f.write(col_report(df))
            f.write("\n\n--- SQUAD NAMES (unique values in Squad column) ---\n")
            for s in squad_names(df):
                f.write(f"  {s!r}\n")
            f.write("\n--- FIRST 5 ROWS (transposed for readability) ---\n")
            f.write(df.head(5).T.to_string())
        print(f"Written: {fname}")

# ---------------------------------------------------------------------------
# Write summary file — the one to read first
# ---------------------------------------------------------------------------
summary_fname = f"fbref_inspect_{SLUG}_summary.txt"
with open(summary_fname, "w") as f:
    f.write(f"FBref Season Inspector — {LEAGUE} {YEAR}\n")
    f.write("=" * 60 + "\n\n")

    for cat, cleaned in results.items():
        f.write(f"\n{'─'*60}\n")
        f.write(f"CATEGORY: {cat.upper()}\n")
        f.write(f"{'─'*60}\n")

        for label, df in cleaned.items():
            f.write(f"\n  {label.upper()} TABLE  ({len(df)} rows × {len(df.columns)} cols)\n\n")

            # Column groups
            if isinstance(df.columns, pd.Index):
                groups: dict = {}
                for col in df.columns:
                    if "_" in col:
                        grp = col.split("_")[0]
                    else:
                        grp = "misc"
                    groups.setdefault(grp, []).append(col)
                for grp, cols in groups.items():
                    f.write(f"    [{grp}]\n")
                    for c in cols:
                        non_null = df[c].notna().sum()
                        null_pct = 100 * (1 - non_null / len(df)) if len(df) else 0
                        f.write(f"      {c:<45}  null={null_pct:.0f}%\n")
                    f.write("\n")

            # Squad names (player table)
            if label == "player":
                squads = squad_names(df)
                f.write(f"  SQUAD NAMES AS RETURNED BY FBREF ({len(squads)}):\n")
                for s in squads:
                    f.write(f"    {s!r}\n")
                f.write("\n")

print(f"\nSummary written: {summary_fname}")
print("\nDone. Open the summary file first, then the per-category files for full column details.")
