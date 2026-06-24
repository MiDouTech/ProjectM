package com.mido.pm.task.template;

import com.mido.pm.project.template.ProjectSkeletonProvisioner;
import com.mido.pm.project.template.TemplateConfig;
import com.mido.pm.task.dto.TaskCreateDTO;
import com.mido.pm.task.service.TaskService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务骨架供给：按模板生成项目时，将模板 config.phases[].tasks[] 落地为项目任务。
 *
 * <p>实现 project 域定义的 {@link ProjectSkeletonProvisioner} 端口（跨域经 Service 接口，符合 CLAUDE.md §4，
 * module-task 可依赖 module-project，反向则成环）。标 {@link Primary} 取代占位 NoopProjectSkeletonProvisioner。
 * 每个任务的 stage 取所属阶段名，便于按阶段分组；建任务复用 {@link TaskService#create}（同事务发事件、记审计）。</p>
 */
@Primary
@Component
public class TaskSkeletonProvisioner implements ProjectSkeletonProvisioner {

    private final TaskService taskService;

    public TaskSkeletonProvisioner(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void provision(Long projectId, TemplateConfig config) {
        if (config == null || config.phases() == null) {
            return;
        }
        for (TemplateConfig.Phase phase : config.phases()) {
            List<String> tasks = phase.tasks();
            if (tasks == null) {
                continue;
            }
            for (String title : tasks) {
                if (title == null || title.isBlank()) {
                    continue;
                }
                // stage=阶段名；其余留空，责任人/排期由后续编辑补充
                taskService.create(new TaskCreateDTO(
                        title, projectId, null, null, null, phase.name(),
                        null, null, 0, null, null));
            }
        }
    }
}
