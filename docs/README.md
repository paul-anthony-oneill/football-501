# Football 501 Documentation

Welcome to the Football 501 documentation! This guide will help you navigate all available documents.

---

## 📚 Documentation Index

### 🚀 Start Here

**New to the project?** Start with these documents in order:

1. **[Project Log](PROJECT_LOG.md)** - What we've implemented so far (Updated: 2026-01-18)
2. **[Scraping Service Summary](SCRAPING_SERVICE_SUMMARY.md)** - Quick overview of how the scraping service works
3. **[CLAUDE.md](../CLAUDE.md)** - Project overview and context for development

---

## 📋 Planning & Requirements

### Core Documents

- **[PRD.md](PRD.md)** - Product Requirements Document
  - Full product vision
  - Feature specifications
  - User stories
  - Success metrics

- **[GAME_RULES.md](GAME_RULES.md)** - Game Mechanics
  - Scoring system (501 darts rules)
  - Question types
  - Turn rules
  - Win conditions

---

## 🏗️ Architecture & Design

### Technical Design

- **[design/TECHNICAL_DESIGN.md](design/TECHNICAL_DESIGN.md)** - System Architecture
  - High-level architecture
  - Frontend design (SvelteKit)
  - Backend design (Spring Boot)
  - Database schema
  - WebSocket protocol
  - Security considerations

### Data Source Implementation

- **[design/SCRAPERFC_INTEGRATION.md](design/SCRAPERFC_INTEGRATION.md)** - Implementation Guide (30+ pages)
  - ScraperFC overview and API reference
  - Python microservice architecture
  - Database schema mapping
  - 4-phase implementation plan (7-12 days)
  - Code examples for all question types
  - Risk assessment and testing strategy

- **[design/SCRAPING_SERVICE_OPERATIONS.md](design/SCRAPING_SERVICE_OPERATIONS.md)** - Operations Guide
  - How the scraping service works
  - Service workflows (initial, weekly, manual)
  - Complete data flow diagrams
  - API endpoint specifications
  - Configuration and monitoring
  - Error handling

---

## 🛠️ Implementation

### Current Status (2026-01-18)

✅ **Completed**:
- Data source research and selection (ScraperFC)
- Proof of concept validation (603 EPL players scraped)
- Comprehensive documentation (3 design docs)

⏳ **Next Steps**:
- Python microservice implementation (3-5 days)
- Database integration (2-3 days)
- Deployment (1-2 days)

### Code Artifacts

- **[backend/scripts/](../backend/scripts/)** - Proof of Concept Scripts
  - `poc_validated.py` - ✅ Working PoC (PRIMARY)
  - `requirements.txt` - Python dependencies
  - `README.md` - Setup instructions

---

## 📖 Quick Reference

### Common Questions

**Q: How does the scraping service work?**
→ Read: [Scraping Service Summary](SCRAPING_SERVICE_SUMMARY.md)

**Q: What's the detailed implementation plan?**
→ Read: [ScraperFC Integration Guide](design/SCRAPERFC_INTEGRATION.md)

**Q: What have we implemented so far?**
→ Read: [Project Log](PROJECT_LOG.md)

**Q: What are the game rules?**
→ Read: [Game Rules](GAME_RULES.md)

**Q: What's the database schema?**
→ Read: [Technical Design - Database Schema](design/TECHNICAL_DESIGN.md#database-schema)

**Q: How do I run the proof of concept?**
→ Read: [backend/scripts/README.md](../backend/scripts/README.md)

---

## 🎯 Document Roadmap

### Essential Reading (Before Development)

1. [Project Log](PROJECT_LOG.md) - Current status
2. [Scraping Service Summary](SCRAPING_SERVICE_SUMMARY.md) - How it works
3. [ScraperFC Integration](design/SCRAPERFC_INTEGRATION.md) - Implementation details

### Reference Documents (During Development)

- [Technical Design](design/TECHNICAL_DESIGN.md) - Architecture reference
- [Scraping Service Operations](design/SCRAPING_SERVICE_OPERATIONS.md) - Operational workflows
- [Game Rules](GAME_RULES.md) - Game mechanics reference

---

## 📊 Project Timeline

### Phase 1: Planning & Design ✅ COMPLETE
- Product requirements defined
- Technical architecture designed
- Data source selected and validated

### Phase 2: MVP Development (Current)
- **Week 1-2**: Python microservice + database integration
- **Week 3**: Spring Boot backend (game engine, WebSocket)
- **Week 4**: SvelteKit frontend (basic UI)
- **Week 5**: Testing and deployment

### Phase 3: Beta Launch
- Limited user testing
- Performance optimization
- Bug fixes

---

## 🔗 External Resources

### Data Sources
- **ScraperFC**: https://scraperfc.readthedocs.io/
- **FBref**: https://fbref.com/
- **API-Football** (future): https://www.api-football.com/

### Technologies
- **Spring Boot**: https://spring.io/projects/spring-boot
- **SvelteKit**: https://kit.svelte.dev/
- **PostgreSQL**: https://www.postgresql.org/
- **FastAPI**: https://fastapi.tiangolo.com/

---

## 📝 Contributing

### Adding New Documentation

When creating new documents, follow this structure:

```markdown
# Document Title

**Version**: 1.0
**Date**: YYYY-MM-DD
**Status**: Draft | Review | Final

## Table of Contents
...

## Overview
...
```

### Document Types

- **Planning**: PRD, requirements, specifications
- **Design**: Architecture, technical design, implementation plans
- **Operations**: Deployment, monitoring, maintenance guides
- **Reference**: API docs, schemas, configuration guides

---

## 📧 Contact

For questions about this documentation:
- See: [CLAUDE.md](../CLAUDE.md) for project context
- Check: [Project Log](PROJECT_LOG.md) for recent changes

---

**Last Updated**: 2026-01-18
**Total Documents**: 8 (3 planning, 4 design, 1 implementation log)
**Status**: MVP Development Phase
