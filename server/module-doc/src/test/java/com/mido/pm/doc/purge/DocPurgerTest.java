package com.mido.pm.doc.purge;

import com.mido.pm.provider.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 文档/附件域清除单测：先删对象存储文件再删表行，单个文件失败不阻断。 */
@ExtendWith(MockitoExtension.class)
class DocPurgerTest {

    @Mock
    private DocPurgeMapper mapper;
    @Mock
    private StorageProvider storageProvider;

    @InjectMocks
    private DocPurger purger;

    @Test
    void purgeRemovesOssObjectsThenRows() {
        when(mapper.selectOssKeys(2L)).thenReturn(List.of("k1", "k2"));
        when(mapper.purgeFavorites(2L)).thenReturn(1);
        when(mapper.purgeShares(2L)).thenReturn(1);
        when(mapper.purgeAcls(2L)).thenReturn(1);
        when(mapper.purgeVersions(2L)).thenReturn(1);
        when(mapper.purgeDocs(2L)).thenReturn(1);
        when(mapper.purgeAttachments(2L)).thenReturn(2);
        when(mapper.purgeTemplates(2L)).thenReturn(1);

        long n = purger.purge(2L);

        assertEquals(8, n);
        verify(storageProvider).remove("k1");
        verify(storageProvider).remove("k2");
    }

    @Test
    void singleObjectRemovalFailureDoesNotAbort() {
        when(mapper.selectOssKeys(2L)).thenReturn(List.of("bad", "ok"));
        doThrow(new RuntimeException("oss down")).when(storageProvider).remove("bad");

        long n = purger.purge(2L);

        // 仍继续删表行（mapper 各方法默认返回 0），不抛异常
        assertEquals(0, n);
        verify(storageProvider).remove("ok");
        verify(mapper).purgeAttachments(2L);
    }
}
