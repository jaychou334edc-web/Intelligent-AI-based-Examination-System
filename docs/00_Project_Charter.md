# Project Charter

Project Name:
AI Examination System (AES)

Version:
2.0.0

Status:
Approved

Document Type:
Engineering Charter

Audience:
Developers / AI Coding Assistant / Architects / Maintainers

Last Updated:
2026-06-30

# 1. Project Vision

## 1.1 Vision

The AI Examination System (AES) is a web-based intelligent examination platform designed for university computer laboratories and campus LAN deployment.

The system focuses on providing a browser-accessible, AI-assisted examination environment capable of:

- AI-based Word examination paper parsing
- Teacher and administrator web workspaces
- Student browser examination workspace
- Question bank management
- Examination management
- Automatic grading
- Manual grading workflow
- Anti-cheating behavior logging
- Result statistics and exporting
- Campus LAN deployment
- Future cloud compatibility

AES is not a simple online quiz website.

AES is designed as a complete intelligent examination platform.

## 1.2 Core Philosophy

The system shall follow five core principles.

### Principle 1

Web First

The system is designed primarily as a Java-based web application.

The browser is the primary user runtime.

### Principle 2

AI Assisted

Artificial Intelligence assists teachers.

Artificial Intelligence never replaces teacher decisions.

Examples:

- AI parses Word documents.
- AI recommends subjective scores.
- Teachers confirm final scores.

### Principle 3

Campus LAN First

The system must run inside a school network.

Internet connectivity is required only for external AI model invocation.

All examination data shall remain locally stored in the school database.

### Principle 4

Modular Architecture

Every subsystem must be independently replaceable.

Examples:

- AI provider
- Database
- Authentication
- Parser
- UI frontend
- Anti-cheat event collector

No subsystem shall tightly depend on another implementation.

### Principle 5

Maintainability

The project is expected to evolve over multiple versions.

Architecture decisions shall always prioritize maintainability over rapid implementation.

# 2. Project Goals

The project shall implement an intelligent web examination platform.

Major objectives include:

- Administrator management
- Teacher management
- Student management
- Word paper importing
- AI paper parsing
- Question bank management
- Examination management
- Automatic grading
- Manual grading
- Anti-cheating behavior logging
- Examination statistics
- Report exporting
- Web deployment package generation

# 3. Project Scope

## Included

Web Frontend

Java Backend Service

MySQL Database

Optional Python AI Parsing Service

AI Integration Service

Automatic Grading Engine

Manual Grading Center

Question Bank

User Management

Role Management

Permission Management

Exam Management

Anti-cheating Event Logging

Logging

System Configuration

Backup

Recovery

Deployment Scripts

## Excluded

The following features are intentionally outside Version 2.0.

Online payment

Mobile native app

Cloud SaaS multi-tenant operation

Live video invigilation

Face recognition

Large Language Model training

OCR model training

Distributed cluster deployment

Multi-school synchronization

Blockchain

# 4. Stakeholders

Primary Users

Administrator

Teacher

Student

Developer

AI Coding Assistant

System Maintainer

# 5. Success Criteria

Version 2.0 is considered successful if all following requirements are met.

Teacher can upload arbitrary Word papers.

AI successfully converts Word into structured examination data.

Teacher can review AI parsing results.

Teacher publishes examination.

Students complete examinations through a browser.

Objective questions are graded automatically.

Subjective questions are manually graded.

Final scores are generated.

Results are exportable.

System can be deployed as a Java web application in a school server environment.

# 6. Design Principles

The following principles shall never be violated.

Single Responsibility Principle

Open Closed Principle

Dependency Injection

Layered Architecture

Database First

API First

Configuration Driven

Logging First

Testable Components

No Business Logic Inside UI

# 7. Architecture Overview

AES consists of six independent subsystems.

Web Frontend

Java Backend Service

MySQL Database

AI Integration Module

Optional Python AI Parsing Service

Anti-cheat Event Module

Each subsystem communicates through clearly defined interfaces.

No subsystem directly manipulates another subsystem's internal state.

# 8. Deployment Model

Recommended deployment topology.

School Server

-> Java Backend

-> MySQL Database

-> Optional Python AI Service

-> DeepSeek API

Computer Lab Browsers

Students and teachers never directly access the database.

Browsers never communicate directly with DeepSeek.

All requests pass through the Java Backend Service.

# 9. Technology Stack

Backend Language

Java 21

Backend Framework

Spring Boot 3.x

Security

Spring Security

ORM / Data Access

MyBatis-Plus or Spring Data JPA

Database

MySQL 8+

Frontend

Vue 3

TypeScript

Vite

Element Plus

AI

DeepSeek API

Word Parsing

Apache POI

Optional Python Parsing Service

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

Frontend unit tests where needed

Packaging

Maven

Frontend static build

Deployment

JAR / ZIP / Docker Compose optional

Version Control

Git

# 10. Development Principles

Every feature shall satisfy:

Maintainable

Replaceable

Testable

Documented

Logged

Configurable

No hard-coded business logic.

No duplicated code.

No circular dependency.

# 11. Version Strategy

Version 2.0

Core web examination platform.

Version 2.5

AI grading enhancement.

Version 3.0

Cloud synchronization.

Version 4.0

Multi-school deployment.

# 12. AI Development Rules

Every AI coding assistant participating in development shall follow these rules.

Never modify project architecture without approval.

Never rename database tables without documentation update.

Never change API contracts without documentation update.

Never modify folder structure without approval.

Never introduce new frameworks without approval.

Always write tests.

Always update documentation.

Always keep backward compatibility when possible.

# 13. Non-Functional Requirements

Availability

>=99%

Backend startup time

<15 seconds

Page first load

<3 seconds in campus LAN

Word parsing

<30 seconds for normal papers

Exam submission

<2 seconds in campus LAN

Automatic grading

<5 seconds

Concurrent student support

Initial target: 100 concurrent students

Browser support

Modern Chrome / Edge

# 14. Security Objectives

Passwords shall never be stored in plaintext.

Sensitive configuration shall not be committed to source code.

Students cannot access teacher interfaces.

Students cannot modify examination data.

Every operation shall be logged.

All grading operations shall be traceable.

# 15. Project Directory

exam-system/

backend/

frontend/

ai-service/

shared/

database/

docs/

tests/

scripts/

deploy/

assets/

logs/

# 16. Coding Philosophy

Readable code is more important than clever code.

Explicit is better than implicit.

Documentation is part of the software.

Every module should be independently testable.

Architecture stability is more important than development speed.

# 17. Document Authority

This document is the project constitution.

All documents, including SRS, database design, API design, AI prompt design, UI design, anti-cheat rules, grading rules, and testing rules, must comply with this Charter.
