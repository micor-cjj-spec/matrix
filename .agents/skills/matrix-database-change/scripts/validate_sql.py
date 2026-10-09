#!/usr/bin/env python3
"""Perform lightweight deterministic checks on changed Matrix SQL files."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

SKIP_DIRS = {".git", "target", "node_modules", ".idea"}
MONEY_NAME = r"(?:amount|balance|price|cost|fee|tax|rate|debit|credit|income|expense)"


@dataclass(frozen=True)
class Finding:
    severity: str
    path: Path
    line: int
    code: str
    message: str


def git_candidates(root: Path) -> list[Path]:
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
    return sorted(root / name for name in names if name.lower().endswith(".sql"))


def expand_paths(root: Path, raw_paths: list[str]) -> list[Path]:
    inputs = [Path(item) for item in raw_paths]
    if not inputs:
        return git_candidates(root)
    result: set[Path] = set()
    for item in inputs:
        path = item if item.is_absolute() else root / item
        if path.is_dir():
            for candidate in path.rglob("*.sql"):
                if not any(part in SKIP_DIRS for part in candidate.parts):
                    result.add(candidate)
        elif path.suffix.lower() == ".sql":
            result.add(path)
    return sorted(result)


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def inspect(path: Path) -> list[Finding]:
    findings: list[Finding] = []
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        return [Finding("ERROR", path, 1, "SQL000", f"cannot read UTF-8 SQL: {exc}")]

    checks = [
        ("ERROR", "SQL001", re.compile(r"\bDROP\s+DATABASE\b", re.I), "DROP DATABASE is prohibited"),
        ("ERROR", "SQL002", re.compile(r"\bTRUNCATE\s+TABLE\b", re.I), "TRUNCATE TABLE requires an explicit reviewed migration plan"),
        ("ERROR", "SQL003", re.compile(r"\bDELETE\s+FROM\s+[\w.`]+\s*;", re.I), "DELETE without WHERE is prohibited"),
        (
            "ERROR",
            "SQL004",
            re.compile(rf"\b\w*{MONEY_NAME}\w*\b\s+(?:FLOAT|DOUBLE|REAL)\b", re.I),
            "financial values must use DECIMAL, not floating-point SQL types",
        ),
        ("WARN", "SQL005", re.compile(r"\bCREATE\s+TABLE\s+(?!IF\s+NOT\s+EXISTS)", re.I), "consider CREATE TABLE IF NOT EXISTS for rerunnable setup scripts"),
    ]
    for severity, code, pattern, message in checks:
        for match in pattern.finditer(text):
            findings.append(Finding(severity, path, line_number(text, match.start()), code, message))

    for match in re.finditer(r"\bCREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`\"]?([^`\"\s(]+)", text, re.I):
        table = match.group(1)
        if not re.fullmatch(r"[a-z][a-z0-9_]*", table):
            findings.append(
                Finding("WARN", path, line_number(text, match.start()), "SQL006", f"table '{table}' should use lowercase snake_case")
            )

    if path.name != path.name.strip() or " " in path.name:
        findings.append(Finding("WARN", path, 1, "SQL007", "SQL filename should not contain spaces"))
    return findings


def display_path(root: Path, path: Path) -> str:
    try:
        return str(path.resolve().relative_to(root.resolve()))
    except ValueError:
        return str(path)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", help="SQL files or directories; defaults to changed files")
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--strict", action="store_true", help="treat warnings as failures")
    args = parser.parse_args()
    root = args.root.resolve()
    paths = expand_paths(root, args.paths)
    if not paths:
        print("No SQL files selected.")
        return 0

    findings = [finding for path in paths for finding in inspect(path)]
    for finding in findings:
        print(
            f"[{finding.severity}] {display_path(root, finding.path)}:{finding.line} "
            f"{finding.code} {finding.message}"
        )
    errors = sum(item.severity == "ERROR" for item in findings)
    warnings = sum(item.severity == "WARN" for item in findings)
    print(f"Checked {len(paths)} SQL file(s): {errors} error(s), {warnings} warning(s).")
    return 1 if errors or (args.strict and warnings) else 0


if __name__ == "__main__":
    sys.exit(main())
