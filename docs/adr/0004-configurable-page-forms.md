# ADR-0004：可配置页面表单（L3，限定模板 + 字段配）

- 状态：已接受（决策已定，待 L3.0 落地）
- 相关：ADR-0003（可配置导航 L1/L2）、module-field（pm_field_def 自定义字段）、table-prefs（列表表头偏好）

## 背景

一级模块组件承载的页面（form/detail/list）当前字段/布局写死。诉求：页面表单可配（限定模板 + 字段配，非自由拖拽）。
现状盘点：`pm_field_def`（scope=task/project）已含 fieldKey/name/type/options/dataSourceId/required/sortNo/enabled，自定义字段已能渲染；缺的是**内置字段纳入可配** + **页面编排层**（分组/只读/宽度/布局/模板类型）。故 L3 是「在既有字段模型上加一层页面编排」，不是从零做字段。

## 决策（已与用户确认）

- **试点**：form 表单，先试点「任务」（其自定义字段渲染已存在，增量最小、风险可控）。
- **存储**：新建 `pm_page_config` 表（租户级），与导航/字段解耦；空配置回落默认（内置字段全显示，fail-safe）。
- **编排粒度**：一次做全——字段选/排序/显隐/必填覆盖 + **分组/只读/宽度/布局**。
- **承载模块**：`module-view`（视图/工作区呈现域）。
- **字段合成**：内置字段由代码 `PageFieldCatalog` 按实体声明；自定义字段沿用 `pm_field_def`。**前端合成**（内置 catalog 接口 + 既有 `fieldDefApi`），避免 module-view↔module-field 耦合。

## 数据模型

`pm_page_config`：`tenant_id, target(task/project/…), template_type(form/detail/list), config JSON` + 公共字段，唯一键 `(tenant_id, target, template_type)`。
config 形如：
```json
{
  "layout": { "columns": 2 },
  "fields": [
    { "fieldKey": "title", "source": "builtin", "group": "基本", "required": true, "readonly": false, "width": 12, "sort": 0 },
    { "fieldKey": "cf_xxx", "source": "custom", "group": "扩展", "required": false, "readonly": false, "width": 6, "sort": 1 }
  ]
}
```

## 接口（module-view）

- `GET /workspace/page-fields/{target}` → 内置字段目录（前端再并自定义字段）。
- `GET/PUT /workspace/page/{target}/{templateType}` → 读/存页面配置（空=默认）。

## 前端

- `DynamicForm.vue`：按 config 渲染 el-form（按字段 type 出控件 + 校验 + 分组/只读/宽度/布局），数据双绑：内置→实体属性、自定义→`custom_fields`。
- 管理后台「页面配置」编辑器：选 target+模板 → 左可选字段（内置+自定义）/右已选（拖拽排序、设分组/必填/只读/宽度/布局）→ 保存。复用 TableColumnSetting 交互范式。

## 分阶段

- **L3.0 基座**：`PmPageConfig` + 迁移 + `PageFieldCatalog`(task) + 字段目录/配置读写接口 + 单测。
- **L3.1 form 渲染 + 编辑器**：`DynamicForm` + 编辑器，接入任务建/编辑表单（**fail-safe**：无配置走原表单）。
- **L3.2 detail**、**L3.3 list**（并入 table-prefs + 筛选）后续。

## 风险 / 不破坏

- 改任务建/编辑表单有风险（校验/类型/内置↔自定义双绑定）；迁移期**无配置回落原表单**，分步 retrofit，可回滚（仅加表）。
- 属架构级；每阶段独立提交、构建验证。
