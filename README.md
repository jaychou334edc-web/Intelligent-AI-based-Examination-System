# 智能化在线考试系统

本项目是面向学校机房部署的 Java Web 智能化在线考试系统。系统采用统一 Web 入口，管理员、教师、学生通过不同角色登录后进入对应功能界面。

## Phase 0 范围

当前阶段提供项目基础工程：

- Spring Boot 后端骨架
- Vue 3 + TypeScript 前端骨架
- 后端健康检查 API
- 前端到后端的 Vite 代理联通验证
- MySQL 基线迁移脚本
- 部署配置模板
- 可选 Python AI 服务占位说明

## Phase 0.5 工程规范

当前工程已建立后端通用基础设施：

- 统一 API 返回体：`ApiResponse`
- 统一错误码：`ErrorCode`
- 全局异常处理：`GlobalExceptionHandler`
- 请求追踪：`X-Request-ID` + MDC
- Bean Validation 参数校验
- Swagger / OpenAPI：`/swagger-ui.html`、`/v3/api-docs`
- 统一分页对象：`PageRequest`、`PageResponse`
- DTO / Entity / VO / Mapper 分层示例
- MapStruct 对象映射
- UTC JSON 时间格式
- 外部化 CORS 配置
- 基础安全上下文过滤器结构

## Phase 1 核心系统骨架

当前工程已接入最小可用认证闭环：

- MySQL 用户、用户档案、登录会话迁移脚本
- 初始管理员自动创建
- BCrypt 密码哈希
- 登录、退出、当前用户接口
- Bearer Token 会话校验
- 管理员、教师、学生后端角色入口
- Vue 登录页与三角色工作台壳页面
- 前端路由登录态与角色拦截

默认初始管理员仅用于本地开发和首次部署：

```text
username: admin
password: Admin@123456
```

生产环境必须通过环境变量或外部配置修改 `AES_INITIAL_ADMIN_PASSWORD`，不要使用默认密码上线。

## 技术栈

- 后端：Java 21 target，Spring Boot 3.x
- 前端：Vue 3，TypeScript，Vite，Element Plus
- 数据库：MySQL 8+
- AI：DeepSeek API；复杂 Word 解析场景可选 Python FastAPI 服务

## API 约定

后端业务接口统一返回：

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

浏览器、前端和未来移动端都必须通过 Java 后端 API 访问业务数据，不直接访问 MySQL 或 DeepSeek。

Phase 1 认证接口：

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/admin/shell`
- `GET /api/teacher/shell`
- `GET /api/student/shell`

完整接口文档运行后访问 `/swagger-ui.html`。

## 本地开发命令

后端：

```powershell
cd backend
& 'E:\MAVEN\apache-maven-3.8.8\bin\mvn.cmd' test
& 'E:\MAVEN\apache-maven-3.8.8\bin\mvn.cmd' spring-boot:run
```

前端：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

本机敏感配置不要提交到仓库。部署示例见：

- `deploy/application-example.yml`
- `deploy/env.example`

## Git 版本标签

每个阶段完成后建立 Git tag：

- `v0.1.0-phase0`
- `v0.1.5-phase0.5`
- `v0.2.0-phase1`
- `v1.0.0-release`
