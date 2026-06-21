package com.mido.pm.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.tool.NpssMcpTools;
import com.mido.pm.mcp.tool.ProjectMcpTools;
import com.mido.pm.mcp.tool.TaskMcpTools;
import com.mido.pm.project.service.ProjectService;
import com.mido.pm.task.service.TaskService;
import com.mido.pm.verify.service.NpssReviewService;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 验证 MCP Server 装配链路：传输、路由函数、聚合工具后的 Server 均可正常构建。 */
class McpServerConfigTest {

    private final McpServerConfig config = new McpServerConfig();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 构建SSE传输与路由函数() {
        WebMvcSseServerTransportProvider transport = config.mcpTransportProvider(objectMapper);
        assertThat(transport).isNotNull();
        assertThat(config.mcpRouterFunction(transport)).isNotNull();
    }

    @Test
    void 聚合全部只读工具构建MCP_Server() {
        WebMvcSseServerTransportProvider transport = config.mcpTransportProvider(objectMapper);
        List<McpToolProvider> providers = List.of(
                new ProjectMcpTools(mock(ProjectService.class), objectMapper),
                new TaskMcpTools(mock(TaskService.class), objectMapper),
                new NpssMcpTools(mock(NpssReviewService.class), objectMapper));

        McpSyncServer server = config.mcpSyncServer(transport, providers);
        assertThat(server).isNotNull();
    }
}
