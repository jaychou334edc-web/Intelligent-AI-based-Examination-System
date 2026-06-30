# AI Examination System (AES)

> Project Requirement Document
>
> Version: 2.0
>
> Status: Approved
>
> Audience:
>
> - Codex
> - GPT
> - Claude
> - Gemini
> - Human Developers

# 1. Project Overview

## 1.1 Project Description

AI Examination System (AES) is a Java-based web intelligent examination platform designed primarily for deployment in university computer laboratories and campus LAN environments.

The system enables teachers to upload examination papers in Microsoft Word format without requiring predefined templates. An integrated AI parsing engine converts the uploaded paper into structured examination data, allowing teachers to review, edit if necessary, and publish examinations for students.

Students take examinations through a browser-based examination page. Objective questions are graded automatically, while subjective questions are graded by teachers with optional AI-generated scoring suggestions.

The system is intended to be modular, maintainable, extensible, and suitable for school server deployment.

# 2. Project Goals

The project aims to build a complete intelligent web examination platform with the following capabilities:

- Administrator web workspace
- Teacher web workspace
- Student browser examination workspace
- AI-assisted Word examination parsing
- Question bank management
- Examination management
- Automatic grading
- Manual grading
- AI-assisted subjective grading
- Anti-cheating behavior logging
- Result statistics
- Local school server deployment
- Web deployment package

# 3. Target Platform

Server Operating System

Windows Server / Linux

Client Runtime

Modern browser

Recommended Browser

Chrome / Edge

Primary Deployment

University Computer Laboratory

Execution Mode

Web Application

# 4. Technical Stack

Backend Language

Java 21

Backend Framework

Spring Boot 3.x

Security

Spring Security

Database

MySQL 8+

ORM / Data Access

MyBatis-Plus or Spring Data JPA

Frontend

Vue 3

TypeScript

Vite

Element Plus

AI Service

DeepSeek API

Word Parsing

Apache POI

Optional AI Parsing Worker

Python 3.13

FastAPI

python-docx

Configuration

Spring Boot external configuration

Logging

SLF4J + Logback

Testing

JUnit 5

Mockito

Packaging

Maven

Frontend static build

Deployment

JAR / ZIP / Docker Compose optional

Version Control

Git

# 5. High-Level Architecture

The project consists of the following independent modules:

Web Frontend

Java Backend Service

MySQL Database Layer

AI Parsing Module

Optional Python AI Parsing Service

Automatic Grading Engine

Manual Grading Center

Question Bank

Authentication

Authorization

Statistics

Anti-Cheat Event Module

System Configuration

Every module should remain independently maintainable.

# 6. User Roles

Administrator

Responsible for:

- User management
- Permission management
- System configuration
- Backup
- Log management

Teacher

Responsible for:

- Upload Word papers
- AI parsing confirmation
- Question management
- Publish examinations
- Grade subjective questions
- View statistics

Student

Responsible for:

- Login
- Participate in examinations
- Submit answers
- View authorized results

# 7. Core Functional Requirements

## Authentication

- Login
- Logout
- Password reset
- Password encryption
- Session management
- Role-based routing

## User Management

- Create users
- Edit users
- Delete users
- Import students
- Export users
- Disable users

## Word Paper Upload

Teachers upload Word (.docx) examination papers.

Requirements:

Support arbitrary layout.

Support:

- Paragraphs
- Tables
- Images
- Mixed content

The original Word file must always be retained.

## AI Paper Parsing

The system calls DeepSeek API to parse uploaded Word documents.

The parser should identify:

- Exam title
- Question number
- Question type
- Question body
- Images
- Options
- Standard answer
- Score
- Knowledge point if detectable

The AI output must be converted into structured JSON.

Teachers must confirm the parsing result before importing it into the question bank.

AI results must never be accepted automatically.

The main backend is Java. Python may be introduced only for AI parsing or document extraction where it provides clear engineering value.

## Question Bank

Support:

- Add
- Edit
- Delete
- Archive
- Search
- Filter
- Categorize
- Random selection
- Duplicate detection

Every imported question originates from a Word document or manual teacher creation.

Questions may later evolve independently.

## Examination Management

Teachers can:

- Create examination
- Select questions
- Configure duration
- Configure participants
- Configure start/end time
- Publish examination

Students can only access published examinations.

## Student Browser Examination

The examination page should support:

- Browser fullscreen request
- Countdown timer
- Auto-save
- Navigation between questions
- Mixed question types
- Image display
- Resume after network interruption where possible
- Page visibility and focus monitoring

## Automatic Grading

Automatically grade:

- Single choice
- Multiple choice
- True/False
- Fill-in-the-blank

Support:

- Partial credit
- Case-insensitive comparison
- Whitespace normalization
- Configurable grading rules

## Manual Grading

Teachers should be able to:

- Review subjective answers
- Assign scores
- Write comments
- View AI-recommended scores
- Modify AI recommendations

Teachers always have final authority.

## Result Statistics

Generate:

- Average score
- Maximum score
- Minimum score
- Ranking
- Question accuracy
- Knowledge point statistics

Export:

Excel

PDF

## Anti-Cheat

The system should record examination behavior.

Recommended monitoring:

- Browser focus loss
- Fullscreen exit
- Copy / paste attempts
- Page refresh
- Tab hidden
- Unexpected disconnect
- Examination interruption

Each event should generate an audit log.

The teacher decides how cheating events affect grades.

# 8. AI Integration Requirements

DeepSeek API is used only for:

Word Parsing

Subjective Grading Suggestion

Future Expansion

AI must never:

Modify database directly.

Publish examinations.

Assign final grades.

Delete data.

Teachers remain responsible for all final decisions.

# 9. Non-Functional Requirements

Backend startup time < 15 seconds.

Page first load < 3 seconds in campus LAN.

Automatic grading < 5 seconds.

Word parsing < 30 seconds for normal papers.

System logs every important operation.

Passwords must be encrypted.

Sensitive configuration must not be hardcoded.

Initial target: 100 concurrent students.

# 10. Development Constraints

The following technologies are mandatory unless approved otherwise.

Java 21

Spring Boot 3.x

Spring Security

MySQL 8+

Vue 3

TypeScript

DeepSeek API

Apache POI

Optional Python service for AI parsing only

# 11. Project Directory

```text
exam-system/

backend/
    src/

frontend/
    src/

ai-service/

database/

shared/

docs/

tests/

scripts/

deploy/

assets/

logs/
```

# 12. Development Principles

The project should follow these principles:

- Modular architecture
- Separation of concerns
- Database-first design
- API-first communication
- Configuration-driven behavior
- Extensive logging
- Maintainable code
- Replaceable AI provider
- Testable components

Business logic must never reside inside frontend components.

# 13. Development Priorities

Every phase must have:

- Runnable system checkpoint
- Definition of Done
- Test verification
- Documentation update
- Git commit reminder
- Git tag after completion

Recommended Git tags:

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

## Phase 0

Project framework

Database

Backend skeleton

Frontend skeleton

Health check

## Phase 0.5

Engineering standards

Unified API response

Global exception handling

Error codes

Request ID logging

Bean Validation

Swagger / OpenAPI

Pagination

DTO / VO / Entity layering

MapStruct

Global time format

CORS

## Phase 1

Authentication

User management

## Phase 2

Word upload

AI parsing

Question bank

## Phase 3

Examination management

Teacher workspace

Student examination page

## Phase 4

Automatic grading

Manual grading

Statistics

## Phase 5

Anti-cheating behavior logging

System optimization

## Phase 6

Deployment

Backup

Release packaging

# 14. Explicit Non-Goals

The following features are out of scope for Version 2.0:

- Native mobile applications
- Desktop client implementation
- AI-based face recognition
- AI-based eye tracking
- Blockchain
- Multi-school synchronization
- Online payment
- Full operating-system-level browser lockdown

These features may be considered in future versions.

# 15. Definition of Done

Version 2.0 is considered complete when all of the following are achieved:

- Teacher can upload arbitrary Word papers.
- AI successfully parses Word into structured examination data.
- Teachers can review and edit parsed results.
- Examinations can be published.
- Students can complete examinations through a browser.
- Objective questions are graded automatically.
- Subjective questions can be graded manually with AI assistance.
- Examination behavior is logged.
- Results can be exported.
- The project can be deployed as a Java web application.

# 16. Important Notes for AI Coding Assistants

Before implementing any feature:

1. Preserve modular architecture.
2. Do not modify project structure without approval.
3. Do not hardcode configuration values.
4. Keep business logic outside frontend components.
5. Write maintainable and well-documented code.
6. Prefer extensibility over shortcuts.
7. Follow existing coding conventions.
8. Avoid introducing unnecessary dependencies.
9. Ensure all new functionality is testable.
10. Update documentation when major architectural changes occur.

This document serves as the primary development guideline for all future implementation.
