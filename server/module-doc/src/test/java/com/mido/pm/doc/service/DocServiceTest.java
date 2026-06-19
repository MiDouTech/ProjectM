package com.mido.pm.doc.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.doc.dto.AttachmentVO;
import com.mido.pm.doc.dto.DocCreateDTO;
import com.mido.pm.doc.dto.DocMoveDTO;
import com.mido.pm.doc.dto.DocSaveDTO;
import com.mido.pm.doc.dto.DocVersionVO;
import com.mido.pm.doc.entity.PmDoc;
import com.mido.pm.doc.entity.PmDocVersion;
import com.mido.pm.doc.mapper.PmDocMapper;
import com.mido.pm.doc.mapper.PmDocVersionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文档服务单测（mock mapper/事件，无 DB）：
 * 建节点发 doc.created；保存正文产生版本(版本号 max+1)并指向当前版本、发 doc.version.created；
 * 目录节点禁存正文；回滚以旧版正文追加新版本；不能移动到自身。
 */
@ExtendWith(MockitoExtension.class)
class DocServiceTest {

    @Mock private PmDocMapper docMapper;
    @Mock private PmDocVersionMapper versionMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private AttachmentService attachmentService;
    @InjectMocks private DocService service;

    private PmDoc doc(long id, String type) {
        PmDoc d = new PmDoc();
        d.setId(id);
        d.setProjectId(7L);
        d.setParentId(0L);
        d.setType(type);
        d.setTitle("设计说明");
        return d;
    }

    @Test
    void createDocInsertsAndEmits() {
        when(docMapper.selectList(any())).thenReturn(List.of()); // nextSortNo 查询

        service.create(new DocCreateDTO(7L, 0L, PmDoc.TYPE_DOC, "需求文档", null));

        verify(docMapper).insert(any(PmDoc.class));
        verify(eventPublisher).publish(eq("doc.created"), any());
    }

    @Test
    void saveContentCreatesVersionAndPointsCurrent() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC));
        when(versionMapper.selectList(any())).thenReturn(List.of()); // 无历史 → 版本号 1

        DocVersionVO vo = service.saveContent(1L, new DocSaveDTO("标题", "{\"type\":\"doc\"}", "纯文本", "首存"));

        assertEquals(1, vo.versionNo());
        verify(versionMapper).insert(any(PmDocVersion.class));
        verify(docMapper).updateById(any(PmDoc.class)); // 指向 currentVersionId
        verify(eventPublisher).publish(eq("doc.version.created"), any());
    }

    @Test
    void saveContentRejectsFolder() {
        when(docMapper.selectById(2L)).thenReturn(doc(2L, PmDoc.TYPE_FOLDER));
        assertThrows(BizException.class,
                () -> service.saveContent(2L, new DocSaveDTO(null, "x", null, null)));
    }

    @Test
    void rollbackAppendsVersionFromSource() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC));
        PmDocVersion src = new PmDocVersion();
        src.setId(50L);
        src.setDocId(1L);
        src.setVersionNo(2);
        src.setTitle("旧标题");
        src.setContent("{\"old\":true}");
        when(versionMapper.selectById(50L)).thenReturn(src);
        when(versionMapper.selectList(any())).thenReturn(List.of()); // 计算新版本号

        DocVersionVO vo = service.rollback(1L, 50L);

        assertEquals("{\"old\":true}", vo.content());
        verify(versionMapper).insert(any(PmDocVersion.class));
        verify(eventPublisher).publish(eq("doc.version.created"), any());
    }

    @Test
    void moveIntoSelfRejected() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC));
        assertThrows(BizException.class, () -> service.move(1L, new DocMoveDTO(1L, null)));
    }

    @Test
    void uploadFileCreatesFileNodeAndEmits() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(attachmentService.upload(eq("doc"), eq(7L), any()))
                .thenReturn(new AttachmentVO(99L, "doc", 7L, "需求.pdf", 2048L, null, null));
        when(docMapper.selectList(any())).thenReturn(List.of()); // nextSortNo

        service.uploadFile(7L, 0L, file);

        verify(docMapper).insert(any(PmDoc.class)); // file 节点
        verify(eventPublisher).publish(eq("doc.created"), any());
    }

    @Test
    void trashCascadesAndEmitsDeleted() {
        PmDoc d = doc(1L, PmDoc.TYPE_DOC);
        d.setTrashed(0);
        when(docMapper.selectById(1L)).thenReturn(d);
        when(docMapper.selectList(any())).thenReturn(List.of(d)); // subtreeIds 取项目全部节点

        service.trash(1L);

        verify(docMapper).updateById(any(PmDoc.class)); // 置 trashed=1
        verify(eventPublisher).publish(eq("doc.deleted"), any());
    }
}
