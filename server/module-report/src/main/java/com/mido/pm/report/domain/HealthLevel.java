package com.mido.pm.report.domain;

/** 项目健康度分级。green 健康 / yellow 关注 / red 风险。 */
public enum HealthLevel {

    GREEN("健康"),
    YELLOW("关注"),
    RED("风险");

    private final String label;

    HealthLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
