package com.mido.pm.stakeholder.domain;

/**
 * 干系人角色。受益方 = sponsor(发起人) + business(业务方)，用于 npss-rule §4 硬校验。
 */
public enum StakeholderRole {

    SPONSOR("sponsor", true),
    BUSINESS("business", true),
    TEAM("team", false),
    FINANCE("finance", false),
    REGULATOR("regulator", false),
    OTHER("other", false);

    private final String code;
    private final boolean beneficiary;

    StakeholderRole(String code, boolean beneficiary) {
        this.code = code;
        this.beneficiary = beneficiary;
    }

    public String getCode() {
        return code;
    }

    public boolean isBeneficiary() {
        return beneficiary;
    }

    public static StakeholderRole fromCode(String code) {
        for (StakeholderRole r : values()) {
            if (r.code.equals(code)) {
                return r;
            }
        }
        return null;
    }

    /** 角色是否受益方；未知角色按非受益方处理。 */
    public static boolean isBeneficiaryRole(String code) {
        StakeholderRole r = fromCode(code);
        return r != null && r.beneficiary;
    }
}
