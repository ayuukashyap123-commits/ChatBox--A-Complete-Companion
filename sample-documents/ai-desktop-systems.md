# AI-Powered Desktop Systems: Architecture & Design

## Overview
Desktop applications provide significant advantages over browser-based interfaces for AI companion tools:
- **Low Latency**: Direct hardware access and zero network overhead for UI rendering.
- **Data Privacy**: Local-first database storage (SQLite) ensures sensitive user logs and memories never leave the machine.
- **Offline Reliability**: Deterministic offline fallbacks ensure companions remain useful even when disconnected.

## Core Architectural Pillars of Chatbox
1. **Digital Doppelgänger**: Persona profiles capturing identity, communication patterns, and values.
2. **Mental Wellness**: Grounding techniques, 4-7-8 breathing exercises, and longitudinal mood tracking.
3. **AI Life Coach**: Goal-oriented decomposition, actionable milestone tracking, and daily habit consistency.
4. **Second Brain**: Document knowledge ingestion, full-text parsing, and grounded context extraction.
5. **Memory Manager**: Explicit user consent and control over AI memories.

## Technology Decisions
- **JavaFX 21**: Declarative CSS styling, hardware-accelerated scene graphs, and rich animation framework.
- **SQLite with WAL Mode**: Concurrent read access and ACID transaction safety.
- **Apache PDFBox 3.x**: Robust document ingestion for PDF, Markdown, and plain text.
