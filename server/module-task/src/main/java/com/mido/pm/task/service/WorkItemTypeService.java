package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.dto.TransitionDTO;
import com.mido.pm.task.dto.TypeFieldDTO;
import com.mido.pm.task.dto.WorkItemTypeSaveDTO;
import com.mido.pm.task.dto.WorkItemTypeVO;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.entity.PmWorkItemTypeField;
import com.mido.pm.task.entity.PmWorkflowTransition;
import com.mido.pm.task.mapper.PmWorkItemTypeFieldMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import com.mido.pm.task.mapper.PmWorkflowTransitionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工作项类型服务：类型 CRUD + 字段绑定 + 工作流转移矩阵维护。
 * 内置默认类型(builtin=1)不可删；code 租户内唯一。流转矩阵由 {@link WorkflowEngine} 消费。
 */
@Service
public class WorkItemTypeService {

    private static final String STATUS_ACTIVE = "active";

    private final PmWorkItemTypeMapper typeMapper;
    private final PmWorkItemTypeFieldMapper typeFieldMapper;
    private final PmWorkflowTransitionMapper transitionMapper;

    public WorkItemTypeService(PmWorkItemTypeMapper typeMapper, PmWorkItemTypeFieldMapper typeFieldMapper,
                               PmWorkflowTransitionMapper transitionMapper) {
        this.typeMapper = typeMapper;
        this.typeFieldMapper = typeFieldMapper;
        this.transitionMapper = transitionMapper;
    }

    public List<WorkItemTypeVO> list(boolean onlyActive) {
        return typeMapper.selectList(Wrappers.<PmWorkItemType>lambdaQuery()
                        .eq(onlyActive, PmWorkItemType::getStatus, STATUS_ACTIVE)
                        .orderByAsc(PmWorkItemType::getSort).orderByAsc(PmWorkItemType::getId))
                .stream().map(this::toVO).toList();
    }

    public Long create(WorkItemTypeSaveDTO dto) {
        assertCodeUnique(dto.code());
        PmWorkItemType t = new PmWorkItemType();
        t.setCode(dto.code());
        t.setName(dto.name());
        t.setGroupName(dto.groupName());
        t.setBuiltin(0);
        t.setSort(dto.sort() == null ? 0 : dto.sort());
        t.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        typeMapper.insert(t);
        return t.getId();
    }

    public void update(Long id, WorkItemTypeSaveDTO dto) {
        PmWorkItemType t = requireExists(id);
        t.setName(dto.name());
        t.setGroupName(dto.groupName());
        if (dto.sort() != null) {
            t.setSort(dto.sort());
        }
        if (dto.status() != null) {
            t.setStatus(dto.status());
        }
        typeMapper.updateById(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PmWorkItemType t = requireExists(id);
        if (t.getBuiltin() != null && t.getBuiltin() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "内置工作项类型不可删除");
        }
        typeFieldMapper.delete(Wrappers.<PmWorkItemTypeField>lambdaQuery().eq(PmWorkItemTypeField::getTypeId, id));
        transitionMapper.delete(Wrappers.<PmWorkflowTransition>lambdaQuery().eq(PmWorkflowTransition::getTypeId, id));
        typeMapper.deleteById(id);
    }

    // ===== 字段绑定 =====

    public List<TypeFieldDTO> getFields(Long typeId) {
        requireExists(typeId);
        return typeFieldMapper.selectList(Wrappers.<PmWorkItemTypeField>lambdaQuery()
                        .eq(PmWorkItemTypeField::getTypeId, typeId)
                        .orderByAsc(PmWorkItemTypeField::getSort).orderByAsc(PmWorkItemTypeField::getId))
                .stream().map(f -> new TypeFieldDTO(f.getFieldKey(),
                        f.getRequired() != null && f.getRequired() == 1, f.getSort())).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFields(Long typeId, List<TypeFieldDTO> fields) {
        requireExists(typeId);
        typeFieldMapper.delete(Wrappers.<PmWorkItemTypeField>lambdaQuery().eq(PmWorkItemTypeField::getTypeId, typeId));
        if (fields == null) {
            return;
        }
        int sort = 0;
        for (TypeFieldDTO f : fields) {
            PmWorkItemTypeField tf = new PmWorkItemTypeField();
            tf.setTypeId(typeId);
            tf.setFieldKey(f.fieldKey());
            tf.setRequired(Boolean.TRUE.equals(f.required()) ? 1 : 0);
            tf.setSort(f.sort() == null ? sort : f.sort());
            typeFieldMapper.insert(tf);
            sort++;
        }
    }

    // ===== 工作流转移矩阵 =====

    public List<TransitionDTO> getTransitions(Long typeId) {
        requireExists(typeId);
        return transitionMapper.selectList(Wrappers.<PmWorkflowTransition>lambdaQuery()
                        .eq(PmWorkflowTransition::getTypeId, typeId))
                .stream().map(t -> new TransitionDTO(t.getFromStatusId(), t.getToStatusId())).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveTransitions(Long typeId, List<TransitionDTO> transitions) {
        requireExists(typeId);
        transitionMapper.delete(Wrappers.<PmWorkflowTransition>lambdaQuery().eq(PmWorkflowTransition::getTypeId, typeId));
        if (transitions == null) {
            return;
        }
        for (TransitionDTO tr : transitions) {
            if (tr.fromStatusId() == null || tr.toStatusId() == null) {
                continue;
            }
            PmWorkflowTransition t = new PmWorkflowTransition();
            t.setTypeId(typeId);
            t.setFromStatusId(tr.fromStatusId());
            t.setToStatusId(tr.toStatusId());
            transitionMapper.insert(t);
        }
    }

    private void assertCodeUnique(String code) {
        Long c = typeMapper.selectCount(Wrappers.<PmWorkItemType>lambdaQuery().eq(PmWorkItemType::getCode, code));
        if (c != null && c > 0) {
            throw new BizException(ErrorCode.CONFLICT, "类型编码已存在");
        }
    }

    private PmWorkItemType requireExists(Long id) {
        PmWorkItemType t = typeMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工作项类型不存在");
        }
        return t;
    }

    private WorkItemTypeVO toVO(PmWorkItemType t) {
        return new WorkItemTypeVO(t.getId(), t.getCode(), t.getName(), t.getGroupName(),
                t.getBuiltin(), t.getSort(), t.getStatus());
    }
}
