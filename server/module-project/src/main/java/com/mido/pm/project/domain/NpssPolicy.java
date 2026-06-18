package com.mido.pm.project.domain;

/**
 * NPSS 适用策略：决定一个项目默认是否需要走「价值验收(NPSS)」两段式。
 *
 * <p>规则（产品决策）：仅 O·定向整改 / O·专项督办 默认<b>不</b>走 NPSS（它们本就无项目奖金，
 * 以铁三角结果验收闭环即可）；其余 S/I/O·常规 默认走 NPSS。创建项目时可显式覆盖。</p>
 *
 * <p>意义：把「是否做 NPSS」从生命周期硬编码中解耦——非 NPSS 项目结案即终止，
 * 不再被定时任务无条件唤醒价值验收。</p>
 */
public final class NpssPolicy {

    /** O 子类：定向整改 */
    private static final String SUB_RECTIFY = "定向整改";
    /** O 子类：专项督办 */
    private static final String SUB_SUPERVISE = "专项督办";

    private NpssPolicy() {
    }

    /**
     * 默认是否需要 NPSS 价值验收。
     *
     * @param category    项目类型 S/I/O
     * @param subCategory O 子类（常规运营/定向整改/专项督办），可空
     * @return true=默认走 NPSS；false=默认不走
     */
    public static boolean defaultRequiresNpss(String category, String subCategory) {
        if ("O".equals(category)
                && (SUB_RECTIFY.equals(subCategory) || SUB_SUPERVISE.equals(subCategory))) {
            return false;
        }
        return true;
    }
}
