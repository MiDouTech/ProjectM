package com.mido.pm.doc.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.doc.dto.DocCreateDTO;
import com.mido.pm.doc.dto.DocDetailVO;
import com.mido.pm.doc.dto.DocListVO;
import com.mido.pm.doc.dto.DocMoveDTO;
import com.mido.pm.doc.dto.DocNodeVO;
import com.mido.pm.doc.dto.DocRenameDTO;
import com.mido.pm.doc.dto.DocAclGrantDTO;
import com.mido.pm.doc.dto.DocAclVO;
import com.mido.pm.doc.dto.DocSaveDTO;
import com.mido.pm.doc.dto.DocSearchVO;
import com.mido.pm.doc.dto.DocShareVO;
import com.mido.pm.doc.dto.DocTemplateVO;
import com.mido.pm.doc.dto.DocTrashVO;
import com.mido.pm.doc.dto.DocVersionVO;
import com.mido.pm.doc.service.DocAclService;
import com.mido.pm.doc.service.DocService;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目知识库文档：目录树、节点 CRUD、正文保存（产生版本）、版本列表/回滚。
 * 正文走 Tiptap JSON；oss 二进制仍走 /attachments（P1 接入）。
 */
@RestController
@RequestMapping("/api/v1/docs")
public class DocController {

    private final DocService docService;
    private final DocAclService aclService;

    public DocController(DocService docService, DocAclService aclService) {
        this.docService = docService;
        this.aclService = aclService;
    }

    /** 项目知识库目录树。 */
    @GetMapping("/tree")
    public R<List<DocNodeVO>> tree(@RequestParam Long projectId) {
        return R.ok(docService.tree(projectId));
    }

    /**
     * 全局文档列表（文档中心首页「全部文档」）：跨 projectIds（前端传我参与的项目）扁平列出
     * 知识库文档（source=doc）+ 项目文件（source=attachment，项目/任务/费用附件）。
     * type 选填（doc/file/folder，附件视为 file）；keyword 选填（标题/文件名包含）。文档经 ACL 过滤。
     */
    @GetMapping("/list")
    public R<List<DocListVO>> list(@RequestParam(required = false) List<Long> projectIds,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String keyword) {
        return R.ok(docService.listAcrossProjects(projectIds, type, keyword));
    }

    /** 文档详情（含当前版本正文）。 */
    @GetMapping("/{id}")
    public R<DocDetailVO> get(@PathVariable Long id) {
        return R.ok(docService.get(id));
    }

    /** 新建目录/文档节点。 */
    @PostMapping
    public R<Long> create(@Valid @RequestBody DocCreateDTO dto) {
        return R.ok(docService.create(dto));
    }

    /** 重命名/改图标。 */
    @PutMapping("/{id}/rename")
    public R<Void> rename(@PathVariable Long id, @Valid @RequestBody DocRenameDTO dto) {
        docService.rename(id, dto);
        return R.ok();
    }

    /** 移动节点（改父/排序）。 */
    @PutMapping("/{id}/move")
    public R<Void> move(@PathVariable Long id, @RequestBody DocMoveDTO dto) {
        docService.move(id, dto);
        return R.ok();
    }

    /** 删除节点：移入回收站（连同子树）。 */
    @DeleteMapping("/{id}")
    public R<Void> trash(@PathVariable Long id) {
        docService.trash(id);
        return R.ok();
    }

    /** 上传文件到目录，建 file 节点。 */
    @PostMapping("/upload")
    public R<Long> upload(@RequestParam Long projectId, @RequestParam(required = false) Long parentId,
                          @RequestParam("file") MultipartFile file) {
        return R.ok(docService.uploadFile(projectId, parentId, file));
    }

    /** file 节点限时下载/预览 URL。 */
    @GetMapping("/{id}/download-url")
    public R<String> downloadUrl(@PathVariable Long id) {
        return R.ok(docService.downloadUrl(id));
    }

    /** 回收站列表。 */
    @GetMapping("/recycle")
    public R<List<DocTrashVO>> recycleBin(@RequestParam Long projectId) {
        return R.ok(docService.recycleBin(projectId));
    }

    /** 从回收站恢复（连同子树）。 */
    @PostMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        docService.restore(id);
        return R.ok();
    }

    /** 彻底删除（不可恢复）。 */
    @DeleteMapping("/{id}/purge")
    public R<Void> purge(@PathVariable Long id) {
        docService.purge(id);
        return R.ok();
    }

    /** 保存正文：生成新版本。 */
    @PutMapping("/{id}/content")
    public R<DocVersionVO> saveContent(@PathVariable Long id, @RequestBody DocSaveDTO dto) {
        return R.ok(docService.saveContent(id, dto));
    }

    /** 版本列表（不含正文）。 */
    @GetMapping("/{id}/versions")
    public R<List<DocVersionVO>> versions(@PathVariable Long id) {
        return R.ok(docService.versions(id));
    }

    /** 单个版本正文。 */
    @GetMapping("/versions/{versionId}")
    public R<DocVersionVO> versionContent(@PathVariable Long versionId) {
        return R.ok(docService.versionContent(versionId));
    }

    /** 回滚到指定版本（追加为新版本）。 */
    @PostMapping("/{id}/rollback/{versionId}")
    public R<DocVersionVO> rollback(@PathVariable Long id, @PathVariable Long versionId) {
        return R.ok(docService.rollback(id, versionId));
    }

    /** 项目内全文搜索（标题 + 正文）。 */
    @GetMapping("/search")
    public R<List<DocSearchVO>> search(@RequestParam Long projectId, @RequestParam String keyword) {
        return R.ok(docService.search(projectId, keyword));
    }

    /** 切换收藏，返回切换后状态。 */
    @PostMapping("/{id}/favorite")
    public R<Boolean> toggleFavorite(@PathVariable Long id) {
        return R.ok(docService.toggleFavorite(id));
    }

    /** 我在该项目的收藏。 */
    @GetMapping("/favorites")
    public R<List<DocSearchVO>> favorites(@RequestParam Long projectId) {
        return R.ok(docService.favorites(projectId));
    }

    /** 文档模板库。 */
    @GetMapping("/templates")
    public R<List<DocTemplateVO>> templates() {
        return R.ok(docService.templates());
    }

    // ===== 权限 ACL（需 admin）=====

    @GetMapping("/{id}/acl")
    public R<List<DocAclVO>> listAcl(@PathVariable Long id) {
        return R.ok(aclService.listAcl(id));
    }

    @PostMapping("/{id}/acl")
    public R<Long> grant(@PathVariable Long id, @Valid @RequestBody DocAclGrantDTO dto) {
        return R.ok(aclService.grant(id, dto));
    }

    @DeleteMapping("/acl/{aclId}")
    public R<Void> revoke(@PathVariable Long aclId) {
        aclService.revoke(aclId);
        return R.ok();
    }

    // ===== 公开分享（需 admin 管理）=====

    /** 创建/复用分享外链。expireTime 可空=永久。 */
    @PostMapping("/{id}/share")
    public R<DocShareVO> createShare(@PathVariable Long id,
                                     @RequestParam(required = false)
                                     @org.springframework.format.annotation.DateTimeFormat(iso =
                                             org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                                     LocalDateTime expireTime) {
        return R.ok(aclService.createShare(id, expireTime));
    }

    /** 查看当前分享（无则 data=null）。 */
    @GetMapping("/{id}/share")
    public R<DocShareVO> getShare(@PathVariable Long id) {
        return R.ok(aclService.getShare(id));
    }

    /** 停用分享。 */
    @DeleteMapping("/{id}/share")
    public R<Void> disableShare(@PathVariable Long id) {
        aclService.disableShare(id);
        return R.ok();
    }
}
