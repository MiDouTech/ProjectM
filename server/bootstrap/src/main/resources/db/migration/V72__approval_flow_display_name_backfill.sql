-- V72 审批流中文展示名补全：V22 已按 name 回填存量；本迁移补 V22 之后由运行时
-- ApprovalTenantProvisioner 新建、display_name 为空的内置流（provisioner 现已直接写入，
-- 此处兜底历史空值）。仅在 display_name 为空时填，避免覆盖运营自定义的展示名。
UPDATE approval_flow SET display_name = '战略级标准流程'   WHERE name = 'S_STANDARD'  AND (display_name IS NULL OR display_name = '');
UPDATE approval_flow SET display_name = '创新级 POC 流程'  WHERE name = 'I_POC'       AND (display_name IS NULL OR display_name = '');
UPDATE approval_flow SET display_name = '常规运营流程'     WHERE name = 'O_NORMAL'    AND (display_name IS NULL OR display_name = '');
UPDATE approval_flow SET display_name = '定向整改流程'     WHERE name = 'O_RECTIFY'   AND (display_name IS NULL OR display_name = '');
UPDATE approval_flow SET display_name = '专项督办流程'     WHERE name = 'O_SUPERVISE' AND (display_name IS NULL OR display_name = '');
UPDATE approval_flow SET display_name = '费用审批（默认）' WHERE name = 'COST_DEFAULT' AND (display_name IS NULL OR display_name = '');
