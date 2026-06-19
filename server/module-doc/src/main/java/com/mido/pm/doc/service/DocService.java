package com.mido.pm.doc.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.doc.dto.AttachmentVO;
import com.mido.pm.doc.dto.DocCreateDTO;
import com.mido.pm.doc.dto.DocDetailVO;
import com.mido.pm.doc.dto.DocMoveDTO;
import com.mido.pm.doc.dto.DocNodeVO;
import com.mido.pm.doc.dto.DocRenameDTO;
import com.mido.pm.doc.dto.DocSaveDTO;
import com.mido.pm.doc.dto.DocTrashVO;
import com.mido.pm.doc.dto.DocVersionVO;
import com.mido.pm.doc.entity.PmDoc;
import org.springframework.web.multipart.MultipartFile;
import com.mido.pm.doc.entity.PmDocVersion;
import com.mido.pm.doc.event.DocEvents;
import com.mido.pm.doc.mapper.PmDocMapper;
import com.mido.pm.doc.mapper.PmDocVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目知识库文档服务：目录树、节点 CRUD、正文保存（产生版本）、版本列表/回滚。
 * 写操作同事务发领域事件（doc.*，取自 docs/domain-events.md §5.1）。
 */
@Service
public class DocService {

    private static final long ROOT = 0L;

    private final PmDocMapper docMapper;
    private final PmDocVersionMapper versionMapper;
    private final DomainEventPublisher eventPublisher;
    private final AttachmentService attachmentService;

    public DocService(PmDocMapper docMapper, PmDocVersionMapper versionMapper,
                      DomainEventPublisher eventPublisher, AttachmentService attachmentService) {
        this.docMapper = docMapper;
        this.versionMapper = versionMapper;
        this.eventPublisher = eventPublisher;
        this.attachmentService = attachmentService;
    }

    // ===== 目录树 =====

    /** 项目知识库目录树：扁平节点按 parentId 组装，同级按 sortNo→id 升序。 */
    public List<DocNodeVO> tree(Long projectId) {
        List<PmDoc> all = docMapper.selectList(Wrappers.<PmDoc>lambdaQuery()
                .eq(PmDoc::getProjectId, projectId)
                .eq(PmDoc::getTrashed, 0)
                .orderByAsc(PmDoc::getSortNo).orderByAsc(PmDoc::getId));
        // 按 parentId 分桶（节点已按 sortNo→id 入桶），再从根递归组装
        Map<Long, List<DocNodeVO>> childrenOf = new LinkedHashMap<>();
        for (PmDoc d : all) {
            childrenOf.computeIfAbsent(parentOf(d), k -> new ArrayList<>()).add(toNode(d));
        }
        return buildChildren(childrenOf, ROOT);
    }

    private List<DocNodeVO> buildChildren(Map<Long, List<DocNodeVO>> childrenOf, Long parentId) {
        List<DocNodeVO> kids = childrenOf.getOrDefault(parentId, List.of());
        List<DocNodeVO> result = new ArrayList<>(kids.size());
        for (DocNodeVO k : kids) {
            result.add(new DocNodeVO(k.id(), k.parentId(), k.type(), k.title(), k.icon(), k.sortNo(),
                    k.updateBy(), k.updateTime(), buildChildren(childrenOf, k.id())));
        }
        return result;
    }

    // ===== 详情 =====

    public DocDetailVO get(Long id) {
        PmDoc d = requireDoc(id);
        PmDocVersion ver = d.getCurrentVersionId() == null ? null : versionMapper.selectById(d.getCurrentVersionId());
        return new DocDetailVO(d.getId(), d.getProjectId(), d.getParentId(), d.getType(), d.getTitle(), d.getIcon(),
                d.getCurrentVersionId(), ver == null ? null : ver.getVersionNo(),
                ver == null ? null : ver.getContent(), d.getUpdateBy(), d.getUpdateTime());
    }

    // ===== 节点 CRUD =====

    @Transactional(rollbackFor = Exception.class)
    public Long create(DocCreateDTO dto) {
        if (!PmDoc.TYPE_FOLDER.equals(dto.type()) && !PmDoc.TYPE_DOC.equals(dto.type())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法节点类型: " + dto.type());
        }
        PmDoc d = new PmDoc();
        d.setProjectId(dto.projectId());
        d.setParentId(dto.parentId() == null ? ROOT : dto.parentId());
        d.setType(dto.type());
        d.setTitle(dto.title().trim());
        d.setIcon(dto.icon());
        d.setSortNo(nextSortNo(dto.projectId(), d.getParentId()));
        d.setCreateBy(currentUserId());
        d.setUpdateBy(currentUserId());
        docMapper.insert(d);
        eventPublisher.publish(DocEvents.CREATED, payload(d, "title", d.getTitle()));
        return d.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, DocRenameDTO dto) {
        PmDoc d = requireDoc(id);
        d.setTitle(dto.title().trim());
        d.setIcon(dto.icon());
        d.setUpdateBy(currentUserId());
        docMapper.updateById(d);
        eventPublisher.publish(DocEvents.UPDATED, payload(d, "title", d.getTitle()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, DocMoveDTO dto) {
        PmDoc d = requireDoc(id);
        Long target = dto.parentId() == null ? ROOT : dto.parentId();
        if (target.equals(d.getId()) || isDescendant(d.getProjectId(), target, d.getId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能移动到自身或子节点下");
        }
        d.setParentId(target);
        if (dto.sortNo() != null) {
            d.setSortNo(dto.sortNo());
        }
        d.setUpdateBy(currentUserId());
        docMapper.updateById(d);
        eventPublisher.publish(DocEvents.MOVED, payload(d, "parentId", target));
    }

    // ===== 文件节点（附件并入树）=====

    /** 上传文件到目录：落附件 + 建 file 节点（title=文件名，attachment_id 指向附件）。 */
    @Transactional(rollbackFor = Exception.class)
    public Long uploadFile(Long projectId, Long parentId, MultipartFile file) {
        AttachmentVO att = attachmentService.upload("doc", projectId, file);
        PmDoc d = new PmDoc();
        d.setProjectId(projectId);
        d.setParentId(parentId == null ? ROOT : parentId);
        d.setType(PmDoc.TYPE_FILE);
        d.setTitle(att.name());
        d.setAttachmentId(att.id());
        d.setSortNo(nextSortNo(projectId, d.getParentId()));
        d.setCreateBy(currentUserId());
        d.setUpdateBy(currentUserId());
        docMapper.insert(d);
        eventPublisher.publish(DocEvents.CREATED, payload(d, "title", d.getTitle()));
        return d.getId();
    }

    /** file 节点的限时下载/预览 URL（经附件预签名，不外泄 oss_key）。 */
    public String downloadUrl(Long id) {
        PmDoc d = requireDoc(id);
        if (!PmDoc.TYPE_FILE.equals(d.getType()) || d.getAttachmentId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该节点不是文件");
        }
        return attachmentService.downloadUrl(d.getAttachmentId());
    }

    // ===== 回收站（软回收 trashed，区别于彻底删除 is_deleted）=====

    /** 移入回收站：连同子树 trashed=1。 */
    @Transactional(rollbackFor = Exception.class)
    public void trash(Long id) {
        PmDoc d = requireDoc(id);
        LocalDateTime now = LocalDateTime.now();
        for (Long nid : subtreeIds(d.getProjectId(), id)) {
            PmDoc n = docMapper.selectById(nid);
            if (n != null && Integer.valueOf(0).equals(n.getTrashed())) {
                n.setTrashed(1);
                n.setTrashedTime(now);
                n.setUpdateBy(currentUserId());
                docMapper.updateById(n);
            }
        }
        eventPublisher.publish(DocEvents.DELETED, payload(d, "title", d.getTitle()));
    }

    /** 从回收站恢复：连同子树 trashed=0。 */
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        PmDoc d = requireDoc(id);
        for (Long nid : subtreeIds(d.getProjectId(), id)) {
            PmDoc n = docMapper.selectById(nid);
            if (n != null && Integer.valueOf(1).equals(n.getTrashed())) {
                n.setTrashed(0);
                n.setTrashedTime(null);
                n.setUpdateBy(currentUserId());
                docMapper.updateById(n);
            }
        }
        eventPublisher.publish(DocEvents.UPDATED, payload(d, "title", d.getTitle()));
    }

    /** 彻底删除：连同子树逻辑删除（is_deleted），不可恢复。 */
    @Transactional(rollbackFor = Exception.class)
    public void purge(Long id) {
        PmDoc d = requireDoc(id);
        for (Long nid : subtreeIds(d.getProjectId(), id)) {
            docMapper.deleteById(nid);
        }
        eventPublisher.publish(DocEvents.DELETED, payload(d, "title", d.getTitle()));
    }

    /** 回收站列表（当前项目已回收、未彻底删除），按回收时间倒序。 */
    public List<DocTrashVO> recycleBin(Long projectId) {
        return docMapper.selectList(Wrappers.<PmDoc>lambdaQuery()
                        .eq(PmDoc::getProjectId, projectId).eq(PmDoc::getTrashed, 1)
                        .orderByDesc(PmDoc::getTrashedTime))
                .stream()
                .map(d -> new DocTrashVO(d.getId(), d.getType(), d.getTitle(), d.getTrashedTime()))
                .toList();
    }

    // ===== 正文与版本 =====

    /** 保存正文：仅 type=doc；追加新版本并指向之。 */
    @Transactional(rollbackFor = Exception.class)
    public DocVersionVO saveContent(Long id, DocSaveDTO dto) {
        PmDoc d = requireDoc(id);
        if (!PmDoc.TYPE_DOC.equals(d.getType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "目录节点不能保存正文");
        }
        String title = dto.title() != null && !dto.title().isBlank() ? dto.title().trim() : d.getTitle();
        PmDocVersion ver = newVersion(d, title, dto.content(), dto.contentText(), dto.changeNote());
        d.setTitle(title);
        d.setCurrentVersionId(ver.getId());
        d.setUpdateBy(currentUserId());
        docMapper.updateById(d);
        eventPublisher.publish(DocEvents.VERSION_CREATED, payload(d, "versionNo", ver.getVersionNo()));
        return toVersionVO(ver, true);
    }

    /** 版本列表（不含正文，按版本号倒序）。 */
    public List<DocVersionVO> versions(Long docId) {
        requireDoc(docId);
        return versionMapper.selectList(Wrappers.<PmDocVersion>lambdaQuery()
                        .eq(PmDocVersion::getDocId, docId)
                        .orderByDesc(PmDocVersion::getVersionNo))
                .stream().map(v -> toVersionVO(v, false)).toList();
    }

    /** 单个版本正文。 */
    public DocVersionVO versionContent(Long versionId) {
        PmDocVersion v = versionMapper.selectById(versionId);
        if (v == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        return toVersionVO(v, true);
    }

    /** 回滚：以指定历史版本正文追加为新版本（不抹历史）。 */
    @Transactional(rollbackFor = Exception.class)
    public DocVersionVO rollback(Long id, Long versionId) {
        PmDoc d = requireDoc(id);
        PmDocVersion src = versionMapper.selectById(versionId);
        if (src == null || !src.getDocId().equals(id)) {
            throw new BizException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        PmDocVersion ver = newVersion(d, src.getTitle(), src.getContent(), src.getContentText(),
                "回滚至 v" + src.getVersionNo());
        d.setTitle(src.getTitle());
        d.setCurrentVersionId(ver.getId());
        d.setUpdateBy(currentUserId());
        docMapper.updateById(d);
        eventPublisher.publish(DocEvents.VERSION_CREATED, payload(d, "versionNo", ver.getVersionNo()));
        return toVersionVO(ver, true);
    }

    // ===== 内部 =====

    private PmDocVersion newVersion(PmDoc d, String title, String content, String contentText, String changeNote) {
        Integer max = versionMapper.selectList(Wrappers.<PmDocVersion>lambdaQuery()
                        .eq(PmDocVersion::getDocId, d.getId())
                        .orderByDesc(PmDocVersion::getVersionNo).last("limit 1"))
                .stream().map(PmDocVersion::getVersionNo).findFirst().orElse(0);
        PmDocVersion ver = new PmDocVersion();
        ver.setDocId(d.getId());
        ver.setVersionNo(max + 1);
        ver.setTitle(title);
        ver.setContent(content);
        ver.setContentText(contentText);
        ver.setChangeNote(changeNote);
        ver.setCreateBy(currentUserId());
        ver.setUpdateBy(currentUserId());
        versionMapper.insert(ver);
        return ver;
    }

    private int nextSortNo(Long projectId, Long parentId) {
        Integer max = docMapper.selectList(Wrappers.<PmDoc>lambdaQuery()
                        .eq(PmDoc::getProjectId, projectId).eq(PmDoc::getParentId, parentId)
                        .orderByDesc(PmDoc::getSortNo).last("limit 1"))
                .stream().map(PmDoc::getSortNo).findFirst().orElse(0);
        return max + 1;
    }

    /** 收集 root 节点（含自身）的所有后代 id。 */
    private List<Long> subtreeIds(Long projectId, Long rootId) {
        List<PmDoc> all = docMapper.selectList(Wrappers.<PmDoc>lambdaQuery().eq(PmDoc::getProjectId, projectId));
        Map<Long, List<Long>> childIds = new LinkedHashMap<>();
        for (PmDoc d : all) {
            childIds.computeIfAbsent(parentOf(d), k -> new ArrayList<>()).add(d.getId());
        }
        List<Long> acc = new ArrayList<>();
        collect(childIds, rootId, acc);
        acc.add(rootId);
        return acc;
    }

    private void collect(Map<Long, List<Long>> childIds, Long node, List<Long> acc) {
        for (Long c : childIds.getOrDefault(node, List.of())) {
            acc.add(c);
            collect(childIds, c, acc);
        }
    }

    private boolean isDescendant(Long projectId, Long candidate, Long rootId) {
        return subtreeIds(projectId, rootId).contains(candidate);
    }

    private PmDoc requireDoc(Long id) {
        PmDoc d = docMapper.selectById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        return d;
    }

    private Long parentOf(PmDoc d) {
        return d.getParentId() == null ? ROOT : d.getParentId();
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    private DocNodeVO toNode(PmDoc d) {
        return new DocNodeVO(d.getId(), parentOf(d), d.getType(), d.getTitle(), d.getIcon(), d.getSortNo(),
                d.getUpdateBy(), d.getUpdateTime(), new ArrayList<>());
    }

    private DocVersionVO toVersionVO(PmDocVersion v, boolean withContent) {
        return new DocVersionVO(v.getId(), v.getDocId(), v.getVersionNo(), v.getTitle(),
                withContent ? v.getContent() : null, v.getChangeNote(), v.getCreateBy(), v.getCreateTime());
    }

    private Map<String, Object> payload(PmDoc d, Object... extra) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("docId", d.getId());
        map.put("projectId", d.getProjectId());
        map.put("type", d.getType());
        for (int i = 0; i + 1 < extra.length; i += 2) {
            map.put(String.valueOf(extra[i]), extra[i + 1]);
        }
        map.put("operatorId", currentUserId());
        map.put("occurredAt", LocalDateTime.now().toString());
        return map;
    }
}
