package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.PersonWorkHourSummaryVO;
import com.mido.pm.task.dto.WorkHourCreateDTO;
import com.mido.pm.task.dto.WorkHourSummaryVO;
import com.mido.pm.task.dto.WorkHourUpdateDTO;
import com.mido.pm.task.dto.WorkHourVO;
import com.mido.pm.task.service.WorkHourService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 工时：登记/修改 + 记录列表 + 任务级/项目级/人员级汇总（口径统一）。 */
@RestController
@RequestMapping("/api/v1/work-hours")
public class WorkHourController {

    private final WorkHourService workHourService;

    public WorkHourController(WorkHourService workHourService) {
        this.workHourService = workHourService;
    }

    @PostMapping
    public R<Long> log(@Valid @RequestBody WorkHourCreateDTO dto) {
        return R.ok(workHourService.log(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WorkHourUpdateDTO dto) {
        workHourService.update(id, dto);
        return R.ok();
    }

    /** 某任务的工时记录列表。 */
    @GetMapping
    public R<List<WorkHourVO>> list(@RequestParam Long taskId) {
        return R.ok(workHourService.list(taskId));
    }

    /** 任务级汇总。 */
    @GetMapping("/summary/task")
    public R<WorkHourSummaryVO> taskSummary(@RequestParam Long taskId) {
        return R.ok(workHourService.taskSummary(taskId));
    }

    /** 项目级汇总。 */
    @GetMapping("/summary/project")
    public R<WorkHourSummaryVO> projectSummary(@RequestParam Long projectId) {
        return R.ok(workHourService.projectSummary(projectId));
    }

    /** 人员级汇总（项目内按人分组）。 */
    @GetMapping("/summary/person")
    public R<List<PersonWorkHourSummaryVO>> personSummary(@RequestParam Long projectId) {
        return R.ok(workHourService.personSummary(projectId));
    }
}
