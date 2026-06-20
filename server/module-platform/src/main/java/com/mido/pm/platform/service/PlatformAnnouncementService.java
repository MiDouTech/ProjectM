package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.AnnouncementSaveDTO;
import com.mido.pm.platform.dto.AnnouncementVO;
import com.mido.pm.platform.entity.SysAnnouncement;
import com.mido.pm.platform.mapper.SysAnnouncementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台公告：运营侧 CRUD/发布；租户侧读取当前生效（已发布、在有效期内）公告。
 */
@Service
public class PlatformAnnouncementService {

    private static final String STATUS_PUBLISHED = "published";

    private final SysAnnouncementMapper announcementMapper;
    private final PlatformAuditService auditService;

    public PlatformAnnouncementService(SysAnnouncementMapper announcementMapper,
                                       PlatformAuditService auditService) {
        this.announcementMapper = announcementMapper;
        this.auditService = auditService;
    }

    /** 运营侧：全部公告（倒序）。 */
    public List<AnnouncementVO> list() {
        return announcementMapper.selectList(Wrappers.<SysAnnouncement>lambdaQuery()
                        .orderByDesc(SysAnnouncement::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AnnouncementSaveDTO dto) {
        SysAnnouncement a = new SysAnnouncement();
        apply(a, dto);
        announcementMapper.insert(a);
        auditService.record(PlatformAuditActions.ANNOUNCEMENT_SAVED,
                PlatformAuditActions.TARGET_ANNOUNCEMENT, a.getId(), Map.of("op", "create", "title", dto.title()));
        return a.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AnnouncementSaveDTO dto) {
        SysAnnouncement a = requireExists(id);
        apply(a, dto);
        announcementMapper.updateById(a);
        auditService.record(PlatformAuditActions.ANNOUNCEMENT_SAVED,
                PlatformAuditActions.TARGET_ANNOUNCEMENT, id, Map.of("op", "update", "title", dto.title()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        announcementMapper.deleteById(id);
        auditService.record(PlatformAuditActions.ANNOUNCEMENT_SAVED,
                PlatformAuditActions.TARGET_ANNOUNCEMENT, id, Map.of("op", "delete"));
    }

    /** 租户侧：当前生效公告（已发布、publish_at<=now、expire_at 空或>now），倒序。 */
    public List<AnnouncementVO> activeAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        return announcementMapper.selectList(Wrappers.<SysAnnouncement>lambdaQuery()
                        .eq(SysAnnouncement::getStatus, STATUS_PUBLISHED)
                        .and(w -> w.isNull(SysAnnouncement::getPublishAt).or().le(SysAnnouncement::getPublishAt, now))
                        .and(w -> w.isNull(SysAnnouncement::getExpireAt).or().gt(SysAnnouncement::getExpireAt, now))
                        .orderByDesc(SysAnnouncement::getPublishAt)
                        .orderByDesc(SysAnnouncement::getId))
                .stream().map(this::toVO).toList();
    }

    private void apply(SysAnnouncement a, AnnouncementSaveDTO dto) {
        a.setTitle(dto.title());
        a.setContent(dto.content());
        a.setLevel(StringUtils.hasText(dto.level()) ? dto.level() : "info");
        a.setStatus(StringUtils.hasText(dto.status()) ? dto.status() : "draft");
        // 发布且未指定发布时间则取当前时间
        a.setPublishAt(dto.publishAt() == null && STATUS_PUBLISHED.equals(a.getStatus())
                ? LocalDateTime.now() : dto.publishAt());
        a.setExpireAt(dto.expireAt());
    }

    private SysAnnouncement requireExists(Long id) {
        SysAnnouncement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return a;
    }

    private AnnouncementVO toVO(SysAnnouncement a) {
        return new AnnouncementVO(a.getId(), a.getTitle(), a.getContent(), a.getLevel(), a.getStatus(),
                a.getPublishAt(), a.getExpireAt(), a.getCreateTime());
    }
}
