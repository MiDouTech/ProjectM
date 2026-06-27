-- V73 平台运营账号安全加固（P0-a）：
--   1) 首登强制改密标记 must_change_password，消除弱种子口令长期生效风险；
--   2) 登录失败锁定 fail_count / locked_until，防暴力破解；
--   3) 存量超管 (id=1) 标记为首登必须改密。
-- 不改动已发布 migration，追加列与回填。

ALTER TABLE sys_platform_admin
  ADD COLUMN must_change_password TINYINT  NOT NULL DEFAULT 0 COMMENT '首次登录需强制改密(1=是)' AFTER status,
  ADD COLUMN fail_count           INT      NOT NULL DEFAULT 0 COMMENT '连续登录失败次数'        AFTER must_change_password,
  ADD COLUMN locked_until         DATETIME NULL                COMMENT '锁定到期时间(为空或已过=未锁)' AFTER fail_count;

-- 存量超管首登强制改密（生产首次登录即被要求改掉默认口令）
UPDATE sys_platform_admin SET must_change_password = 1 WHERE id = 1;
