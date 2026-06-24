package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.mcp.support.McpToolGuard;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.support.McpToolSupport;
import com.mido.pm.task.dto.TaskCreateDTO;
import com.mido.pm.task.dto.TaskQueryDTO;
import com.mido.pm.task.service.TaskService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务域工具：只读（分页查任务、查任务详情）+ 写（建任务、改状态、指派）。
 * 经 {@link TaskService} 调用，自动继承租户隔离、数据范围，并在同事务发 Outbox 领域事件与审计日志，
 * 与 REST 写入行为完全一致（直接执行 + 审计）。
 */
@Component
public class TaskMcpTools implements McpToolProvider {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;
    private final McpToolGuard guard;

    public TaskMcpTools(TaskService taskService, ObjectMapper objectMapper, McpToolGuard guard) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
        this.guard = guard;
    }

    @Override
    public List<SyncToolSpecification> tools() {
        return List.of(queryTasks(), getTask(), createTask(), updateTaskStatus(), assignTask());
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
        return guard.readOnly(tool, (exchange, args) -> {
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
        return guard.readOnly(tool, (exchange, args) -> {
            try {
                long taskId = McpToolSupport.requireLong(args, "taskId");
                return McpToolSupport.ok(objectMapper, taskService.get(taskId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification createTask() {
        Tool tool = new Tool("create_task",
                "在指定项目下创建任务。成功后返回创建的任务详情。",
                """
                {
                  "type": "object",
                  "properties": {
                    "projectId": {"type": "integer", "description": "所属项目 ID"},
                    "title": {"type": "string", "description": "任务标题"},
                    "description": {"type": "string", "description": "任务描述"},
                    "parentId": {"type": "integer", "description": "父任务 ID（拆子任务时填）"},
                    "assigneeId": {"type": "integer", "description": "负责人用户 ID"},
                    "priority": {"type": "integer", "description": "优先级"},
                    "stage": {"type": "string", "description": "项目阶段"},
                    "startDate": {"type": "string", "description": "开始日期 YYYY-MM-DD"},
                    "dueDate": {"type": "string", "description": "截止日期 YYYY-MM-DD"},
                    "isMilestone": {"type": "integer", "description": "是否里程碑：1 是 / 0 否"}
                  },
                  "required": ["projectId", "title"]
                }
                """);
        return guard.write(tool, (exchange, args) -> {
            try {
                TaskCreateDTO dto = new TaskCreateDTO(
                        McpToolSupport.requireString(args, "title"),
                        McpToolSupport.requireLong(args, "projectId"),
                        McpToolSupport.optLong(args, "parentId"),
                        McpToolSupport.optLong(args, "assigneeId"),
                        McpToolSupport.optInteger(args, "priority"),
                        McpToolSupport.optString(args, "stage"),
                        McpToolSupport.optLocalDate(args, "startDate"),
                        McpToolSupport.optLocalDate(args, "dueDate"),
                        McpToolSupport.optInteger(args, "isMilestone"),
                        McpToolSupport.optString(args, "description"),
                        null);
                Long id = taskService.create(dto);
                return McpToolSupport.ok(objectMapper, taskService.get(id));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification updateTaskStatus() {
        Tool tool = new Tool("update_task_status",
                "流转任务状态（受工作流校验约束）。",
                """
                {
                  "type": "object",
                  "properties": {
                    "taskId": {"type": "integer", "description": "任务 ID"},
                    "targetStatus": {"type": "string", "description": "目标状态码"}
                  },
                  "required": ["taskId", "targetStatus"]
                }
                """);
        return guard.write(tool, (exchange, args) -> {
            try {
                long taskId = McpToolSupport.requireLong(args, "taskId");
                String targetStatus = McpToolSupport.requireString(args, "targetStatus");
                taskService.changeStatus(taskId, targetStatus);
                return McpToolSupport.ok(objectMapper, taskService.get(taskId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification assignTask() {
        Tool tool = new Tool("assign_task",
                "指派或改派任务负责人。",
                """
                {
                  "type": "object",
                  "properties": {
                    "taskId": {"type": "integer", "description": "任务 ID"},
                    "assigneeId": {"type": "integer", "description": "新负责人用户 ID"}
                  },
                  "required": ["taskId", "assigneeId"]
                }
                """);
        return guard.write(tool, (exchange, args) -> {
            try {
                long taskId = McpToolSupport.requireLong(args, "taskId");
                long assigneeId = McpToolSupport.requireLong(args, "assigneeId");
                taskService.assign(taskId, assigneeId);
                return McpToolSupport.ok(objectMapper, taskService.get(taskId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }
}
