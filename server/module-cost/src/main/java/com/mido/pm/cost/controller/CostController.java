package com.mido.pm.cost.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.cost.dto.CostCreateDTO;
import com.mido.pm.cost.dto.CostUpdateDTO;
import com.mido.pm.cost.dto.CostVO;
import com.mido.pm.cost.service.CostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 费用：CRUD + 提报审批（biz_type=cost）。预算超限由服务侧发事件预警，不阻断。 */
@RestController
@RequestMapping("/api/v1/costs")
public class CostController {

    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody CostCreateDTO dto) {
        return R.ok(costService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CostUpdateDTO dto) {
        costService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        costService.delete(id);
        return R.ok();
    }

    /** 某项目的费用清单（费用管理 Tab）。 */
    @GetMapping
    public R<List<CostVO>> listByProject(@RequestParam Long projectId) {
        return R.ok(costService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<CostVO> get(@PathVariable Long id) {
        return R.ok(costService.get(id));
    }

    /** 提报审批：发起 cost 审批实例。 */
    @PostMapping("/{id}/submit")
    public R<Long> submit(@PathVariable Long id) {
        return R.ok(costService.submit(id));
    }
}
