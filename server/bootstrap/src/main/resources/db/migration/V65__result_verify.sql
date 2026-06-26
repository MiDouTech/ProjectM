-- V65 结果验收（铁三角）结论表：补齐「结果验收 → 已结案」缺失的后端判定与硬闸门。
-- 原本结果验收仅前端只读展示、可手动直接结案；本表记录 PMO 结论(pass/fail) + 时间/成本/范围
-- 三项达标快照，ResultVerifyGate 据「最新 pass 结论」放行结案，否则拦截（architecture-overview §2.2）。
-- 每次验收追加一条，最新一条为权威（支持验收不通过打回后再次验收）。

CREATE TABLE pm_result_verify (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  verdict VARCHAR(8) NOT NULL,              -- pass 达标 / fail 不达标
  on_time TINYINT,                          -- 时间达标 1是/0否
  in_budget TINYINT,                        -- 成本达标 1是/0否
  in_scope TINYINT,                         -- 范围达标 1是/0否
  completion_rate DECIMAL(5,2),             -- 验收时任务完成率快照
  remark VARCHAR(1000),
  verified_by BIGINT,                       -- 录入人(PMO)
  verified_at DATETIME,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结果验收(铁三角)结论';
