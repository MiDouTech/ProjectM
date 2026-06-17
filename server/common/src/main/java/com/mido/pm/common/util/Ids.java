package com.mido.pm.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * 雪花 ID 生成工具。统一全系统主键生成入口，禁止各处自行 new Snowflake。
 */
public final class Ids {

    private Ids() {
    }

    /** 下一个雪花 ID（long）。 */
    public static long nextId() {
        return IdUtil.getSnowflakeNextId();
    }

    /** 下一个雪花 ID（字符串，便于前端避免精度丢失）。 */
    public static String nextIdStr() {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
