import request from './request'

/**
 * 项目知识库文档（在线文档 + 版本）。正文为 Tiptap JSON，经版本表留痕。
 */
export const docApi = {
  // 目录树
  tree: (projectId) => request.get('/docs/tree', { params: { projectId } }),
  // 文档详情（含当前版本正文）
  get: (id) => request.get(`/docs/${id}`),
  // 新建目录/文档节点：{ projectId, parentId, type: 'folder'|'doc', title, icon }
  create: (data) => request.post('/docs', data),
  // 重命名/改图标：{ title, icon }
  rename: (id, data) => request.put(`/docs/${id}/rename`, data),
  // 移动节点：{ parentId, sortNo }
  move: (id, data) => request.put(`/docs/${id}/move`, data),
  remove: (id) => request.delete(`/docs/${id}`),
  // 保存正文（产生新版本）：{ title, content, contentText, changeNote }
  saveContent: (id, data) => request.put(`/docs/${id}/content`, data),
  // 版本列表（不含正文）
  versions: (id) => request.get(`/docs/${id}/versions`),
  // 单个版本正文
  versionContent: (versionId) => request.get(`/docs/versions/${versionId}`),
  // 回滚到指定版本（追加为新版本）
  rollback: (id, versionId) => request.post(`/docs/${id}/rollback/${versionId}`),
}
