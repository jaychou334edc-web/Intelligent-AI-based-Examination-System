# AI Paper Parsing System (Word to Structured Exam)

Version: 2.0

Status: Core Feature

Priority: P0

# 1. Feature Overview

This module converts arbitrary Microsoft Word (.docx) exam papers into structured exam data using deterministic parsing and AI understanding.

The main system is Java-based.

Python may be used as an optional AI parsing service when it provides clear advantages for document extraction, image handling, or prompt preprocessing.

The system must support:

- Unstructured Word documents
- Mixed layout
- Text
- Tables
- Images
- No required template
- Multiple question formats
- Automatic question segmentation
- Option extraction
- Answer extraction if present
- Score inference
- Knowledge point inference

# 2. Design Philosophy

This system is not a simple OCR tool.

It is a hybrid deterministic parsing plus AI understanding pipeline.

Key principles:

1. AI is used for understanding, not final storage.
2. Java backend is the source of truth.
3. Python parsing service is optional and replaceable.
4. Deterministic code is used for validation.
5. Human review is mandatory before final import.
6. Raw AI output is never trusted directly.

# 3. Input / Output Definition

## Input

```text
.docx file uploaded by teacher
```

Stored in:

papers.file_path

## Output

Structured JSON:

```json
{
  "title": "",
  "questions": [
    {
      "type": "single_choice | multiple_choice | fill_blank | subjective | true_false",
      "stem": "",
      "options": [
        {"key": "A", "text": ""},
        {"key": "B", "text": ""}
      ],
      "answer": "",
      "score": 5,
      "knowledge_point": "",
      "difficulty": ""
    }
  ]
}
```

# 4. System Architecture

Pipeline:

```text
Word Upload
    |
    v
Java Backend File Storage
    |
    v
Extraction Layer
    |-- Java Apache POI default
    |-- Optional Python python-docx service
    |
    v
Raw Text + Image References
    |
    v
Segmentation Engine
    |
    v
AI Prompt Builder
    |
    v
DeepSeek API
    |
    v
Raw AI JSON Output
    |
    v
Validation Layer
    |
    v
Normalization Layer
    |
    v
Teacher Review Web Page
    |
    v
Approved -> Question Bank
```

# 5. Module Breakdown

## 5.1 Word Extraction Module

### Responsibility

Extract raw content from `.docx`.

### Java Tooling

- Apache POI

### Optional Python Tooling

- python-docx
- Pillow if image processing is needed
- FastAPI for service interface

### Output format

```json
{
  "text_blocks": [],
  "tables": [],
  "images": []
}
```

### Rules

- Preserve order
- Preserve paragraph structure
- Extract embedded images as file references
- Never store extracted images in AI output only

## 5.2 Segmentation Engine

### Purpose

Split raw document into logical question blocks.

### Method

Hybrid approach:

1. Rule-based segmentation
   - Detect "1."
   - Detect Chinese numbering
   - Detect "(1)"
   - Detect option prefixes such as "A."
2. AI-assisted segmentation fallback

### Output

```json
[
  {
    "block_id": 1,
    "raw_text": ""
  }
]
```

## 5.3 AI Prompt Builder

### Purpose

Convert segmented blocks into structured prompt for DeepSeek.

### Prompt Template

System Prompt:

```text
You are an expert exam paper parser.

Task:
Convert the following exam content into structured JSON.

Rules:
- Do not omit any question
- Preserve question order
- Detect question type
- Extract options if present
- Extract correct answer if present
- If unknown, set null
- Return JSON only
```

User Input:

```text
{segmented_blocks}
```

## 5.4 AI Parsing Service

### API

DeepSeek Chat Completion API

### Expected Output

Strict JSON only:

```json
{
  "questions": []
}
```

### Constraints

- No markdown
- No explanation text
- No extra fields unless explicitly versioned

## 5.5 Optional Python Service Contract

The Python service may expose internal APIs:

```http
POST /internal/ai/parse-docx
POST /internal/ai/extract-docx
```

The Java backend calls this service only through internal configuration.

The Python service must not:

- Access MySQL directly
- Store final business data
- Make authorization decisions
- Publish exams
- Assign final grades

## 5.6 Validation Layer

This is the most important safety layer.

### Responsibilities

- Validate JSON schema
- Fix missing optional fields when safe
- Normalize question types
- Ensure score exists
- Ensure options consistency
- Ensure question count is reasonable

### Rules

If invalid:

- Reject AI output
- Retry prompt, max 2 times
- If still invalid, fallback to manual review mode

## 5.7 Normalization Engine

Standardize data:

### Question types mapping

| AI Output | System Type     |
| --------- | --------------- |
| single    | single_choice   |
| multi     | multiple_choice |
| judge     | true_false      |
| blank     | fill_blank      |
| essay     | subjective      |

### Score inference rules

If missing:

- single choice default = configurable
- multiple choice default = configurable
- true false default = configurable
- fill blank default = configurable
- subjective default = configurable

## 5.8 Storage Layer

Raw AI data is stored in:

ai_parse_jobs

ai_parsed_questions

ai_logs

Approved questions are later converted into:

questions

question_options

question_answers

## 5.9 Teacher Review System

### UI Flow

1. Upload Word.
2. AI parses.
3. Display structured questions.
4. Teacher actions:

- Accept
- Modify
- Delete
- Merge
- Split

5. Final confirm -> push to question bank.

# 6. Error Handling Strategy

## AI Failure Cases

### Case 1: Invalid JSON

Retry parsing.

### Case 2: Missing Questions

Trigger re-segmentation and retry.

### Case 3: Partial extraction

Mark question as:

```text
status = "needs_review"
```

## System fallback

If AI fails 3 times:

Switch to manual parsing review page.

# 7. Data Flow Mapping

```text
papers.id
    |
    v
ai_parse_jobs
    |
    v
ai_parsed_questions
    |
    v
teacher review
    |
    v
questions
```

# 8. API Design

## Upload Paper

```http
POST /api/papers
```

## Start Parsing

```http
POST /api/ai/parse-paper
```

```json
{
  "paperId": 1
}
```

## Get Parse Result

```http
GET /api/ai/parse-result/{paperId}
```

## Confirm Import

```http
POST /api/questions/import
```

# 9. Performance Requirements

- Word extraction < 5s for normal papers
- Segmentation < 2s
- AI call < 20s
- Validation < 1s
- Total pipeline < 30s for normal papers

# 10. Logging Requirements

Every stage must log:

- input size
- parser type
- AI request
- AI response
- validation result
- retry count
- elapsed time

Stored in:

ai_logs

# 11. Security Constraints

- No raw Word sent to browser unless explicitly previewed through authorized backend API
- AI API key stored server-side only
- No student access to parsing API
- All parsing requires authentication
- Python service, if used, is internal only

# 12. Future Extensions

Pluggable components:

- OCR module
- Multi-model AI routing
- Fine-tuned parsing model
- Template detection engine
- Exam format classifier
- Message queue based asynchronous parsing

# 13. Definition of Done

Feature is complete when:

- Any .docx file can be uploaded.
- System extracts questions reliably.
- AI produces structured JSON.
- Teacher can review results.
- Approved questions enter question bank.
- Failed cases fallback safely.
- Java backend remains the source of truth.
