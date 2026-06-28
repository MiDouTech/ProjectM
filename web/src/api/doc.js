import request from './request'

/**
 * 项目知识库文档（在线文档 + 版本）。正文为 Tiptap JSON，经版本表留痕。
 */
export const docApi = {
  // 全局文档列表（文档中心首页「全部文档」）：跨项目扁平、ACL 过滤。projectIds 为我参与的项目 id 数组
  list: (projectIds, params = {}) => request.get('/docs/list', {
    params: { projectIds: (projectIds || []).join(','), ...params },
  }),
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

  // —— P1：文件节点 / 回收站 ——
  // 上传文件到目录，建 file 节点
  upload: (projectId, parentId, file) => {
    const fd = new FormData()
    fd.append('projectId', projectId)
    if (parentId != null) fd.append('parentId', parentId)
    fd.append('file', file)
    return request.post('/docs/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  // file 节点限时下载/预览 URL（字符串）
  downloadUrl: (id) => request.get(`/docs/${id}/download-url`),
  // 回收站列表
  recycle: (projectId) => request.get('/docs/recycle', { params: { projectId } }),
  // 恢复
  restore: (id) => request.post(`/docs/${id}/restore`),
  // 彻底删除
  purge: (id) => request.delete(`/docs/${id}/purge`),

  // —— P2：搜索 / 收藏 / 模板 ——
  search: (projectId, keyword) => request.get('/docs/search', { params: { projectId, keyword } }),
  toggleFavorite: (id) => request.post(`/docs/${id}/favorite`),
  favorites: (projectId) => request.get('/docs/favorites', { params: { projectId } }),
  templates: () => request.get('/docs/templates'),

  // —— P3：权限 ACL / 分享 ——
  listAcl: (id) => request.get(`/docs/${id}/acl`),
  grantAcl: (id, data) => request.post(`/docs/${id}/acl`, data),
  revokeAcl: (aclId) => request.delete(`/docs/acl/${aclId}`),
  getShare: (id) => request.get(`/docs/${id}/share`),
  createShare: (id, expireTime) => request.post(`/docs/${id}/share`, null, { params: expireTime ? { expireTime } : {} }),
  disableShare: (id) => request.delete(`/docs/${id}/share`),
  // 公开只读（匿名）：token → { title, content }
  publicView: (token) => request.get(`/public/docs/${token}`),
}
