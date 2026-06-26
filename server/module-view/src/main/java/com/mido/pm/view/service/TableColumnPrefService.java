package com.mido.pm.view.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.view.entity.PmView;
import com.mido.pm.view.mapper.PmViewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 列表表头偏好：每用户每列表（listKey）保存一份「展示列 + 冻结列」配置，跨设备一致。
 * 复用 pm_view（scope = cols:&lt;listKey&gt;，owner_id=用户），借既有唯一键 (tenant,owner,scope)
 * 保证每用户每列表至多一行。config 为前端定义的轻量 JSON（{columns:[...], frozen:[...]}），
 * 后端透传存取、不解释列含义（属用户 UI 偏好，低风险）。
 */
@Service
public class TableColumnPrefService {

    private static final String SCOPE_PREFIX = "cols:";
    private static final String TYPE = "table-pref";
    /** listKey 限定小写字母/数字/连字符，且 cols:<key> 不超过 scope(32) */
    private static final Pattern LIST_KEY = Pattern.compile("^[a-z0-9-]{1,24}$");

    private final PmViewMapper viewMapper;

    public TableColumnPrefService(PmViewMapper viewMapper) {
        this.viewMapper = viewMapper;
    }

    /** 当前用户某列表的表头偏好；未设置返回 null。 */
    public Object get(String listKey) {
        PmView v = find(listKey);
        return v == null || v.getConfig() == null ? null : JSONUtil.parse(v.getConfig());
    }

    /** upsert 当前用户某列表的表头偏好。 */
    @Transactional(rollbackFor = Exception.class)
    public void save(String listKey, Map<String, Object> config) {
        String scope = scope(listKey);
        String json = JSONUtil.toJsonStr(config == null ? Map.of() : config);
        PmView v = find(listKey);
        if (v == null) {
            v = new PmView();
            v.setScope(scope);
            v.setOwnerId(UserContext.currentUserId());
            v.setType(TYPE);
            v.setConfig(json);
            viewMapper.insert(v);
        } else {
            v.setConfig(json);
            viewMapper.updateById(v);
        }
    }

    private PmView find(String listKey) {
        return viewMapper.selectOne(Wrappers.<PmView>lambdaQuery()
                .eq(PmView::getOwnerId, UserContext.currentUserId())
                .eq(PmView::getScope, scope(listKey))
                .last("limit 1"));
    }

    private String scope(String listKey) {
        if (listKey == null || !LIST_KEY.matcher(listKey).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法列表标识: " + listKey);
        }
        return SCOPE_PREFIX + listKey;
    }
}
