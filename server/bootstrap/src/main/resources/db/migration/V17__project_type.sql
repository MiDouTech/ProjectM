-- V17 项目类型注册表：把原硬编码枚举 S/I/O 升级为 SaaS 租户可自配的一等实体。
-- 原本散落的硬规则（立项职级门槛 JobLevelRule / 是否走 NPSS NpssPolicy / 默认审批流 / 干系人权重模板）
-- 收敛为「项目类型」的可配置属性；本迁移只建表 + 种子，规则去硬编码与项目绑定在 P1 落地。
-- 设计：扁平建模——每个可立项的类型一条记录（O 三个子类各自独立，因其 NPSS/审批流/门槛不同）；
--       parent_code 提供报表汇总（O_* → O）。code 为租户内程序引用，禁与显示名混用。

CREATE TABLE pm_project_type (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,               -- 租户内唯一程序引用（如 S/I/O_NORMAL）
  name VARCHAR(64) NOT NULL,               -- 显示名（战略级/创新级/运营级·常规运营）
  parent_code VARCHAR(32),                 -- 上级类型码（报表汇总用，如 O_NORMAL→O）
  color VARCHAR(16),                       -- design-system 颜色 token 名（禁裸 hex）
  icon VARCHAR(32),
  sort INT DEFAULT 0,                      -- 排序（小在前）
  min_job_level VARCHAR(8),                -- 立项 Leader 最低职级门槛（空=不限），取代 JobLevelRule 硬编码
  requires_npss TINYINT DEFAULT 1,         -- 默认是否走 NPSS 价值验收，取代 NpssPolicy 硬编码
  default_flow_id BIGINT,                  -- 绑定默认审批流（approval_flow.id），取代 template.config.approvalFlow
  stakeholder_tpl JSON,                    -- 默认干系人权重模板（可选）
  status VARCHAR(16) DEFAULT 'active',     -- active/disabled
  description VARCHAR(255),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_code(tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目类型';

-- ===== 种子：把现有 S/I/O（含 O 三子类）落为租户 1 的 5 条类型数据 =====
-- min_job_level：S→L3 / O→L2 / I 不限（npss-rule §8）；requires_npss：整改/督办=0，其余=1（V14 同源）；
-- default_flow_id：指向 V4 已建的 5 条审批流（1 S_STANDARD / 2 I_POC / 3 O_NORMAL / 4 O_RECTIFY / 5 O_SUPERVISE）。
INSERT INTO pm_project_type
 (id, tenant_id, code, name, parent_code, color, sort, min_job_level, requires_npss, default_flow_id, status, description, is_deleted) VALUES
 (1, 1, 'S',           '战略级',          NULL, 'danger',  10, 'L3',  1, 1, 'active', 'IMP/MAP/EBC 等年度重点', 0),
 (2, 1, 'I',           '创新级',          NULL, 'warning', 20, NULL,  1, 2, 'active', '一米宽十米深探索/MTS 类', 0),
 (3, 1, 'O_NORMAL',    '运营级·常规运营', 'O',  'primary', 30, 'L2',  1, 3, 'active', '米多星球/PDA 改造等攻坚', 0),
 (4, 1, 'O_RECTIFY',   '运营级·定向整改', 'O',  'info',    40, 'L2',  0, 4, 'active', '部门月度问题及对策转化', 0),
 (5, 1, 'O_SUPERVISE', '运营级·专项督办', 'O',  'info',    50, 'L2',  0, 5, 'active', '管委会基础素养处分转化', 0);
