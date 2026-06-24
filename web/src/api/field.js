import request from './request'

/** 自定义字段定义：按作用域(task/project)增删改查（配置页 P2-4b） */
export const fieldDefApi = {
  list: (scope, enabledOnly = false) =>
    request.get('/field-defs', { params: { scope, enabledOnly } }),
  create: (data) => request.post('/field-defs', data),
  update: (id, data) => request.put(`/field-defs/${id}`, data),
  remove: (id) => request.delete(`/field-defs/${id}`),
}

/** 自定义字段值：按实体读取「定义+值」、批量写入（详情抽屉读写） */
export const fieldValueApi = {
  list: (entityType, entityId) =>
    request.get('/field-values', { params: { entityType, entityId } }),
  // data: { entityType, entityId, values: [{ fieldId, value }] }
  save: (data) => request.put('/field-values', data),
}

/** 视图中自定义字段列/筛选/排序的引用前缀（与后端 TaskViewCustomField.PREFIX 一致） */
export const CF_PREFIX = 'cf:'
/** 构造自定义字段引用：fieldKey → "cf:<fieldKey>" */
export const cfRef = (fieldKey) => `${CF_PREFIX}${fieldKey}`
/** 是否为自定义字段引用 */
export const isCfRef = (ref) => typeof ref === 'string' && ref.startsWith(CF_PREFIX)
/** 从引用取回 fieldKey */
export const cfKey = (ref) => ref.slice(CF_PREFIX.length)

/** 字段类型字典（与后端 FieldType 一致） */
export const FIELD_TYPES = [
  { value: 'text', label: '文本' },
  { value: 'number', label: '数字' },
  { value: 'date', label: '日期' },
  { value: 'select', label: '单选' },
  { value: 'multi_select', label: '多选' },
  { value: 'checkbox', label: '勾选' },
  { value: 'user', label: '用户' },
]
