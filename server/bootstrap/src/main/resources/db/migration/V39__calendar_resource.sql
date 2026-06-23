-- V39 日历资源 + 占用（calendar.* P1）：会议室/设备资源与日程占用，用于资源冲突检测。
--   pm_calendar_resource  资源台账：会议室(room)/设备(device)，capacity 容量、location 位置。
--   pm_schedule_resource  日程-资源占用：一条日程占用一个资源；冲突=同资源被另一未取消日程时间重叠。

CREATE TABLE pm_calendar_resource (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  name        VARCHAR(64) NOT NULL COMMENT '资源名称',
  type        VARCHAR(16) NOT NULL DEFAULT 'room' COMMENT '类型: room 会议室 / device 设备',
  capacity    INT         COMMENT '容量(会议室人数)',
  location    VARCHAR(256) COMMENT '位置',
  status      VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active 启用 / disabled 停用',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) COMMENT='日历资源台账';

CREATE TABLE pm_schedule_resource (
  id          BIGINT   NOT NULL PRIMARY KEY,
  tenant_id   BIGINT   NOT NULL,
  schedule_id BIGINT   NOT NULL COMMENT '所属日程(pm_schedule.id)',
  resource_id BIGINT   NOT NULL COMMENT '占用资源(pm_calendar_resource.id)',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT  NOT NULL DEFAULT 0,
  KEY idx_sch (schedule_id),
  KEY idx_res (resource_id)
) COMMENT='日程资源占用';
