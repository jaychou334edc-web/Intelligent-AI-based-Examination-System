# AES API Documentation

Version: Phase 1

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
