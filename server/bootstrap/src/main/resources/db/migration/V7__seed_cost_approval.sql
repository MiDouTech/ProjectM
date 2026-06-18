-- 费用审批默认流（biz_type=cost）：复用审批引擎；单节点 PMO 审批（审批人见 V4 占位账号 11）。
-- name=COST_DEFAULT 供 CostService 经 ApprovalFlowService.resolveFlowId 绑定。
INSERT INTO approval_flow (id, tenant_id, name, biz_type, mode, definition, is_deleted)
VALUES (6, 1, 'COST_DEFAULT', 'cost', 'fixed',
'{"nodes":[{"key":"pmo","name":"PMO审批","approvers":[11],"mode":"or"}]}',
0);
