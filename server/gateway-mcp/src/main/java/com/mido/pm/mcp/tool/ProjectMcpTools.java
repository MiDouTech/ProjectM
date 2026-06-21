package com.mido.pm.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.mcp.support.McpToolProvider;
import com.mido.pm.mcp.support.McpToolSupport;
import com.mido.pm.project.dto.ProjectQueryDTO;
import com.mido.pm.project.service.ProjectService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 项目域只读工具：分页查项目、查项目详情、查"我参与的项目"。均经 {@link ProjectService} 调用，
 * 自动继承租户隔离与成员可见性数据范围。
 */
@Component
public class ProjectMcpTools implements McpToolProvider {

    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ProjectMcpTools(ProjectService projectService, ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SyncToolSpecification> tools() {
        return List.of(listProjects(), getProject(), listMyProjects());
    }

    private SyncToolSpecification listProjects() {
        Tool tool = new Tool("list_projects",
                "分页查询当前用户可见的项目，支持按类型、状态、关键字筛选。",
                """
                {
                  "type": "object",
                  "properties": {
                    "page": {"type": "integer", "description": "页码，从 1 起，默认 1"},
                    "size": {"type": "integer", "description": "每页条数，默认 20，上限 100"},
                    "category": {"type": "string", "description": "项目类型筛选，如 S/I/O"},
                    "status": {"type": "string", "description": "项目状态筛选"},
                    "keyword": {"type": "string", "description": "名称/编号关键字"}
                  }
                }
                """);
        return new SyncToolSpecification(tool, (exchange, args) -> {
            try {
                ProjectQueryDTO query = new ProjectQueryDTO(
                        McpToolSupport.optLong(args, "page"),
                        McpToolSupport.optLong(args, "size"),
                        McpToolSupport.optString(args, "category"),
                        McpToolSupport.optString(args, "status"),
                        null,
                        McpToolSupport.optString(args, "keyword"));
                return McpToolSupport.ok(objectMapper, projectService.page(query));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification getProject() {
        Tool tool = new Tool("get_project",
                "按 ID 查询项目详情。",
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
                return McpToolSupport.ok(objectMapper, projectService.get(projectId));
            } catch (IllegalArgumentException e) {
                return McpToolSupport.error(e.getMessage());
            }
        });
    }

    private SyncToolSpecification listMyProjects() {
        Tool tool = new Tool("list_my_projects",
                "查询当前用户参与的全部项目。",
                """
                {"type": "object", "properties": {}}
                """);
        return new SyncToolSpecification(tool,
                (exchange, args) -> McpToolSupport.ok(objectMapper, projectService.myProjects()));
    }
}
