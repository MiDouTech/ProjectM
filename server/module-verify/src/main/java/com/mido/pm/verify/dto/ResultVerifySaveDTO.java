package com.mido.pm.verify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 结果验收录入：PMO 结论 verdict（pass/fail）+ 三角达标项快照 + 完成率 + 备注。
 * 三项达标(onTime/inBudget/inScope)由前端按项目数据自动预填、PMO 可调整。
 */
public record ResultVerifySaveDTO(
        @NotBlank(message = "验收结论不能为空")
        @Pattern(regexp = "pass|fail", message = "验收结论只能为 pass/fail") String verdict,
        Boolean onTime,
        Boolean inBudget,
        Boolean inScope,
        BigDecimal completionRate,
        @Size(max = 1000, message = "备注不超过 1000 字") String remark) {
}
