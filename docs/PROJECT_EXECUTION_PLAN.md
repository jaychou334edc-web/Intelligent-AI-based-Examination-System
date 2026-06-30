# AI Examination System (AES)

# Project Execution Plan

Version: 2.1

Status: Execution Strategy Document

Audience:

- Codex
- AI Coding Assistants
- Human Developers
- System Architects

# 1. Execution Philosophy

This project must not be developed in a big bang approach.

Instead, it must follow an incremental, vertical-slice delivery model:

Each phase must produce a runnable, testable web system.

Rules:

- No phase is allowed to produce non-executable code.
- Every phase must end with a stable checkpoint.
- No module is considered complete without integration testing.
- AI features are introduced only after core system stability.
- Every phase must have a Definition of Done.
- Every phase must end with a Git commit reminder.
- Every completed phase should be marked with a Git tag.

# 2. Version Tag Strategy

Each phase uses one Git tag after its Definition of Done is satisfied.

Recommended tags:

```text
v0.1.0-phase0
v0.1.5-phase0.5
v0.2.0-phase1
v0.3.0-phase2
v0.4.0-phase3
v0.5.0-phase4
v0.6.0-phase5
v1.0.0-release
```

Rules:

- Do not create a tag before tests pass.
- Do not create a tag before documentation is updated.
- Do not create a tag for broken builds.
- If remote push fails because of environment restrictions, provide manual Git commands to the user.

# 3. Overall Development Strategy

The system will be built in 7 phases:

Phase 0 -> Foundation Setup

Phase 0.5 -> Engineering Standards

Phase 1 -> Core System Skeleton

Phase 2 -> AI Question Import + Paper Parsing

Phase 3 -> Examination + Student Flow

Phase 4 -> Grading System

Phase 5 -> Anti-Cheat + Analytics

Phase 6 -> Deployment + Release

# 4. Phase 0 - Project Foundation

## Goal

Establish a clean, maintainable, production-grade Java web project structure.

## Deliverables

- Project directory structure
- Spring Boot backend skeleton
- Vue 3 frontend skeleton
- MySQL connection configuration
- Flyway migration baseline
- Logging system bootstrap
- External configuration templates
- Basic authentication stub
- CI-ready structure
- Optional Python AI service placeholder

## Output Requirement

System must be runnable:

- Backend starts successfully.
- Frontend dev server starts successfully.
- Backend health API works.
- Frontend can call backend health API.

## Definition of Done

Phase 0 is complete only when:

- `backend/` project exists and compiles.
- `frontend/` project exists and starts.
- `database/` contains initial migration scripts.
- `deploy/` contains environment configuration templates.
- Backend `/api/health` endpoint returns success.
- Frontend health page displays backend health result.
- Basic backend test passes.
- Basic frontend build or type check passes when dependencies are available.
- Documentation reflects the actual project structure.
- Git commit is ready.
- Git tag `v0.1.0-phase0` is ready after commit.

# 5. Phase 0.5 - Engineering Standards

## Goal

Build cross-cutting engineering infrastructure before business features expand.

This phase prevents later large-scale refactoring.

## Deliverables

- Unified API response body
- Unified error code system
- Global exception handling
- Request ID logging with MDC
- Bean Validation integration
- Swagger / OpenAPI documentation
- SpringDoc integration
- Unified pagination object
- DTO / VO / Entity layering rules
- MapStruct object mapping
- Global time format
- CORS configuration
- Basic security filter structure

## Definition of Done

Phase 0.5 is complete only when:

- All backend API responses use `ApiResponse`.
- All known business failures use `ErrorCode`.
- `GlobalExceptionHandler` handles validation, auth, business, and unknown errors.
- Every request has a request ID in logs.
- Bean Validation works for controller request DTOs.
- Swagger / OpenAPI page is accessible.
- Pagination request and response objects exist.
- Entity, DTO, VO, Mapper package conventions are documented and used.
- MapStruct sample mapping works.
- Global JSON time format is consistent.
- CORS policy is configured through external config.
- Unit tests cover response body, error handling, and validation.
- Documentation is updated.
- Git commit is ready.
- Git tag `v0.1.5-phase0.5` is ready after commit.

# 6. Phase 1 - Core System Skeleton

## Goal

Build a minimal but complete vertical system loop.

## Features

- User login
- Role system
- Basic API communication
- Session or token management
- Basic role-based frontend routing
- Basic admin / teacher / student shell pages

## Deliverables

- Login system fully working
- Role-based routing
- Backend API connected
- MySQL tables active
- Initial administrator account

## Definition of Done

Phase 1 is complete only when:

- User tables are created by migration scripts.
- Initial administrator account can be created.
- Passwords are stored with BCrypt.
- Login API works.
- Logout API works.
- Current user API works.
- Role-based access control works.
- Admin, teacher, and student route shells exist.
- Unauthorized users cannot access protected routes.
- Backend tests pass.
- Frontend build or type check passes when dependencies are available.
- API documentation is updated.
- Git commit is ready.
- Git tag `v0.2.0-phase1` is ready after commit.

# 7. Phase 2 - AI Question Import + Paper Parsing

## Goal

Enable AI-assisted question import from Word and text source files.

## Features

- Word (.docx) and text (.txt) upload
- File storage system
- Java Apache POI extraction for Word paragraphs, tables, and image placeholders
- UTF-8 text extraction for txt files
- Optional Python LangChain / OCR / visual parsing service if it provides clear value later
- AI parsing pipeline integration with DeepSeek
- Chunking engine
- AI JSON generation using the short-field question import protocol
- Validation layer
- Teacher review web UI

## Deliverables

- Upload Word or TXT source file
- Parse into structured questions
- Display parsed questions
- Teacher approves or edits
- Save into question bank

## Definition of Done

Phase 2 is complete only when:

- Word and TXT upload succeeds.
- Original source file is retained.
- Apache POI extraction succeeds.
- TXT extraction succeeds.
- Images embedded in Word are represented as stable `[IMG:image_n]` placeholders.
- DeepSeek returns JSON or configured mock fallback works in development.
- DeepSeek prompt follows the short-field protocol `t/c/o/a/e/s`.
- JSON schema validation passes.
- Invalid AI output is rejected and logged.
- Teacher review page can edit parsed questions, answers, options, analysis, and score.
- Approved questions enter official question bank tables.
- AI raw output remains in AI temporary tables.
- DeepSeek API Key stays in ignored local config or environment variables, never in committed code.
- Unit tests pass.
- Integration tests cover upload, parse result, and import flow.
- Frontend build passes.
- API documentation is updated.
- Git commit is ready.
- Git tag `v0.3.0-phase2` is ready after commit.

## Current Phase 2 Implementation Note

The current implementation uses Java as the source of truth and follows a LangChain-style chain inside the backend:

```text
Document extraction -> chunking -> prompt building -> DeepSeek call -> JSON mapping -> validation -> teacher review -> official import
```

The local development implementation can still use a rule-based mock parser when `AES_AI_MOCK_ENABLED=true`.

This satisfies the runnable local review flow before a real DeepSeek API key is configured.

When DeepSeek is enabled, the Java backend must keep the same validation, temporary AI storage, teacher review, and official import path. Python LangChain may be introduced later only as an internal optional service for OCR, vision, or complex layout preprocessing; it must not access MySQL or write final business data.

# 8. Phase 3 - Examination System

## Goal

Complete examination lifecycle.

## Features

- Exam creation
- Question selection from bank
- Exam publishing
- Student exam joining
- Countdown timer
- Auto-save answers
- Submission system

## Deliverables

- Teacher creates exam
- Student joins exam through browser
- Student submits exam
- Data stored correctly

## Definition of Done

Phase 3 is complete only when:

- Teacher can create an exam. Implemented in `/api/teacher/exams` and the teacher exam management page.
- Teacher can select questions. Implemented through question bank selection on draft exams.
- Teacher can publish an exam. Publishing locks the draft and assigns it to active student users.
- Published exams appear for assigned students. Implemented in `/api/student/exams`.
- Student can enter exam page. Entering creates or resumes a submission.
- Countdown timer works. The frontend counts down from backend `submissionStartedAt`.
- Auto-save works. The frontend silently saves the current answer after answer changes and before navigation.
- Final submit works. Submitted exams are locked against further answer changes.
- Submitted answers are persisted. Answers are stored in `submission_answers`.
- Browser refresh or short interruption does not lose saved answers. Reloading the exam restores saved answers and remaining time.
- Backend tests pass.
- Frontend build or type check passes when dependencies are available.
- API documentation is updated.
- Git commit is ready.
- Git tag `v0.4.0-phase3` is ready after commit.

## Current Phase 3 Implementation Note

Phase 3 delivers the runnable examination lifecycle:

```text
Teacher creates draft -> selects question bank items -> publishes exam -> student enters -> answers are auto-saved -> student submits
```

Objective auto-grading and teacher manual grading are intentionally left for Phase 4 so that the exam-taking flow remains stable before score computation expands.

# 9. Phase 4 - Grading System

## Goal

Introduce automatic and manual grading system.

## Features

### Automatic Grading

- Single choice
- Multiple choice
- True/False
- Fill-in-the-blank

### Manual Grading

- Subjective grading UI
- AI suggested score
- Teacher final confirmation

### Grading Engine

- Rule-based scoring engine
- Extensible scoring strategy pattern

## Deliverables

- Fully computed exam scores
- Teacher override support
- Score persistence

## Definition of Done

Phase 4 is complete only when:

- Objective grading works for supported question types. Implemented for single choice, multiple choice, and true/false on submission.
- Fill blank matching rules are configurable. Implemented behind `aes.grading.auto-grade-fill-blank`, defaulting to manual review.
- Subjective answers appear in grading UI. Implemented in the teacher grading page.
- Teacher can assign and modify subjective scores. Implemented through manual score save.
- AI suggested score is optional and never final by itself. Phase 4 stores room for AI suggestion fields but does not allow AI to finalize grades.
- Final score is persisted. Implemented through `grading_records.final_score` and `submissions.total_score`.
- Grading records are traceable. Implemented through one grading record per submission answer with status, grader, comment, and timestamps.
- Statistics can read final scores. `submissions.total_score` and `graded_at` are now available for Phase 5 analytics.
- Backend tests pass.
- Frontend build or type check passes when dependencies are available.
- API documentation is updated.
- Git commit is ready.
- Git tag `v0.5.0-phase4` is ready after commit.

## Current Phase 4 Implementation Note

Phase 4 delivers the runnable grading and result workflow:

```text
Student submits exam -> objective answers are auto-graded -> pending answers enter teacher grading -> teacher saves final score -> student views result
```

The current classroom behavior follows the user's requirement: the system automatically grades choices and true/false questions, while fill blank and subjective/code answers wait for teacher confirmation by default.

Fill blank auto-grading exists as a configurable backend capability for later school policy changes, but it is disabled by default.

AI suggested scoring is intentionally not active yet. The database contains optional AI suggestion fields, but Phase 4 keeps the teacher as the only final authority for non-objective answers.

# 10. Phase 5 - Anti-Cheat + Analytics

## Goal

Add monitoring and reporting layer.

## Features

- Browser focus detection
- Fullscreen exit logging
- Tab visibility logging
- Copy / paste attempt logging
- Page refresh logging
- Network interruption logging
- Event logging system
- Exam behavior timeline

## Analytics

- Score distribution
- Question accuracy
- Difficulty analysis
- Knowledge point statistics

## Definition of Done

Phase 5 is complete only when:

- Browser focus loss is logged.
- Fullscreen exit is logged.
- Tab visibility changes are logged.
- Copy / paste attempts are logged where browser permits.
- Network interruption is recorded where detectable.
- Teacher can view behavior timeline.
- Teacher can view score distribution.
- Teacher can view question accuracy.
- Teacher can view knowledge point statistics.
- Backend tests pass.
- Frontend build or type check passes when dependencies are available.
- API documentation is updated.
- Git commit is ready.
- Git tag `v0.6.0-phase5` is ready after commit.

# 11. Phase 6 - Deployment + Release

## Goal

Production-ready web delivery.

## Features

- Maven production build
- Vue production build
- Static frontend served by backend or Nginx
- MySQL initialization scripts
- Deployment configuration templates
- Backup / restore scripts
- Optional Docker Compose deployment
- Release ZIP generation

## Deliverables

- Backend JAR
- Frontend build output
- Database schema scripts
- Deployment configuration
- Optional Python AI service package
- Deployment guide

## Definition of Done

Phase 6 is complete only when:

- Backend production JAR builds successfully.
- Frontend production build succeeds.
- MySQL initialization script works.
- External configuration templates are complete.
- Deployment guide is updated.
- Backup script exists.
- Restore script exists.
- Release ZIP can be generated.
- System can be deployed on a school server and accessed from lab browsers.
- Final smoke test passes.
- Git commit is ready.
- Git tag `v1.0.0-release` is ready after commit.

# 12. Critical Engineering Rules

## 12.1 No Big Bang Development

Do not implement:

- AI parsing + grading + anti-cheat at once
- UI + backend + database without a runnable checkpoint

## 12.2 Each Phase Must Be Runnable

At the end of each phase:

- Backend must start.
- Frontend must start when frontend exists.
- No broken dependencies allowed.
- No placeholder-only modules.

## 12.3 AI Features Are Not Core Runtime Authority

AI is:

- a helper service
- not system authority

## 12.4 Backend is the Only Source of Truth

Browser clients:

- Never access database.
- Never call AI provider directly.
- Only call backend APIs.

## 12.5 Strict Incremental Expansion

Each phase can only:

- Add new modules.
- Extend existing modules.

Not:

- Rewrite architecture.
- Replace database schema without migration plan.

# 13. Risk Control Strategy

## High Risk Areas

### AI Parsing

Mitigation:

- Validation layer
- Retry mechanism
- Manual review fallback

### Browser Anti-Cheat

Mitigation:

- Event logging and warning first
- Avoid claiming complete OS-level control
- Teacher decides penalty

### Grading System

Mitigation:

- Rule-based first
- AI suggestion second

### Deployment

Mitigation:

- Early production build testing from Phase 1
- Keep environment variables external

# 14. Success Definition

Project is considered successful when:

- System runs on a school server.
- Teachers can upload Word exams.
- AI converts them into structured questions.
- Students can take exams through a browser.
- System grades objective questions automatically.
- Teachers can manually adjust grades.
- Results can be exported.
- System has deployable release artifacts.

# 15. Final Execution Principle

Build vertically, not horizontally.

Every phase must feel like a small complete product.

Never build unfinished subsystems.
