package com.mido.pm.verify.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.verify.dto.NpssReviewVO;
import com.mido.pm.verify.dto.NpssScoreVO;
import com.mido.pm.verify.dto.ScoreSubmitDTO;
import com.mido.pm.verify.service.NpssReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NPSS 价值验收：干系人评分（幂等）+ 轮次详情。轮次发起由定时 Job 触发，无对外创建接口。
 */
@RestController
@RequestMapping("/api/v1/npss/reviews")
public class NpssController {

    private final NpssReviewService npssReviewService;

    public NpssController(NpssReviewService npssReviewService) {
        this.npssReviewService = npssReviewService;
    }

    /**
     * 干系人提交评分。幂等：同一 (review, stakeholder) 重复提交不重复计分（防重复打分）。
     * 支持 Idempotency-Key 头（客户端重试令牌；服务端按 review+stakeholder 去重保证幂等）。
     */
    @PostMapping("/{reviewId}/scores")
    public R<NpssScoreVO> submitScore(@PathVariable Long reviewId,
                                      @Valid @RequestBody ScoreSubmitDTO dto,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return R.ok(npssReviewService.submitScore(reviewId, dto));
    }

    @GetMapping("/{reviewId}")
    public R<NpssReviewVO> get(@PathVariable Long reviewId) {
        return R.ok(npssReviewService.get(reviewId));
    }

    /** 某项目的 NPSS 轮次（验收 Tab 用）。 */
    @GetMapping
    public R<java.util.List<NpssReviewVO>> listByProject(@org.springframework.web.bind.annotation.RequestParam Long projectId) {
        return R.ok(npssReviewService.listByProject(projectId));
    }
}
