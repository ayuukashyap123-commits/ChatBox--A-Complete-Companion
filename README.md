# Chatbox — A Complete Companion

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://adoptium.net)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-FF8000?logo=oracle&logoColor=white)](https://openjfx.io)
[![SQLite](https://img.shields.io/badge/Database-SQLite%203%20WAL-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org)
[![Build Status](https://img.shields.io/badge/Build-Passing%20(Maven)-success)](pom.xml)
[![Tests](https://img.shields.io/badge/Tests-65%2F65%20Passing-brightgreen)](src/test)

A professional, offline-first Java 21 desktop companion combining multi-mode conversational AI, longitudinal mental wellness tracking, life coaching and habit consistency, document intelligence (Second Brain), and privacy-centric user memory governance.

---

## Table of Contents
- [Project Overview & Objectives](#project-overview--objectives)
- [Companion Modules](#companion-modules)
- [Architecture & Technology Stack](#architecture--technology-stack)
- [Prerequisites](#prerequisites)
- [Build and Verification Commands](#build-and-verification-commands)
- [Application Launch Instructions](#application-launch-instructions)
  - [Option A: 1-Click Root Launcher (launch.bat)](#option-a-1-click-root-launcher-launchbat)
  - [Option B: Native Portable Windows Executable](#option-b-native-portable-windows-executable)
  - [Option C: Portable ZIP Distribution Archive](#option-c-portable-zip-distribution-archive)
  - [Option D: Standalone Shaded Executable Fat JAR](#option-d-standalone-shaded-executable-fat-jar)
  - [Option E: Development Run with Maven](#option-e-development-run-with-maven)
- [Portable Application vs. MSI Notice](#portable-application-vs-msi-notice)
- [First-Run Setup & Guidance](#first-run-setup--guidance)
- [AI Modes & Provider Setup](#ai-modes--provider-setup)
  - [1. Offline Demonstration Mode (Default)](#1-offline-demonstration-mode-default)
  - [2. Live Gemini API Configuration](#2-live-gemini-api-configuration)
  - [3. OpenAI-Compatible Providers](#3-openai-compatible-providers)
- [Document Intelligence (Second Brain) & OCR Limits](#document-intelligence-second-brain--ocr-limits)
- [Local Database Storage & Backup Guidance](#local-database-storage--backup-guidance)
- [Security & Privacy Architecture](#security--privacy-architecture)
- [Troubleshooting & Known Limitations](#troubleshooting--known-limitations)

---

## Project Overview & Objectives

**Chatbox — A Complete Companion** is an offline-capable native Java desktop application built as a senior capstone project. Unlike web wrappers, Electron containers, or hybrid web apps, Chatbox is implemented directly on **Java 21 LTS** and **JavaFX 21**, delivering instant cold starts, native desktop rendering, modest memory footprint, and true local data sovereignty.

### Primary Objectives:
1. **Holistic Student Companion**: Consolidate personal coaching, mental grounding, document study, and AI conversation into a single, cohesive desktop interface.
2. **Offline-First Resilience**: Operate reliably without network connectivity, providing deterministic rule-based offline responses and local productivity tracking.
3. **Transparent Privacy Governance**: Guarantee that private journal reflections never enter AI prompts and that only explicitly user-approved memories personalize AI responses.
4. **Zero Remote Dependencies for Core Operations**: Store all data, settings, history, and document embeddings in a local SQLite database (`~/.chatbox/chatbox.db`).

---

## Companion Modules

### 1. Unified Dashboard (`DashboardView`)
- Live summary metrics: active goal progress, completed tasks, habit consistency streaks, average 7-day mood rating, and imported document count.
- Quick navigation shortcuts and real-time AI provider connectivity status.

### 2. Multi-Mode AI Chatbox (`ChatController`)
- **Five Dedicated Conversation Modes**:
  - `GENERAL`: Conversational assistance, brainstorming, and writing support.
  - `DOPPELGANGER`: AI persona reflecting the user's declared interests, communication style, and ambitions.
  - `WELLNESS`: Compassionate, supportive, non-clinical reflective sounding board.
  - `COACH`: Action-oriented accountability partner focused on execution and momentum.
  - `KNOWLEDGE`: Grounded document question answering citing imported Second Brain files.
- Full multi-turn conversation persistence, session creation, renaming, and filtering.

### 3. Digital Doppelgänger (`DoppelgangerController`)
- Define identity parameters: display name, preferred moniker, occupation, personal bio, primary interests, communication style, and long-term goals.
- Provides system prompt personalization tokens to align AI responses with user individuality.

### 4. Mental Wellness Hub (`WellnessController`)
- **Visual Mood Check-In**: 5-point validated scale with contextual tags and optional qualitative reflection notes.
- **Private Journal**: Longitudinal reflective journal with mood correlation and local search.
- **Guided 4-7-8 Breathing**: Hardware-accelerated JavaFX pulsing circle for anxiety reduction and autonomic regulation.
- **5-4-3-2-1 Grounding**: Step-by-step cognitive behavioral sensory grounding exercise.

### 5. AI Life Coach (`CoachController`)
- **Goal Management**: Create and track goals across Career, Learning, Health, Personal, and Financial categories.
- **Automatic Progress Calculation**: Completing subtasks automatically recalculates the parent goal's progress bar (0–100%).
- **Daily Plan & Tasks**: Filter tasks by priority (Urgent, High, Medium, Low) and status (Todo, In Progress, Completed).
- **Habit Tracker**: Daily check-offs with streak counter to establish behavioral consistency.

### 6. Personal Second Brain (`KnowledgeController`)
- Document ingestion for `.txt`, `.md`, and `.pdf` files.
- Text parsing and passage ranking powered by Apache PDFBox 3.x.
- Passage search with keyword snippet extraction and direct grounding into AI chat sessions.

### 7. Memory Governance (`MemoryController`)
- Complete transparency: inspect every fact, preference, or goal the companion knows about you.
- **Strict Approval Gate**: Unapproved candidate memories are highlighted and blocked from AI context until approved by the user.
- Full CRUD operations: add, edit, toggle approval, or delete memories at will.

### 8. Settings & Provider Configuration (`SettingsController`)
- Select AI provider (`GEMINI` or `OPENAI_COMPATIBLE`).
- Configure custom API endpoints, model identifiers, and request timeouts (10–60 seconds).
- Test provider connectivity with live roundtrip verification.
- Runtime session key support (in-memory only) vs persistent key storage.
- Toggle explicit Offline Mode at any time.

---

## Architecture & Technology Stack

| Component | Technology | Version | Purpose |
|:---|:---|:---|:---|
| **Runtime** | OpenJDK | 21 LTS | Pattern matching, records, switch expressions, HttpClient |
| **Desktop UI** | JavaFX | 21.0.2 | Hardware-accelerated scene graph, native controls, CSS |
| **Local Storage** | SQLite JDBC | 3.45.3.0 | Embedded database with WAL mode and foreign key constraints |
| **Document Engine**| Apache PDFBox | 3.0.2 | PDF text parsing via modern `Loader` API |
| **JSON Parser** | Jackson | 2.17.1 | Serialization and deserialization with JavaTimeModule |
| **HTTP Transport** | Java HttpClient | Java 21 | Asynchronous HTTP/2 REST client with configurable timeouts |
| **Testing** | JUnit 5 | 5.10.2 | Unit, integration, and restart persistence testing |
| **Packaging** | Maven Shade & jpackage | 3.5.3 / JDK 21 | Executable fat JAR and native Windows app bundle |

---

## Prerequisites

- **Operating System**: Windows 10 / 11 (64-bit).
- **Java**: JDK 21 LTS (Eclipse Adoptium Temurin 21 recommended).
- **Maven**: Apache Maven 3.9+ (or configured wrapper).

Verify your environment:
```powershell
java -version
mvn -version
```

---

## Build and Verification Commands

Execute clean compilation and run the full 65-test automated test suite:
```powershell
mvn clean test
```

Package the application into an executable fat JAR:
```powershell
mvn package -DskipTests
```
*Output: `target/chatbox-companion-1.0.0.jar` (29.5 MB self-contained JAR).*

Generate the native Windows application bundle using `jpackage`:
```powershell
# Create staging input and build native app-image
if (Test-Path target/package-input) { Remove-Item -Recurse -Force target/package-input }
mkdir target/package-input
copy target/chatbox-companion-1.0.0.jar target/package-input/
jpackage --type app-image --name "Chatbox" --input target/package-input --main-jar chatbox-companion-1.0.0.jar --main-class com.chatbox.app.Main --dest dist --vendor "Chatbox" --app-version 1.0.0 --description "Chatbox - A Complete Companion"
```
*Output: `dist/Chatbox/Chatbox.exe` (Complete Windows desktop application with bundled OpenJDK 21 JRE).*

---

## Application Launch Instructions

### Option A: 1-Click Root Launcher (launch.bat)
*The simplest method to start the native application on Windows:*
Double-click `launch.bat` in the project root, or execute:
```cmd
launch.bat
```

### Option B: Native Portable Windows Executable
*Runs directly using the self-contained OpenJDK 21 runtime bundled inside `dist\Chatbox` (no external Java installation required):*
```powershell
.\dist\Chatbox\Chatbox.exe
```

### Option C: Portable ZIP Distribution Archive
*A standalone, portable release archive ready for distribution or relocation:*
- **Location:** [`dist/Chatbox-1.0.0-portable-win-x64.zip`](file:///c:/Users/AYUSH%20KASHYAP/Desktop/AI-%20chatbox/dist/Chatbox-1.0.0-portable-win-x64.zip) (approx. 80 MB)
- **How to use:**
  1. Extract `Chatbox-1.0.0-portable-win-x64.zip` to any folder (e.g. `C:\Users\<User>\Desktop\Chatbox`).
  2. Double-click `Chatbox\Chatbox.exe` to run.
  3. No administrative privileges or system installation required.

### Option D: Standalone Shaded Executable Fat JAR
*Requires Java 21 LTS installed on the system PATH:*
```powershell
java -jar target/chatbox-companion-1.0.0.jar
```

### Option E: Development Run with Maven
```powershell
mvn javafx:run
```

---

## Portable Application vs. MSI Notice

> [!NOTE]
> **This release is a Portable Windows Application Image, NOT an MSI installer.**
> 
> - **Self-Contained:** It bundles the required Java 21 runtime and native dependencies directly in `dist\Chatbox\runtime`, so end users do not need to install Java.
> - **Zero Installation Overhead:** It does not write to the Windows Registry or require administrator privileges.
> - **Self-Contained Data:** Your persistent data and settings are maintained in user space at `%USERPROFILE%\.chatbox\chatbox.db`, ensuring that deleting or updating the app folder never destroys your personal journals, habits, or memories.

---

## First-Run Setup & Guidance

1. **Automatic Initialization**: On first launch, Chatbox automatically creates `%USERPROFILE%\.chatbox\chatbox.db` and populates sample demonstration data (student profile, goal decomposition, sample habits, and welcome conversation).
2. **AI Provider Configuration**:
   - To connect Google Gemini: Open **Settings**, select **Google Gemini**, enter your API key, ensure the model is set to `gemini-3.8-flash` or `gemini-3.5-flash`, and click **Test Connection**.
   - Once verified, click **Save & Persist Key** to keep the key stored locally.
   - Alternatively, you can use **Runtime Key** mode (in-memory only, discarded on exit) or use **Offline Demonstration Mode** without an API key.
3. **Windows Defender SmartScreen**: Because this application image is built for academic capstone demonstration without a commercial code-signing certificate, Windows SmartScreen may show an initial prompt on new systems. Click **"More info"** $\rightarrow$ **"Run anyway"**.

---

## AI Modes & Provider Setup

### 1. Offline Demonstration Mode (Default)
When no API key is configured or when "Force Offline Mode" is toggled in Settings:
- The status indicator in the sidebar displays `○ Offline Demo`.
- All AI chat modes operate with a deterministic rule-based engine.
- Supported offline slash commands:
  - `/help` or `/commands`: Displays available offline commands.
  - `/plan` or `/study`: Generates a structured 3-phase study and focus schedule.
  - `/goals`: Outlines an actionable goal decomposition framework.
  - `/habits`: Provides behavioral consistency and habit-stacking guidance.
  - `/wellness`: Displays grounding steps and 4-7-8 breathing instructions.
  - `/profile`: Outputs your declared Digital Doppelgänger persona blueprint.

### 2. Live Gemini API Configuration
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. In the application:
   - Navigate to the **Settings** tab.
   - Select Provider: `Google Gemini`.
   - Default Model: `gemini-1.5-flash` (or `gemini-1.5-pro`).
   - Enter your API Key.
   - Click **Test Connection** to verify roundtrip connectivity.
   - Click **Save Settings**.
3. Alternatively, supply the key as an environment variable prior to launch:
   ```powershell
   $env:GEMINI_API_KEY="your-api-key-here"
   java -jar target/chatbox-companion-1.0.0.jar
   ```

### 3. OpenAI-Compatible Providers
Chatbox supports any OpenAI-compatible REST endpoint (e.g., LocalAI, vLLM, Ollama, Groq):
1. In **Settings**, choose `OpenAI-Compatible`.
2. Set Endpoint URL (e.g., `http://localhost:11434/v1/chat/completions` or `https://api.openai.com/v1/chat/completions`).
3. Set Model Name (e.g., `llama3`, `gpt-4o-mini`).
4. Enter Bearer API Key (optional for local Ollama).
5. Click **Test Connection** to validate.

---

## Document Intelligence (Second Brain) & OCR Limits

### Supported Document Formats
- **Plain Text (`.txt`)**: Full text ingestion with word counting.
- **Markdown (`.md`)**: Full text parsing and structural heading recognition.
- **PDF Documents (`.pdf`)**: Native text stream extraction using Apache PDFBox 3.x.

### OCR Limitation Notice
> [!IMPORTANT]
> **Text Extraction Boundary**: Chatbox relies on Apache PDFBox to extract digital text streams from PDF files. Scanned paper documents, photographs, or bitmap images inside PDFs that lack an embedded OCR text layer cannot be parsed without external OCR pre-processing (such as Tesseract). Use digitally authored PDFs, exported lecture slides, or markdown notes for best results.

---

## Local Database Storage & Backup Guidance

### Database Location
The persistent SQLite database is located at:
```
Windows: %USERPROFILE%\.chatbox\chatbox.db
```
*(Full path example: `C:\Users\<Username>\.chatbox\chatbox.db`)*

### Backup and Restore
Because Chatbox is a single-file SQLite database with Write-Ahead Logging:
1. **To Backup**: Exit the application, then copy the folder `%USERPROFILE%\.chatbox\` to your backup drive or cloud storage.
2. **To Restore**: Replace `%USERPROFILE%\.chatbox\chatbox.db` with your backup copy.
3. **To Reset**: Delete `~/.chatbox/chatbox.db` while the app is closed. On the next launch, Chatbox will automatically create a fresh schema and populate default sample items.

---

## Security & Privacy Architecture

- **Zero Hardcoded Secrets**: The application source code contains no API keys or credentials.
- **Parameterized SQL**: All database operations use `PreparedStatement` with parameterized `?` bindings to prevent SQL injection.
- **Journal Privacy Isolation**: Entries written in the **Mental Wellness Journal** are strictly confined to `WellnessRepository`. They are **never** queried by `AiService`, never passed to external AI providers, and never indexed into Second Brain search passages.
- **Memory Consent Gating**: Memories discovered or recorded are only injected into AI system prompts if `isApproved == true`. Unapproved memories are excluded.
- **Honest Key Storage Disclosure**: API keys saved in Settings are stored locally in plaintext within `~/.chatbox/chatbox.db`. They are protected by operating system user permissions. If operating on a shared public terminal, use runtime session keys or set the `GEMINI_API_KEY` environment variable instead of saving the key to disk.

---

## Troubleshooting & Known Limitations

| Symptom | Cause | Resolution |
|:---|:---|:---|
| **App fails to start via Maven** | Java version is older than 21 | Install OpenJDK 21 LTS and verify with `java -version`. |
| **AI Status shows "Offline Demo"** | No API key entered or offline toggle enabled | Go to Settings, enter key, ensure "Force Offline" is unchecked, and click "Test Connection". |
| **"Authentication failed (HTTP 401)"** | Invalid Gemini or OpenAI API key | Check for extra whitespace or expired API key in Settings. |
| **"Rate limit exceeded (HTTP 429)"** | Provider quota exhausted | Wait 60 seconds before retrying, or switch model in Settings. |
| **Document import shows 0 words** | Scanned/image-only PDF | Ensure PDF contains digital selectable text (see OCR limitation). |
| **Database locked error** | Another instance is already running | Close existing Chatbox processes via Task Manager and restart. |
