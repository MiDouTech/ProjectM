-- 数据范围（按部门）：pm_project/pm_task 增加 dept_id（归属部门）列，供数据范围拦截器按部门过滤。
-- 项目部门 = 立项时 leader 所属部门；任务部门 = 所属项目部门（创建时落库）。
ALTER TABLE pm_project
  ADD COLUMN dept_id BIGINT NULL AFTER leader_id,
  ADD KEY idx_proj_dept (dept_id);
ALTER TABLE pm_task
  ADD COLUMN dept_id BIGINT NULL AFTER assignee_id,
  ADD KEY idx_task_dept (dept_id);

-- 存量回填：项目部门取 leader 部门；任务部门取所属项目部门。
UPDATE pm_project p JOIN sys_user u ON u.id = p.leader_id
  SET p.dept_id = u.dept_id WHERE p.dept_id IS NULL;
UPDATE pm_task t JOIN pm_project p ON p.id = t.project_id
  SET t.dept_id = p.dept_id WHERE t.dept_id IS NULL;
