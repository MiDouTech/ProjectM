import request from './request'

/**
 * 租户应用级 API（无 /platform 前缀，走租户登录态）。
 * - 当前生效公告：顶栏铃铛展示。
 * - 启用的功能码：前端功能门控（fail-open，取不到默认全显示）。
 */
export const appApi = {
  // 当前生效公告 [AnnouncementVO]
  announcements: () => request.get('/announcements'),
  // 启用的功能码 [string]
  features: () => request.get('/features'),
}
