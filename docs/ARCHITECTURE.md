# AI Examination System (AES)

# System Architecture

Version: 2.0

Status: Approved

Audience:

- Codex
- GPT
- Claude
- Human Developers

This document defines the architecture of the AI Examination System (AES).

Every implementation must follow this architecture.

No module may violate the dependency rules defined below.

# 1. Architecture Goals

The architecture is designed around the following objectives:

- Modular
- Maintainable
- Extensible
- Testable
- Web-first
- Java-centered
- AI-assisted
- Campus LAN deployable

Architecture stability has higher priority than development speed.

# 2. Architectural Style

AES adopts a layered modular web architecture.

The system is not a single-page frontend with business logic hidden in components.

The system is divided into independent components communicating through well-defined interfaces.

Architecture Layers

Presentation Layer

Application Layer

Business Layer

Infrastructure Layer

Persistence Layer

Each layer only depends on the layer directly below it.

Reverse dependency is prohibited.

# 3. System Components

The project consists of the following components.

Web Frontend

Java Backend Service

MySQL Database

AI Integration Module

Optional Python AI Parsing Service

Shared Contracts

Configuration

Logging

Deployment

Testing

Every component should be independently replaceable.

# 4. Overall Architecture

```text
+---------------------------+
|        Web Browser        |
| Admin / Teacher / Student |
+-------------+-------------+
              |
              | HTTPS / REST API
              v
+-------------+----------------------------------+
| Java Backend Service                            |
| ------------------------------------------------|
| Authentication                                  |
| Authorization                                   |
| User Management                                 |
| Paper Management                                |
| Question Bank                                   |
| Examination Management                          |
| AI Integration                                  |
| Auto Grading                                    |
| Manual Grading                                  |
| Statistics                                      |
| Anti-Cheat Event Processing                     |
+-------------+-------------------+--------------+
              |                   |
              | JDBC              | HTTP
              v                   v
        MySQL 8+          Optional Python AI Service
                                  |
                                  | HTTP
                                  v
                             DeepSeek API
```

# 5. Project Directory

```text
exam-system/

backend/
    src/
        main/
        test/

frontend/
    src/

ai-service/

shared/

database/

docs/

tests/

scripts/

deploy/

assets/

logs/
```

The directory structure is considered part of the architecture.

Avoid changing it without approval.

# 6. Module Responsibilities

## Web Frontend

Responsible for:

- Login page
- Admin workspace
- Teacher workspace
- Student examination page
- Form validation for user experience
- API communication
- UI state management
- Token persistence in browser local storage for the current web session

Frontend never accesses the database directly.

Frontend never communicates directly with DeepSeek.

Frontend must not contain core business rules.

## Java Backend Service

Responsible for:

Authentication

Authorization

Business Logic

Question Bank

Examination

AI communication

Database access

Logging

Statistics

The backend is the only module allowed to access the database.

## AI Integration Module

Responsibilities

Build AI prompts

Call DeepSeek API

Validate AI response

Normalize AI output

Store AI logs

The AI integration module never writes final question data without teacher approval.

## Optional Python AI Parsing Service

Responsibilities

Complex Word extraction

Image extraction

AI-oriented segmentation

Prompt preparation assistance

This service is optional and replaceable.

It is never the source of truth.

It never accesses MySQL directly.

## MySQL Database

Responsible only for persistence.

No business logic should exist in database triggers.

## Shared Contracts

Contains:

DTO design

API conventions

Enums

Validation rules

Error code definitions

# 7. Layer Responsibilities

## Presentation Layer

Contains:

Vue pages

Vue components

Router

Store

No business logic.

No SQL.

No direct AI calls.

## Application Layer

Contains:

Spring controllers

Request DTOs

Response DTOs

Use case orchestration

## Business Layer

Contains:

Services

Business Rules

Validation

Grading Logic

Examination Logic

Question Logic

Business Layer must not depend on frontend code.

## Infrastructure Layer

Contains:

Repository implementation

External API client

File Storage

Configuration

Logging

Infrastructure may depend on third-party libraries.

## Persistence Layer

MySQL

Migration scripts

Backup

Only repositories access persistence.

# 8. Dependency Rules

Allowed

Frontend -> REST API -> Backend Controller -> Service -> Repository -> MySQL

Backend Service -> AI Client -> DeepSeek API

Backend Service -> Optional Python AI Service

Forbidden

Business -> Frontend

Database -> Frontend

AI Service -> Database

Browser -> Database

Browser -> DeepSeek API

Frontend -> MySQL

# 9. Communication Model

Browser

-> HTTP REST

-> Java Backend

-> Repository

-> MySQL

No direct database communication from browser clients.

No direct browser-to-AI communication in Version 2.0.

# 10. AI Architecture

The AI integration is isolated.

Teacher uploads Word

-> Backend stores original file

-> Backend extracts or delegates extraction

-> Backend builds structured prompt

-> DeepSeek API or Python AI Service

-> JSON Result

-> Validation

-> Teacher Review

-> Question Bank

AI output is never trusted directly.

Every AI result requires validation.

# 11. Anti-Cheat Architecture

Browser Examination Page

-> Event Collector

-> Behavior Events

-> Backend

-> Audit Log

-> Teacher Dashboard

Anti-cheat does not directly punish students.

Teachers decide penalties.

Browser-based anti-cheat is limited by browser security rules.

The system records evidence and risk signals rather than claiming complete device lockdown.

# 12. Automatic Grading Architecture

Student submits answers.

-> Backend receives submission.

-> Answer Validator

-> Grading Engine

-> Result Generator

-> MySQL

-> Teacher Review

-> Statistics

Objective grading is automatic.

Subjective grading requires teacher confirmation.

# 13. Logging Architecture

Every important operation generates logs.

Categories

Authentication

Operation

AI

Examination

Submission

Grading

Anti-cheat

System

Logs should be structured.

Application logs use Logback.

Business audit logs are persisted in MySQL.

# 14. Configuration Architecture

All configuration should reside outside source code.

Configuration examples

Database

AI API Key

Timeout

Upload path

School name

Server port

Configuration should support:

Development

Testing

Production

Secrets must not be committed to Git.

# 15. Error Handling

Every layer should handle only its own errors.

Frontend

Displays messages.

Business

Validates operations.

Infrastructure

Retries network requests when safe.

Database

Handles persistence exceptions.

No layer should expose internal exceptions directly.

# 16. Security Architecture

Passwords

BCrypt hashing.

Session

Phase 1 uses token-based sessions.

The backend generates a random token after login, stores only the token hash in `login_sessions`, and returns the raw token to the browser once.

The browser sends the token through the `Authorization: Bearer <token>` header.

Logout revokes the current session in the database.

API

Authentication required.

Phase 1 role access rules:

- `/api/admin/**` requires `admin`.
- `/api/teacher/**` requires `teacher`.
- `/api/student/**` requires `student`.
- `/api/auth/login`, `/api/health`, Swagger, and OpenAPI endpoints are public.

Database

Parameterized queries.

Configuration

Secrets separated from source code.

Logs

Sensitive data masked.

# 17. Scalability

Architecture should support future upgrades.

Future modules

Redis cache

OCR

Multiple AI providers

Object Storage

Message Queue

Cloud deployment

These modules should be pluggable.

# 18. Design Principles

Single Responsibility Principle

Open Closed Principle

Dependency Inversion Principle

Composition over Inheritance

Repository Pattern

Service Layer Pattern

Configuration First

Database First

API First

# 19. Version Strategy

Version 2.0

Java Web

MySQL

DeepSeek

Single School

Version 2.5

Enhanced AI grading

Version 3.0

Cloud-ready deployment

Version 4.0

Multi-school operation

# 20. Architectural Constraints

The following rules must never be violated.

Business logic must never exist inside frontend components.

Browser clients never access databases.

Browser clients never call AI providers directly.

AI never modifies persistence directly.

Repositories never contain business logic.

Services never directly manipulate frontend state.

Configuration values are never hardcoded.

Every new module must include tests.

Every new feature must include documentation.

Architecture changes require documentation updates.

This document is the authoritative architectural reference for AES.

# 21. Engineering Standards

Phase 0.5 establishes mandatory backend engineering conventions.

All backend business APIs return `ApiResponse<T>`.

Successful responses use:

- `success = true`
- `code = OK`
- `message = 请求成功`
- `data = response payload`
- `requestId = X-Request-ID / generated request id`
- `timestamp = UTC time`

Failed responses must use `ErrorCode`.

Known failures must be represented as:

- `BusinessException`
- `AuthException`
- Bean Validation violations

Unknown exceptions are handled by `GlobalExceptionHandler` and must not expose internal stack traces to clients.

Every request receives an `X-Request-ID` response header. Application logs include the MDC key `requestId`.

JSON time values use UTC ISO-8601 format. Timestamps must not be serialized as numeric arrays.

CORS policy is externalized under `aes.cors`.

Swagger UI is exposed at `/swagger-ui.html`. OpenAPI JSON is exposed at `/v3/api-docs`.

Backend package conventions:

```text
com.aes.exam.<module>.controller
com.aes.exam.<module>.service
com.aes.exam.<module>.repository
com.aes.exam.<module>.dto
com.aes.exam.<module>.entity
com.aes.exam.<module>.vo
com.aes.exam.<module>.mapper
```

Layer rules:

- DTO: receives client input.
- Entity: represents persisted or domain data shape.
- VO: returns data to clients.
- Mapper: converts DTO / Entity / VO through MapStruct.

Manual field-by-field mapping is allowed only when MapStruct is not suitable.

Common infrastructure lives under `com.aes.exam.common`.

# 22. Phase 1 Implementation Notes

The current vertical slice contains:

- Java authentication controller and service layer.
- JDBC repositories for users and login sessions.
- Flyway migration `V2__auth_core.sql`.
- Initial administrator bootstrap from external configuration.
- Vue router guards for login and role-specific page access.
- Admin, teacher, and student shell pages.

Phase 1 intentionally does not implement full user management yet. Creating teacher and student accounts belongs to the next business expansion of the user module.
