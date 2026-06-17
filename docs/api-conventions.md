# API 约定（事实源）

> 所有 Controller 严格遵守。目标：多端（PC/H5/小程序/OpenAPI）共用同一套契约，AI 生成接口风格一致。

## 1. 路径与版本
- 前缀：`/api/v1`，资源用名词复数：`/api/v1/projects`、`/api/v1/projects/{id}/tasks`。
- 动作型用子路径：`POST /api/v1/projects/{id}/submit-approval`、`POST /api/v1/npss-reviews/{id}/score`。
- 多端不分叉：同一能力同一接口；端差异在前端处理，后端不写端专用逻辑。

## 2. 统一响应包装
```json
{ "code": 0, "message": "ok", "data": { } , "traceId": "..." }
```
- `code=0` 成功；非 0 为业务错误码。HTTP 状态：成功 200/201，校验失败 400，未认证 401，无权限 403，不存在 404，冲突 409，服务端 500。
- 分页响应：`data: { list: [], total, page, size }`。
- 错误：`{ "code": 40001, "message": "受益方权重合计需≥50%", "data": null }`。错误码集中登记在 `common/ErrorCode`，禁散落硬编码。

## 3. 请求约定
- 分页参数：`page`(从1起)、`size`(默认20，上限100)。
- 列表筛选：复杂多条件（当…且…或，对齐 Worktile）用 POST `/query` 传筛选 DTO；简单筛选用 query string。
- 排序：`sort=field,asc|desc`，多字段逗号分隔。
- 幂等：所有 `POST` 创建类接口支持 `Idempotency-Key` 头（Redis 去重），防重复提交（立项、评分尤其重要）。

## 4. 认证授权
- 认证：JWT，`Authorization: Bearer <token>`。Token 由 `SsoProvider` 签发（本地或企微 SSO）。
- 授权：方法级 `@PreAuthorize` + 权限码（perm_code）；数据级走数据范围（self/dept/dept_and_sub/all/custom），由 MyBatis-Plus 拦截器按 `sys_role_data_scope` 注入条件。
- 多租户：拦截器自动注入 `tenant_id`，接口层不显式接收。

## 5. CRUD 标准动词
| 操作 | 方法 | 路径 |
|---|---|---|
| 列表 | GET | `/projects` |
| 详情 | GET | `/projects/{id}` |
| 创建 | POST | `/projects` |
| 更新 | PUT | `/projects/{id}` |
| 局部更新 | PATCH | `/projects/{id}` |
| 删除(逻辑) | DELETE | `/projects/{id}` |
| 复杂查询 | POST | `/projects/query` |

## 6. DTO 约定
- 入参 `XxxCreateDTO / XxxUpdateDTO / XxxQueryDTO`，出参 `XxxVO`。**禁直接暴露 Entity**。
- 校验注解写在 DTO（`@NotNull/@Size/@DecimalMin` 等），错误信息中文、可读。
- 时间一律 ISO-8601 字符串；金额用字符串或带精度的数字，前端等宽展示。

## 7. Swagger/OpenAPI
- 每个 Controller、DTO 有 `@Operation/@Schema` 注解；OpenAPI 3.0 自动导出，作为 H5/小程序/OpenAPI 网关的契约源。

## 8. 写操作与事件
- 任何写操作成功后，同事务写 `sys_domain_event`（见 `docs/domain-events.md`），事件投递失败不影响主事务（Outbox 异步补偿）。
