# ADR-0003：可配置工作区导航（一级模块顶部导航组件化）

- 状态：已接受（2026-06，待 P0 落地后转「已实现」）
- 相关：architecture-overview §1.2（信息架构）、V62 项目组件化（pm_component）、module-field（自定义字段）

## 背景与诉求

一级模块（项目/目标/审批/报表/文档/日历/简报…）进入后的二级导航当前为前端硬编码、左右布局不一、正文区被挤占。诉求：

1. **L1 导航可配**：一级模块顶部二级导航由「组件」拼装，租户可增删/排序/改名/显隐；全模块统一「顶部横向导航 + 全宽正文」。
2. **L2 子菜单可配**：组件下可再挂三级子菜单。
3. **L3 页面表单可配**（后续专题）：限定页面模板 + 字段配置，复用 module-field。

## 决策

- **配置归属：租户级为主**；未配置回落内置默认（fail-safe，零破坏）。个人级覆盖暂不做。
- **承载模块：`module-view`**（既有视图/工作区呈现域，已含 pm_view/工作台/表头偏好），不新建 Maven 模块，降低接线与耦合风险。
- **组件库（catalog）P0 由代码内置**（每模块默认组件清单 + 默认导航），租户编排结果持久化到新表 `pm_module_nav`；自定义新组件随 L3 再开放。
- **前端 `WorkspaceShell`**：统一渲染「顶部横向组件导航（el-menu horizontal 自带"更多"溢出）+ 可选三级子菜单 + 全宽正文」；组件 code→异步页面前端注册；未识别 code 占位。
- **不破坏**：迁移期老页面照常；migration 仅加表（向后兼容、可回滚）。

## 数据模型

- 新增 `pm_module_nav`（租户导航编排）：`tenant_id, module, component_code, parent_code, display_name, icon, sort, enabled` + 公共字段。空配置→内置默认。
- 复用既有 `pm_project_component`（项目级 Tab）；后续项目工作区并入同一套（项目=module=project 的 detail 容器）。

## 分阶段

- **P0（L1）**：`pm_module_nav` + 内置 catalog/默认导航 + `GET /workspace/nav/{module}` 解析 + `WorkspaceShell` + 各模块接入 + 管理后台编排页 + 文档页修复 + 全模块顶部导航统一。
- **P1（L2）**：`parent_code` 子菜单树渲染 + 编排器挂三级。
- **L3**：页面模板 + 字段配，单列专题。

## 影响 / 回滚

架构级、影响全局前端外壳与各模块路由；分阶段独立验收，fail-safe 回落，加表可回滚。
