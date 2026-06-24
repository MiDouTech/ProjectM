-- V46 日历订阅 token 补列。
--   V38 建 pm_calendar 时漏建 subscribe_token 列，但 PmCalendar 实体与
--   PmCalendarMapper.@Select 都依赖该列：listMine 查询生成的 SQL 含未知列 subscribe_token，
--   日历页加载即 500（前端兜底「系统繁忙，请稍后重试」）。此处补列修复。
--   token 由 IcsService 用 IdUtil.fastSimpleUUID() 生成（32 位 hex），全局唯一、按 token 匿名查日历。

ALTER TABLE pm_calendar
  ADD COLUMN subscribe_token VARCHAR(64) NULL COMMENT 'ics 匿名订阅 token(全局唯一, 惰性生成)' AFTER is_default,
  ADD KEY idx_subscribe_token (subscribe_token);
