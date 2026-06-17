import request from './request'

/** 评论：发表 + 列表（按对象 entityType/entityId，Step 6） */
export const commentApi = {
  list: (entityType, entityId) => request.get('/comments', { params: { entityType, entityId } }),
  create: (data) => request.post('/comments', data),
}

/** 站内信：列表 / 未读数 / 标记已读（当前登录用户，Step 6） */
export const notificationApi = {
  list: (params) => request.get('/notifications', { params }),
  unreadCount: () => request.get('/notifications/unread-count'),
  markRead: (id) => request.put(`/notifications/${id}/read`),
  markAllRead: () => request.put('/notifications/read-all'),
}
