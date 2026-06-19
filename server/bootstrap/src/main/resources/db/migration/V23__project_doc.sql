-- V23 项目知识库：在线文档 + 版本历史（文档域 doc.*）。
--   pm_doc        文档目录树：type=folder/doc，parent_id 构成层级，归属 project 知识库。
--   pm_doc_version 版本历史：每次保存内容追加一版，doc.current_version_id 指向最新版，支持回滚。
--   文档正文存 content(LONGTEXT, Tiptap JSON)；content_text 为纯文本，供摘要/后续全文检索。

CREATE TABLE pm_doc (
  id                 BIGINT       NOT NULL PRIMARY KEY,
  tenant_id          BIGINT       NOT NULL,
  project_id         BIGINT       NOT NULL COMMENT '归属项目(知识库空间)',
  parent_id          BIGINT       NOT NULL DEFAULT 0 COMMENT '父节点 id, 0=根',
  type               VARCHAR(16)  NOT NULL COMMENT '节点类型: folder 目录 / doc 在线文档',
  title              VARCHAR(256) NOT NULL COMMENT '标题',
  icon               VARCHAR(32)  COMMENT '图标(可选)',
  sort_no            INT          NOT NULL DEFAULT 0 COMMENT '同级排序, 越小越前',
  current_version_id BIGINT       COMMENT '当前版本 id(type=doc); 指向 pm_doc_version',
  create_by          BIGINT,
  create_time        DATETIME,
  update_by          BIGINT,
  update_time        DATETIME,
  is_deleted         TINYINT      NOT NULL DEFAULT 0,
  KEY idx_project_parent (project_id, parent_id)
) COMMENT='项目知识库文档目录树';

CREATE TABLE pm_doc_version (
  id           BIGINT       NOT NULL PRIMARY KEY,
  tenant_id    BIGINT       NOT NULL,
  doc_id       BIGINT       NOT NULL COMMENT '所属文档(pm_doc.id)',
  version_no   INT          NOT NULL COMMENT '版本号, 文档内自增 1,2,3...',
  title        VARCHAR(256) COMMENT '该版本标题快照',
  content      LONGTEXT     COMMENT '正文(Tiptap JSON)',
  content_text MEDIUMTEXT   COMMENT '正文纯文本(摘要/全文检索用)',
  change_note  VARCHAR(256) COMMENT '变更说明(可选)',
  create_by    BIGINT,
  create_time  DATETIME,
  update_by    BIGINT,
  update_time  DATETIME,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  KEY idx_doc (doc_id)
) COMMENT='文档版本历史(append-only)';
