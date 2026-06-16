package com.mido.pm.collab.controller;

import com.mido.pm.collab.dto.NotificationVO;
import com.mido.pm.collab.service.NotificationService;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 站内信：列表 / 未读数 / 标记已读（当前登录用户）。 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public R<PageResult<NotificationVO>> list(@RequestParam(required = false) Long page,
                                              @RequestParam(required = false) Long size,
                                              @RequestParam(required = false) Boolean unread) {
        return R.ok(notificationService.page(page, size, unread));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead();
        return R.ok();
    }
}
