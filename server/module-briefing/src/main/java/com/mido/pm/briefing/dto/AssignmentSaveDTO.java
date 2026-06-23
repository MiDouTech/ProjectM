package com.mido.pm.briefing.dto;

import java.util.List;

/** 模板指派：指派给用户 userIds 与部门 deptIds（整体覆盖）。 */
public record AssignmentSaveDTO(
        List<Long> userIds,
        List<Long> deptIds) {
}
