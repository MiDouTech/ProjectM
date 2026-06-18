# 静态资源（public）

此目录下的文件按原样部署，通过站点根路径访问（如 `web/public/login-bg.jpg` → `/login-bg.jpg`）。

## 登录页背景图

登录页品牌墙引用 `/login-bg.jpg`（蓝色星球）。请把图片放到本目录并命名为 `login-bg.jpg`：

```
web/public/login-bg.jpg
```

- 缺失时登录页会优雅回落到纯 CSS 渐变背景，不影响功能。
- 已在 `LoginView.vue` 中以半透明 + 左侧渐隐方式融入品牌墙（`opacity: 0.5` + mask 渐变），不突兀。
