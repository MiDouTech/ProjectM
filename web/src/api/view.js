import request from './request'

/** 可配置工作区导航（ADR-0003）：一级模块顶部导航树 + 编排（管理后台）。 */
export const workspaceNavApi = {
  nav: (module) => request.get(`/workspace/nav/${module}`),
  catalog: (module) => request.get(`/workspace/catalog/${module}`),
  rawConfig: (module) => request.get(`/workspace/nav/${module}/config`),
  saveNav: (module, items) => request.put(`/workspace/nav/${module}`, items),
}

/** 可配置页面表单（ADR-0004 · L3）：内置字段目录 + 页面配置读写（前端再并自定义字段）。 */
export const pageConfigApi = {
  fields: (target) => request.get(`/workspace/page/fields/${target}`),
  get: (target, templateType) => request.get(`/workspace/page/${target}/${templateType}`),
  save: (target, templateType, config) => request.put(`/workspace/page/${target}/${templateType}`, config),
}

/** 列表表头偏好（每用户每列表，跨设备）。config: { columns:[key...], frozen:[key...] }。 */
export const tablePrefApi = {
  get: (listKey) => request.get(`/table-prefs/${listKey}`),
  save: (listKey, config) => request.put(`/table-prefs/${listKey}`, config),
}

/** 视图设计器：命名视图 CRUD（个人/项目级）。config 结构见后端 ViewConfig（锁定）。 */
export const viewApi = {
  list: (projectId) => request.get('/views', { params: { projectId } }),
  get: (id) => request.get(`/views/${id}`),
  create: (data) => request.post('/views', data),
  update: (id, data) => request.put(`/views/${id}`, data),
  remove: (id) => request.delete(`/views/${id}`),
}

/** 设计器字段字典（须与后端 ViewQueryTranslator 白名单一致） */
export const VIEW_GROUP_FIELDS = [
  { value: '', label: '不分组' },
  { value: 'status', label: '状态' },
  { value: 'assigneeId', label: '负责人' },
  { value: 'priority', label: '优先级' },
  { value: 'stage', label: '阶段' },
]
export const VIEW_SORT_FIELDS = [
  { value: 'dueDate', label: '截止日' },
  { value: 'startDate', label: '开始日' },
  { value: 'priority', label: '优先级' },
  { value: 'createTime', label: '创建时间' },
  { value: 'status', label: '状态' },
]
export const VIEW_FILTER_FIELDS = [
  { value: 'status', label: '状态' },
  { value: 'assigneeId', label: '负责人' },
  { value: 'priority', label: '优先级' },
  { value: 'stage', label: '阶段' },
  { value: 'dueDate', label: '截止日' },
  { value: 'title', label: '标题' },
]
export const VIEW_OPS = [
  { value: 'eq', label: '等于' },
  { value: 'ne', label: '不等于' },
  { value: 'gt', label: '大于' },
  { value: 'ge', label: '大于等于' },
  { value: 'lt', label: '小于' },
  { value: 'le', label: '小于等于' },
  { value: 'like', label: '包含' },
  { value: 'in', label: '属于(逗号分隔)' },
  { value: 'isNull', label: '为空' },
  { value: 'notNull', label: '非空' },
]
export const VIEW_COLUMNS = [
  { value: 'title', label: '标题' },
  { value: 'status', label: '状态' },
  { value: 'assigneeId', label: '负责人' },
  { value: 'priority', label: '优先级' },
  { value: 'stage', label: '阶段' },
  { value: 'dueDate', label: '截止日' },
  { value: 'startDate', label: '开始日' },
]
