#!/usr/bin/env bash
# 递增 versionCode 并设置 versionName，写入项目根 version.properties。
#
# 用法：
#   ./scripts/bump-version.sh 0.2.0
#   ./scripts/bump-version.sh        # 仅递增 versionCode，保留当前 versionName
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/version.properties"

if [[ ! -f "$PROPS" ]]; then
  echo "缺少 $PROPS" >&2
  exit 1
fi

get_prop() {
  local key="$1"
  grep -E "^${key}=" "$PROPS" | head -1 | cut -d= -f2- | tr -d '\r'
}

CURRENT_CODE="$(get_prop VERSION_CODE)"
CURRENT_NAME="$(get_prop VERSION_NAME)"
NEW_NAME="${1:-$CURRENT_NAME}"

if [[ -z "$CURRENT_CODE" || -z "$CURRENT_NAME" ]]; then
  echo "version.properties 需包含 VERSION_CODE 与 VERSION_NAME" >&2
  exit 1
fi

if ! [[ "$CURRENT_CODE" =~ ^[0-9]+$ ]]; then
  echo "VERSION_CODE 必须为非负整数，当前: $CURRENT_CODE" >&2
  exit 1
fi

NEW_CODE=$((CURRENT_CODE + 1))

if [[ -n "${1:-}" ]] && ! [[ "$NEW_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]*)?$ ]]; then
  echo "versionName 建议使用语义化版本，例如 0.2.0（收到: $NEW_NAME）" >&2
  exit 1
fi

cat >"$PROPS" <<EOF
# 应用版本单一来源：发版前更新此文件，或运行 scripts/bump-version.sh
# versionCode 必须单调递增；versionName 建议语义化版本（与 Git tag v* 一致）
VERSION_CODE=$NEW_CODE
VERSION_NAME=$NEW_NAME
EOF

echo "已更新 version.properties:"
echo "  VERSION_CODE=$NEW_CODE  (was $CURRENT_CODE)"
echo "  VERSION_NAME=$NEW_NAME  (was $CURRENT_NAME)"
echo ""
echo "下一步: git add version.properties && git commit"
echo "打 tag: git tag v$NEW_NAME && git push origin v$NEW_NAME"
