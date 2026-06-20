package com.mido.pm.doc.usage;

import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.quota.UsageContributor;
import com.mido.pm.doc.mapper.PmAttachmentMapper;
import org.springframework.stereotype.Component;

/** 用量贡献：当前租户附件存储（MB，向上取整；经多租户拦截器按 TenantContext 隔离）。 */
@Component
public class StorageUsageContributor implements UsageContributor {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final PmAttachmentMapper attachmentMapper;

    public StorageUsageContributor(PmAttachmentMapper attachmentMapper) {
        this.attachmentMapper = attachmentMapper;
    }

    @Override
    public String resource() {
        return QuotaResources.STORAGE_MB;
    }

    @Override
    public long currentCount() {
        long bytes = attachmentMapper.sumSizeBytes();
        return (bytes + BYTES_PER_MB - 1) / BYTES_PER_MB;
    }
}
