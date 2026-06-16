package com.mido.pm.project.domain;

/**
 * 项目生命周期状态（对应 docs/data-model.md 状态字典 / architecture-overview §2.2）。
 * code 为落库中文值，禁散落魔法值。
 */
public enum ProjectStatus {

    DRAFT("草稿"),
    APPROVING("审批中"),
    REGISTERED("已注册"),
    IN_PROGRESS("进行中"),
    RESULT_VERIFY("结果验收"),
    CLOSED("已结案"),
    VALUE_VERIFY("价值验收中"),
    EVALUATED("已评价");

    private final String code;

    ProjectStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 按中文 code 解析；未知返回 null。 */
    public static ProjectStatus fromCode(String code) {
        for (ProjectStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
