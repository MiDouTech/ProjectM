import request from './request'

/** 工作台布局（当前用户，pm_view scope=workbench）：取/存有序卡片 id 列表。 */
export const workbenchApi = {
  // 返回 { cards: [...] | null }，null 表示未保存过（用默认布局）
  getLayout: () => request.get('/workbench/layout'),
  saveLayout: (cards) => request.put('/workbench/layout', { cards }),
}
