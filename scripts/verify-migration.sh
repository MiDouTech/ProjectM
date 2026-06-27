#!/usr/bin/env bash
# 从零迁移校验 —— 在一个全新空库上跑完全部 Flyway 迁移，验证「从零启动」可成功。
# 用法：bash scripts/verify-migration.sh
# 机制：起一个一次性 mysql:8 容器（空库），用 flyway 容器对 db/migration 执行 migrate；
#       任一脚本失败即非零退出并打印报错。结束自动清理容器。
# 与生产一致：baseline-on-migrate、utf8mb4；不依赖本机 MySQL，只需 Docker。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MIGRATION_DIR="$ROOT/server/bootstrap/src/main/resources/db/migration"
DB="mido_pm_verify"
NET="mido-verify-net"
MYSQL_CT="mido-verify-mysql"
MYSQL_IMG="${MYSQL_IMAGE:-mysql:8}"
FLYWAY_IMG="${FLYWAY_IMAGE:-flyway/flyway:10-alpine}"

[ -d "$MIGRATION_DIR" ] || { echo "未找到迁移目录: $MIGRATION_DIR" >&2; exit 1; }

cleanup() {
  docker rm -f "$MYSQL_CT" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "→ 准备一次性空库（$MYSQL_IMG）…"
docker network create "$NET" >/dev/null 2>&1 || true
docker rm -f "$MYSQL_CT" >/dev/null 2>&1 || true
docker run -d --name "$MYSQL_CT" --network "$NET" \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE="$DB" \
  "$MYSQL_IMG" --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci >/dev/null

echo "→ 等待 MySQL 就绪…"
for i in $(seq 1 40); do
  if docker exec "$MYSQL_CT" mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; then break; fi
  [ "$i" = 40 ] && { echo "MySQL 启动超时" >&2; exit 1; }
  sleep 3
done

echo "→ 从零执行全部 Flyway 迁移…"
docker run --rm --network "$NET" \
  -v "$MIGRATION_DIR":/flyway/sql:ro \
  "$FLYWAY_IMG" \
  -url="jdbc:mysql://$MYSQL_CT:3306/$DB?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8" \
  -user=root -password=root -baselineOnMigrate=true -connectRetries=30 \
  -locations=filesystem:/flyway/sql \
  migrate

echo "✓ 从零迁移校验通过：空库已成功跑完全部迁移。"
