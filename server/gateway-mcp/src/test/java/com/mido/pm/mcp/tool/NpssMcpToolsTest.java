package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.verify.service.NpssReviewService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpssMcpToolsTest {

    private final NpssReviewService npssReviewService = mock(NpssReviewService.class);
    private final NpssMcpTools tools = new NpssMcpTools(npssReviewService, new ObjectMapper());

    @Test
    void 暴露两个NPSS只读工具() {
        List<String> names = tools.tools().stream().map(s -> s.tool().name()).toList();
        assertThat(names).containsExactlyInAnyOrder("list_npss_reviews", "get_npss_review");
    }

    @Test
    void list_npss_reviews_按项目委派查询() {
        when(npssReviewService.listByProject(5L)).thenReturn(List.of());
        CallToolResult result = invoke("list_npss_reviews", Map.of("projectId", 5));
        verify(npssReviewService).listByProject(5L);
        assertThat(result.isError()).isFalse();
    }

    @Test
    void get_npss_review_缺少ID返回错误结果() {
        CallToolResult result = invoke("get_npss_review", Map.of());
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("reviewId");
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
