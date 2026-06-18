package com.mido.pm.task.dto;

import java.util.List;

/** 按视图配置查询的任务结果：groupBy 为空时单组(groupKey=null)。expandLevel/columns 回显给前端渲染。 */
public record GroupedTasksVO(
        String groupBy,
        Integer expandLevel,
        List<String> columns,
        List<TaskGroup> groups) {

    public record TaskGroup(Object groupKey, List<TaskVO> tasks) {
    }
}
