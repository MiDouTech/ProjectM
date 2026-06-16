package com.mido.pm.stakeholder.domain;

import java.math.BigDecimal;

/** 角色权重对（权重以百分比存，0-100）。 */
public record RoleWeight(String role, BigDecimal weight) {
}
