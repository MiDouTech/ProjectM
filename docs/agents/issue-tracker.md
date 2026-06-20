# Issue tracker: GitHub

issue 与 PRD 记录在本仓库（`MiDouTech/ProjectM`）的 **GitHub Issues**。

## 环境适配（重要）

- **Claude Code web 会话（本环境）无 `gh` CLI**：所有 GitHub 操作改走 **GitHub MCP 工具**（`mcp__github__*`，例如 `issue_write`、`add_issue_comment`、`list_issues`、`issue_read`），且**仅限仓库 `midoutech/projectm`**。
- **本地/CI 环境**：可用下方 `gh` CLI 约定。
- 两套等价，按当前环境是否有 `gh` 自动选择。

## 约定（本地 `gh` CLI）

- **创建 issue**：`gh issue create --title "..." --body "..."`（多行 body 用 heredoc）。
- **读 issue**：`gh issue view <number> --comments`。
- **列 issue**：`gh issue list --state open --json number,title,body,labels,comments`，配合 `--label` / `--state` 过滤。
- **评论**：`gh issue comment <number> --body "..."`
- **加/去标签**：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **关闭**：`gh issue close <number> --comment "..."`

repo 由 `git remote -v` 推断（`gh` 在 clone 内自动识别）。

## Pull requests as a triage surface

**PRs as a request surface: no.** 本系统是米多内部业务产品（非开源收外部 PR），外部 PR 不进入 triage 队列。`/triage` 读取此标志。

## 当 skill 说"发布到 issue tracker"

创建一个 GitHub issue（web 会话用 `mcp__github__issue_write`，本地用 `gh issue create`）。

## 当 skill 说"获取相关工单"

读取对应 issue（web 会话用 `mcp__github__issue_read`，本地用 `gh issue view <number> --comments`）。
