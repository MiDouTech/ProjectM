package com.mido.pm.field.service;

import com.mido.pm.field.dto.DataSourceSaveDTO;
import com.mido.pm.field.dto.FieldOption;
import com.mido.pm.field.entity.PmDataSourceOption;
import com.mido.pm.field.mapper.PmDataSourceMapper;
import com.mido.pm.field.mapper.PmDataSourceOptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据源服务单测：创建落库并写选项；resolveOptions 映射为 FieldOption。
 */
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock private PmDataSourceMapper dsMapper;
    @Mock private PmDataSourceOptionMapper optionMapper;

    private DataSourceService service() {
        return new DataSourceService(dsMapper, optionMapper);
    }

    @Test
    void createPersistsSourceAndOptions() {
        service().create(new DataSourceSaveDTO("缺陷类型", "IT", null, null,
                List.of(new FieldOption("bug", "缺陷"), new FieldOption("feature", "需求"))));
        verify(dsMapper).insert(any(com.mido.pm.field.entity.PmDataSource.class));
        verify(optionMapper).delete(any());
        verify(optionMapper, times(2)).insert(any(PmDataSourceOption.class));
    }

    @Test
    void resolveOptionsMapsToFieldOption() {
        PmDataSourceOption o = new PmDataSourceOption();
        o.setValue("bug");
        o.setLabel("缺陷");
        when(optionMapper.selectList(any())).thenReturn(List.of(o));
        List<FieldOption> opts = service().resolveOptions(9L);
        assertEquals(1, opts.size());
        assertEquals("bug", opts.get(0).value());
        assertEquals("缺陷", opts.get(0).label());
    }
}
