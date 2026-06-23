-- V41 日程提醒（calendar.* P1）：提醒发送去重日志。
--   pm_schedule.reminder 存提前提醒分钟数 JSON 数组，如 [15,60]（提前15分钟、提前1小时）。
--   pm_schedule_reminder_log：记录某日程某提醒档已发送，避免定时扫描重复发送。
--   定时任务每 5 分钟扫描即将开始且到点的非循环日程，发 calendar.reminder.due 事件。

CREATE TABLE pm_schedule_reminder_log (
  id            BIGINT   NOT NULL PRIMARY KEY,
  tenant_id     BIGINT   NOT NULL,
  schedule_id   BIGINT   NOT NULL COMMENT '日程(pm_schedule.id)',
  remind_minute INT      NOT NULL COMMENT '提前分钟数档位',
  sent_at       DATETIME COMMENT '发送时间',
  create_by     BIGINT,
  create_time   DATETIME,
  update_by     BIGINT,
  update_time   DATETIME,
  is_deleted    TINYINT  NOT NULL DEFAULT 0,
  KEY idx_sch (schedule_id)
) COMMENT='日程提醒发送去重日志';
