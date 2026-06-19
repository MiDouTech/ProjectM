-- V22 审批流中文展示名：approval_flow.name 是代码键（resolveFlowId/模板/测试引用，不可变），
--   新增 display_name 仅供前端展示，运营按中文识别审批流。已有流程回填中文名。

ALTER TABLE approval_flow
  ADD COLUMN display_name VARCHAR(64) COMMENT '中文展示名(UI 显示用; name 仍为代码键)' AFTER name;

-- 回填 V4/V7 种子流程的中文展示名（按 name 代码键匹配）
UPDATE approval_flow SET display_name = '费用审批（默认）'   WHERE name = 'COST_DEFAULT';
UPDATE approval_flow SET display_name = '战略级标准流程'     WHERE name = 'S_STANDARD';
UPDATE approval_flow SET display_name = '创新级 POC 流程'    WHERE name = 'I_POC';
UPDATE approval_flow SET display_name = '常规运营流程'       WHERE name = 'O_NORMAL';
UPDATE approval_flow SET display_name = '定向整改流程'       WHERE name = 'O_RECTIFY';
UPDATE approval_flow SET display_name = '专项督办流程'       WHERE name = 'O_SUPERVISE';
