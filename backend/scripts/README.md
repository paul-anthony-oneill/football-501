# ScraperFC Proof of Concept

This directory contains a proof-of-concept script to validate ScraperFC integration with Football 501.

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Run the PoC Script

```bash
python poc_scraperfc.py
```

**Expected Output**:
- ✅ Successfully scrapes EPL 2023-24 player statistics
- ✅ Demonstrates all Football 501 question types
- ✅ Validates data quality for game engine
- ⏱️ Takes ~30-45 seconds (due to FBref rate limiting)

## What This Tests

The PoC validates that ScraperFC can provide:

1. **Team League Appearances**: Filter players by team
2. **Combined Stats**: Calculate appearances + goals
3. **Goalkeeper Stats**: Appearances + clean sheets
4. **Nationality Filter**: Filter players by country
5. **Data Quality**: Check for null values, invalid scores
6. **League Coverage**: List available competitions

## Next Steps

If the PoC succeeds:

1. Read the full integration plan: `../../docs/design/SCRAPERFC_INTEGRATION.md`
2. Build the Python microservice (FastAPI)
3. Implement database population scripts
4. Schedule periodic updates

## Troubleshooting

### "Module not found: ScraperFC"
```bash
pip install ScraperFC
```

### Scraping takes too long
- This is expected (7-second wait between requests)
- FBref enforces rate limiting: max 10 requests/minute

### "Invalid league name"
- Check available leagues with: `list(fb.comps.keys())`
- See ScraperFC docs: https://scraperfc.readthedocs.io

## Documentation

- ScraperFC Docs: https://scraperfc.readthedocs.io
- Integration Plan: `../../docs/design/SCRAPERFC_INTEGRATION.md`
- Football 501 Rules: `../../CLAUDE.md`
