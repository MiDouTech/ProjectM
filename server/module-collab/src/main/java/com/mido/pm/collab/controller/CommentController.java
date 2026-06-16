package com.mido.pm.collab.controller;

import com.mido.pm.collab.dto.CommentCreateDTO;
import com.mido.pm.collab.dto.CommentVO;
import com.mido.pm.collab.service.CommentService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 评论：发表 + 列表（按对象）。 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody CommentCreateDTO dto) {
        return R.ok(commentService.create(dto));
    }

    @GetMapping
    public R<List<CommentVO>> list(@RequestParam String entityType, @RequestParam Long entityId) {
        return R.ok(commentService.list(entityType, entityId));
    }
}
