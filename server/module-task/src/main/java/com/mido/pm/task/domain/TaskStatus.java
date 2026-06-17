package com.mido.pm.task.domain;

/**
 * 任务默认状态（data-model 状态字典）。pm_workflow 自定义工作流留 P1，本步用默认流。
 */
public enum TaskStatus {

    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    DONE("已完成"),
    ACCEPTED("已验收");

    private final String code;

    TaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
