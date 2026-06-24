package com.mido.pm.field.domain;

import java.util.Arrays;

/**
 * 自定义字段类型。
 * <ul>
 *   <li>optionBased：值须落在 options 选项集合内（select/multi_select）。</li>
 *   <li>multi：值为多值，存储为 JSON 数组字符串（multi_select）。</li>
 * </ul>
 */
public enum FieldType {
    TEXT("text", false, false),
    NUMBER("number", false, false),
    DATE("date", false, false),
    SELECT("select", true, false),
    MULTI_SELECT("multi_select", true, true),
    CHECKBOX("checkbox", false, false),
    USER("user", false, false);

    private final String code;
    private final boolean optionBased;
    private final boolean multi;

    FieldType(String code, boolean optionBased, boolean multi) {
        this.code = code;
        this.optionBased = optionBased;
        this.multi = multi;
    }

    public String getCode() {
        return code;
    }

    public boolean isOptionBased() {
        return optionBased;
    }

    public boolean isMulti() {
        return multi;
    }

    /** 解析类型；非法返回 null。 */
    public static FieldType fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst().orElse(null);
    }
}
