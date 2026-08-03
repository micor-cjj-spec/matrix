#!/usr/bin/env bash
set -euo pipefail

# Apply the dev database scripts that are not managed by Flyway yet.
#
# Defaults are chosen for the current dev server. Override with env vars when
# running elsewhere:
#   MYSQL_CONTAINER=matrix-mysql MYSQL_DATABASE=matrix_open_api bash scripts/apply-dev-db-scripts.sh
#   MYSQL_USER=matrix MYSQL_PWD=... bash scripts/apply-dev-db-scripts.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-matrix-mysql}"
MYSQL_USER="${MYSQL_USER:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-}"

find_fi_pid() {
  for env_file in /proc/[0-9]*/environ; do
    local pid="${env_file#/proc/}"
    pid="${pid%/environ}"
    local cmdline=""
    cmdline="$(tr '\0' ' ' < "/proc/${pid}/cmdline" 2>/dev/null || true)"
    if [[ "${cmdline}" == java\ -jar\ *"/fi-service/target/fi-service-"* ]]; then
      printf '%s\n' "${pid}"
      return 0
    fi
  done
  return 1
}

read_proc_env_value() {
  local pid="$1"
  local key="$2"
  tr '\0' '\n' < "/proc/${pid}/environ" 2>/dev/null | sed -n "s/^${key}=//p" | head -n 1
}

database_from_jdbc_url() {
  local jdbc_url="$1"
  local without_scheme="${jdbc_url#*://}"
  local without_host="${without_scheme#*/}"
  printf '%s\n' "${without_host%%[?;]*}"
}

if [[ -z "${MYSQL_USER}" || -z "${MYSQL_DATABASE}" || -z "${MYSQL_PWD:-}" ]]; then
  if fi_pid="$(find_fi_pid)"; then
    MYSQL_USER="${MYSQL_USER:-$(read_proc_env_value "${fi_pid}" SPRING_DATASOURCE_USERNAME)}"
    MYSQL_PWD="${MYSQL_PWD:-$(read_proc_env_value "${fi_pid}" SPRING_DATASOURCE_PASSWORD)}"
    jdbc_url="$(read_proc_env_value "${fi_pid}" SPRING_DATASOURCE_URL)"
    if [[ -z "${MYSQL_DATABASE}" && -n "${jdbc_url}" ]]; then
      MYSQL_DATABASE="$(database_from_jdbc_url "${jdbc_url}")"
    fi
  fi
fi

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-matrix_open_api}"

if [[ -z "${MYSQL_PWD:-}" ]]; then
  echo "MYSQL_PWD is required when fi-service is not running or does not expose SPRING_DATASOURCE_PASSWORD." >&2
  exit 2
fi

run_sql() {
  local sql_path="$1"
  if [[ ! -f "${ROOT_DIR}/${sql_path}" ]]; then
    echo "missing sql file: ${sql_path}" >&2
    exit 3
  fi

  local tmp_path="/tmp/matrix-$(basename "${sql_path}")-$$.sql"
  docker cp "${ROOT_DIR}/${sql_path}" "${MYSQL_CONTAINER}:${tmp_path}"
  docker exec \
    -e MYSQL_PWD="${MYSQL_PWD}" \
    -e MYSQL_USER="${MYSQL_USER}" \
    -e MYSQL_DATABASE="${MYSQL_DATABASE}" \
    -e SQL_PATH="${tmp_path}" \
    "${MYSQL_CONTAINER}" \
    sh -c 'mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < "$SQL_PATH"'
  docker exec "${MYSQL_CONTAINER}" rm -f "${tmp_path}" >/dev/null
  echo "applied ${sql_path}"
}

scripts=(
  "fi-service/src/main/resources/sql/fi_workflow_expense_v1.sql"
  "fi-service/src/main/resources/sql/fi_workflow_expense_v2.sql"
  "sql/bizfi_ai_tool_audit_v1.sql"
  "sql/bizfi_ai_tool_audit_v2.sql"
  "sql/bizfi_ai_tool_audit_v3.sql"
)

for sql_path in "${scripts[@]}"; do
  run_sql "${sql_path}"
done

docker exec \
  -e MYSQL_PWD="${MYSQL_PWD}" \
  "${MYSQL_CONTAINER}" \
  mysql -u"${MYSQL_USER}" -N "${MYSQL_DATABASE}" -e \
  "SHOW TABLES LIKE 'fi_event_outbox'; SHOW TABLES LIKE 'bizfi_ai_audit_access_log'; SHOW TABLES LIKE 'bizfi_ai_tool_execution';"

echo "dev database scripts applied to ${MYSQL_DATABASE}."
