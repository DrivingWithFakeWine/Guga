# 项目规划：一日交付的博客网站（全栈工程师面试作业）

> 本文档是项目的唯一需求来源，请全程参照本文执行，不得自行增删功能或更换技术栈。
> 生成代码时严格遵循下文版本号、目录结构、API 契约与安全规范。
>
> **调整记录（2026-10-15，经确认）**：
> 1. 部署由 Vercel + Render + Neon 改为 **Docker Compose 一键部署**（本地与云端同一套编排）。
> 2. Spring Boot 3.4.x → **3.5.16**（3.4 已停止开源维护，仍遵守"不用 4.x"）；JJWT 0.12.6 → **0.12.7**（0.12 线末版）；Node 按本机实际为 24。
> 3. 原文档 §3 及之后章节缺失，§3–§8 由实现方补充设计。

---

## 1. 项目背景与硬约束

- **性质**：全栈工程师岗位面试作业，考核工程能力而非产品完整度
- **用时上限**：一天（约 10 小时），**完成度优先于功能数量**
- **交付物**：
  - 可公开访问的部署链接（Docker Compose 一键部署于任意有 Docker 的主机）
  - 一份 README（源码仓库保持私有：https://github.com/DrivingWithFakeWine/Guga）
- **功能范围**：类似掘金的技术博客站，含文章浏览/发布/评论/点赞 + 用户认证
- **UI 允许有自己的想法**，不要求像素级还原掘金；界面语言为中文

---

## 2. 技术栈清单

### 后端
| 组件               | 版本                | 说明                                            |
| ------------------ | ------------------- | ----------------------------------------------- |
| JDK                | 21 (LTS)            | 本机位于 `~/.jdks/openjdk-21.0.1`，构建需 `JAVA_HOME` 指定 |
| Spring Boot        | 3.5.16              | 3.x 最新 patch；**不要用 4.x**，生态未跟上       |
| 构建工具           | Maven 3.9.11        | 使用 Maven Wrapper                              |
| Spring Web         | 随 Boot BOM         | REST API                                        |
| Spring Data JPA    | 随 Boot BOM         | ORM，`ddl-auto: validate`                       |
| Spring Security    | 6.5.x（随 BOM）     | 只做 JWT filter，禁用 OAuth2/FormLogin          |
| JJWT               | **0.12.7**          | ⚠️ 必须用 0.12.x API（api + impl + jackson 三件套，详见 §6） |
| Flyway             | 11.x（随 BOM）      | ⚠️ 必须显式加 `flyway-database-postgresql` 模块 |
| Jakarta Validation | 随 Boot BOM         | 入参校验                                        |
| PostgreSQL Driver  | 随 Boot BOM         | Postgres 17（compose 容器）                     |
| 测试               | JUnit 5 + Testcontainers（Postgres） | 不用 H2：IDENTITY/TIMESTAMPTZ/ON CONFLICT 不兼容 |

### 前端
| 组件          | 版本                                              | 说明                         |
| ------------- | ------------------------------------------------- | ---------------------------- |
| Node          | 本机 24 LTS（≥20 均可）                            |                              |
| 构建工具      | Vite ^6.4.4                                        | 6.x 仍在安全维护；不要用 4.x/5.x |
| 框架          | React 19 + TypeScript 5.x                         | SPA，不使用 Next.js          |
| 样式          | Tailwind CSS ^3.4.19                               | **不要用 4.x**，破坏性变更多 |
| UI 组件库     | shadcn/ui                                         | 按需引入（Tailwind v3 模式，勿跑 migrate） |
| 路由          | react-router ^7（从 `'react-router'` 导入）        |                              |
| 状态/请求     | TanStack Query v5 + axios                         | 服务端状态统一走 Query       |
| 表单          | react-hook-form + zod v4（`z.email()` 新写法）     | 前端校验                     |
| Markdown 渲染 | react-markdown 10 + remark-gfm 4 + **rehype-sanitize 6** | sanitize 必须开启，防 XSS |
| 代码高亮      | rehype-highlight 7（内含 highlight.js 11）         | 高亮插件在 sanitize 之前     |
| 部署          | Docker（nginx 容器托管静态资源并反代 /api）        | 见 §7                        |

---

## 3. 项目结构

Monorepo：后端 Maven 工程在仓库根目录，前端在 `frontend/` 子目录。

```
guga/
├── pom.xml  mvnw  .mvn/wrapper/          # 后端（artifactId=guga）
├── src/main/java/pl/piomin/services/guga/
│   ├── GugaApplication.java
│   ├── config/SecurityConfig.java        # 过滤链 + PasswordEncoder
│   ├── security/{JwtService,JwtAuthFilter}.java
│   ├── web/                              # 各 Controller + GlobalExceptionHandler + ErrorResponse
│   ├── service/                          # AuthService, ArticleService, CommentService, LikeService
│   ├── repo/                             # Spring Data JPA 仓储
│   ├── entity/                           # 纯 Java 类（不用 Lombok），DTO 一律用 record
│   ├── dto/                              # AuthDtos / ArticleDtos / CommentDtos
│   └── common/                           # NotFound→404 / Duplicate→409 / Forbidden→403 异常
├── src/main/resources/{application.yml, db/migration/V1__init.sql}
├── src/test/java/...                     # 单测 + @SpringBootTest/Testcontainers 集成测试
├── frontend/                             # Vite + React + TS
│   ├── Dockerfile  nginx.conf            # web 容器：构建产物 + SPA 回退 + /api 反代
│   └── src/{api, lib, components, pages, test}
├── Dockerfile                            # api 容器（maven 多阶段构建 → JRE 21）
├── docker-compose.yml                    # db + api + web 三容器一键部署
├── .env.example                          # JWT_SECRET / 数据库口令模板（.env 入 gitignore）
├── .circleci/config.yml                  # CI：后端 verify / 前端 build+test / docker 构建
└── README.md                             # zh-CN，含版本历史
```

---

## 4. API 契约

- 前缀 `/api`，JSON 进出；认证头 `Authorization: Bearer <jwt>`
- 时间一律 ISO-8601 UTC（`Instant`）
- 分页响应：`{content[], page, size, totalElements, totalPages}`
- **统一错误体**（MVC 层与 Security 层 401/403 同格式，message 为中文）：

```json
{ "status": 403, "error": "Forbidden", "message": "只能修改自己的文章",
  "path": "/api/articles/k3f9x2a1p0qz", "timestamp": "2026-10-15T09:31:22Z" }
```

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/register | - | `{username(3-20, [a-zA-Z0-9_]), email, password(8-72)}` → 201 `{token, user}`；用户名/邮箱重复 → 409 |
| POST | /api/auth/login | - | `{email, password}` → 200 同上；凭据错误 → 401 |
| GET | /api/auth/me | JWT | 当前用户 |
| GET | /api/articles | - | `?page&size&tag&q&sort(latest\|hottest)`；列表项为摘要（excerpt=正文前 200 字纯文本） |
| GET | /api/articles/{slug} | 可选 | 详情（含正文原文、tags、likeCount、commentCount；有 token 时返回 `likedByMe`） |
| POST | /api/articles | JWT | `{title(5-120), content(10-50000), tags[]≤5}` → 201 详情 |
| PUT | /api/articles/{slug} | JWT+作者 | 更新；非作者 → 403 |
| DELETE | /api/articles/{slug} | JWT+作者 | → 204 |
| GET | /api/tags | - | 标签云 `[{name, articleCount}]` |
| GET | /api/articles/{slug}/comments | - | 平铺按时间 `[{id, content, authorName, createdAt}]` |
| POST | /api/articles/{slug}/comments | JWT | `{content(1-1000)}` → 201 |
| DELETE | /api/comments/{id} | JWT+评论作者 | → 204 |
| POST | /api/articles/{slug}/like | JWT | 切换点赞 → `{liked, likeCount}` |

- **slug**：服务端生成 12 位随机 `[0-9a-z]`（SecureRandom，唯一约束，冲突重试 ≤3 次）；中文标题无需转音
- 校验失败 → 400（message 汇总"字段名: 消息"）；资源不存在 → 404

---

## 5. 数据库（Flyway V1__init.sql）

- `users(id IDENTITY PK, username VC(20) UNIQUE, email VC(255) UNIQUE, password_hash VC(72), created_at TIMESTAMPTZ)`
- `articles(id, slug VC(32) UNIQUE, title VC(120), excerpt VC(200), content TEXT, author_id FK→users ON DELETE CASCADE, like_count BIGINT DEFAULT 0, comment_count INT DEFAULT 0, created_at, updated_at)`；索引：`created_at DESC`、`author_id`
- `tags(id, name VC(30) UNIQUE)`；`article_tags(article_id, tag_id, 复合主键)` + `tag_id` 索引
- `comments(id, article_id FK, author_id FK, content VC(1000), created_at)` + `article_id` 索引
- `article_likes(article_id, user_id, 复合主键)` —— 复合主键即并发防线
- `like_count` / `comment_count` 为冗余计数，与点赞/评论写入同事务增减（列表与热门排序单次扫描）
- 时间戳由实体 `@PrePersist/@PreUpdate` 填充；实体用 `Instant`（Hibernate 6.6 ↔ timestamptz）

---

## 6. 安全规范

- **JJWT 0.12.x API（与 0.11 不兼容）**：
  - 密钥：`Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`，secret ≥32 字节，来自环境变量 `JWT_SECRET`（不足抛 `WeakKeyException`）
  - 签发：`Jwts.builder().subject(username).issuedAt(...).expiration(...).signWith(key).compact()`（`signWith(key)` 自动 HS256，无需 SignatureAlgorithm 枚举）
  - 校验：`Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()`（0.11 的 `parserBuilder()/setSigningKey/parseClaimsJws/getBody` 均已废弃）
  - 解析失败（过期/篡改/异钥）catch `JwtException` 返回空，**filter 永不抛异常**
- **过滤链（Security 6 lambda DSL，无状态）**：csrf off；`requestMatchers(GET, "/api/articles/**", "/api/tags").permitAll()` → `"/api/auth/**".permitAll()` → `anyRequest().authenticated()`；JwtAuthFilter 注册在 `UsernamePasswordAuthenticationFilter` 之前；自定义 401 entryPoint / 403 accessDeniedHandler 输出统一错误体（匿名访问受保护路由必须是 401 而非默认 403）
- **JwtAuthFilter**：解析 Bearer → 按 subject 查库 → Spring 内置 `User.withUsername(...).roles("USER")` 填充 SecurityContext；无 token/解析失败 → 匿名放行（由授权规则决定 401/放行）
- 密码 BCrypt（Service 层 `matches()` 比对，不引 AuthenticationManager）；`open-in-view: false`
- **CORS 一行不写**：开发期 Vite dev server 代理 `/api → localhost:8080`，生产 nginx 反代，全程同源

---

## 7. 部署与运行（Docker 一键）

- **一键部署**（任意有 Docker 20.10+ 与 Compose v2 的主机）：
  1. `cp .env.example .env`，修改 `JWT_SECRET`（`openssl rand -hex 32`）与数据库口令
  2. `docker compose up -d --build` → `http://<主机>`（防火墙放行 80）
- 容器拓扑：`db`（postgres:17-alpine，数据卷 + healthcheck，端口仅绑 127.0.0.1）→ `api`（多阶段构建的 Spring Boot jar，JRE 21 非 root 运行，仅绑 127.0.0.1）→ `web`（nginx 托管前端静态资源，`/api` 反代 api:8080，SPA 回退 index.html）
- **本地开发**：`docker compose up -d db` → 后端 `JAVA_HOME=<jdk21> ./mvnw spring-boot:run`（8080）→ 前端 `npm run dev`（5173，Vite 代理 /api）
- **CI（CircleCI）**：backend（JDK 21 镜像 + remote docker 跑 Testcontainers，`./mvnw verify`）、frontend（Node 镜像，vitest + build）、docker（compose 构建校验）；全部通过才允许合并

---

## 8. 测试与验收

- 后端：JwtService 单测（签发/解析 roundtrip + 过期/篡改/异钥/弱密钥四负例）；Auth/Article/Comment/Like 五组 `@SpringBootTest + MockMvc + Testcontainers Postgres` 集成测试，覆盖正负例（401/403/404/400/409 与核心流程）
- 前端：Vitest 三件——MarkdownView XSS 净化（`<script>` 被剥、hljs 类保留）、token 存取、axios 拦截器（附带/401 清除）；`npm run build` 作为类型门禁
- 端到端验收：compose 全栈起 → 游客浏览 / 注册 / 发布含代码块的 Markdown 文章 / 点赞与取消 / 评论 / 修改与删除 / XSS 样张渲染为惰性
