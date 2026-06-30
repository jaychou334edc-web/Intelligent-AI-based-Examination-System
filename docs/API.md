# AES API Documentation

Version: Phase 2

Status: Active

Runtime OpenAPI:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

# 1. Response Envelope

All backend business APIs return `ApiResponse<T>`.

```json
{
  "success": true,
  "code": "OK",
  "message": "请求成功",
  "data": {},
  "requestId": "request-id",
  "timestamp": "2026-07-01T00:00:00Z"
}
```

# 2. Authentication

## 2.1 Login

`POST /api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "Admin@123456"
}
```

Response data:

```json
{
  "token": "bearer-token",
  "user": {
    "id": 1,
    "username": "admin",
    "role": "admin",
    "realName": "系统管理员"
  }
}
```

Passwords are verified against BCrypt hashes. The returned token must be sent as:

```text
Authorization: Bearer <token>
```

## 2.2 Logout

`POST /api/auth/logout`

Requires `Authorization: Bearer <token>`.

The current login session is revoked.

## 2.3 Current User

`GET /api/auth/me`

Requires `Authorization: Bearer <token>`.

Response data:

```json
{
  "id": 1,
  "username": "admin",
  "role": "admin",
  "realName": "系统管理员"
}
```

# 3. Role Shells

All role shell APIs require login. Each endpoint also requires the matching role.

| Method | Path | Required Role |
| --- | --- | --- |
| GET | `/api/admin/shell` | `admin` |
| GET | `/api/teacher/shell` | `teacher` |
| GET | `/api/student/shell` | `student` |

Response data:

```json
{
  "role": "admin",
  "title": "管理员工作台"
}
```

# 4. Phase 1 Security Rules

- Missing or invalid token returns authentication failure.
- Logged-in users cannot access another role's shell.
- Passwords are never returned by APIs.
- Browser clients never access MySQL directly.
- Browser clients never call DeepSeek directly.

# 5. AI Question Import

Phase 2 APIs require `teacher` role.

## 5.1 Upload Import Source

`POST /api/papers`

Content type: `multipart/form-data`

Fields:

- `file`: `.docx` or `.txt`
- `title`: optional import title

Response data:

```json
{
  "id": 1,
  "title": "本地测试试卷",
  "fileName": "sample.docx",
  "fileSize": 10240,
  "parseStatus": "pending",
  "uploadTime": "2026-07-01T00:00:00"
}
```

## 5.2 Parse Import Source

`POST /api/ai/parse-paper`

Request:

```json
{
  "paperId": 1
}
```

Response data:

```json
{
  "paperId": 1,
  "parseJobId": 1,
  "status": "parsed",
  "rawText": "extracted document text with [IMG:image_1] placeholders",
  "questions": [
    {
      "parsedQuestionId": 1,
      "questionType": "single_choice",
      "stem": "下列哪个选项是 Java 后端框架？",
      "options": [
        {"key": "A", "text": "Spring Boot"}
      ],
      "answer": "A",
      "analysis": "",
      "score": 5,
      "knowledgePoint": "",
      "difficulty": "normal",
      "reviewStatus": "needs_review",
      "reviewComment": null
    }
  ]
}
```

The Java backend uses a chain-style import pipeline: document extraction, chunking, DeepSeek prompt invocation, JSON validation/mapping, teacher review, and final import.

DeepSeek is expected to return short-field JSON:

```json
{
  "questions": [
    {
      "t": "single",
      "c": "题干，不含选项，保留 [IMG:image_1]",
      "o": ["选项一", "选项二"],
      "a": ["A"],
      "e": "解析",
      "s": 2
    }
  ]
}
```

The backend maps `single/multi/judge/blank/essay` to internal question types and stores AI output in temporary tables before teacher approval.

## 5.3 Get Parse Result

`GET /api/ai/parse-result/{paperId}`

Returns the latest parsed questions for teacher review.

## 5.4 Import Reviewed Questions

`POST /api/questions/import`

Request:

```json
{
  "paperId": 1,
  "questions": [
    {
      "parsedQuestionId": 1,
      "questionType": "single_choice",
      "stem": "下列哪个选项是 Java 后端框架？",
      "options": [
        {"key": "A", "text": "Spring Boot"},
        {"key": "B", "text": "Photoshop"}
      ],
      "answer": "A",
      "analysis": "Spring Boot 是 Java 后端框架。",
      "score": 5,
      "knowledgePoint": "Java Web",
      "difficulty": "normal",
      "reviewComment": "已审核"
    }
  ]
}
```

Response data:

```json
{
  "paperId": 1,
  "importedCount": 1
}
```

Imported questions are written to `questions`, `question_options`, and `question_answers`. AI temporary rows are marked `approved`.
