# Domain Docs

工程类 skill 在探索代码库时应如何消费本仓库的领域文档。布局：**单 context**。

## 探索前先读

- 根目录 **`CONTEXT.md`**（暂未建；由 `/domain-modeling` 在术语/决策实际产生时懒创建）。
- **`docs/adr/`** —— 与改动相关的架构决策记录（暂未建；同样懒创建）。
- **在 `CONTEXT.md` 建好前**，本仓库领域术语以下列既有事实源为准（不要忽略它们）：
  - `CLAUDE.md §6 命名与术语`（项目/任务/目标/干系人/验收 NPSS/立项审批/工时/费用 等统一术语，禁同义混用）
  - `docs/data-model.md`（完整 DDL，实体/字段事实源）
  - `docs/npss-rule.md`（NPSS 算分/权重/奖金硬校验）
  - `docs/domain-events.md`（领域事件清单）

若上述懒创建文件不存在，**静默继续**，不要提示缺失、不要主动建议预建。

## 文件结构（单 context）

```
/
├── CONTEXT.md            ← 懒创建
├── docs/
│   ├── adr/              ← 懒创建
│   ├── data-model.md     ← 现有：DDL 事实源
│   ├── npss-rule.md      ← 现有：NPSS 规则
│   └── domain-events.md  ← 现有：领域事件
└── server/ , web/
```

## 用术语表的词汇

输出命名领域概念（issue 标题、重构提案、假设、测试名）时，用 `CLAUDE.md §6` / `CONTEXT.md` 定义的术语，**不得漂移到被明确禁止的同义词**（如项目类型禁按 S/I/O 字符串硬编码，统一经 `ProjectTypeResolver`）。
若需要的概念尚未在术语表，是个信号：要么在造项目不用的语言（重新考虑），要么是真缺口（交给 `/domain-modeling`）。

## 标注 ADR 冲突

若输出与既有 ADR 矛盾，显式标注而非静默覆盖：

> _与 ADR-0007 冲突——但值得重开，因为…_
