package com.mido.pm.report.domain;

import java.util.List;

/**
 * 状态元类别端口（报表域定义，任务域实现，分层不成环 task→report）。
 * 供报表按状态库「元类别」判定完成，而非硬编码字符串状态，从而正确统计自定义状态。
 */
public interface StatusMetaPort {

    /** 当前租户「已完成」元类别的全部状态 id；未配置状态库返回空（报表回落字符串终态）。 */
    List<Long> doneStatusIds();
}
