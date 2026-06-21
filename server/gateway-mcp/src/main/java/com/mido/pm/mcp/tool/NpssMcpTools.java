package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.support.McpToolSupport;
import com.mido.pm.verify.service.NpssReviewService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NPSS 验收域只读工具：按项目查 NPSS 评分轮次、查单轮次详情（含各干系人评分）。
 * 经 {@link NpssReviewService} 调用，自动继承租户隔离。
 */
@Component
public class NpssMcpTools implements McpToolProvider {

    private final NpssReviewService npssReviewService;
    private final ObjectMapper objectMapper;

    public NpssMcpTools(NpssReviewService npssReviewService, ObjectMapper objectMapper) {
        this.npssReviewService = npssReviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SyncToolSpecification> tools() {
        return List.of(listNpssReviews(), getNpssReview());
    }

    private SyncToolSpecification listNpssReviews() {
        Tool tool = new Tool("list_npss_reviews",
                "查询某项目的全部 NPSS 价值验收轮次。",
                """
                {
                  "type": "object",
                  "properties": {
                    "projectId": {"type": "integer", "description": "项目 ID"}
                  },
                  "required": ["projectId"]
                }
                """);
        return new SyncToolSpecification(tool, (exchange, args) -> {
            try {
                long projectId = McpToolSupport.requireLong(args, "projectId");
                return McpToolSupport.ok(objectMapper, npssReviewService.listByProject(projectId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification getNpssReview() {
        Tool tool = new Tool("get_npss_review",
                "按 ID 查询单个 NPSS 评分轮次详情，含各干系人加权评分。",
                """
                {
                  "type": "object",
                  "properties": {
                    "reviewId": {"type": "integer", "description": "NPSS 轮次 ID"}
                  },
                  "required": ["reviewId"]
                }
                """);
        return new SyncToolSpecification(tool, (exchange, args) -> {
            try {
                long reviewId = McpToolSupport.requireLong(args, "reviewId");
                return McpToolSupport.ok(objectMapper, npssReviewService.get(reviewId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }
}
