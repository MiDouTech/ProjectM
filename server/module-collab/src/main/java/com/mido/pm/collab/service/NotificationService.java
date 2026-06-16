package com.mido.pm.collab.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.collab.dto.NotificationVO;
import com.mido.pm.collab.entity.PmNotification;
import com.mido.pm.collab.mapper.PmNotificationMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内信服务：未读数、列表、标记已读（均限当前登录用户）。
 */
@Service
public class NotificationService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final PmNotificationMapper mapper;

    public NotificationService(PmNotificationMapper mapper) {
        this.mapper = mapper;
    }

    public long unreadCount() {
        Long count = mapper.selectCount(Wrappers.<PmNotification>lambdaQuery()
                .eq(PmNotification::getUserId, currentUserId())
                .eq(PmNotification::getIsRead, 0));
        return count == null ? 0 : count;
    }

    public PageResult<NotificationVO> page(Long pageNoIn, Long sizeIn, Boolean unreadOnly) {
        long pageNo = pageNoIn == null || pageNoIn < 1 ? 1 : pageNoIn;
        long size = sizeIn == null || sizeIn < 1 ? 20 : Math.min(sizeIn, MAX_PAGE_SIZE);
        Page<PmNotification> page = new Page<>(pageNo, size);
        Page<PmNotification> result = mapper.selectPage(page, Wrappers.<PmNotification>lambdaQuery()
                .eq(PmNotification::getUserId, currentUserId())
                .eq(Boolean.TRUE.equals(unreadOnly), PmNotification::getIsRead, 0)
                .orderByDesc(PmNotification::getId));
        List<NotificationVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        PmNotification n = mapper.selectById(id);
        if (n == null || !currentUserId().equals(n.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        n.setIsRead(1);
        mapper.updateById(n);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        PmNotification update = new PmNotification();
        update.setIsRead(1);
        mapper.update(update, Wrappers.<PmNotification>lambdaUpdate()
                .eq(PmNotification::getUserId, currentUserId())
                .eq(PmNotification::getIsRead, 0));
    }

    private NotificationVO toVO(PmNotification n) {
        return new NotificationVO(n.getId(), n.getType(), n.getTitle(), n.getPayload(),
                n.getIsRead(), n.getChannel(), n.getCreateTime());
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }
}
