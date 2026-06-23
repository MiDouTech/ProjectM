import request from './request'

/**
 * 日历/日程（独立事件型日程，区别于任务日历视图）。
 * 时间参数为 ISO-8601 字符串。
 */
export const calendarApi = {
  // 当前用户的日历列表（含默认「我的日程」）
  listCalendars: () => request.get('/calendars'),
  // 时间段查询日程（月/周/日视图取数）：{ from, to }
  range: (from, to) => request.get('/schedules', { params: { from, to } }),
  // 日程详情（含参与人）
  get: (id) => request.get(`/schedules/${id}`),
  // 新建日程
  create: (data) => request.post('/schedules', data),
  // 更新日程
  update: (id, data) => request.put(`/schedules/${id}`, data),
  // 删除日程（逻辑删，仅组织者）
  remove: (id) => request.delete(`/schedules/${id}`),
  // RSVP 反馈：{ status: 'accepted'|'tentative'|'declined' }
  rsvp: (id, status) => request.post(`/schedules/${id}/rsvp`, { status }),
  // 循环日程单次例外：{ occurDate, action: 'cancel'|'modify', override? }
  addException: (id, data) => request.post(`/schedules/${id}/exceptions`, data),
  // 资源台账（会议室/设备）
  listResources: () => request.get('/calendar-resources'),
  // 日历叠加：当前用户截止日落在 [from,to] 的任务（含里程碑），from/to 为 YYYY-MM-DD
  tasksInRange: (from, to) => request.get('/tasks/calendar', { params: { from, to } }),
}
