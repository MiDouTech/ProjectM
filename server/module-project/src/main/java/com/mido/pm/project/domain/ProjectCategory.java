package com.mido.pm.project.domain;

/**
 * 项目类型：S 战略级 / I 创新级 / O 运营级（CLAUDE.md §6）。
 */
public enum ProjectCategory {

    S("S"),
    I("I"),
    O("O");

    private final String code;

    ProjectCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ProjectCategory fromCode(String code) {
        for (ProjectCategory c : values()) {
            if (c.code.equals(code)) {
                return c;
            }
        }
        return null;
    }
}
