package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.tenant.TenantDataExporter;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.entity.SysTenantExport;
import com.mido.pm.platform.mapper.SysTenantExportMapper;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.security.PlatformContext;
import com.mido.pm.platform.support.PlatformTenantScope;
import com.mido.pm.provider.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户数据导出：核心域异步导出。创建 pending 任务 → 定时处理器按租户聚合各域数据 →
 * JSON 写对象存储 → 标记 done；下载走限时预签名 URL（不外泄存储 key）。
 */
@Service
public class PlatformExportService {

    private static final Logger log = LoggerFactory.getLogger(PlatformExportService.class);
    private static final int BATCH = 5;

    private final SysTenantExportMapper exportMapper;
    private final SysTenantMapper tenantMapper;
    private final List<TenantDataExporter> exporters;
    private final StorageProvider storageProvider;
    private final ObjectMapper objectMapper;
    private final PlatformAuditService auditService;

    public PlatformExportService(SysTenantExportMapper exportMapper, SysTenantMapper tenantMapper,
                                 List<TenantDataExporter> exporters, StorageProvider storageProvider,
                                 ObjectMapper objectMapper, PlatformAuditService auditService) {
        this.exportMapper = exportMapper;
        this.tenantMapper = tenantMapper;
        this.exporters = exporters;
        this.storageProvider = storageProvider;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    /** 发起导出（异步）：创建 pending 任务。 */
    public Long requestExport(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        SysTenantExport task = new SysTenantExport();
        task.setTenantId(tenantId);
        task.setStatus("pending");
        task.setRequestedBy(PlatformContext.currentAdminId());
        exportMapper.insert(task);
        auditService.record(PlatformAuditActions.TENANT_EXPORT_REQUESTED,
                PlatformAuditActions.TARGET_TENANT, tenantId, Map.of("exportId", String.valueOf(task.getId())));
        return task.getId();
    }

    /** 某租户的导出任务列表。 */
    public List<SysTenantExport> list(Long tenantId) {
        return exportMapper.selectList(Wrappers.<SysTenantExport>lambdaQuery()
                .eq(SysTenantExport::getTenantId, tenantId).orderByDesc(SysTenantExport::getId));
    }

    /** 取导出文件下载 URL（限时预签名）。 */
    public String downloadUrl(Long exportId) {
        SysTenantExport task = exportMapper.selectById(exportId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导出任务不存在");
        }
        if (!"done".equals(task.getStatus()) || task.getFileKey() == null) {
            throw new BizException(ErrorCode.CONFLICT, "导出尚未完成");
        }
        return storageProvider.presignedGetUrl(task.getFileKey(), Duration.ofHours(1));
    }

    /** 定时处理待导出任务（异步落地，避免请求线程做重活）。 */
    public int processPending() {
        List<SysTenantExport> pending = exportMapper.selectList(Wrappers.<SysTenantExport>lambdaQuery()
                .eq(SysTenantExport::getStatus, "pending").orderByAsc(SysTenantExport::getId).last("limit " + BATCH));
        for (SysTenantExport task : pending) {
            process(task);
        }
        return pending.size();
    }

    private void process(SysTenantExport task) {
        task.setStatus("processing");
        exportMapper.updateById(task);
        try {
            Map<String, Object> bundle = new LinkedHashMap<>();
            bundle.put("tenantId", String.valueOf(task.getTenantId()));
            try (PlatformTenantScope ignored = PlatformTenantScope.of(task.getTenantId())) {
                for (TenantDataExporter exporter : exporters) {
                    bundle.put(exporter.domain(), exporter.exportData());
                }
            }
            byte[] json = objectMapper.writeValueAsBytes(bundle);
            String key = "exports/tenant-" + task.getTenantId() + "/" + task.getId() + ".json";
            storageProvider.put(key, new ByteArrayInputStream(json), json.length, "application/json");
            task.setStatus("done");
            task.setFileKey(key);
            exportMapper.updateById(task);
        } catch (Exception e) {
            log.error("租户数据导出失败 exportId={}", task.getId(), e);
            task.setStatus("failed");
            task.setError(e.getMessage());
            exportMapper.updateById(task);
        }
    }
}
