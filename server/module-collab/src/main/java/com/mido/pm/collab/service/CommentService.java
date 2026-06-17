package com.mido.pm.collab.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.collab.dto.CommentCreateDTO;
import com.mido.pm.collab.dto.CommentVO;
import com.mido.pm.collab.entity.PmComment;
import com.mido.pm.collab.event.CollabEvents;
import com.mido.pm.collab.mapper.PmCommentMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论服务：对任务/项目/目标评论，支持 @用户。写操作发 comment.created（@提醒由通知监听器处理）。
 */
@Service
public class CommentService {

    private final PmCommentMapper commentMapper;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public CommentService(PmCommentMapper commentMapper, DomainEventPublisher eventPublisher,
                          ObjectMapper objectMapper) {
        this.commentMapper = commentMapper;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(CommentCreateDTO dto) {
        List<Long> mention = dto.mention() == null ? List.of() : dto.mention();
        PmComment c = new PmComment();
        c.setEntityType(dto.entityType());
        c.setEntityId(dto.entityId());
        c.setUserId(currentUserId());
        c.setContent(dto.content());
        c.setMention(writeJson(mention));
        commentMapper.insert(c);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commentId", c.getId());
        payload.put("entityType", c.getEntityType());
        payload.put("entityId", c.getEntityId());
        payload.put("userId", c.getUserId());
        payload.put("mention", mention);
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(CollabEvents.COMMENT_CREATED, payload);
        return c.getId();
    }

    public List<CommentVO> list(String entityType, Long entityId) {
        return commentMapper.selectList(Wrappers.<PmComment>lambdaQuery()
                        .eq(PmComment::getEntityType, entityType)
                        .eq(PmComment::getEntityId, entityId)
                        .orderByAsc(PmComment::getId))
                .stream().map(this::toVO).toList();
    }

    private CommentVO toVO(PmComment c) {
        return new CommentVO(c.getId(), c.getEntityType(), c.getEntityId(), c.getUserId(),
                c.getContent(), readMention(c.getMention()), c.getCreateTime());
    }

    private String writeJson(List<Long> mention) {
        try {
            return objectMapper.writeValueAsString(mention);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "mention 序列化失败");
        }
    }

    private List<Long> readMention(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }
}
