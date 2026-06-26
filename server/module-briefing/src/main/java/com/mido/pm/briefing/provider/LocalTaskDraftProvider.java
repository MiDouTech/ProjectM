package com.mido.pm.briefing.provider;

import com.mido.pm.task.dto.CalendarTaskVO;
import com.mido.pm.task.service.TaskService;
import com.mido.pm.task.service.WorkItemMetaResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则式草稿生成（本地实现）：拉当前用户周期内截止的任务流水，汇总为要点列表。
 * 不调用 LLM；待 AI 层启用后由 LLM 实现替换此 Bean。
 */
@Component
public class LocalTaskDraftProvider implements BriefingDraftProvider {

    private final TaskService taskService;
    private final WorkItemMetaResolver metaResolver;

    public LocalTaskDraftProvider(TaskService taskService, WorkItemMetaResolver metaResolver) {
        this.taskService = taskService;
        this.metaResolver = metaResolver;
    }

    @Override
    public String summarizeWork(Long userId, LocalDate from, LocalDate to) {
        List<CalendarTaskVO> tasks = taskService.calendarTasks(from, to);
        if (tasks.isEmpty()) {
            return "本周期暂无关联任务，请手动补充。";
        }
        // 完成标记按状态库「已完成」元类别判定（含自定义状态/已验收），未配置回落默认终态
        Set<String> done = metaResolver.doneStatusNames();
        return "本周期相关任务：\n" + tasks.stream()
                .map(t -> "- " + t.title() + (done.contains(t.status()) ? "（已完成）" : ""))
                .collect(Collectors.joining("\n"));
    }
}
