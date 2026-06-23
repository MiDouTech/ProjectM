package com.mido.pm.briefing.controller;

import com.mido.pm.briefing.dto.BriefingSaveDTO;
import com.mido.pm.briefing.dto.BriefingVO;
import com.mido.pm.briefing.service.BriefingService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 简报：保存草稿 / 提交 / 我的简报 / 详情。 */
@RestController
@RequestMapping("/api/v1/briefings")
public class BriefingController {

    private final BriefingService briefingService;

    public BriefingController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    /** 我的简报列表（type=daily/weekly/monthly，可空取全部）。 */
    @GetMapping
    public R<List<BriefingVO>> listMine(@RequestParam(required = false) String type) {
        return R.ok(briefingService.listMine(type));
    }

    @GetMapping("/{id}")
    public R<BriefingVO> get(@PathVariable Long id) {
        return R.ok(briefingService.get(id));
    }

    /** 保存草稿（按 模板+周期 幂等 upsert）。 */
    @PostMapping
    public R<Long> save(@Valid @RequestBody BriefingSaveDTO dto) {
        return R.ok(briefingService.save(dto));
    }

    /** 提交简报。 */
    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        briefingService.submit(id);
        return R.ok();
    }
}
