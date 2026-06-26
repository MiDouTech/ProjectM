package com.mido.pm.view.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.view.domain.PageFieldCatalog;
import com.mido.pm.view.domain.PageFieldCatalog.FieldDef;
import com.mido.pm.view.entity.PmPageConfig;
import com.mido.pm.view.mapper.PmPageConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 可配置页面表单（ADR-0004 · L3.0）：内置字段目录 + 页面配置读写。
 * 内置字段由代码声明，自定义字段（pm_field_def）由前端合成，避免跨域耦合；config 透传存取（校验为合法 JSON）。
 */
@Service
public class PageConfigService {

    private static final Set<String> TEMPLATES = Set.of("form", "detail", "list");
    private static final Pattern TARGET = Pattern.compile("^[a-z_]{1,32}$");

    private final PmPageConfigMapper mapper;

    public PageConfigService(PmPageConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 某实体的内置字段目录（前端再并自定义字段）。 */
    public List<FieldDef> builtinFields(String target) {
        requireKnownTarget(target);
        return PageFieldCatalog.of(target);
    }

    /** 某实体某模板的页面配置；未配置返回 null（前端回落默认）。 */
    public Object get(String target, String templateType) {
        PmPageConfig c = find(target, templateType);
        return c == null || c.getConfig() == null ? null : JSONUtil.parse(c.getConfig());
    }

    /** upsert 页面配置。 */
    @Transactional(rollbackFor = Exception.class)
    public void save(String target, String templateType, Map<String, Object> config) {
        requireKnownTarget(target);
        requireTemplate(templateType);
        String json = JSONUtil.toJsonStr(config == null ? Map.of() : config);
        PmPageConfig c = find(target, templateType);
        if (c == null) {
            c = new PmPageConfig();
            c.setTarget(target);
            c.setTemplateType(templateType);
            c.setConfig(json);
            mapper.insert(c);
        } else {
            c.setConfig(json);
            mapper.updateById(c);
        }
    }

    private PmPageConfig find(String target, String templateType) {
        requireKnownTarget(target);
        requireTemplate(templateType);
        return mapper.selectOne(Wrappers.<PmPageConfig>lambdaQuery()
                .eq(PmPageConfig::getTarget, target)
                .eq(PmPageConfig::getTemplateType, templateType)
                .last("limit 1"));
    }

    private void requireKnownTarget(String target) {
        if (target == null || !TARGET.matcher(target).matches() || !PageFieldCatalog.known(target)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知页面实体: " + target);
        }
    }

    private void requireTemplate(String templateType) {
        if (!TEMPLATES.contains(templateType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知页面模板: " + templateType);
        }
    }
}
