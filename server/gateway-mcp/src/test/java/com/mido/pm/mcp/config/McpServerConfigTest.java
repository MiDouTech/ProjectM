package com.mido.pm.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.collab.service.CommentService;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.mcp.support.McpToolGuard;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.tool.CollabMcpTools;
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
    void 聚合全部工具构建MCP_Server() {
        WebMvcSseServerTransportProvider transport = config.mcpTransportProvider(objectMapper);
        McpToolGuard guard = new McpToolGuard(mock(AuditLogService.class), id -> true);
        List<McpToolProvider> providers = List.of(
                new ProjectMcpTools(mock(ProjectService.class), objectMapper, guard),
                new TaskMcpTools(mock(TaskService.class), objectMapper, guard),
                new NpssMcpTools(mock(NpssReviewService.class), objectMapper, guard),
                new CollabMcpTools(mock(CommentService.class), objectMapper, guard));

        McpSyncServer server = config.mcpSyncServer(transport, providers);
        assertThat(server).isNotNull();
    }
}
