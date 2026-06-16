package com.mido.pm.common.datascope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * scoped 封装单测：确保正常与异常路径都清理 ThreadLocal，杜绝串号。
 */
class DataScopeContextTest {

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    @Test
    void scopedSetsDuringAndClearsAfter() {
        String r = DataScopeContext.scoped("user", "dept_id", "create_by", () -> {
            assertNotNull(DataScopeContext.get(), "执行期间应已设置");
            assertEquals("user", DataScopeContext.get().resource());
            return "ok";
        });
        assertEquals("ok", r);
        assertNull(DataScopeContext.get(), "执行后必须清理");
    }

    @Test
    void scopedClearsOnException() {
        assertThrows(RuntimeException.class, () ->
                DataScopeContext.scoped("user", "dept_id", "create_by", () -> {
                    throw new RuntimeException("boom");
                }));
        assertNull(DataScopeContext.get(), "异常路径也必须清理");
    }
}
