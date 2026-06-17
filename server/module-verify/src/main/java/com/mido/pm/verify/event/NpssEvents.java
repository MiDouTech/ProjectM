package com.mido.pm.verify.event;

/** NPSS 验收域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class NpssEvents {

    public static final String REVIEW_STARTED = "npss.review.started";
    public static final String SCORED = "npss.scored";
    public static final String REVIEW_COMPLETED = "npss.review.completed";

    private NpssEvents() {
    }
}
