/**
 * 接入层 · MCP（Model Context Protocol）连接器。
 *
 * <p>把现有领域 Service 的<strong>只读能力</strong>包装为 MCP 工具，供 Claude cowork / WorkBuddy 等
 * 智能体平台以「连接器」形式调用。定位等同 OpenAPI 网关（接入层），<strong>不是 Provider</strong>。</p>
 *
 * <p>合规要点：①工具只调各域 Service 接口，跨域不查对方表（CLAUDE.md §4）；②鉴权复用既有
 * {@code ApiKeyAuthenticationFilter}（{@code X-API-Key} / PAT）——智能体带 PAT 调用即等同其绑定用户身份，
 * 自动继承租户隔离、权限码与数据范围；③WebMvc SSE 传输在 servlet 请求线程上同步处理，
 * 故 ThreadLocal 形态的租户/安全上下文对工具执行天然可见。</p>
 *
 * <p>P0 仅提供只读工具；写工具、审计、权限码细化、OAuth 2.1 留待后续优先级。</p>
 */
package com.mido.pm.mcp;
