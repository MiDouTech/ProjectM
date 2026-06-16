package com.mido.pm.stakeholder.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.stakeholder.dto.SaveWeightsDTO;
import com.mido.pm.stakeholder.dto.StakeholderCreateDTO;
import com.mido.pm.stakeholder.dto.WeightItemDTO;
import com.mido.pm.stakeholder.entity.PmStakeholder;
import com.mido.pm.stakeholder.mapper.PmStakeholderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 干系人服务单测：注册发事件、保存权重的 §4 硬校验。
 */
@ExtendWith(MockitoExtension.class)
class StakeholderServiceTest {

    @Mock private PmStakeholderMapper mapper;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private StakeholderService service;

    private PmStakeholder sh(Long id, String role, String weight) {
        PmStakeholder s = new PmStakeholder();
        s.setId(id);
        s.setProjectId(9L);
        s.setRole(role);
        s.setNpssWeight(new BigDecimal(weight));
        return s;
    }

    private WeightItemDTO wi(Long id, String w) {
        return new WeightItemDTO(id, new BigDecimal(w));
    }

    @Test
    void createEmitsRegistered() {
        service.create(new StakeholderCreateDTO(9L, 7L, null, "sponsor", "internal", 4, 5, new BigDecimal("30")));
        verify(mapper).insert(any(PmStakeholder.class));
        verify(eventPublisher).publish(eq("stakeholder.registered"), any());
    }

    @Test
    void saveWeightsRejectsWhenBeneficiaryBelow50() {
        when(mapper.selectList(any())).thenReturn(List.of(
                sh(1L, "sponsor", "20"), sh(2L, "business", "20"), sh(3L, "team", "60")));
        SaveWeightsDTO dto = new SaveWeightsDTO(9L,
                List.of(wi(1L, "20"), wi(2L, "20"), wi(3L, "60")));   // 受益方=40 <50
        assertThrows(BizException.class, () -> service.saveWeights(dto));
        verify(mapper, never()).updateById(any(PmStakeholder.class));
    }

    @Test
    void saveWeightsValidPersists() {
        when(mapper.selectList(any())).thenReturn(List.of(
                sh(1L, "sponsor", "30"), sh(2L, "business", "30"), sh(3L, "team", "40")));
        SaveWeightsDTO dto = new SaveWeightsDTO(9L,
                List.of(wi(1L, "30"), wi(2L, "30"), wi(3L, "40")));   // 和=100 受益方=60
        service.saveWeights(dto);
        verify(mapper, times(3)).updateById(any(PmStakeholder.class));
    }
}
