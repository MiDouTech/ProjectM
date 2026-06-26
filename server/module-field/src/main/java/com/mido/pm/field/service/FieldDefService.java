package com.mido.pm.field.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.field.domain.FieldScope;
import com.mido.pm.field.domain.FieldType;
import com.mido.pm.field.dto.FieldDefCreateDTO;
import com.mido.pm.field.dto.FieldDefUpdateDTO;
import com.mido.pm.field.dto.FieldDefVO;
import com.mido.pm.field.dto.FieldOption;
import com.mido.pm.field.entity.PmFieldDef;
import com.mido.pm.field.mapper.PmFieldDefMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字段定义服务：租户自配自定义字段（scope=task/project）。
 * fieldKey 在(租户,scope)内唯一（仅校验未删除项，停用/删除后可重建）。
 */
@Service
public class FieldDefService {

    private final PmFieldDefMapper defMapper;
    private final ObjectMapper objectMapper;
    private final DataSourceService dataSourceService;

    public FieldDefService(PmFieldDefMapper defMapper, ObjectMapper objectMapper,
                           DataSourceService dataSourceService) {
        this.defMapper = defMapper;
        this.objectMapper = objectMapper;
        this.dataSourceService = dataSourceService;
    }

    /** 按作用域列出字段定义（含停用，前端配置页用）；按 sortNo、id 升序。 */
    public List<FieldDefVO> list(String scope, boolean enabledOnly) {
        FieldScope fs = requireScope(scope);
        var wrapper = Wrappers.<PmFieldDef>lambdaQuery()
                .eq(PmFieldDef::getScope, fs.getCode())
                .orderByAsc(PmFieldDef::getSortNo)
                .orderByAsc(PmFieldDef::getId);
        if (enabledOnly) {
            wrapper.eq(PmFieldDef::getEnabled, 1);
        }
        return defMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(FieldDefCreateDTO dto) {
        FieldScope scope = requireScope(dto.scope());
        FieldType type = requireType(dto.type());
        String optionsJson = normalizeOptions(type, dto.options(), dto.dataSourceId());
        assertKeyUnique(scope, dto.fieldKey(), null);

        PmFieldDef def = new PmFieldDef();
        def.setScope(scope.getCode());
        def.setFieldKey(dto.fieldKey());
        def.setName(dto.name());
        def.setType(type.getCode());
        def.setOptions(optionsJson);
        def.setDataSourceId(dto.dataSourceId());
        def.setRequired(Boolean.TRUE.equals(dto.required()) ? 1 : 0);
        def.setSortNo(dto.sortNo() == null ? 0 : dto.sortNo());
        def.setEnabled(1);
        defMapper.insert(def);
        return def.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FieldDefUpdateDTO dto) {
        PmFieldDef def = requireExists(id);
        FieldType type = requireType(dto.type());
        def.setName(dto.name());
        def.setType(type.getCode());
        def.setOptions(normalizeOptions(type, dto.options(), dto.dataSourceId()));
        def.setDataSourceId(dto.dataSourceId());
        if (dto.required() != null) {
            def.setRequired(dto.required() ? 1 : 0);
        }
        if (dto.sortNo() != null) {
            def.setSortNo(dto.sortNo());
        }
        if (dto.enabled() != null) {
            def.setEnabled(dto.enabled() ? 1 : 0);
        }
        defMapper.updateById(def);
    }

    /** 逻辑删除字段定义。存量字段值留存不清理（旧值由前端按 def 缺失时忽略渲染）。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        defMapper.deleteById(id);
    }

    PmFieldDef requireExists(Long id) {
        PmFieldDef def = defMapper.selectById(id);
        if (def == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "字段定义不存在: " + id);
        }
        return def;
    }

    private void assertKeyUnique(FieldScope scope, String fieldKey, Long excludeId) {
        var wrapper = Wrappers.<PmFieldDef>lambdaQuery()
                .eq(PmFieldDef::getScope, scope.getCode())
                .eq(PmFieldDef::getFieldKey, fieldKey);
        if (excludeId != null) {
            wrapper.ne(PmFieldDef::getId, excludeId);
        }
        if (defMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "字段标识已存在: " + fieldKey);
        }
    }

    /**
     * 选项规范化：非选项型清空 options；选项型若引用数据源(dataSourceId 非空)则内联 options 置空，
     * 否则必须提供内联 options 且 value 不重复。
     */
    private String normalizeOptions(FieldType type, List<FieldOption> options, Long dataSourceId) {
        if (!type.isOptionBased()) {
            return null;
        }
        if (dataSourceId != null) {
            return null; // 选项来自数据源，不存内联
        }
        if (options == null || options.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "选项型字段必须提供 options 或引用数据源");
        }
        Set<String> seen = new HashSet<>();
        for (FieldOption o : options) {
            if (!seen.add(o.value())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "选项值重复: " + o.value());
            }
        }
        return writeJson(options);
    }

    private FieldDefVO toVO(PmFieldDef def) {
        return new FieldDefVO(def.getId(), def.getScope(), def.getFieldKey(), def.getName(),
                def.getType(), effectiveOptions(def), def.getDataSourceId(),
                def.getRequired() != null && def.getRequired() == 1, def.getSortNo(),
                def.getEnabled() != null && def.getEnabled() == 1);
    }

    /** 字段有效选项：引用数据源时取数据源选项，否则取内联 options。 */
    private List<FieldOption> effectiveOptions(PmFieldDef def) {
        if (def.getDataSourceId() != null) {
            return dataSourceService.resolveOptions(def.getDataSourceId());
        }
        return readOptions(def.getOptions());
    }

    private FieldScope requireScope(String scope) {
        FieldScope fs = FieldScope.fromCode(scope);
        if (fs == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法作用域: " + scope);
        }
        return fs;
    }

    private FieldType requireType(String type) {
        FieldType ft = FieldType.fromCode(type);
        if (ft == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法字段类型: " + type);
        }
        return ft;
    }

    private String writeJson(List<FieldOption> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "选项序列化失败");
        }
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
}
