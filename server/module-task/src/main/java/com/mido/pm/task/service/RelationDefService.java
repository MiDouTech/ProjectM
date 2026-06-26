package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.domain.RelationKind;
import com.mido.pm.task.dto.RelationDefSaveDTO;
import com.mido.pm.task.dto.RelationDefVO;
import com.mido.pm.task.entity.PmRelationDef;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.mapper.PmRelationDefMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关联定义服务：管理员定义"哪种工作项类型可关联哪种类型、关系语义"（related/derived）。
 * 实例关联({@link RelationService})当前不强制匹配定义（双轨），定义用于文档/后续治理。
 */
@Service
public class RelationDefService {

    private final PmRelationDefMapper defMapper;
    private final PmWorkItemTypeMapper typeMapper;

    public RelationDefService(PmRelationDefMapper defMapper, PmWorkItemTypeMapper typeMapper) {
        this.defMapper = defMapper;
        this.typeMapper = typeMapper;
    }

    public List<RelationDefVO> list() {
        List<PmRelationDef> defs = defMapper.selectList(Wrappers.<PmRelationDef>lambdaQuery()
                .orderByDesc(PmRelationDef::getId));
        Map<Long, String> typeNames = new HashMap<>();
        for (PmWorkItemType t : typeMapper.selectList(Wrappers.<PmWorkItemType>lambdaQuery())) {
            typeNames.put(t.getId(), t.getName());
        }
        return defs.stream().map(d -> new RelationDefVO(d.getId(),
                d.getSourceTypeId(), typeNames.get(d.getSourceTypeId()),
                d.getTargetTypeId(), typeNames.get(d.getTargetTypeId()),
                d.getRelationKind(), d.getName())).toList();
    }

    public Long create(RelationDefSaveDTO dto) {
        validate(dto);
        PmRelationDef d = new PmRelationDef();
        d.setSourceTypeId(dto.sourceTypeId());
        d.setTargetTypeId(dto.targetTypeId());
        d.setRelationKind(dto.relationKind());
        d.setName(dto.name());
        defMapper.insert(d);
        return d.getId();
    }

    public void update(Long id, RelationDefSaveDTO dto) {
        validate(dto);
        PmRelationDef d = defMapper.selectById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "关联定义不存在");
        }
        d.setSourceTypeId(dto.sourceTypeId());
        d.setTargetTypeId(dto.targetTypeId());
        d.setRelationKind(dto.relationKind());
        d.setName(dto.name());
        defMapper.updateById(d);
    }

    public void delete(Long id) {
        defMapper.deleteById(id);
    }

    private void validate(RelationDefSaveDTO dto) {
        if (!RelationKind.isValid(dto.relationKind())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法关系类型: " + dto.relationKind());
        }
    }
}
