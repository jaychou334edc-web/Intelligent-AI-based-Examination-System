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

## 技术栈

- 后端：Java 21 target，Spring Boot 3.x
- 前端：Vue 3，TypeScript，Vite，Element Plus
- 数据库：MySQL 8+
- AI：DeepSeek API；复杂 Word 解析场景可选 Python FastAPI 服务

## 本地开发命令

后端：

```powershell
cd backend
mvn test
mvn spring-boot:run
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
