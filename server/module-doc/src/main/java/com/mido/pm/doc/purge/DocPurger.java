package com.mido.pm.doc.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.provider.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** 文档/附件域数据清除。先清对象存储文件，再物理删表行。 */
@Component
public class DocPurger implements TenantDataPurger {

    private static final Logger log = LoggerFactory.getLogger(DocPurger.class);

    private final DocPurgeMapper mapper;
    private final StorageProvider storageProvider;

    public DocPurger(DocPurgeMapper mapper, StorageProvider storageProvider) {
        this.mapper = mapper;
        this.storageProvider = storageProvider;
    }

    @Override
    public String domain() {
        return "doc";
    }

    @Override
    public long purge(Long tenantId) {
        // 先删对象存储文件；任一失败则中止本租户清除（抛错使 purgeTenant 事务回滚、不记 purged），
        // 保留 DB 行待下次调度重试，避免"DB 已删但 OSS 残留孤儿文件 + 误判已清除"。
        List<String> ossKeys = mapper.selectOssKeys(tenantId);
        int removed = 0;
        int failed = 0;
        for (String key : ossKeys) {
            try {
                storageProvider.remove(key);
                removed++;
            } catch (Exception e) {
                failed++;
                log.warn("清除对象存储文件失败 tenantId={} key={}", tenantId, key, e);
            }
        }
        if (failed > 0) {
            throw new IllegalStateException(
                    "对象存储清除未完成(" + failed + "/" + ossKeys.size() + ")，已保留数据待重试 tenantId=" + tenantId);
        }
        if (!ossKeys.isEmpty()) {
            log.info("清除对象存储文件 tenantId={} 成功={}/{}", tenantId, removed, ossKeys.size());
        }
        return mapper.purgeFavorites(tenantId)
                + mapper.purgeShares(tenantId)
                + mapper.purgeAcls(tenantId)
                + mapper.purgeVersions(tenantId)
                + mapper.purgeDocs(tenantId)
                + mapper.purgeAttachments(tenantId)
                + mapper.purgeTemplates(tenantId);
    }
}
