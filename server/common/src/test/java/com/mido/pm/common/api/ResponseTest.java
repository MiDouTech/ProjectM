package com.mido.pm.common.api;

import com.mido.pm.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一响应/分页的冒烟测试，同时验证测试脚手架（JUnit5）可运行。
 */
class ResponseTest {

    @Test
    void okWrapsData() {
        R<String> r = R.ok("x");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMessage());
        assertEquals("x", r.getData());
    }

    @Test
    void failCarriesErrorCode() {
        R<Void> r = R.fail(ErrorCode.PARAM_ERROR);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), r.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getMessage(), r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void pageResultOf() {
        PageResult<Integer> p = PageResult.of(List.of(1, 2), 2L, 1L, 20L);
        assertEquals(2L, p.getTotal());
        assertEquals(1L, p.getPage());
        assertEquals(2, p.getList().size());
    }
}
