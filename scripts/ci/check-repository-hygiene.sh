#!/usr/bin/env bash
set -euo pipefail

artifact_pattern='(^|/)(target|dist|node_modules|\.idea|\.vscode)/|\.(class|jar|war|log|tmp|bak|swp)$'
secret_file_pattern='(^|/)\.env($|\.)|(^|/)application-local\.ya?ml$|\.(pem|key|p12|jks)$|(^|/)id_rsa$'

tracked_artifacts="$(git ls-files | grep -E "${artifact_pattern}" || true)"
tracked_secret_files="$(git ls-files | grep -E "${secret_file_pattern}" || true)"

failed=0

if [[ -n "${tracked_artifacts}" ]]; then
  echo 'Tracked build, IDE, log, or temporary artifacts are not allowed:'
  printf '%s\n' "${tracked_artifacts}"
  failed=1
fi

if [[ -n "${tracked_secret_files}" ]]; then
  echo 'Tracked local or secret-bearing files are not allowed:'
  printf '%s\n' "${tracked_secret_files}"
  failed=1
fi

if [[ "${failed}" -ne 0 ]]; then
  echo
  echo 'Remove the listed files from Git tracking and keep them covered by .gitignore.'
  exit 1
fi

echo 'Repository hygiene check passed.'
