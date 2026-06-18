package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.GroupedTasksVO;
import com.mido.pm.task.dto.ViewQueryRequest;
import com.mido.pm.task.service.TaskViewQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 按视图（viewId 或内联 config）查询任务，返回分组/排序/筛选后的结果。 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskViewQueryController {

    private final TaskViewQueryService taskViewQueryService;

    public TaskViewQueryController(TaskViewQueryService taskViewQueryService) {
        this.taskViewQueryService = taskViewQueryService;
    }

    @PostMapping("/view-query")
    public R<GroupedTasksVO> viewQuery(@Valid @RequestBody ViewQueryRequest req) {
        return R.ok(taskViewQueryService.query(req));
    }
}
