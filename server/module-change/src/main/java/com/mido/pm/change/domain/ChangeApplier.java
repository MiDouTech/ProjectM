package com.mido.pm.change.domain;

import com.mido.pm.change.entity.PmChangeRequest;

/**
 * 变更应用端口（SPI）：变更通过后由被改业务域回写自身实体。变更域定义接口，业务域实现（如 goal 的
 * GoalChangeApplier），变更域不反向依赖业务域（无环）。与目标域 ProjectCompletionPort 同范式。
 */
public interface ChangeApplier {

    /** 是否处理该 biz_type（被改实体域，如 "goal"）。 */
    boolean supports(String bizType);

    /** 把变更单的 after_payload 应用到被改实体（在被改域内写表 + 发自身领域事件）。 */
    void apply(PmChangeRequest request);
}
