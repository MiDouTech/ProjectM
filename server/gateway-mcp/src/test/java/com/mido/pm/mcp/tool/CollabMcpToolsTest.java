package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.collab.dto.CommentCreateDTO;
import com.mido.pm.collab.service.CommentService;
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

class CollabMcpToolsTest {

    private final CommentService commentService = mock(CommentService.class);
    private final CollabMcpTools tools = new CollabMcpTools(commentService, new ObjectMapper());

    @Test
    void 暴露加评论工具() {
        List<String> names = tools.tools().stream().map(s -> s.tool().name()).toList();
        assertThat(names).containsExactly("add_comment");
    }

    @Test
    void add_comment_解析对象与提醒并委派创建() {
        when(commentService.create(any())).thenReturn(88L);

        CallToolResult result = invoke("add_comment", Map.of(
                "entityType", "task", "entityId", 5, "content", "请尽快跟进", "mention", List.of(9, 10)));

        ArgumentCaptor<CommentCreateDTO> captor = ArgumentCaptor.forClass(CommentCreateDTO.class);
        verify(commentService).create(captor.capture());
        CommentCreateDTO dto = captor.getValue();
        assertThat(dto.entityType()).isEqualTo("task");
        assertThat(dto.entityId()).isEqualTo(5L);
        assertThat(dto.content()).isEqualTo("请尽快跟进");
        assertThat(dto.mention()).containsExactly(9L, 10L);
        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("88");
    }

    @Test
    void add_comment_缺少内容返回错误结果() {
        CallToolResult result = invoke("add_comment", Map.of("entityType", "task", "entityId", 5));
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("content");
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
