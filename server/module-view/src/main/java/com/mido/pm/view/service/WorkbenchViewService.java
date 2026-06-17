package com.mido.pm.view.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.view.dto.WorkbenchLayoutDTO;
import com.mido.pm.view.entity.PmView;
import com.mido.pm.view.mapper.PmViewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工作台布局服务：按 (owner_id, scope=workbench) 持久化卡片有序列表到 pm_view。
 * 视图配置为个人偏好，非业务事实，写操作不发领域事件（与 CLAUDE.md §6 事件本意一致）。
 */
@Service
public class WorkbenchViewService {

    /** pm_view.scope：工作台 */
    private static final String SCOPE_WORKBENCH = "workbench";
    /** pm_view.type：卡片布局 */
    private static final String TYPE_CARDS = "cards";

    private final PmViewMapper viewMapper;
    private final ObjectMapper objectMapper;

    public WorkbenchViewService(PmViewMapper viewMapper, ObjectMapper objectMapper) {
        this.viewMapper = viewMapper;
        this.objectMapper = objectMapper;
    }

    /** 取当前用户的工作台布局；未保存过返回 cards=null（前端用默认布局）。 */
    public WorkbenchLayoutDTO getMyLayout() {
        PmView view = findMine();
        return new WorkbenchLayoutDTO(view == null ? null : readCards(view.getConfig()));
    }

    /** 保存（upsert）当前用户的工作台布局。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveMyLayout(WorkbenchLayoutDTO dto) {
        List<String> cards = dto == null || dto.cards() == null ? List.of() : dto.cards();
        String config = writeCards(cards);
        PmView existing = findMine();
        if (existing == null) {
            PmView view = new PmView();
            view.setScope(SCOPE_WORKBENCH);
            view.setOwnerId(currentUserId());
            view.setType(TYPE_CARDS);
            view.setConfig(config);
            viewMapper.insert(view);
        } else {
            existing.setConfig(config);
            viewMapper.updateById(existing);
        }
    }

    private PmView findMine() {
        return viewMapper.selectOne(Wrappers.<PmView>lambdaQuery()
                .eq(PmView::getScope, SCOPE_WORKBENCH)
                .eq(PmView::getOwnerId, currentUserId())
                .last("limit 1"));
    }

    private List<String> readCards(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeCards(List<String> cards) {
        try {
            return objectMapper.writeValueAsString(cards);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "布局序列化失败: " + e.getMessage());
        }
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }
}
