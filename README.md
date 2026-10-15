# Guga — 掘金式技术博客

全栈工程师面试作业：类掘金技术博客站（文章浏览 / 发布 / 评论 / 点赞 + JWT 认证）。
需求与设计详见 [need.md](need.md)。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | JDK 21 · Spring Boot 3.5.16 · Spring Security(JWT, JJWT 0.12.7) · Spring Data JPA · Flyway · PostgreSQL 17 · Testcontainers |
| 前端 | React 19 · TypeScript · Vite 6 · Tailwind CSS 3.4 · shadcn/ui · react-router 7 · TanStack Query 5 · react-hook-form + zod · react-markdown(+sanitize) |
| 部署 | Docker Compose 一键三容器（db + api + web/nginx 反代） |
| CI | CircleCI（后端 verify / 前端 build+test / docker 构建校验） |

## 快速开始（本地开发）

前置：JDK 21（`JAVA_HOME` 指向）、Docker、Node 20+。

```bash
# 1. 启动数据库
docker compose up -d db

# 2. 启动后端（8080）
JAVA_HOME="C:\Users\14265\.jdks\openjdk-21.0.1" ./mvnw spring-boot:run

# 3. 启动前端（5173，已代理 /api）
cd frontend && npm install && npm run dev
```

## 一键部署

```bash
cp .env.example .env   # 修改 JWT_SECRET（openssl rand -hex 32）与数据库口令
docker compose up -d --build
# 访问 http://<主机>
```

## 版本历史

| 版本 | 内容 |
| --- | --- |
| 1.0.3 | 前端骨架：Vite 6 + React 19 + Tailwind 3.4 + shadcn 风格组件、路由、登录/注册（RHF + zod4）、首页列表、axios 拦截器；7 个 Vitest 用例；浏览器实测注册/登录/退出全通 |
| 1.0.2 | 业务 API：文章 CRUD/分页/标签/搜索/热门排序、评论、点赞切换（34 个测试全绿 + 双用户冒烟验证） |
| 1.0.1 | JWT 认证：注册/登录/me，数据库 V1 迁移（全部表结构），Spring Security 无状态过滤链，统一错误响应，15 个单测/集成测试 |
| 1.0.0 | 项目骨架：Maven 工程 + 空应用、Docker Compose 三容器编排、CI 流水线、需求文档补全 |
