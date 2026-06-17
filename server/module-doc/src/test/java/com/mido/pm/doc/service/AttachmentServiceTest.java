package com.mido.pm.doc.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.cost.dto.CostVO;
import com.mido.pm.cost.service.CostService;
import com.mido.pm.doc.dto.AttachmentRegisterDTO;
import com.mido.pm.doc.dto.UploadTicketVO;
import com.mido.pm.doc.entity.PmAttachment;
import com.mido.pm.doc.mapper.PmAttachmentMapper;
import com.mido.pm.provider.storage.StorageProvider;
import com.mido.pm.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 附件服务单测（mock mapper/存储/事件，无 DB/MinIO）：
 * 预签名下载 URL 按存储 oss_key 生成并返回；附件不存在则报错且不触达存储。
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock private PmAttachmentMapper attachmentMapper;
    @Mock private StorageProvider storageProvider;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private TaskService taskService;
    @Mock private CostService costService;
    @InjectMocks private AttachmentService service;

    private PmAttachment attachment(String ossKey) {
        PmAttachment a = new PmAttachment();
        a.setId(1L);
        a.setEntityType("task");
        a.setEntityId(9L);
        a.setName("设计稿.png");
        a.setOssKey(ossKey);
        a.setSize(1024L);
        return a;
    }

    private PmAttachment att(long id, String type) {
        PmAttachment a = new PmAttachment();
        a.setId(id);
        a.setEntityType(type);
        a.setEntityId(id);
        a.setName("f" + id);
        a.setSize(1L);
        return a;
    }

    @Test
    void registerCreatesRowAndReturnsPresignedPutUrl() {
        when(storageProvider.presignedPutUrl(any(), eq(Duration.ofMinutes(10))))
                .thenReturn("http://minio/put-url");

        UploadTicketVO ticket = service.register(
                new AttachmentRegisterDTO("project", 7L, "需求.pdf", 2048L, "application/pdf"));

        assertEquals("http://minio/put-url", ticket.uploadUrl());
        verify(attachmentMapper).insert(any(PmAttachment.class));
        verify(eventPublisher).publish(eq("attachment.uploaded"), any());
        // UploadTicketVO 无 ossKey 字段（结构上不外泄）
    }

    @Test
    void listByProjectAggregatesProjectTaskCostSortedDesc() {
        when(taskService.taskIdsByProject(7L)).thenReturn(List.of(10L, 11L));
        when(costService.listByProject(7L)).thenReturn(List.of(
                new CostVO(20L, 7L, "餐费", "餐费", null, null, null, null, "未发生", null, null)));
        // 三次 selectList：项目级 / 任务 / 费用
        when(attachmentMapper.selectList(any())).thenReturn(
                List.of(att(1, "project")), List.of(att(5, "task")), List.of(att(3, "cost")));

        List<?> result = service.listByProject(7L);

        assertEquals(3, result.size());
        assertEquals(5L, ((com.mido.pm.doc.dto.AttachmentVO) result.get(0)).id());
        assertEquals(3L, ((com.mido.pm.doc.dto.AttachmentVO) result.get(1)).id());
        assertEquals(1L, ((com.mido.pm.doc.dto.AttachmentVO) result.get(2)).id());
    }

    @Test
    void downloadUrlIsPresignedForStoredKey() {
        when(attachmentMapper.selectById(1L)).thenReturn(attachment("t1/task/9/abc/设计稿.png"));
        when(storageProvider.presignedGetUrl(eq("t1/task/9/abc/设计稿.png"), eq(Duration.ofMinutes(5))))
                .thenReturn("http://minio/signed-url");

        String url = service.downloadUrl(1L);

        assertEquals("http://minio/signed-url", url);
        // 以存储的 oss_key 生成预签名 URL（key 不外泄前端）
        verify(storageProvider).presignedGetUrl(eq("t1/task/9/abc/设计稿.png"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void downloadUrlRejectsMissingAttachment() {
        when(attachmentMapper.selectById(2L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.downloadUrl(2L));
        verifyNoInteractions(storageProvider);
    }

    @Test
    void deleteLogicallyRemovesRowAndEmitsEvent() {
        when(attachmentMapper.selectById(1L)).thenReturn(attachment("t1/task/9/abc/x.png"));
        service.delete(1L);
        verify(attachmentMapper).deleteById(1L);
        verify(eventPublisher).publish(eq("attachment.deleted"), org.mockito.ArgumentMatchers.any());
    }
}
