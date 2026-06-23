-- V38 日历/日程域（calendar.*）：独立事件型日程。
--   pm_calendar              日历容器：我的日程/会议安排/团队/资源；is_default 标记用户默认「我的日程」。
--   pm_schedule              日程主记录：起止时间/全天/地点/RSVP 开关/来源(手建或任务派生)。
--   pm_schedule_participant  参与人 + RSVP 状态(pending/accepted/tentative/declined)。
--   说明：循环(recur_rule)、提醒(reminder)列本期预留不启用，循环展开/提醒投递为 P1。
--   区别于任务日历视图(pm_view type=calendar)，二者不共表。

CREATE TABLE pm_calendar (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  name        VARCHAR(64) NOT NULL COMMENT '日历名称',
  type        VARCHAR(16) NOT NULL DEFAULT 'personal' COMMENT '类型: personal 我的日程 / meeting 会议安排 / team 团队 / resource 资源',
  owner_id    BIGINT      COMMENT '归属用户(personal 默认日历/会议创建者)',
  color       VARCHAR(16) COMMENT '颜色 token(design-system)',
  visibility  VARCHAR(16) NOT NULL DEFAULT 'private' COMMENT '可见性: private 私有 / busy 仅忙闲 / public 公开',
  is_default  TINYINT     NOT NULL DEFAULT 0 COMMENT '是否用户默认日历(我的日程)',
  status      VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active 启用 / archived 归档',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_owner (tenant_id, owner_id)
) COMMENT='日历容器';

CREATE TABLE pm_schedule (
  id             BIGINT       NOT NULL PRIMARY KEY,
  tenant_id      BIGINT       NOT NULL,
  calendar_id    BIGINT       NOT NULL COMMENT '归属日历(pm_calendar.id)',
  title          VARCHAR(256) NOT NULL COMMENT '日程标题',
  description    TEXT         COMMENT '描述',
  start_time     DATETIME     NOT NULL COMMENT '开始时间',
  end_time       DATETIME     NOT NULL COMMENT '结束时间',
  all_day        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否全天',
  location       VARCHAR(256) COMMENT '地点',
  recur_rule     VARCHAR(512) COMMENT 'RRULE 循环规则(P1 启用), 空=不重复',
  reminder       JSON         COMMENT '提醒配置(P1)',
  allow_feedback TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许参与人 RSVP 反馈',
  source_type    VARCHAR(16)  NOT NULL DEFAULT 'manual' COMMENT '来源: manual 手建 / task 任务派生 / meeting 会议',
  source_id      BIGINT       COMMENT '来源源 id(source_type=task → pm_task.id)',
  organizer_id   BIGINT       COMMENT '组织者/创建者(sys_user.id)',
  status         VARCHAR(16)  NOT NULL DEFAULT 'confirmed' COMMENT 'confirmed 确认 / cancelled 取消',
  create_by      BIGINT,
  create_time    DATETIME,
  update_by      BIGINT,
  update_time    DATETIME,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  KEY idx_cal (calendar_id),
  KEY idx_range (tenant_id, start_time, end_time)
) COMMENT='日程主记录';

CREATE TABLE pm_schedule_participant (
  id            BIGINT       NOT NULL PRIMARY KEY,
  tenant_id     BIGINT       NOT NULL,
  schedule_id   BIGINT       NOT NULL COMMENT '所属日程(pm_schedule.id)',
  user_id       BIGINT       COMMENT '内部参与人(sys_user.id)',
  external_name VARCHAR(128) COMMENT '外部参与人姓名(user_id 为空时)',
  role          VARCHAR(16)  NOT NULL DEFAULT 'required' COMMENT 'organizer 组织者 / required 必须 / optional 可选',
  rsvp_status   VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending 待反馈 / accepted 参加 / tentative 暂定 / declined 谢绝',
  create_by     BIGINT,
  create_time   DATETIME,
  update_by     BIGINT,
  update_time   DATETIME,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  KEY idx_sch (schedule_id),
  KEY idx_user (tenant_id, user_id)
) COMMENT='日程参与人 + RSVP';
