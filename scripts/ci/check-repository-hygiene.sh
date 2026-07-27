#!/usr/bin/env bash
set -euo pipefail

artifact_pattern='(^|/)(target|dist|node_modules|\.idea|\.vscode)/|\.(class|jar|war|log|tmp|bak|swp)$'
secret_file_pattern='(^|/)\.env($|\.)|(^|/)application-local\.ya?ml$|\.(pem|key|p12|jks)$|(^|/)id_rsa$'
report_file="${HYGIENE_REPORT_FILE:-hygiene-report.txt}"

tracked_artifacts="$(git ls-files | grep -E "${artifact_pattern}" || true)"
tracked_secret_files="$(git ls-files | grep -E "${secret_file_pattern}" || true)"

{
  echo '# Repository hygiene report'
  echo
  echo '## Tracked build, IDE, log, or temporary artifacts'
  if [[ -n "${tracked_artifacts}" ]]; then
    printf '%s\n' "${tracked_artifacts}"
  else
    echo '(none)'
  fi
  echo
  echo '## Tracked local or secret-bearing files'
  if [[ -n "${tracked_secret_files}" ]]; then
    printf '%s\n' "${tracked_secret_files}"
  else
    echo '(none)'
  fi
} > "${report_file}"

if [[ -n "${tracked_artifacts}" || -n "${tracked_secret_files}" ]]; then
  cat "${report_file}"
  echo
  echo 'Remove the listed files from Git tracking and keep them covered by .gitignore.'
  exit 1
fi

echo 'Repository hygiene check passed.'
