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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @Mock private com.mido.pm.doc.mapper.PmDocFavoriteMapper favoriteMapper;
    @Mock private com.mido.pm.doc.mapper.PmDocTemplateMapper templateMapper;
    @Mock private DocAclService aclService;
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

        DocVersionVO vo = service.saveContent(1L, new DocSaveDTO("标题", "{\"type\":\"doc\"}", "纯文本", "首存", null));

        assertEquals(1, vo.versionNo());
        verify(versionMapper).insert(any(PmDocVersion.class));
        verify(docMapper).updateById(any(PmDoc.class)); // 指向 currentVersionId
        verify(eventPublisher).publish(eq("doc.version.created"), any());
    }

    @Test
    void saveContentRejectsStaleBaseVersion() {
        PmDoc d = doc(1L, PmDoc.TYPE_DOC);
        d.setCurrentVersionId(5L); // 当前已是 v5
        when(docMapper.selectById(1L)).thenReturn(d);
        // 客户端基于 v3 保存 → 冲突
        assertThrows(BizException.class,
                () -> service.saveContent(1L, new DocSaveDTO("t", "c", null, null, 3L)));
    }

    @Test
    void saveContentRejectsFolder() {
        when(docMapper.selectById(2L)).thenReturn(doc(2L, PmDoc.TYPE_FOLDER));
        assertThrows(BizException.class,
                () -> service.saveContent(2L, new DocSaveDTO(null, "x", null, null, null)));
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
    void listAcrossProjectsFiltersFoldersUnreadableAndMarksFavorite() {
        PmDoc d1 = doc(1L, PmDoc.TYPE_DOC);
        PmDoc folder = doc(2L, PmDoc.TYPE_FOLDER);
        PmDoc file = doc(3L, PmDoc.TYPE_FILE);
        PmDoc hidden = doc(4L, PmDoc.TYPE_DOC); // ACL 不可读
        when(docMapper.selectList(any())).thenReturn(List.of(d1, folder, file, hidden));
        when(aclService.readableDocIds(any())).thenReturn(java.util.Set.of(1L, 3L));
        com.mido.pm.doc.entity.PmDocFavorite fav = new com.mido.pm.doc.entity.PmDocFavorite();
        fav.setDocId(1L);
        when(favoriteMapper.selectList(any())).thenReturn(List.of(fav));

        var result = service.listAcrossProjects(List.of(7L, 8L), null, null);

        // 目录(2)默认剔除、不可读(4)剔除 → 仅文档1 与 文件3；1 标记收藏
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertTrue(result.get(0).favorited());
        assertEquals(3L, result.get(1).id());
        assertFalse(result.get(1).favorited());
    }

    @Test
    void listAcrossProjectsEmptyWhenNoProjects() {
        var result = service.listAcrossProjects(List.of(), null, null);

        assertEquals(0, result.size());
        verify(docMapper, never()).selectList(any());
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

    @Test
    void toggleFavoriteInsertsWhenAbsent() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC));
        when(favoriteMapper.selectOne(any())).thenReturn(null);

        boolean on = service.toggleFavorite(1L);

        org.junit.jupiter.api.Assertions.assertTrue(on);
        verify(favoriteMapper).insert(any(com.mido.pm.doc.entity.PmDocFavorite.class));
    }

    @Test
    void createRejectsInvalidType() {
        // 类型白名单校验先于父节点权限，故无需 mock
        assertThrows(BizException.class,
                () -> service.create(new DocCreateDTO(7L, 0L, "sheet", "x", null)));
        verify(docMapper, org.mockito.Mockito.never()).insert(any(PmDoc.class));
    }

    @Test
    void downloadUrlRejectsNonFile() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC)); // 非 file 节点
        assertThrows(BizException.class, () -> service.downloadUrl(1L));
    }

    @Test
    void versionContentRejectsMissing() {
        when(versionMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.versionContent(999L));
    }

    @Test
    void rollbackRejectsMissingVersion() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC));
        when(versionMapper.selectById(50L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.rollback(1L, 50L));
        verify(versionMapper, org.mockito.Mockito.never()).insert(any(PmDocVersion.class));
    }

    @Test
    void rollbackRejectsVersionOfAnotherDoc() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, PmDoc.TYPE_DOC));
        PmDocVersion src = new PmDocVersion();
        src.setId(50L);
        src.setDocId(2L); // 属于另一个文档
        when(versionMapper.selectById(50L)).thenReturn(src);
        assertThrows(BizException.class, () -> service.rollback(1L, 50L));
    }

    @Test
    void searchMatchesTitle() {
        PmDoc d = doc(1L, PmDoc.TYPE_DOC); // title 设计说明
        when(docMapper.selectList(any())).thenReturn(List.of(d));
        when(aclService.readableDocIds(any())).thenReturn(java.util.Set.of(1L));

        var hits = service.search(7L, "设计");

        assertEquals(1, hits.size());
        assertEquals(1L, hits.get(0).id());
    }
}
