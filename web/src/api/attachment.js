import request from './request'

/**
 * 附件（通用挂载 entityType+entityId，如 task）。
 * oss_key 不外泄：下载先取限时预签名 URL 再访问对象存储。
 */
export const attachmentApi = {
  list: (entityType, entityId) => request.get('/attachments', { params: { entityType, entityId } }),
  // 项目文件：汇总项目级文档 + 项目下任务/费用附件
  listByProject: (projectId) => request.get('/attachments/by-project', { params: { projectId } }),
  // 上传登记（预签名直传）：返回 { attachmentId, uploadUrl, expireSeconds }
  register: (data) => request.post('/attachments/register', data),
  upload: (entityType, entityId, file) => {
    const fd = new FormData()
    fd.append('entityType', entityType)
    fd.append('entityId', entityId)
    fd.append('file', file)
    return request.post('/attachments', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  // 返回限时预签名下载 URL（字符串）
  downloadUrl: (id) => request.get(`/attachments/${id}/download-url`),
  remove: (id) => request.delete(`/attachments/${id}`),
}
