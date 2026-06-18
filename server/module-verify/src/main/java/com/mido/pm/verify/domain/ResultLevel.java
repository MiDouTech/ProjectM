package com.mido.pm.verify.domain;

/** NPSS 结果分级（pm_npss_review.result_level，落库 code）。 */
public enum ResultLevel {

    SUCCESS("success"),
    MIXED("mixed"),
    FAILURE("failure");

    private final String code;

    ResultLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
