-- V24 文档模块 P1：附件并入目录树(file 节点) + 回收站。
--   attachment_id: type=file 节点指向 pm_attachment；正文文档(doc)/目录(folder)为空。
--   trashed/trashed_time: 软回收(回收站)，区别于 is_deleted(彻底删除)。tree 仅取 trashed=0。

ALTER TABLE pm_doc
  ADD COLUMN attachment_id BIGINT  COMMENT '附件 id(type=file 时指向 pm_attachment)' AFTER current_version_id,
  ADD COLUMN trashed       TINYINT NOT NULL DEFAULT 0 COMMENT '回收站: 0 正常 / 1 已移入回收站' AFTER attachment_id,
  ADD COLUMN trashed_time  DATETIME COMMENT '移入回收站时间' AFTER trashed;
