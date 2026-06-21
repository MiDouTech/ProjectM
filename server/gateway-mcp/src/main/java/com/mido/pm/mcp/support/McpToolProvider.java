package com.mido.pm.mcp.support;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.util.List;

/**
 * MCP 工具提供者。各领域把自己的只读工具暴露为一组 {@link SyncToolSpecification}，
 * 由 {@code McpServerConfig} 聚合后注册到 MCP Server。新增一域工具只需新增一个实现并交给 Spring 管理。
 */
public interface McpToolProvider {

    /** 本域对外暴露的 MCP 工具规格列表。 */
    List<SyncToolSpecification> tools();
}
