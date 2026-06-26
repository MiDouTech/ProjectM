#!/usr/bin/env bash
# 加载演示数据（demo-seed）到本地 MySQL —— 仅用于团队演示 / 联调，勿在生产执行。
# 用法：bash scripts/load-demo.sh
# 前置：docker compose up -d 起好 mido-mysql，且后端已跑过 Flyway 迁移（表已建好）。
# 幂等：可反复执行，演示数据按 id 段 [9000000,9999999] 先清后插。
set -euo pipefail

SQL_FILE="$(cd "$(dirname "$0")/.." && pwd)/server/bootstrap/src/main/resources/db/demo/demo-seed.sql"
CONTAINER="${MYSQL_CONTAINER:-mido-mysql}"
DB="${MYSQL_DATABASE:-mido_pm}"
USER="${MYSQL_USER:-root}"
PASS="${MYSQL_PASSWORD:-root}"

[ -f "$SQL_FILE" ] || { echo "未找到 SQL 文件: $SQL_FILE" >&2; exit 1; }

echo "→ 正在向容器 $CONTAINER 的数据库 $DB 写入演示数据…"
docker exec -i "$CONTAINER" mysql -u"$USER" -p"$PASS" "$DB" < "$SQL_FILE"
echo "✓ 演示数据加载完成。登录 admin/admin123（演示员工密码统一 admin123）。"
