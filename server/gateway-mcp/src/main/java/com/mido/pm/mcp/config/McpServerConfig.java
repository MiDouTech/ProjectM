package com.mido.pm.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.mcp.support.McpToolProvider;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * MCP Server 装配：以 Spring WebMvc SSE 传输对外暴露连接器端点，并把各域只读工具注册进 MCP Server。
 *
 * <ul>
 *   <li>SSE 建连端点：{@code GET /mcp/sse}；JSON-RPC 调用端点：{@code POST /mcp/message}。</li>
 *   <li>两端点均落入既有「租户应用」安全链（{@code anyRequest().authenticated()}），由
 *       {@code ApiKeyAuthenticationFilter} 校验 {@code X-API-Key}，故无需改动 SecurityConfig。</li>
 *   <li>传输在 servlet 请求线程上同步处理消息，工具内可直接读到 ThreadLocal 租户/安全上下文。</li>
 * </ul>
 */
@Configuration
public class McpServerConfig {

    /** SSE 建连端点路径。 */
    public static final String SSE_ENDPOINT = "/mcp/sse";
    /** JSON-RPC 消息端点路径。 */
    public static final String MESSAGE_ENDPOINT = "/mcp/message";

    /** WebMvc SSE 传输提供者（持有会话工厂，由 MCP Server 构建时注入）。 */
    @Bean
    public WebMvcSseServerTransportProvider mcpTransportProvider(ObjectMapper objectMapper) {
        return WebMvcSseServerTransportProvider.builder()
                .objectMapper(objectMapper)
                .sseEndpoint(SSE_ENDPOINT)
                .messageEndpoint(MESSAGE_ENDPOINT)
                .build();
    }

    /** 把传输的路由函数注册为 Bean，纳入 Spring MVC 函数式路由。 */
    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(WebMvcSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    /** 构建 MCP Server：聚合全部 {@link McpToolProvider} 的只读工具并注册。 */
    @Bean
    public McpSyncServer mcpSyncServer(WebMvcSseServerTransportProvider transportProvider,
                                       List<McpToolProvider> toolProviders) {
        List<SyncToolSpecification> tools = toolProviders.stream()
                .flatMap(provider -> provider.tools().stream())
                .toList();
        return McpServer.sync(transportProvider)
                .serverInfo("mido-pm-mcp", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .instructions("米多通用项目管理系统 MCP 连接器：提供项目、任务、NPSS 等只读查询工具。"
                        + "调用须携带有效的 X-API-Key（PAT），返回数据已按调用者所属租户与数据范围过滤。")
                .tools(tools)
                .build();
    }
}
