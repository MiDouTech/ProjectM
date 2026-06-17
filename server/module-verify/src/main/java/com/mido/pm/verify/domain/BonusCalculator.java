package com.mido.pm.verify.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * 奖金硬校验（npss-rule §7，纯函数）。
 * 奖金 = 基数 × 价值系数 × (满意度/100)。硬规则：
 * ① 满意度 &lt; 60 → 直接归零（所有类型）；
 * ② O·定向整改、O·专项督办 → 无奖金（返回 0，不报错）。
 */
public final class BonusCalculator {

    /** 归零线（npss-rule §7 硬规则 1） */
    private static final BigDecimal ZERO_LINE = BigDecimal.valueOf(60);
    /** 无奖金的运营级子类（npss-rule §7 硬规则 2） */
    private static final Set<String> NO_BONUS_O_SUBCATEGORIES = Set.of("定向整改", "专项督办");

    private BonusCalculator() {
    }

    /**
     * @param category    项目类型 S/I/O
     * @param subCategory 子类（O 细分：常规运营/定向整改/专项督办）
     * @param base        类型奖金基数
     * @param valueCoeff  项目价值系数（基准 1）
     * @param satisfaction 加权满意度 0-100
     */
    public static BigDecimal compute(String category, String subCategory,
                                     BigDecimal base, BigDecimal valueCoeff, BigDecimal satisfaction) {
        if ("O".equals(category) && NO_BONUS_O_SUBCATEGORIES.contains(subCategory)) {
            return zero();
        }
        if (satisfaction == null || satisfaction.compareTo(ZERO_LINE) < 0) {
            return zero();
        }
        BigDecimal b = base == null ? BigDecimal.ZERO : base;
        BigDecimal c = valueCoeff == null ? BigDecimal.ZERO : valueCoeff;
        return b.multiply(c)
                .multiply(satisfaction.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
