package com.mido.pm.change.service;

import com.mido.pm.change.dto.ChangePolicyUpsertDTO;
import com.mido.pm.change.entity.PmChangePolicy;
import com.mido.pm.change.mapper.PmChangePolicyMapper;
import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 变更策略保存单测：必审需绑流；按 changeType 幂等 upsert。 */
@ExtendWith(MockitoExtension.class)
class ChangePolicyServiceTest {

    @Mock
    private PmChangePolicyMapper policyMapper;
    @InjectMocks
    private ChangePolicyService service;

    @Test
    void rejectsRequireApprovalWithoutFlow() {
        assertThrows(BizException.class, () -> service.save(
                new ChangePolicyUpsertDTO("project_schedule", 1, null, 1)));
        verify(policyMapper, never()).insert(any(PmChangePolicy.class));
        verify(policyMapper, never()).updateById(any(PmChangePolicy.class));
    }

    @Test
    void insertsWhenAbsent() {
        when(policyMapper.selectOne(any())).thenReturn(null);
        service.save(new ChangePolicyUpsertDTO("project_schedule", 0, null, 1));
        verify(policyMapper).insert(any(PmChangePolicy.class));
        verify(policyMapper, never()).updateById(any(PmChangePolicy.class));
    }

    @Test
    void updatesWhenExists() {
        PmChangePolicy existing = new PmChangePolicy();
        existing.setChangeType("goal_target");
        when(policyMapper.selectOne(any())).thenReturn(existing);
        service.save(new ChangePolicyUpsertDTO("goal_target", 1, 500L, 1));
        verify(policyMapper).updateById(any(PmChangePolicy.class));
        verify(policyMapper, never()).insert(any(PmChangePolicy.class));
    }
}
