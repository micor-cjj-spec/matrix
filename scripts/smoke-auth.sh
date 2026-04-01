#!/usr/bin/env bash
set -euo pipefail

# 用法：
#   bash scripts/smoke-auth.sh <username> <password> [gateway_url]
# 示例：
#   bash scripts/smoke-auth.sh admin 123456 http://127.0.0.1:10000

USERNAME=${1:-}
PASSWORD=${2:-}
GATEWAY=${3:-http://127.0.0.1:10000}

if [[ -z "$USERNAME" || -z "$PASSWORD" ]]; then
  echo "Usage: bash scripts/smoke-auth.sh <username> <password> [gateway_url]"
  exit 1
fi

echo "[1/4] 获取验证码..."
CAPTCHA_JSON=$(curl -sS "${GATEWAY}/api/auth/captcha")
CAPTCHA_KEY=$(echo "$CAPTCHA_JSON" | sed -n 's/.*"captchaKey":"\([^"]*\)".*/\1/p')

if [[ -z "$CAPTCHA_KEY" ]]; then
  echo "❌ 获取 captchaKey 失败：$CAPTCHA_JSON"
  exit 1
fi

echo "⚠️ 说明：脚本无法自动识别图片验证码内容。"
echo "请在浏览器登录页输入一次验证码后，再继续用此脚本做 token 校验。"

echo "[2/4] 尝试账号登录（此处验证码填空，预期可能失败）..."
LOGIN_JSON=$(curl -sS -X POST "${GATEWAY}/api/auth/login/account" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"captcha\":\"\",\"captchaKey\":\"${CAPTCHA_KEY}\"}")

echo "登录响应：$LOGIN_JSON"
TOKEN=$(echo "$LOGIN_JSON" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [[ -z "$TOKEN" ]]; then
  echo "❌ 未拿到 token（如果提示需要验证码，请用前端先走一次正确登录）"
  exit 2
fi

echo "[3/4] 调用 base-service 受保护接口..."
BASE_RESP=$(curl -sS "${GATEWAY}/api/user/list?page=1&size=1" \
  -H "Authorization: Bearer ${TOKEN}")
echo "base-service 响应：$BASE_RESP"

echo "[4/4] 健康检查..."
AUTH_HEALTH=$(curl -sS "${GATEWAY}/api/actuator/health" || true)
echo "gateway health: $AUTH_HEALTH"

echo "✅ smoke test 执行完成"
