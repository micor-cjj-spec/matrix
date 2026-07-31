#!/usr/bin/env python3
"""Heuristically check changed Spring controllers for Matrix API-contract risks."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Finding:
    severity: str
    path: Path
    line: int
    code: str
    message: str


def changed_java(root: Path) -> list[Path]:
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
    return sorted(root / name for name in names if name.endswith("Controller.java"))


def expand(root: Path, raw_paths: list[str]) -> list[Path]:
    if not raw_paths:
        return changed_java(root)
    result: set[Path] = set()
    for item in raw_paths:
        path = Path(item)
        path = path if path.is_absolute() else root / path
        if path.is_dir():
            result.update(path.rglob("*Controller.java"))
        elif path.name.endswith("Controller.java"):
            result.add(path)
    return sorted(result)


def inspect(path: Path) -> list[Finding]:
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        return [Finding("ERROR", path, 1, "API000", f"cannot read controller: {exc}")]
    if "@RestController" not in text:
        return []

    findings: list[Finding] = []
    lines = text.splitlines()
    mapping_pending = False
    for index, line in enumerate(lines, start=1):
        stripped = line.strip()
        if re.search(r"@(Get|Post|Put|Delete|Patch|Request)Mapping\b", stripped):
            mapping_pending = True
            continue

        if mapping_pending and re.search(r"\bpublic\s+", stripped) and "(" in stripped:
            mapping_pending = False
            method_match = re.search(
                r"\bpublic\s+(?:static\s+)?(?:final\s+)?([^\s]+(?:<[^;{]+?>)?)\s+(\w+)\s*\(",
                stripped,
            )
            if method_match:
                return_type = method_match.group(1).replace(" ", "")
                method_name = method_match.group(2)
                if not return_type.startswith("ApiResponse<") and return_type != "ApiResponse":
                    findings.append(
                        Finding("WARN", path, index, "API001", f"endpoint {method_name} returns {return_type}; new Matrix APIs should use ApiResponse<T>")
                    )
                if "Map<String,Object>" in return_type or return_type in {"Map", "Object"}:
                    findings.append(
                        Finding("WARN", path, index, "API002", f"endpoint {method_name} uses an untyped response; prefer a response DTO")
                    )

        for match in re.finditer(r"@PathVariable(?!\s*\()\s+", line):
            findings.append(
                Finding("WARN", path, index, "API003", "@PathVariable should declare its external parameter name explicitly")
            )

        body_match = re.search(r"@RequestBody(?:\([^)]*\))?\s+([A-Z][A-Za-z0-9_]*)\s+\w+", line)
        if body_match:
            type_name = body_match.group(1)
            safe_suffixes = ("Request", "Req", "DTO", "Dto", "Map", "List", "String")
            if not type_name.endswith(safe_suffixes):
                findings.append(
                    Finding("WARN", path, index, "API004", f"request body type {type_name} may expose a persistence entity; prefer a request DTO for new APIs")
                )

    if "@Autowired" in text:
        findings.append(
            Finding("WARN", path, 1, "API005", "controller uses field injection; prefer constructor injection for new code")
        )
    return findings


def relative(root: Path, path: Path) -> str:
    try:
        return str(path.resolve().relative_to(root.resolve()))
    except ValueError:
        return str(path)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", help="controller files or directories; defaults to changed controllers")
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--strict", action="store_true", help="treat warnings as failures")
    args = parser.parse_args()
    root = args.root.resolve()
    paths = expand(root, args.paths)
    if not paths:
        print("No controller files selected.")
        return 0

    findings = [finding for path in paths for finding in inspect(path)]
    for finding in findings:
        print(f"[{finding.severity}] {relative(root, finding.path)}:{finding.line} {finding.code} {finding.message}")
    errors = sum(item.severity == "ERROR" for item in findings)
    warnings = sum(item.severity == "WARN" for item in findings)
    print(f"Checked {len(paths)} controller file(s): {errors} error(s), {warnings} warning(s).")
    return 1 if errors or (args.strict and warnings) else 0


if __name__ == "__main__":
    sys.exit(main())
