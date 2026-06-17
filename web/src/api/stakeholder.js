import request from './request'

/** 干系人 CRUD / 默认权重 / 权重保存(硬校验) / 权力利益矩阵（Step 5） */
export const stakeholderApi = {
  list: (projectId) => request.get('/stakeholders', { params: { projectId } }),
  get: (id) => request.get(`/stakeholders/${id}`),
  create: (data) => request.post('/stakeholders', data),
  update: (id, data) => request.put(`/stakeholders/${id}`, data),
  remove: (id) => request.delete(`/stakeholders/${id}`),
  defaultWeights: (category, subCategory) =>
    request.get('/stakeholders/default-weights', { params: { category, subCategory } }),
  // 受益方(发起人+业务方)合计≥50% 且 总和=100%，后端硬校验(npss-rule §4)
  saveWeights: (data) => request.post('/stakeholders/weights', data),
  matrix: (projectId) => request.get('/stakeholders/matrix', { params: { projectId } }),
}

/** 干系人角色字典（与后端 StakeholderRole 对齐；beneficiary=受益方） */
export const STAKEHOLDER_ROLES = [
  { value: 'sponsor', label: '发起人', beneficiary: true },
  { value: 'business', label: '业务方', beneficiary: true },
  { value: 'team', label: '项目团队', beneficiary: false },
  { value: 'finance', label: '财务', beneficiary: false },
  { value: 'regulator', label: '监管/督办', beneficiary: false },
  { value: 'other', label: '其他(含管理层)', beneficiary: false },
]

export const ROLE_LABEL = Object.fromEntries(STAKEHOLDER_ROLES.map((r) => [r.value, r.label]))
export const isBeneficiaryRole = (role) =>
  STAKEHOLDER_ROLES.find((r) => r.value === role)?.beneficiary === true

/** 四象限说明（与后端 quadrant 文案一致；HIGH 阈值=3） */
export const QUADRANTS = {
  重点管理: '高权力高利益：紧密管理、共同决策',
  令其满意: '高权力低利益：令其满意、定期沟通',
  随时告知: '低权力高利益：随时告知、吸纳意见',
  监督: '低权力低利益：监督即可、低成本维护',
}
