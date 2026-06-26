package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.dto.PriorityLevelDTO;
import com.mido.pm.task.dto.PriorityModeSaveDTO;
import com.mido.pm.task.dto.PriorityModeVO;
import com.mido.pm.task.entity.PmPriorityLevel;
import com.mido.pm.task.entity.PmPriorityMode;
import com.mido.pm.task.mapper.PmPriorityLevelMapper;
import com.mido.pm.task.mapper.PmPriorityModeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 优先级模式服务：CRUD + 档位维护。内置模式(builtin=1)不可删；档位以子行存储(先清后插)。
 */
@Service
public class PriorityModeService {

    private static final String STATUS_ACTIVE = "active";

    private final PmPriorityModeMapper modeMapper;
    private final PmPriorityLevelMapper levelMapper;

    public PriorityModeService(PmPriorityModeMapper modeMapper, PmPriorityLevelMapper levelMapper) {
        this.modeMapper = modeMapper;
        this.levelMapper = levelMapper;
    }

    public List<PriorityModeVO> list() {
        return modeMapper.selectList(Wrappers.<PmPriorityMode>lambdaQuery()
                        .orderByDesc(PmPriorityMode::getId))
                .stream().map(m -> toVO(m, levelsOf(m.getId()))).toList();
    }

    public PriorityModeVO get(Long id) {
        PmPriorityMode m = requireExists(id);
        return toVO(m, levelsOf(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PriorityModeSaveDTO dto) {
        PmPriorityMode m = new PmPriorityMode();
        m.setName(dto.name());
        m.setRemark(dto.remark());
        m.setBuiltin(0);
        m.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        modeMapper.insert(m);
        saveLevels(m.getId(), dto.levels());
        return m.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PriorityModeSaveDTO dto) {
        PmPriorityMode m = requireExists(id);
        m.setName(dto.name());
        m.setRemark(dto.remark());
        if (dto.status() != null) {
            m.setStatus(dto.status());
        }
        modeMapper.updateById(m);
        saveLevels(id, dto.levels());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PmPriorityMode m = requireExists(id);
        if (m.getBuiltin() != null && m.getBuiltin() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "内置优先级模式不可删除");
        }
        levelMapper.delete(Wrappers.<PmPriorityLevel>lambdaQuery().eq(PmPriorityLevel::getModeId, id));
        modeMapper.deleteById(id);
    }

    private void saveLevels(Long modeId, List<PriorityLevelDTO> levels) {
        levelMapper.delete(Wrappers.<PmPriorityLevel>lambdaQuery().eq(PmPriorityLevel::getModeId, modeId));
        if (levels == null) {
            return;
        }
        int sort = 0;
        for (PriorityLevelDTO l : levels) {
            PmPriorityLevel level = new PmPriorityLevel();
            level.setModeId(modeId);
            level.setName(l.name());
            level.setColor(l.color());
            level.setLevelValue(l.levelValue());
            level.setSort(l.sort() == null ? sort : l.sort());
            levelMapper.insert(level);
            sort++;
        }
    }

    private List<PriorityLevelDTO> levelsOf(Long modeId) {
        return levelMapper.selectList(Wrappers.<PmPriorityLevel>lambdaQuery()
                        .eq(PmPriorityLevel::getModeId, modeId)
                        .orderByAsc(PmPriorityLevel::getSort).orderByAsc(PmPriorityLevel::getId))
                .stream().map(l -> new PriorityLevelDTO(l.getId(), l.getName(), l.getColor(),
                        l.getLevelValue(), l.getSort())).toList();
    }

    private PmPriorityMode requireExists(Long id) {
        PmPriorityMode m = modeMapper.selectById(id);
        if (m == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "优先级模式不存在");
        }
        return m;
    }

    private PriorityModeVO toVO(PmPriorityMode m, List<PriorityLevelDTO> levels) {
        return new PriorityModeVO(m.getId(), m.getName(), m.getRemark(), m.getBuiltin(), m.getStatus(), levels);
    }
}
