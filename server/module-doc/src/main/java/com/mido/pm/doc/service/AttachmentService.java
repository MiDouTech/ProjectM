package com.mido.pm.doc.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.doc.dto.AttachmentVO;
import com.mido.pm.doc.entity.PmAttachment;
import com.mido.pm.doc.event.AttachmentEvents;
import com.mido.pm.doc.mapper.PmAttachmentMapper;
import com.mido.pm.provider.storage.StorageProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 附件服务：上传（后端代理 put 到对象存储）/列表/预签名下载 URL/逻辑删除。
 * oss_key 服务端生成且不外泄；写操作发领域事件（attachment.uploaded / attachment.deleted）。
 */
@Service
public class AttachmentService {

    /** 预签名下载 URL 有效期（短时，避免被长期转发）。 */
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(5);

    private final PmAttachmentMapper attachmentMapper;
    private final StorageProvider storageProvider;
    private final DomainEventPublisher eventPublisher;

    public AttachmentService(PmAttachmentMapper attachmentMapper, StorageProvider storageProvider,
                             DomainEventPublisher eventPublisher) {
        this.attachmentMapper = attachmentMapper;
        this.storageProvider = storageProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(String entityType, Long entityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        String fileName = sanitize(file.getOriginalFilename());
        String ossKey = buildKey(entityType, entityId, fileName);
        try {
            storageProvider.put(ossKey, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败: " + e.getMessage());
        }

        PmAttachment a = new PmAttachment();
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setName(fileName);
        a.setOssKey(ossKey);
        a.setSize(file.getSize());
        a.setCreateBy(currentUserId()); // 上传人：createBy 不在自动填充范围，显式写入
        attachmentMapper.insert(a);

        eventPublisher.publish(AttachmentEvents.UPLOADED, payload(a, "name", fileName));
        return toVO(a);
    }

    public List<AttachmentVO> list(String entityType, Long entityId) {
        return attachmentMapper.selectList(Wrappers.<PmAttachment>lambdaQuery()
                        .eq(PmAttachment::getEntityType, entityType)
                        .eq(PmAttachment::getEntityId, entityId)
                        .orderByDesc(PmAttachment::getId))
                .stream().map(this::toVO).toList();
    }

    /** 生成限时预签名下载 URL（不外泄 oss_key）。 */
    public String downloadUrl(Long id) {
        PmAttachment a = requireExists(id);
        return storageProvider.presignedGetUrl(a.getOssKey(), DOWNLOAD_URL_TTL);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PmAttachment a = requireExists(id);
        attachmentMapper.deleteById(id); // 逻辑删除（@TableLogic）
        eventPublisher.publish(AttachmentEvents.DELETED, payload(a, "name", a.getName()));
    }

    // ===== 内部 =====

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    private PmAttachment requireExists(Long id) {
        PmAttachment a = attachmentMapper.selectById(id);
        if (a == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        return a;
    }

    /** key 形如 t{tenant}/{entityType}/{entityId}/{uuid}/{name}，按租户与实体维度隔离。 */
    private String buildKey(String entityType, Long entityId, String fileName) {
        Long tenant = TenantContext.getOrDefault(TenantContext.DEFAULT_TENANT_ID);
        return "t" + tenant + "/" + entityType + "/" + entityId + "/"
                + UUID.randomUUID().toString().replace("-", "") + "/" + fileName;
    }

    /** 去掉路径分隔符，避免 key 注入/穿越；空名兜底。 */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        return name.replaceAll("[\\\\/]+", "_").trim();
    }

    private Map<String, Object> payload(PmAttachment a, Object... extra) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("attachmentId", a.getId());
        map.put("entityType", a.getEntityType());
        map.put("entityId", a.getEntityId());
        map.put("size", a.getSize());
        for (int i = 0; i + 1 < extra.length; i += 2) {
            map.put(String.valueOf(extra[i]), extra[i + 1]);
        }
        map.put("operatorId", UserContext.get() == null ? null : UserContext.get().getUserId());
        map.put("occurredAt", LocalDateTime.now().toString());
        return map;
    }

    private AttachmentVO toVO(PmAttachment a) {
        return new AttachmentVO(a.getId(), a.getEntityType(), a.getEntityId(),
                a.getName(), a.getSize(), a.getCreateBy(), a.getCreateTime());
    }
}
