package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.collab.dto.CommentCreateDTO;
import com.mido.pm.collab.service.CommentService;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.support.McpToolSupport;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 协作域写工具：对任务/项目/目标添加评论（支持 @ 提醒）。经 {@link CommentService} 调用，
 * 同事务发 comment.created 事件，与 REST 行为一致（直接执行 + 审计）。
 */
@Component
public class CollabMcpTools implements McpToolProvider {

    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public CollabMcpTools(CommentService commentService, ObjectMapper objectMapper) {
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SyncToolSpecification> tools() {
        return List.of(addComment());
    }

    private SyncToolSpecification addComment() {
        Tool tool = new Tool("add_comment",
                "对某业务对象（任务/项目/目标）添加评论，可 @ 提醒指定用户。",
                """
                {
                  "type": "object",
                  "properties": {
                    "entityType": {"type": "string", "description": "评论对象类型：task/project/goal"},
                    "entityId": {"type": "integer", "description": "评论对象 ID"},
                    "content": {"type": "string", "description": "评论内容"},
                    "mention": {"type": "array", "items": {"type": "integer"}, "description": "@ 提醒的用户 ID 列表"}
                  },
                  "required": ["entityType", "entityId", "content"]
                }
                """);
        return new SyncToolSpecification(tool, (exchange, args) -> {
            try {
                CommentCreateDTO dto = new CommentCreateDTO(
                        McpToolSupport.requireString(args, "entityType"),
                        McpToolSupport.requireLong(args, "entityId"),
                        McpToolSupport.requireString(args, "content"),
                        McpToolSupport.optLongList(args, "mention"));
                Long id = commentService.create(dto);
                return McpToolSupport.ok(objectMapper, Map.of("id", id));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }
}
