# Guga — 掘金式技术博客

全栈工程师面试作业：类掘金技术博客站，支持文章浏览 / 发布 / 评论 / 点赞 + JWT 认证。
需求与设计详见 [need.md](need.md)。

## 功能特性

- 文章：Markdown 发布与渲染（GFM 表格、代码高亮）、标签、关键词搜索、最新/热门排序、分页
- 互动：点赞（一键切换）、评论（仅作者可删）、浏览无需登录
- 认证：注册 / 登录 / JWT（BCrypt 密码存储，接口统一 401/403/404 错误体）
- 安全：rehype-sanitize 全量净化渲染内容（script / 内联事件 / `javascript:` 链接一律剥除）
- 部署：Docker Compose 一键三容器，任意有 Docker 的主机一条命令拉起

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | JDK 21 · Spring Boot 3.5.16 · Spring Security 6.5（JWT，JJWT 0.12.7）· Spring Data JPA · Flyway · PostgreSQL 17 |
| 前端 | React 19 · TypeScript · Vite 6.4 · Tailwind CSS 3.4 · shadcn 风格组件 · react-router 7 · TanStack Query 5 · react-hook-form + zod 4 · react-markdown（sanitize） |
| 部署 | Docker Compose：`db`（Postgres）+ `api`（多阶段构建 Spring Boot）+ `web`（nginx 托管静态资源并反代 `/api`） |
| CI | CircleCI：后端 verify（Testcontainers）、前端 vitest + build、compose 构建校验（main 分支） |

## 架构

```
浏览器 ──http:80──▶ web (nginx)
                     ├── /            → 静态资源（Vite 构建产物，SPA 回退 index.html）
                     └── /api/ ──────▶ api (Spring Boot :8080)
                                          └── jdbc ──▶ db (PostgreSQL :5432，数据卷)
```

开发与生产全程同源（开发期 Vite 代理 `/api`，生产期 nginx 反代），后端零 CORS 代码。

## 快速开始（本地开发）

前置：JDK 21、Docker（含 Compose v2）、Node 20+。

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
# 访问 http://<主机>（防火墙放行 80）
```

`db` 与 `api` 端口仅绑定 127.0.0.1（本机调试用），对外只暴露 80。

## API 摘要

完整契约见 [need.md](need.md) §4。

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/register · /login | - | 注册 / 登录，返回 JWT |
| GET | /api/auth/me | JWT | 当前用户 |
| GET | /api/articles | - | 分页列表：`?page&size&tag&q&sort=latest\|hottest` |
| GET | /api/articles/{slug} | 可选 | 详情（带 token 返回 likedByMe） |
| POST / PUT / DELETE | /api/articles[/{slug}] | JWT+作者 | 发布 / 修改 / 删除 |
| GET | /api/tags | - | 标签云 |
| GET / POST | /api/articles/{slug}/comments | GET 免费 | 评论列表 / 发布 |
| DELETE | /api/comments/{id} | JWT+评论作者 | 删除评论 |
| POST | /api/articles/{slug}/like | JWT | 切换点赞 → `{liked, likeCount}` |

## 测试

```bash
# 后端：单元 + Testcontainers Postgres 集成测试（需要本机 Docker）
JAVA_HOME="<jdk21>" ./mvnw verify

# 前端：Vitest + 构建类型门禁
cd frontend && npm run test -- --run && npm run build
```

覆盖：JWT 签发/校验四类异常、注册登录正负例、文章 CRUD 权限（非作者 403）、
点赞并发切换（原生 ON CONFLICT + 复合主键）、评论计数一致性、XSS 净化断言。

## 项目结构

```
guga/
├── pom.xml              # 后端 Maven 工程（artifactId=guga）
├── src/                 # Spring Boot 源码与 Flyway 迁移
├── frontend/            # Vite + React 前端（含 nginx.conf 与 web Dockerfile）
├── Dockerfile           # api 镜像（maven 构建 → JRE 21 非 root 运行）
├── docker-compose.yml   # db + api + web 三容器编排
├── .circleci/           # CI 流水线
└── need.md              # 需求与设计文档（API 契约 / 数据库 / 安全规范）
```

## 技术选型说明（与原需求的差异）

- **Spring Boot 3.5.16**（原 3.4.x）：3.4 已停止开源维护，3.5 是"不用 4.x"约束下的最新维护线
- **JJWT 0.12.7**（原 0.12.6）：0.12 线末版，API 与 0.12.6 完全一致
- **Vite 6.4.4**：6.x 仍在安全维护分支，低版本存在 2026 年已披露 CVE
- **部署 Docker 化**（原 Vercel + Render + Neon）：按实际要求改为一键 Docker Compose

## 已知取舍 / 未来工作

- 搜索为 ILIKE 顺序扫描，数据量大时应引入 pg_trgm 索引
- 点赞计数冗余在 articles 表，与点赞行同事务更新；并发正确性由复合主键与行数判定保证
- 无刷新令牌机制（JWT 72h 过期）；生产化应补 refresh token / 黑名单
- 无头像上传、文章封面、草稿箱；保持一日交付的精简范围

## 版本历史

| 版本 | 内容 |
| --- | --- |
| 1.0.4 | 前端业务：首页（搜索/标签筛选/排序/分页）、文章详情（Markdown 渲染 + 代码高亮 + XSS 净化）、编辑器（新建/编辑）、点赞、评论；Markdown 栈按需加载；12 个 Vitest 用例；浏览器全流程走查（发布/点赞/评论/编辑/删除/XSS 惰性） |
| 1.0.3 | 前端骨架：Vite 6 + React 19 + Tailwind 3.4 + shadcn 风格组件、路由、登录/注册（RHF + zod4）、首页列表、axios 拦截器；7 个 Vitest 用例；浏览器实测注册/登录/退出全通 |
| 1.0.2 | 业务 API：文章 CRUD/分页/标签/搜索/热门排序、评论、点赞切换（34 个测试全绿 + 双用户冒烟验证） |
| 1.0.1 | JWT 认证：注册/登录/me，数据库 V1 迁移（全部表结构），Spring Security 无状态过滤链，统一错误响应，15 个单测/集成测试 |
| 1.0.0 | 项目骨架：Maven 工程 + 空应用、Docker Compose 三容器编排、CI 流水线、需求文档补全 |
