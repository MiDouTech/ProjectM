# 通用项目管理系统

业务侧通用项目管理系统。差异化护城河：**立项审批引擎 + 干系人管理 + NPSS 两段式价值验收**。

## 技术栈

- **后端**：Java 17 + Spring Boot 3 + Spring Security/JWT + MyBatis-Plus + MySQL 8 + Redis 7 + RabbitMQ 3 + ShardingSphere（多租户预埋）
- **前端**：Vue 3 + Element Plus + Pinia + Vue Router + Vite + AntV G2/G6
- **存储/部署**：S3 兼容（MinIO/OSS）、Docker Compose

## 目录结构

```
server/        模块化单体 DDD：common / module-*（project/task/goal/...）/ provider / bootstrap
web/           Vue 3 前端（views / api / components / store）
docs/          事实源文档（data-model / npss-rule / api-conventions / 设计系统 等）
scripts/       运维脚本（load-demo.sh 等）
docker-compose.yml  本地中间件（MySQL/Redis/RabbitMQ/MinIO）
```

## 快速启动

```bash
# 1. 起本地中间件（MySQL/Redis/RabbitMQ/MinIO）
docker compose up -d

# 2. 起后端（默认 8080，首次启动自动执行 Flyway 迁移建表）
cd server && mvn -pl bootstrap -am spring-boot:run

# 3. 起前端（Vite，默认 5173，/api 代理到 8080）
cd web && npm install && npm run dev
```

浏览器打开前端地址，登录 **admin / admin123**（租户应用）；平台运营后台见 `/ops/login`。

## 演示数据

系统初始为空。导入一套覆盖各模块的仿真演示数据（仅用于演示/联调，勿在生产执行）：

```bash
bash scripts/load-demo.sh    # 幂等，可反复重灌复位
```

包含组织/项目/任务/目标/干系人/NPSS/审批/变更/费用/文档/日历/简报/协作及平台运营等。演示员工密码统一 `admin123`。脚本位于 `server/bootstrap/src/main/resources/db/demo/demo-seed.sql`，**不在 Flyway 默认路径，生产不会自动加载**。

## 企业微信集成

通讯录同步链路已可用，两种启用方式：

- **租户自助（可视化）**：`管理后台 → 企业微信集成`（`/admin/org`）填写 CorpID + 通讯录 Secret 并开启开关，保存后「企微同步」按钮即可点。Secret **加密入库、接口脱敏**，加密密钥取环境变量 `mido.secret.enc-key`。
- **环境变量（运维）**：注入 `MIDO_WECOM_CORP_ID` / `MIDO_WECOM_CONTACTS_SECRET`，置 `MIDO_WECOM_CONTACTS_ENABLED=true`。

凭证取用顺序：优先租户 DB 配置，回落环境变量。详见 `docs/wecom-integration.md`。

## 安全约定

密钥、Token、企微/数据库等真实凭证一律走环境变量注入，**严禁提交到仓库**；仓库内只保留占位默认值。详见 `CLAUDE.md`。

## 文档

事实源集中在 `docs/`：`data-model.md`（DDL）、`npss-rule.md`（算分规则）、`api-conventions.md`（接口约定）、`design-system.md`（前端视觉契约）、`domain-events.md`（领域事件）、`architecture-overview.md`（架构总览）。
