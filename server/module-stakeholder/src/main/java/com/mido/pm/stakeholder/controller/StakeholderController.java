package com.mido.pm.stakeholder.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.stakeholder.domain.RoleWeight;
import com.mido.pm.stakeholder.dto.MatrixPointVO;
import com.mido.pm.stakeholder.dto.SaveWeightsDTO;
import com.mido.pm.stakeholder.dto.StakeholderCreateDTO;
import com.mido.pm.stakeholder.dto.StakeholderUpdateDTO;
import com.mido.pm.stakeholder.dto.StakeholderVO;
import com.mido.pm.stakeholder.service.StakeholderService;
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

/** 干系人 CRUD / 默认权重预置 / 权重保存(硬校验) / 权力利益矩阵。 */
@RestController
@RequestMapping("/api/v1/stakeholders")
public class StakeholderController {

    private final StakeholderService stakeholderService;

    public StakeholderController(StakeholderService stakeholderService) {
        this.stakeholderService = stakeholderService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody StakeholderCreateDTO dto) {
        return R.ok(stakeholderService.create(dto));
    }

    @GetMapping("/{id}")
    public R<StakeholderVO> get(@PathVariable Long id) {
        return R.ok(stakeholderService.get(id));
    }

    @GetMapping
    public R<List<StakeholderVO>> list(@RequestParam Long projectId) {
        return R.ok(stakeholderService.list(projectId));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody StakeholderUpdateDTO dto) {
        stakeholderService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stakeholderService.delete(id);
        return R.ok();
    }

    /** 立项默认权重预置（npss-rule §6）。 */
    @GetMapping("/default-weights")
    public R<List<RoleWeight>> defaultWeights(@RequestParam String category,
                                              @RequestParam(required = false) String subCategory) {
        return R.ok(stakeholderService.defaultWeights(category, subCategory));
    }

    /** 保存/微调项目干系人权重（受益方≥50% 且和=100% 硬校验）。 */
    @PostMapping("/weights")
    public R<Void> saveWeights(@Valid @RequestBody SaveWeightsDTO dto) {
        stakeholderService.saveWeights(dto);
        return R.ok();
    }

    /** 权力利益矩阵数据（四象限）。 */
    @GetMapping("/matrix")
    public R<List<MatrixPointVO>> matrix(@RequestParam Long projectId) {
        return R.ok(stakeholderService.matrix(projectId));
    }
}
