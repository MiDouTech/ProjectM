package com.mido.pm.field.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.Audited;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.field.dto.DataSourceSaveDTO;
import com.mido.pm.field.dto.DataSourceVO;
import com.mido.pm.field.dto.FieldOption;
import com.mido.pm.field.entity.PmDataSource;
import com.mido.pm.field.entity.PmDataSourceOption;
import com.mido.pm.field.mapper.PmDataSourceMapper;
import com.mido.pm.field.mapper.PmDataSourceOptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据源（选项集库）服务：CRUD + 选项集维护 + 供字段解析选项。
 * 选项以子行存于 pm_data_source_option（保存时先清后插）。
 */
@Service
public class DataSourceService {

    private static final String STATUS_ACTIVE = "active";

    private final PmDataSourceMapper dsMapper;
    private final PmDataSourceOptionMapper optionMapper;

    public DataSourceService(PmDataSourceMapper dsMapper, PmDataSourceOptionMapper optionMapper) {
        this.dsMapper = dsMapper;
        this.optionMapper = optionMapper;
    }

    public List<DataSourceVO> list(boolean onlyActive) {
        return dsMapper.selectList(Wrappers.<PmDataSource>lambdaQuery()
                        .eq(onlyActive, PmDataSource::getStatus, STATUS_ACTIVE)
                        .orderByDesc(PmDataSource::getId))
                .stream().map(d -> toVO(d, resolveOptions(d.getId()))).toList();
    }

    public DataSourceVO get(Long id) {
        PmDataSource d = requireExists(id);
        return toVO(d, resolveOptions(id));
    }

    /** 供字段定义解析：返回数据源的选项集（按 sort_no 升序）；不存在则空。 */
    public List<FieldOption> resolveOptions(Long dataSourceId) {
        if (dataSourceId == null) {
            return List.of();
        }
        return optionMapper.selectList(Wrappers.<PmDataSourceOption>lambdaQuery()
                        .eq(PmDataSourceOption::getDataSourceId, dataSourceId)
                        .orderByAsc(PmDataSourceOption::getSortNo).orderByAsc(PmDataSourceOption::getId))
                .stream().map(o -> new FieldOption(o.getValue(), o.getLabel())).toList();
    }

    @Audited(module = AuditActions.MODULE_CONFIG, action = AuditActions.CREATED, target = AuditActions.TARGET_DATA_SOURCE)
    @Transactional(rollbackFor = Exception.class)
    public Long create(DataSourceSaveDTO dto) {
        PmDataSource d = new PmDataSource();
        d.setName(dto.name());
        d.setGroupName(dto.groupName());
        d.setRemark(dto.remark());
        d.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        dsMapper.insert(d);
        saveOptions(d.getId(), dto.options());
        return d.getId();
    }

    @Audited(module = AuditActions.MODULE_CONFIG, action = AuditActions.UPDATED, target = AuditActions.TARGET_DATA_SOURCE)
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DataSourceSaveDTO dto) {
        PmDataSource d = requireExists(id);
        d.setName(dto.name());
        d.setGroupName(dto.groupName());
        d.setRemark(dto.remark());
        if (dto.status() != null) {
            d.setStatus(dto.status());
        }
        dsMapper.updateById(d);
        saveOptions(id, dto.options());
    }

    @Audited(module = AuditActions.MODULE_CONFIG, action = AuditActions.DELETED, target = AuditActions.TARGET_DATA_SOURCE)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        optionMapper.delete(Wrappers.<PmDataSourceOption>lambdaQuery().eq(PmDataSourceOption::getDataSourceId, id));
        dsMapper.deleteById(id);
    }

    private void saveOptions(Long dataSourceId, List<FieldOption> options) {
        optionMapper.delete(Wrappers.<PmDataSourceOption>lambdaQuery()
                .eq(PmDataSourceOption::getDataSourceId, dataSourceId));
        if (options == null) {
            return;
        }
        int sort = 0;
        for (FieldOption opt : options) {
            PmDataSourceOption o = new PmDataSourceOption();
            o.setDataSourceId(dataSourceId);
            o.setValue(opt.value());
            o.setLabel(opt.label());
            o.setSortNo(sort++);
            optionMapper.insert(o);
        }
    }

    private PmDataSource requireExists(Long id) {
        PmDataSource d = dsMapper.selectById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        return d;
    }

    private DataSourceVO toVO(PmDataSource d, List<FieldOption> options) {
        return new DataSourceVO(d.getId(), d.getName(), d.getGroupName(), d.getRemark(), d.getStatus(), options);
    }
}
