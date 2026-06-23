package com.mido.pm.calendar.job;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleReminderLog;
import com.mido.pm.calendar.event.CalendarEvents;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleReminderLogMapper;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日程提醒扫描：每 5 分钟扫描未来 24h 内、已到提醒点且未发送过的非循环日程，发 calendar.reminder.due。
 * 去重靠 pm_schedule_reminder_log。单个失败不阻断其余。
 * 注：阶段一不启用分片，扫描走默认租户（同 NpssReviewJob）；多租户逐租户扫描为分片启用后的事项。
 * 循环日程的逐次提醒为 P2。
 */
@Component
public class ReminderScanJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderScanJob.class);
    private static final int LOOKAHEAD_HOURS = 24;
    private static final int GRACE_DAYS = 1;

    private final PmScheduleMapper scheduleMapper;
    private final PmScheduleReminderLogMapper reminderLogMapper;
    private final DomainEventPublisher eventPublisher;

    public ReminderScanJob(PmScheduleMapper scheduleMapper,
                           PmScheduleReminderLogMapper reminderLogMapper,
                           DomainEventPublisher eventPublisher) {
        this.scheduleMapper = scheduleMapper;
        this.reminderLogMapper = reminderLogMapper;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        // 下界回看 GRACE_DAYS：覆盖宕机恢复后「提醒点已过但事件刚开始/未久」的漏发，配合去重日志只发一次。
        List<PmSchedule> upcoming = scheduleMapper.selectList(Wrappers.<PmSchedule>lambdaQuery()
                .eq(PmSchedule::getStatus, "confirmed")
                .isNull(PmSchedule::getRecurRule)
                .isNotNull(PmSchedule::getReminder)
                .ne(PmSchedule::getReminder, "")
                .ge(PmSchedule::getStartTime, now.minusDays(GRACE_DAYS))
                .le(PmSchedule::getStartTime, now.plusHours(LOOKAHEAD_HOURS)));
        for (PmSchedule s : upcoming) {
            try {
                dispatch(s, now);
            } catch (Exception e) {
                log.warn("日程提醒发送失败 scheduleId={}: {}", s.getId(), e.getMessage());
            }
        }
    }

    /** 对单条日程的各提醒档位：已到点且未发过则发事件并记日志。 */
    void dispatch(PmSchedule s, LocalDateTime now) {
        for (int minutes : parseMinutes(s.getReminder())) {
            LocalDateTime remindAt = s.getStartTime().minusMinutes(minutes);
            if (remindAt.isAfter(now)) {
                continue;
            }
            Long sent = reminderLogMapper.selectCount(Wrappers.<PmScheduleReminderLog>lambdaQuery()
                    .eq(PmScheduleReminderLog::getScheduleId, s.getId())
                    .eq(PmScheduleReminderLog::getRemindMinute, minutes));
            if (sent != null && sent > 0) {
                continue;
            }
            Map<String, Object> payload = Map.of(
                    "scheduleId", s.getId(),
                    "title", s.getTitle(),
                    "startTime", s.getStartTime().toString(),
                    "minutesBefore", minutes,
                    "organizerId", s.getOrganizerId() == null ? 0L : s.getOrganizerId(),
                    "occurredAt", now.toString());
            eventPublisher.publish(CalendarEvents.REMINDER_DUE, payload);

            PmScheduleReminderLog rl = new PmScheduleReminderLog();
            rl.setScheduleId(s.getId());
            rl.setRemindMinute(minutes);
            rl.setSentAt(now);
            reminderLogMapper.insert(rl);
        }
    }

    private List<Integer> parseMinutes(String reminderJson) {
        try {
            return JSONUtil.toList(JSONUtil.parseArray(reminderJson), Integer.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
