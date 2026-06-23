package com.mido.pm.briefing.controller;

import com.mido.pm.briefing.dto.BriefingReviewDTO;
import com.mido.pm.briefing.dto.BriefingReviewVO;
import com.mido.pm.briefing.dto.BriefingSaveDTO;
import com.mido.pm.briefing.dto.BriefingVO;
import com.mido.pm.briefing.service.BriefingReviewService;
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
    private final BriefingReviewService reviewService;

    public BriefingController(BriefingService briefingService, BriefingReviewService reviewService) {
        this.briefingService = briefingService;
        this.reviewService = reviewService;
    }

    /** 我的简报列表（type=daily/weekly/monthly，可空取全部）。 */
    @GetMapping
    public R<List<BriefingVO>> listMine(@RequestParam(required = false) String type) {
        return R.ok(briefingService.listMine(type));
    }

    /** 我评审的：当前用户作为评审人的已提交简报。 */
    @GetMapping("/review")
    public R<List<BriefingVO>> myReview(@RequestParam(required = false) String type) {
        return R.ok(reviewService.reviewBriefings(type, null));
    }

    /** 成员简报：我评审范围内的成员 id（前端配合成员资料展示/筛选）。 */
    @GetMapping("/reviewees")
    public R<List<Long>> reviewees() {
        return R.ok(reviewService.reviewees());
    }

    /** 简报统计：我评审范围内某类型的提交概览。 */
    @GetMapping("/stats")
    public R<com.mido.pm.briefing.dto.BriefingStatsVO> stats(@RequestParam(required = false) String type) {
        return R.ok(reviewService.stats(type));
    }

    /** 成员简报：某成员的已提交简报（须在我评审范围内）。 */
    @GetMapping("/members")
    public R<List<BriefingVO>> members(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) Long authorId) {
        return R.ok(reviewService.reviewBriefings(type, authorId));
    }

    /** 简报的评审批注列表（作者或评审人可见）。 */
    @GetMapping("/{id}/reviews")
    public R<List<BriefingReviewVO>> reviews(@PathVariable Long id) {
        return R.ok(reviewService.listReviews(id));
    }

    /** 评审人对简报批注。 */
    @PostMapping("/{id}/reviews")
    public R<Long> addReview(@PathVariable Long id, @Valid @RequestBody BriefingReviewDTO dto) {
        return R.ok(reviewService.addReview(id, dto));
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
