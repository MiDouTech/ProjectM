package com.mido.pm.view.dto;

import java.util.List;

/** 解析后的导航节点（WorkspaceShell 渲染用）。children 为三级子菜单（L2）。 */
public record NavNodeVO(String code, String name, String icon, String route, List<NavNodeVO> children) {
}
