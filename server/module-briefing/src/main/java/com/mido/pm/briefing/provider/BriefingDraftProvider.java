package com.mido.pm.briefing.provider;

import java.time.LocalDate;

/**
 * 简报草稿生成 port（智能层占位）：汇总用户在周期内的工作产出为可读文本。
 *
 * <p>当前为规则式本地实现（拉任务流水汇总）；待 AI 层启用后可替换为 LLM 实现，
 * 业务侧不变（遵循 CLAUDE.md §4「外部能力走 provider 接口、AI 默认不启用」）。</p>
 */
public interface BriefingDraftProvider {

    /**
     * 汇总 [from,to] 内某用户的工作产出。
     *
     * @return 可直接落入简报首字段的草稿文本
     */
    String summarizeWork(Long userId, LocalDate from, LocalDate to);
}
