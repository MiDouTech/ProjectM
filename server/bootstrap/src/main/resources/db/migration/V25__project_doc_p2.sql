-- V25 文档模块 P2：收藏 + 模板。（全文搜索复用现有表，无需建表）
--   pm_doc_favorite   用户对文档的收藏（user_id + doc_id 唯一）。
--   pm_doc_template   文档模板库（租户级，content 为 Tiptap JSON），供"从模板新建"。

CREATE TABLE pm_doc_favorite (
  id          BIGINT  NOT NULL PRIMARY KEY,
  tenant_id   BIGINT  NOT NULL,
  user_id     BIGINT  NOT NULL COMMENT '收藏人',
  doc_id      BIGINT  NOT NULL COMMENT '被收藏文档(pm_doc.id)',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT NOT NULL DEFAULT 0,
  KEY idx_user_doc (user_id, doc_id)
) COMMENT='文档收藏';

CREATE TABLE pm_doc_template (
  id          BIGINT       NOT NULL PRIMARY KEY,
  tenant_id   BIGINT       NOT NULL,
  name        VARCHAR(64)  NOT NULL COMMENT '模板名',
  content     LONGTEXT     COMMENT '模板正文(Tiptap JSON)',
  sort_no     INT          NOT NULL DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) COMMENT='文档模板库';

-- 内置模板种子（tenant 1）：会议纪要 / PRD / 项目复盘。content 为最简 Tiptap JSON 骨架。
INSERT INTO pm_doc_template (id, tenant_id, name, content, sort_no, is_deleted) VALUES
 (1, 1, '会议纪要',
  '{"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"会议纪要"}]},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"基本信息"}]},{"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"时间："}]}]},{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"参会人："}]}]}]},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"议题与结论"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"待办事项"}]},{"type":"paragraph"}]}',
  1, 0),
 (2, 1, 'PRD 需求文档',
  '{"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"PRD 需求文档"}]},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"背景与目标"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"功能需求"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"非功能需求"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"验收标准"}]},{"type":"paragraph"}]}',
  2, 0),
 (3, 1, '项目复盘',
  '{"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"项目复盘"}]},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"目标回顾"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"做得好的"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"待改进的"}]},{"type":"paragraph"},{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"行动项"}]},{"type":"paragraph"}]}',
  3, 0);
