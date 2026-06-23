package com.mido.pm.briefing.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简报到点提醒：每日 18:00 扫描周期已结束(近 {@value #RECENT_DAYS} 天内)仍为草稿的简报，
 * 发 briefing.reminder.due 催交作者；未提交则逐日重提，单条失败不阻断。
 * 注：阶段一不启用分片，扫描走默认租户(同 NpssReviewJob)；多租户逐租户扫描为分片启用后的事项。
 * 「全员应交」提醒需简报指派/配置模型，属 P2。
 */
@Component
public class BriefingReminderJob {

    private static final Logger log = LoggerFactory.getLogger(BriefingReminderJob.class);
    private static final int RECENT_DAYS = 3;

    private final PmBriefingMapper briefingMapper;
    private final DomainEventPublisher eventPublisher;

    public BriefingReminderJob(PmBriefingMapper briefingMapper, DomainEventPublisher eventPublisher) {
        this.briefingMapper = briefingMapper;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void scan() {
        LocalDate today = LocalDate.now();
        List<PmBriefing> drafts = briefingMapper.selectList(Wrappers.<PmBriefing>lambdaQuery()
                .eq(PmBriefing::getStatus, "draft")
                .isNotNull(PmBriefing::getPeriodEnd)
                .le(PmBriefing::getPeriodEnd, today)
                .ge(PmBriefing::getPeriodEnd, today.minusDays(RECENT_DAYS)));
        for (PmBriefing b : drafts) {
            try {
                publishReminder(b);
            } catch (Exception e) {
                log.warn("简报催交失败 briefingId={}: {}", b.getId(), e.getMessage());
            }
        }
    }

    void publishReminder(PmBriefing b) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("briefingId", b.getId());
        payload.put("authorId", b.getAuthorId());
        payload.put("type", b.getType());
        payload.put("periodKey", b.getPeriodKey());
        payload.put("occurredAt", LocalDate.now().toString());
        eventPublisher.publish(BriefingEvents.REMINDER_DUE, payload);
    }
}
