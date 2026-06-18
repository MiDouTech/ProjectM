-- V15 通知跳转定位：为站内信增加业务定位字段，支持「点击通知 → 跳转到对应业务详情」。
-- biz_type/biz_id 标识业务对象（如 approval/task/project）；link 为前端可直接跳转的路由（best-effort，可空）。
ALTER TABLE pm_notification ADD COLUMN biz_type VARCHAR(32) NULL COMMENT '业务类型(approval/task/project)' AFTER type;
ALTER TABLE pm_notification ADD COLUMN biz_id BIGINT NULL COMMENT '业务对象ID' AFTER biz_type;
ALTER TABLE pm_notification ADD COLUMN link VARCHAR(256) NULL COMMENT '前端跳转路由(可空)' AFTER payload;
