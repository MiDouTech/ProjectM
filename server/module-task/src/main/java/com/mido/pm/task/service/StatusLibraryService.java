package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.domain.MetaCategory;
import com.mido.pm.task.dto.StatusSaveDTO;
import com.mido.pm.task.dto.StatusVO;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.mapper.PmStatusMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 状态库服务：租户自配状态 + 元类别归约。内置三态(builtin=1)不可删。
 * 阶段1 仅配置（双轨）；阶段2 工作流引擎据此取代 TaskStatus enum。
 */
@Service
public class StatusLibraryService {

    private static final String STATUS_ACTIVE = "active";

    private final PmStatusMapper statusMapper;

    public StatusLibraryService(PmStatusMapper statusMapper) {
        this.statusMapper = statusMapper;
    }

    public List<StatusVO> list(boolean onlyActive) {
        return statusMapper.selectList(Wrappers.<PmStatus>lambdaQuery()
                        .eq(onlyActive, PmStatus::getStatus, STATUS_ACTIVE)
                        .orderByAsc(PmStatus::getSort).orderByAsc(PmStatus::getId))
                .stream().map(this::toVO).toList();
    }

    public Long create(StatusSaveDTO dto) {
        assertMeta(dto.metaCategory());
        PmStatus s = new PmStatus();
        s.setName(dto.name());
        s.setColor(dto.color());
        s.setMetaCategory(dto.metaCategory());
        s.setGroupName(dto.groupName());
        s.setSort(dto.sort() == null ? 0 : dto.sort());
        s.setBuiltin(0);
        s.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        statusMapper.insert(s);
        return s.getId();
    }

    public void update(Long id, StatusSaveDTO dto) {
        assertMeta(dto.metaCategory());
        PmStatus s = requireExists(id);
        s.setName(dto.name());
        s.setColor(dto.color());
        s.setMetaCategory(dto.metaCategory());
        s.setGroupName(dto.groupName());
        if (dto.sort() != null) {
            s.setSort(dto.sort());
        }
        if (dto.status() != null) {
            s.setStatus(dto.status());
        }
        statusMapper.updateById(s);
    }

    public void delete(Long id) {
        PmStatus s = requireExists(id);
        if (s.getBuiltin() != null && s.getBuiltin() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "内置状态不可删除");
        }
        statusMapper.deleteById(id);
    }

    private void assertMeta(String metaCategory) {
        if (!MetaCategory.isValid(metaCategory)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法元类别: " + metaCategory);
        }
    }

    private PmStatus requireExists(Long id) {
        PmStatus s = statusMapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "状态不存在");
        }
        return s;
    }

    private StatusVO toVO(PmStatus s) {
        return new StatusVO(s.getId(), s.getName(), s.getColor(), s.getMetaCategory(),
                s.getGroupName(), s.getSort(), s.getBuiltin(), s.getStatus());
    }
}
