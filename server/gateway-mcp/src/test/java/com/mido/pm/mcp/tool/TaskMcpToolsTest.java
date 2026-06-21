package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.task.dto.TaskQueryDTO;
import com.mido.pm.task.service.TaskService;
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

class TaskMcpToolsTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskMcpTools tools = new TaskMcpTools(taskService, new ObjectMapper());

    @Test
    void 暴露两个任务只读工具() {
        List<String> names = tools.tools().stream().map(s -> s.tool().name()).toList();
        assertThat(names).containsExactlyInAnyOrder("query_tasks", "get_task");
    }

    @Test
    void query_tasks_解析项目与逾期参数并委派分页查询() {
        when(taskService.page(any())).thenReturn(PageResult.empty(1, 20));

        CallToolResult result = invoke("query_tasks",
                Map.of("projectId", 7, "overdue", true, "status", "进行中"));

        ArgumentCaptor<TaskQueryDTO> captor = ArgumentCaptor.forClass(TaskQueryDTO.class);
        verify(taskService).page(captor.capture());
        TaskQueryDTO query = captor.getValue();
        assertThat(query.projectId()).isEqualTo(7L);
        assertThat(query.overdue()).isTrue();
        assertThat(query.status()).isEqualTo("进行中");
        assertThat(result.isError()).isFalse();
    }

    @Test
    void get_task_缺少ID返回错误结果() {
        CallToolResult result = invoke("get_task", Map.of());
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("taskId");
    }

    private CallToolResult invoke(String toolName, Map<String, Object> args) {
        SyncToolSpecification spec = tools.tools().stream()
                .filter(s -> s.tool().name().equals(toolName))
                .findFirst().orElseThrow();
        return spec.call().apply(null, args);
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }
}
