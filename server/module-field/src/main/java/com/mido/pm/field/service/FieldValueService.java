package com.mido.pm.field.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.field.domain.FieldScope;
import com.mido.pm.field.domain.FieldType;
import com.mido.pm.field.dto.FieldOption;
import com.mido.pm.field.dto.FieldValueVO;
import com.mido.pm.field.dto.FieldValuesWriteDTO;
import com.mido.pm.field.entity.PmFieldDef;
import com.mido.pm.field.entity.PmFieldValue;
import com.mido.pm.field.mapper.PmFieldDefMapper;
import com.mido.pm.field.mapper.PmFieldValueMapper;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字段值服务：按实体读取「定义+值」，按 (fieldId,value) 批量 upsert。
 *
 * <p>写入按字段类型校验（数值/日期/布尔/选项/多选/用户），必填字段提交空值即拒绝；
 * 值变更并入业务实体活动流（{@link AuditLogService}，action=updated, detail.changes=[{field,from,to}]），
 * 并在同一事务发布实体 updated 领域事件（task.updated/project.updated，取自 docs/domain-events.md）。</p>
 */
@Service
public class FieldValueService {

    private final PmFieldValueMapper valueMapper;
    private final PmFieldDefMapper defMapper;
    private final AuditLogService auditLogService;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final DataSourceService dataSourceService;

    public FieldValueService(PmFieldValueMapper valueMapper, PmFieldDefMapper defMapper,
                             AuditLogService auditLogService, DomainEventPublisher eventPublisher,
                             ObjectMapper objectMapper, DataSourceService dataSourceService) {
        this.valueMapper = valueMapper;
        this.defMapper = defMapper;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.dataSourceService = dataSourceService;
    }

    /**
     * 读取实体的字段「定义+值」，含尚无值的启用字段（value=null），按 sortNo、id 升序。
     * 供任务/项目详情抽屉渲染。
     */
    public List<FieldValueVO> getValues(String entityType, Long entityId) {
        FieldScope scope = requireScope(entityType);
        List<PmFieldDef> defs = defMapper.selectList(Wrappers.<PmFieldDef>lambdaQuery()
                .eq(PmFieldDef::getScope, scope.getCode())
                .eq(PmFieldDef::getEnabled, 1)
                .orderByAsc(PmFieldDef::getSortNo)
                .orderByAsc(PmFieldDef::getId));
        if (defs.isEmpty()) {
            return List.of();
        }
        Map<Long, String> valueByField = valueMapper.selectList(Wrappers.<PmFieldValue>lambdaQuery()
                        .eq(PmFieldValue::getEntityType, scope.getCode())
                        .eq(PmFieldValue::getEntityId, entityId))
                .stream().collect(Collectors.toMap(PmFieldValue::getFieldId, v -> v.getValue() == null ? "" : v.getValue(),
                        (a, b) -> a));
        return defs.stream().map(def -> new FieldValueVO(
                def.getId(), def.getFieldKey(), def.getName(), def.getType(),
                effectiveOptions(def),
                def.getRequired() != null && def.getRequired() == 1,
                def.getSortNo(),
                valueByField.get(def.getId()))).toList();
    }

    /**
     * 批量读取多个实体的字段值，供列表视图按 fieldKey 渲染自定义字段列。
     * 返回 entityId → (fieldKey → 原始值字符串)；仅含启用字段、有值的条目。
     */
    public Map<Long, Map<String, String>> valuesForEntities(String entityType, Collection<Long> entityIds) {
        FieldScope scope = requireScope(entityType);
        if (entityIds == null || entityIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> keyByFieldId = defMapper.selectList(Wrappers.<PmFieldDef>lambdaQuery()
                        .eq(PmFieldDef::getScope, scope.getCode())
                        .eq(PmFieldDef::getEnabled, 1))
                .stream().collect(Collectors.toMap(PmFieldDef::getId, PmFieldDef::getFieldKey, (a, b) -> a));
        if (keyByFieldId.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<String, String>> result = new LinkedHashMap<>();
        for (PmFieldValue v : valueMapper.selectList(Wrappers.<PmFieldValue>lambdaQuery()
                .eq(PmFieldValue::getEntityType, scope.getCode())
                .in(PmFieldValue::getEntityId, entityIds))) {
            String key = keyByFieldId.get(v.getFieldId());
            if (key == null || v.getValue() == null) {
                continue;
            }
            result.computeIfAbsent(v.getEntityId(), k -> new LinkedHashMap<>()).put(key, v.getValue());
        }
        return result;
    }

    /**
     * 批量写入字段值。仅处理提交的字段（缺省字段不动）；value 为空表示清除。
     * 任一字段非法整批回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveValues(FieldValuesWriteDTO dto) {
        FieldScope scope = requireScope(dto.entityType());
        if (dto.values() == null || dto.values().isEmpty()) {
            return;
        }
        // 批量预取字段定义与该实体已有值，避免逐字段 N 次查询
        List<Long> fieldIds = dto.values().stream().map(FieldValuesWriteDTO.Item::fieldId).distinct().toList();
        Map<Long, PmFieldDef> defById = defMapper.selectBatchIds(fieldIds).stream()
                .collect(Collectors.toMap(PmFieldDef::getId, d -> d, (a, b) -> a));
        Map<Long, PmFieldValue> existingByField = valueMapper.selectList(Wrappers.<PmFieldValue>lambdaQuery()
                        .eq(PmFieldValue::getEntityType, scope.getCode())
                        .eq(PmFieldValue::getEntityId, dto.entityId())
                        .in(PmFieldValue::getFieldId, fieldIds))
                .stream().collect(Collectors.toMap(PmFieldValue::getFieldId, v -> v, (a, b) -> a));

        List<Map<String, Object>> changes = new ArrayList<>();
        for (FieldValuesWriteDTO.Item item : dto.values()) {
            PmFieldDef def = defById.get(item.fieldId());
            if (def == null || def.getEnabled() == null || def.getEnabled() != 1) {
                throw new BizException(ErrorCode.PARAM_ERROR, "字段不存在或已停用: " + item.fieldId());
            }
            if (!scope.getCode().equals(def.getScope())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "字段作用域与实体不符: " + item.fieldId());
            }
            String normalized = normalizeValue(def, item.value());
            if ((normalized == null || normalized.isBlank())
                    && def.getRequired() != null && def.getRequired() == 1) {
                throw new BizException(ErrorCode.PARAM_ERROR, "必填字段不能为空: " + def.getName());
            }
            PmFieldValue existing = existingByField.get(item.fieldId());
            String oldValue = existing == null ? null : existing.getValue();
            if (java.util.Objects.equals(oldValue, normalized)) {
                continue; // 无变化跳过
            }
            // 回写到 map：同一请求内重复 fieldId 时按更新处理，避免重复插入
            existingByField.put(item.fieldId(), upsert(scope, dto.entityId(), def.getId(), normalized, existing));
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("field", def.getFieldKey());
            change.put("from", oldValue);
            change.put("to", normalized);
            changes.add(change);
        }
        if (!changes.isEmpty()) {
            // entityType 与 AuditActions.TARGET_TASK/TARGET_PROJECT 取值一致
            auditLogService.record(scope.getCode(), dto.entityId(), AuditActions.UPDATED,
                    Map.of("changes", changes));
            // 同事务发布实体 updated 领域事件（Outbox），供报表/AI/活动流订阅
            eventPublisher.publish(scope.getUpdatedEvent(),
                    Map.of("entityType", scope.getCode(), "entityId", dto.entityId(), "changes", changes));
        }
    }

    private PmFieldValue upsert(FieldScope scope, Long entityId, Long fieldId, String value, PmFieldValue existing) {
        if (existing != null) {
            existing.setValue(value);
            valueMapper.updateById(existing);
            return existing;
        }
        PmFieldValue row = new PmFieldValue();
        row.setEntityType(scope.getCode());
        row.setEntityId(entityId);
        row.setFieldId(fieldId);
        row.setValue(value);
        valueMapper.insert(row);
        return row;
    }

    /** 按类型校验并归一化为入库字符串；空值返回 null（清除）。 */
    private String normalizeValue(PmFieldDef def, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        FieldType type = FieldType.fromCode(def.getType());
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "字段类型已失效: " + def.getType());
        }
        return switch (type) {
            case TEXT -> value;
            case NUMBER -> normalizeNumber(def, value);
            case DATE -> normalizeDate(def, value);
            case CHECKBOX -> normalizeCheckbox(def, value);
            case USER -> normalizeUser(def, value);
            case SELECT -> requireInOptions(def, value);
            case MULTI_SELECT -> normalizeMultiSelect(def, value);
        };
    }

    private String normalizeNumber(PmFieldDef def, String value) {
        try {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            throw badValue(def, value);
        }
    }

    private String normalizeDate(PmFieldDef def, String value) {
        try {
            return LocalDate.parse(value).toString();
        } catch (Exception e) {
            throw badValue(def, value);
        }
    }

    private String normalizeCheckbox(PmFieldDef def, String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return value.toLowerCase();
        }
        throw badValue(def, value);
    }

    private String normalizeUser(PmFieldDef def, String value) {
        try {
            return Long.valueOf(value).toString();
        } catch (NumberFormatException e) {
            throw badValue(def, value);
        }
    }

    private String requireInOptions(PmFieldDef def, String value) {
        if (!optionValues(def).contains(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "值不在选项内[" + def.getName() + "]: " + value);
        }
        return value;
    }

    private String normalizeMultiSelect(PmFieldDef def, String value) {
        List<String> selected;
        try {
            selected = objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "多选值须为 JSON 数组[" + def.getName() + "]: " + value);
        }
        if (selected.isEmpty()) {
            return null;
        }
        Set<String> opts = optionValues(def);
        for (String s : selected) {
            if (!opts.contains(s)) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "值不在选项内[" + def.getName() + "]: " + s);
            }
        }
        try {
            return objectMapper.writeValueAsString(selected);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "多选值序列化失败");
        }
    }

    private Set<String> optionValues(PmFieldDef def) {
        return effectiveOptions(def).stream().map(FieldOption::value)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** 字段有效选项：引用数据源时取数据源选项，否则取内联 options。 */
    private List<FieldOption> effectiveOptions(PmFieldDef def) {
        if (def.getDataSourceId() != null) {
            return dataSourceService.resolveOptions(def.getDataSourceId());
        }
        return readOptions(def.getOptions());
    }

    private BizException badValue(PmFieldDef def, String value) {
        return new BizException(ErrorCode.PARAM_ERROR,
                "字段值格式不符[" + def.getName() + "/" + def.getType() + "]: " + value);
    }

    private List<FieldOption> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FieldOption>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private FieldScope requireScope(String entityType) {
        FieldScope scope = FieldScope.fromCode(entityType);
        if (scope == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法实体类型: " + entityType);
        }
        return scope;
    }
}
