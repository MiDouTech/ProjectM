package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 项目集成员（pm_portfolio_member）。创建人默认即首个成员。 */
@TableName("pm_portfolio_member")
public class PmPortfolioMember extends BaseEntity {

    private Long portfolioId;
    private Long userId;

    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
