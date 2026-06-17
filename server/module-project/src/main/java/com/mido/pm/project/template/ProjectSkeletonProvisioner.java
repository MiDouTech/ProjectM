package com.mido.pm.project.template;

/**
 * 项目骨架供给端口：按模板生成项目时，落地跨域骨架（任务/干系人/审批流）。
 *
 * <p>遵循 CLAUDE.md「跨域只经 Service 接口或领域事件」：本端口由 project 域定义，
 * 任务/干系人/审批等域在各自模块建成后（Step 3/4/5）提供实现来落地对应骨架。
 * 当前为 no-op，建项目本体先行，骨架延后落地。</p>
 */
public interface ProjectSkeletonProvisioner {

    /**
     * 为新建项目供给骨架。
     *
     * @param projectId 新建项目 ID
     * @param config    已解析的模板配置（阶段/任务/干系人权重/审批流/验收方式）
     */
    void provision(Long projectId, TemplateConfig config);
}
