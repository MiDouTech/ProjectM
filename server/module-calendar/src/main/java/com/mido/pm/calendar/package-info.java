/**
 * 日历/日程域（calendar.*）。
 *
 * <p>独立的「事件型日程」领域：日程(Schedule)、会议安排、参与人 + RSVP(允许反馈)。
 * 与「任务日历视图」（{@code pm_view} type=calendar，按截止日渲染任务）是两套数据，
 * 本域不复制任务表；日历叠加任务截止/里程碑由前端聚合读取（P1）。</p>
 *
 * <p>分层：controller / service / entity / mapper / dto / event，跨域只经 Service 接口或领域事件。</p>
 */
package com.mido.pm.calendar;
