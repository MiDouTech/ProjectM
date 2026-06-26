package com.mido.pm.verify.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.verify.dto.ResultVerifySaveDTO;
import com.mido.pm.verify.entity.PmResultVerify;
import com.mido.pm.verify.mapper.PmResultVerifyMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 结果验收闸门单测：无结论/不达标拒绝结案、达标放行；录入落库并发事件。
 */
@ExtendWith(MockitoExtension.class)
class ResultVerifyServiceTest {

    @Mock
    private PmResultVerifyMapper mapper;
    @Mock
    private DomainEventPublisher eventPublisher;

    private ResultVerifyService service() {
        return new ResultVerifyService(mapper, eventPublisher);
    }

    private PmResultVerify verdict(String v) {
        PmResultVerify e = new PmResultVerify();
        e.setProjectId(42L);
        e.setVerdict(v);
        return e;
    }

    @Test
    void assertClosable_noVerdict_throws() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service().assertClosable(42L));
    }

    @Test
    void assertClosable_fail_throws() {
        when(mapper.selectOne(any())).thenReturn(verdict("fail"));
        assertThrows(BizException.class, () -> service().assertClosable(42L));
    }

    @Test
    void assertClosable_pass_ok() {
        when(mapper.selectOne(any())).thenReturn(verdict("pass"));
        assertDoesNotThrow(() -> service().assertClosable(42L));
    }

    @Test
    void save_insertsRowAndPublishesEvent() {
        ResultVerifySaveDTO dto = new ResultVerifySaveDTO(
                "pass", true, true, false, new BigDecimal("100.00"), "按期交付");
        service().save(42L, dto);

        verify(mapper).insert(any(PmResultVerify.class));
        verify(eventPublisher).publish(anyString(), any());
    }

    @Test
    void save_mapsBooleanFlagsToTinyint() {
        service().save(42L, new ResultVerifySaveDTO("fail", false, true, null, null, null));

        ArgumentCaptor<PmResultVerify> captor = ArgumentCaptor.forClass(PmResultVerify.class);
        verify(mapper).insert(captor.capture());
        PmResultVerify saved = captor.getValue();
        assertEquals(0, saved.getOnTime(), "false→0");
        assertEquals(1, saved.getInBudget(), "true→1");
        assertEquals(0, saved.getInScope(), "null→0");
        assertEquals("fail", saved.getVerdict());
    }
}
