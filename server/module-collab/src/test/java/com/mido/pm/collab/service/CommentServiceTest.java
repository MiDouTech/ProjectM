package com.mido.pm.collab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.collab.dto.CommentCreateDTO;
import com.mido.pm.collab.dto.CommentVO;
import com.mido.pm.collab.entity.PmComment;
import com.mido.pm.collab.event.CollabEvents;
import com.mido.pm.collab.mapper.PmCommentMapper;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 评论服务单测：mention 序列化、空 mention、当前用户、列表映射、事件 payload。 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private PmCommentMapper commentMapper;
    @Mock
    private DomainEventPublisher eventPublisher;
    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentMapper, eventPublisher, new ObjectMapper());
        CurrentUser u = new CurrentUser();
        u.setUserId(7L);
        UserContext.set(u);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createSerializesMentionAndEmitsEvent() {
        ArgumentCaptor<PmComment> entity = ArgumentCaptor.forClass(PmComment.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);

        service.create(new CommentCreateDTO("task", 100L, "@张三 看下", List.of(1L, 2L, 3L)));

        verify(commentMapper).insert(entity.capture());
        assertEquals("[1,2,3]", entity.getValue().getMention());
        assertEquals(7L, entity.getValue().getUserId()); // 取自当前用户上下文

        verify(eventPublisher).publish(eq(CollabEvents.COMMENT_CREATED), payload.capture());
        var map = (java.util.Map<String, Object>) payload.getValue();
        assertEquals("task", map.get("entityType"));
        assertEquals(100L, map.get("entityId"));
        assertEquals(List.of(1L, 2L, 3L), map.get("mention"));
    }

    @Test
    void createWithNullMentionUsesEmptyArray() {
        ArgumentCaptor<PmComment> entity = ArgumentCaptor.forClass(PmComment.class);
        service.create(new CommentCreateDTO("project", 9L, "纯文本评论", null));
        verify(commentMapper).insert(entity.capture());
        assertEquals("[]", entity.getValue().getMention());
    }

    @Test
    void listMapsAndReadsMention() {
        PmComment c = new PmComment();
        c.setId(1L);
        c.setEntityType("task");
        c.setEntityId(100L);
        c.setUserId(7L);
        c.setContent("内容");
        c.setMention("[5,6]");
        when(commentMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(c));

        List<CommentVO> vos = service.list("task", 100L);
        assertEquals(1, vos.size());
        assertEquals(List.of(5L, 6L), vos.get(0).mention());
    }

    @Test
    void listReadsBlankMentionAsEmpty() {
        PmComment c = new PmComment();
        c.setId(2L);
        c.setMention(null);
        when(commentMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(c));
        assertEquals(List.of(), service.list("task", 1L).get(0).mention());
    }
}
