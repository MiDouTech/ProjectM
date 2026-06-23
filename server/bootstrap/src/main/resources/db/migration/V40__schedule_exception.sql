-- V40 循环日程例外（calendar.* P1）：对循环日程的单次「取消/改期」覆盖。
--   pm_schedule.recur_rule 存紧凑 JSON：{"freq":"DAILY|WEEKLY|MONTHLY|YEARLY","interval":1,"count":10,"until":"2026-12-31"}
--   空=不循环。展开在区间查询时按规则生成虚拟实例，再套用本表例外。
--   pm_schedule_exception：occur_date=被改实例的原始开始日期；action=cancel 取消 / modify 改期；
--   override=modify 覆盖内容 JSON {startTime,endTime,title,location}。

CREATE TABLE pm_schedule_exception (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  schedule_id BIGINT      NOT NULL COMMENT '所属循环日程(pm_schedule.id)',
  occur_date  DATE        NOT NULL COMMENT '被改实例的原始开始日期',
  action      VARCHAR(16) NOT NULL DEFAULT 'cancel' COMMENT 'cancel 取消 / modify 改期',
  override    JSON        COMMENT 'modify 覆盖内容: {startTime,endTime,title,location}',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_sch_date (schedule_id, occur_date)
) COMMENT='循环日程例外';
