# Software Requirement Specification (SRS)

Project:
AI Examination System (AES)

Version:
2.0.0

Status:
Approved

Audience:
Developers
AI Coding Assistants
Software Architects
Maintainers
QA Engineers

Dependencies:

00_Project_Charter.md

This document is the highest-level functional specification.

All architecture, implementation, database design and API design must comply with this document.

# Chapter 1

# Introduction

## 1.1 Purpose

This document defines every functional and non-functional requirement of the AI Examination System (AES).

This specification is the authoritative functional reference for:

- System Architecture
- Database Design
- API Specification
- Web UI Design
- AI Module
- Testing
- Deployment

No implementation may contradict this document.

## 1.2 Project Background

Traditional examination systems generally require teachers to manually create questions using predefined templates.

This process introduces several limitations:

- Large workload
- Poor flexibility
- Difficult maintenance
- Low automation
- Weak anti-cheating traceability

AES aims to solve these problems by introducing AI-assisted examination management.

Teachers should only upload an existing Word examination paper.

The system will automatically:

- Read Word
- Extract text
- Extract images
- Identify question types
- Identify options
- Identify answers
- Generate structured examination data

The teacher only needs to review and publish.

## 1.3 Objectives

The system shall provide a complete web examination platform capable of:

Administrator Management

Teacher Management

Student Management

Question Bank

Word AI Parsing

Automatic Examination Generation

Automatic Objective Grading

Manual Subjective Grading

Examination Statistics

Anti-cheating Behavior Logging

Campus LAN Deployment

Java Web Deployment

## 1.4 Intended Audience

Administrator

Teacher

Student

Developer

Software Architect

AI Coding Assistant

QA Engineer

# Chapter 2

# Definitions

## Examination

A complete examination released to students.

Contains:

Metadata

Question List

Time Limit

Participants

Scoring Rules

## Question

Smallest examination unit.

Each question contains:

Question Number

Question Type

Question Body

Images

Options

Answer

Score

Knowledge Point

Difficulty

## Paper

Original Word document uploaded by teacher.

Paper is immutable.

Questions are generated from Paper.

## Examination Session

A running examination instance in a browser.

Contains:

Start Time

End Time

Participants

Status

Logs

## Submission

Complete answer sheet submitted by student.

## Parsing

Converting an unstructured Word document into structured examination data.

## AI Engine

Independent module or service responsible for semantic understanding.

AI never stores final business data.

AI never performs scoring persistence.

## Anti-Cheat Event

Any abnormal student behavior detected during examination.

Examples:

Browser Tab Hidden

Window Lost Focus

Fullscreen Exit

Copy / Paste Attempt

Page Refresh

Network Interruption

# Chapter 3

# Product Scope

AES is a web examination platform.

AES is not:

Learning Management System

Video Conference System

Office Automation Platform

Cloud Education Platform

Its only responsibility is examination management.

# Chapter 4

# User Roles

Four roles exist.

Administrator

Teacher

Student

System

Each role owns independent permissions.

Role interfaces are separated after login.

## Administrator

Responsibilities

User Management

Permission Management

System Configuration

Database Backup

Log Review

System Monitoring

## Teacher

Responsibilities

Upload Word

Review AI Parsing

Question Management

Publish Examination

Manual Grading

Statistics

Export Results

## Student

Responsibilities

Login

Join Examination

Answer Questions

Submit Examination

View Authorized Results

## System

Responsibilities

Authentication

Automatic Saving

Automatic Grading

Log Recording

AI Invocation

Anti-cheating Behavior Collection

Notification

# Chapter 5

# Functional Requirements Overview

The system contains twelve core subsystems.

FR-01 Authentication

FR-02 User Management

FR-03 Paper Management

FR-04 AI Parsing

FR-05 Question Bank

FR-06 Examination Management

FR-07 Browser Examination Client

FR-08 Automatic Grading

FR-09 Manual Grading

FR-10 Statistics

FR-11 Anti-cheating Behavior Logging

FR-12 System Management

Each subsystem will be described separately.

# Chapter 6

# Functional Requirement

## FR-01 Authentication

Purpose

Authenticate users.

Actors

Administrator

Teacher

Student

Functions

Login

Logout

Password Change

Password Reset

Session Management

Requirements

Passwords must be encrypted.

Passwords shall never be stored in plaintext.

Session timeout shall be configurable.

Concurrent login policy shall be configurable.

## FR-02 User Management

Administrator functions.

Create User

Delete User

Modify User

Reset Password

Assign Role

Import Student List

Export User List

Disable Account

Enable Account

## FR-03 Paper Management

Teacher uploads Word documents.

Supported formats

docx

Future support

doc

pdf

Teacher can

Upload

Delete

Archive

Duplicate

Restore

Paper itself shall remain immutable.

AI parsing generates derived data.

Original file shall always be retained.

## FR-04 AI Parsing

Input

Word document

Output

Structured JSON

Teacher review page

AI shall identify

Title

Question Number

Question Type

Options

Answer

Images

Score

Knowledge Point

Difficulty

Every parsing operation shall generate logs.

Teacher approval is mandatory.

AI results never directly enter production data.

The Java backend is responsible for business validation and persistence.

An optional Python AI Parsing Service may be used for complex Word extraction and AI prompt orchestration.

## FR-05 Question Bank

Every approved parsed question enters Question Bank.

Functions

Search

Filter

Edit

Disable

Archive

Tag

Random Selection

Duplicate Detection

Question Version History

## FR-06 Examination Management

Teacher creates examination.

Teacher selects

Question Bank

Duration

Start Time

End Time

Participants

Random Strategy

Scoring Strategy

Teacher publishes examination.

Students receive examination list after login.

## FR-07 Browser Examination Client

Student enters examination in a supported browser.

System shall

Request fullscreen mode

Monitor page visibility

Monitor focus changes

Auto-save answers

Display countdown

Support image questions

Support essay questions

Support multiple choice

Support fill blank

Support code block in future version

Browser-based anti-cheat is event logging and warning based.

It shall not claim complete operating-system-level lockdown.

## FR-08 Automatic Grading

Automatically grade

Single Choice

Multiple Choice

True False

Fill Blank

Configurable

Case insensitive

Whitespace ignored

Regular Expression Matching

Partial Score

Teacher may re-grade.

## FR-09 Manual Grading

Teacher reviews subjective answers.

Functions

Batch Grading

Single Grading

Comment

Score

AI Suggested Score

Score Adjustment

Review History

## FR-10 Statistics

Generate

Average

Maximum

Minimum

Ranking

Question Accuracy

Knowledge Point Analysis

Difficulty Analysis

Export Excel

Export PDF

## FR-11 Anti-Cheat Behavior Logging

System records

Browser Focus Loss

Fullscreen Exit

Clipboard Action

Copy / Paste Attempt

Page Refresh

Abnormal Disconnect

Repeated Submission Attempt

Behavior Log

Cheating Count

Teacher may define penalties.

## FR-12 System Management

Database Backup

Restore

Configuration

System Log

Operation Log

AI Log

Storage Cleanup

Update Check

# Chapter 7

# Business Rules

Every examination belongs to one teacher.

Every question belongs to one paper or manual source.

Every submission belongs to one examination.

Every grading operation generates logs.

Original Word files never change.

Questions may evolve independently.

Deleted users shall never remove historical data.

Students cannot view unauthorized exams.

Teachers cannot grade exams they do not own unless explicitly authorized.

# Chapter 8

# Constraints

Web application only.

Java backend is the primary implementation.

MySQL is the official database.

Python may be used only as an optional AI parsing service.

Backend must expose REST API.

All configurations stored outside source code.

Browser clients must never access database or AI provider directly.
