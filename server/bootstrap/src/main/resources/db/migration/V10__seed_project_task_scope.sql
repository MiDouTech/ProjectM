-- 数据范围资源示例种子：为超级管理员(role 1)登记 project/task 资源范围=all。
-- 作用：让 project/task 资源在角色数据范围中可见可配；admin 看全部（与默认 ALL 一致，此处显式登记）。
-- 其他角色按需在「角色 → 数据范围」中配 dept/self 等收敛。
INSERT INTO sys_role_data_scope (id, tenant_id, role_id, resource, scope, is_deleted) VALUES
  (2, 1, 1, 'project', 'all', 0),
  (3, 1, 1, 'task', 'all', 0);
