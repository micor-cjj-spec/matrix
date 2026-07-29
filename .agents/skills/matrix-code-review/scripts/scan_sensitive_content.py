#!/usr/bin/env python3
"""Scan changed Matrix files for likely credentials and sensitive literals."""

from __future__ import annotations

import argparse
import ipaddress
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

SKIP_DIRS = {".git", "target", "node_modules", ".idea", "dist"}
MAX_BYTES = 2_000_000


@dataclass(frozen=True)
class Finding:
    severity: str
    path: Path
    line: int
    code: str
    message: str


def changed_files(root: Path) -> list[Path]:
    commands = [
        ["git", "diff", "--name-only", "--diff-filter=ACMR", "HEAD"],
        ["git", "ls-files", "--others", "--exclude-standard"],
    ]
    names: set[str] = set()
    for command in commands:
        try:
            output = subprocess.check_output(command, cwd=root, text=True, stderr=subprocess.DEVNULL)
        except (subprocess.CalledProcessError, FileNotFoundError):
            continue
        names.update(line.strip() for line in output.splitlines() if line.strip())
    return sorted(root / name for name in names)


def expand(root: Path, raw_paths: list[str]) -> list[Path]:
    inputs = [Path(item) for item in raw_paths]
    if not inputs:
        return changed_files(root)
    result: set[Path] = set()
    for item in inputs:
        path = item if item.is_absolute() else root / item
        if path.is_dir():
            for candidate in path.rglob("*"):
                if candidate.is_file() and not any(part in SKIP_DIRS for part in candidate.parts):
                    result.add(candidate)
        elif path.is_file():
            result.add(path)
    return sorted(result)


def is_probably_text(path: Path) -> bool:
    try:
        if path.stat().st_size > MAX_BYTES:
            return False
        sample = path.read_bytes()[:4096]
    except OSError:
        return False
    return b"\x00" not in sample


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def inspect(path: Path) -> list[Finding]:
    if not is_probably_text(path):
        return []
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return []

    findings: list[Finding] = []
    token_patterns = [
        ("SEC001", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"), "private key material detected"),
        ("SEC002", re.compile(r"\bAKIA[0-9A-Z]{16}\b"), "AWS access key detected"),
        ("SEC003", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{30,}\b"), "GitHub token detected"),
        ("SEC004", re.compile(r"\bAIza[0-9A-Za-z_-]{30,}\b"), "Google API key detected"),
        ("SEC005", re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"), "API secret token detected"),
        ("SEC006", re.compile(r"Authorization\s*[:=]\s*['\"]Bearer\s+[A-Za-z0-9._-]{12,}['\"]", re.I), "hard-coded bearer token detected"),
    ]
    for code, pattern, message in token_patterns:
        for match in pattern.finditer(text):
            findings.append(Finding("ERROR", path, line_number(text, match.start()), code, message))

    assignment = re.compile(
        r"(?im)^\s*(?:[\w.-]*)(password|passwd|secret|api[_-]?key|access[_-]?token|client[_-]?secret)\s*[:=]\s*['\"]?([^\s'\"#]{8,})"
    )
    safe_markers = ("${", "#{", "<", "changeme", "example", "placeholder", "your_", "env:", "localhost")
    for match in assignment.finditer(text):
        value = match.group(2).lower()
        if any(marker in value for marker in safe_markers):
            continue
        findings.append(
            Finding("ERROR", path, line_number(text, match.start()), "SEC007", f"hard-coded {match.group(1)} value detected")
        )

    for match in re.finditer(r"(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])", text):
        raw = match.group(0)
        try:
            address = ipaddress.ip_address(raw)
        except ValueError:
            continue
        if address.is_loopback or address.is_unspecified or address.is_private:
            continue
        findings.append(
            Finding("WARN", path, line_number(text, match.start()), "SEC008", f"public IP literal {raw} should normally come from external configuration")
        )
    return findings


def relative(root: Path, path: Path) -> str:
    try:
        return str(path.resolve().relative_to(root.resolve()))
    except ValueError:
        return str(path)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", help="files or directories; defaults to changed files")
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--strict", action="store_true", help="treat warnings as failures")
    args = parser.parse_args()
    root = args.root.resolve()
    paths = expand(root, args.paths)
    if not paths:
        print("No files selected.")
        return 0

    findings = [finding for path in paths for finding in inspect(path)]
    for finding in findings:
        print(f"[{finding.severity}] {relative(root, finding.path)}:{finding.line} {finding.code} {finding.message}")
    errors = sum(item.severity == "ERROR" for item in findings)
    warnings = sum(item.severity == "WARN" for item in findings)
    print(f"Scanned {len(paths)} file(s): {errors} error(s), {warnings} warning(s).")
    return 1 if errors or (args.strict and warnings) else 0


if __name__ == "__main__":
    sys.exit(main())
