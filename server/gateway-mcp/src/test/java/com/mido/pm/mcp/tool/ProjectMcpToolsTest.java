package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.mcp.support.McpToolGuard;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.project.dto.ProjectQueryDTO;
import com.mido.pm.project.service.ProjectService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMcpToolsTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final McpToolGuard guard = new McpToolGuard(mock(AuditLogService.class), id -> true);
    private final ProjectMcpTools tools = new ProjectMcpTools(projectService, new ObjectMapper(), guard);

    @Test
    void 暴露三个项目只读工具() {
        List<String> names = tools.tools().stream().map(s -> s.tool().name()).toList();
        assertThat(names).containsExactlyInAnyOrder("list_projects", "get_project", "list_my_projects");
    }

    @Test
    void list_projects_解析筛选参数并委派分页查询() {
        when(projectService.page(any())).thenReturn(PageResult.empty(1, 20));

        CallToolResult result = invoke("list_projects",
                Map.of("page", 2, "size", 50, "keyword", "营销", "status", "进行中"));

        ArgumentCaptor<ProjectQueryDTO> captor = ArgumentCaptor.forClass(ProjectQueryDTO.class);
        verify(projectService).page(captor.capture());
        ProjectQueryDTO query = captor.getValue();
        assertThat(query.page()).isEqualTo(2L);
        assertThat(query.size()).isEqualTo(50L);
        assertThat(query.keyword()).isEqualTo("营销");
        assertThat(query.status()).isEqualTo("进行中");
        assertThat(result.isError()).isFalse();
    }

    @Test
    void get_project_按ID委派详情查询() {
        invoke("get_project", Map.of("projectId", 42));
        verify(projectService).get(42L);
    }

    @Test
    void get_project_缺少ID返回错误结果() {
        CallToolResult result = invoke("get_project", Map.of());
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("projectId");
    }

    private CallToolResult invoke(String toolName, Map<String, Object> args) {
        SyncToolSpecification spec = ((McpToolProvider) tools).tools().stream()
                .filter(s -> s.tool().name().equals(toolName))
                .findFirst().orElseThrow();
        return spec.call().apply(null, args);
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }
}
