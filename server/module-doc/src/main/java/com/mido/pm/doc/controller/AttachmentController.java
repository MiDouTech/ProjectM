package com.mido.pm.doc.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.doc.dto.AttachmentRegisterDTO;
import com.mido.pm.doc.dto.AttachmentVO;
import com.mido.pm.doc.dto.UploadTicketVO;
import com.mido.pm.doc.service.AttachmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件：上传（后端代理）/列表/预签名下载 URL/逻辑删除。通用挂载 entityType+entityId（如 task）。
 * oss_key 不出现在任何响应中；下载需先取 /{id}/download-url 拿限时 URL。
 */
@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public R<AttachmentVO> upload(@RequestParam String entityType, @RequestParam Long entityId,
                                  @RequestParam("file") MultipartFile file) {
        return R.ok(attachmentService.upload(entityType, entityId, file));
    }

    /** 上传登记（预签名直传）：返回 attachmentId + 限时预签名 PUT URL，客户端据此直传对象。 */
    @PostMapping("/register")
    public R<UploadTicketVO> register(@Valid @RequestBody AttachmentRegisterDTO dto) {
        return R.ok(attachmentService.register(dto));
    }

    @GetMapping
    public R<List<AttachmentVO>> list(@RequestParam String entityType, @RequestParam Long entityId) {
        return R.ok(attachmentService.list(entityType, entityId));
    }

    /** 项目文件：汇总项目级文档 + 项目下任务/费用附件。 */
    @GetMapping("/by-project")
    public R<List<AttachmentVO>> listByProject(@RequestParam Long projectId) {
        return R.ok(attachmentService.listByProject(projectId));
    }

    /** 取限时预签名下载 URL（不外泄 oss_key）。 */
    @GetMapping("/{id}/download-url")
    public R<String> downloadUrl(@PathVariable Long id) {
        return R.ok(attachmentService.downloadUrl(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return R.ok();
    }
}
