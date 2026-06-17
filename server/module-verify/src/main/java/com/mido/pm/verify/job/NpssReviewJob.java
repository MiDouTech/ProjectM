package com.mido.pm.verify.job;

import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.service.ProjectService;
import com.mido.pm.verify.service.NpssReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * NPSS 价值验收到点扫描（npss-rule §2）：每日扫描 已结案 且 value_review_due_date&lt;=today 的项目，
 * 逐个发起价值验收（置价值验收中 + 建轮次 + 评分待办 + 通知 + 事件）。单个失败不阻断其余。
 * 注：阶段一不启用分片，扫描走默认租户；多租户逐租户扫描为分片启用后的事项。
 */
@Component
public class NpssReviewJob {

    private static final Logger log = LoggerFactory.getLogger(NpssReviewJob.class);

    private final ProjectService projectService;
    private final NpssReviewService npssReviewService;

    public NpssReviewJob(ProjectService projectService, NpssReviewService npssReviewService) {
        this.projectService = projectService;
        this.npssReviewService = npssReviewService;
    }

    /** 每日 02:00 扫描。 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scanDueReviews() {
        for (ProjectVO p : projectService.dueForValueReview()) {
            try {
                npssReviewService.startReview(p.id());
                log.info("NPSS 价值验收已发起：projectId={}", p.id());
            } catch (Exception e) {
                log.warn("NPSS 价值验收发起失败 projectId={}: {}", p.id(), e.getMessage());
            }
        }
    }
}
