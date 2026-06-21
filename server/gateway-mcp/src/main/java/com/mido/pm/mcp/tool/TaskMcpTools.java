package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.support.McpToolSupport;
import com.mido.pm.task.dto.TaskQueryDTO;
import com.mido.pm.task.service.TaskService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务域只读工具：分页查任务、查任务详情。经 {@link TaskService} 调用，自动继承租户隔离与数据范围。
 */
@Component
public class TaskMcpTools implements McpToolProvider {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public TaskMcpTools(TaskService taskService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SyncToolSpecification> tools() {
        return List.of(queryTasks(), getTask());
    }

    private SyncToolSpecification queryTasks() {
        Tool tool = new Tool("query_tasks",
                "分页查询任务，可按项目、负责人、状态、是否逾期筛选。",
                """
                {
                  "type": "object",
                  "properties": {
                    "page": {"type": "integer", "description": "页码，从 1 起，默认 1"},
                    "size": {"type": "integer", "description": "每页条数，默认 20，上限 100"},
                    "projectId": {"type": "integer", "description": "所属项目 ID"},
                    "assigneeId": {"type": "integer", "description": "负责人用户 ID"},
                    "status": {"type": "string", "description": "任务状态"},
                    "overdue": {"type": "boolean", "description": "仅看逾期任务"},
                    "sort": {"type": "string", "description": "排序，如 dueDate,asc"}
                  }
                }
                """);
        return new SyncToolSpecification(tool, (exchange, args) -> {
            try {
                TaskQueryDTO query = new TaskQueryDTO(
                        McpToolSupport.optLong(args, "page"),
                        McpToolSupport.optLong(args, "size"),
                        McpToolSupport.optLong(args, "projectId"),
                        McpToolSupport.optLong(args, "assigneeId"),
                        McpToolSupport.optString(args, "status"),
                        McpToolSupport.optBoolean(args, "overdue"),
                        McpToolSupport.optString(args, "sort"));
                return McpToolSupport.ok(objectMapper, taskService.page(query));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification getTask() {
        Tool tool = new Tool("get_task",
                "按 ID 查询任务详情。",
                """
                {
                  "type": "object",
                  "properties": {
                    "taskId": {"type": "integer", "description": "任务 ID"}
                  },
                  "required": ["taskId"]
                }
                """);
        return new SyncToolSpecification(tool, (exchange, args) -> {
            try {
                long taskId = McpToolSupport.requireLong(args, "taskId");
                return McpToolSupport.ok(objectMapper, taskService.get(taskId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }
}
