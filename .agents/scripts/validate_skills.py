#!/usr/bin/env python3
"""Validate the repository-local Agent Skills structure using only stdlib."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


def parse_frontmatter(text: str) -> dict[str, str]:
    if not text.startswith("---\n"):
        return {}
    end = text.find("\n---\n", 4)
    if end < 0:
        return {}
    result: dict[str, str] = {}
    for raw_line in text[4:end].splitlines():
        if ":" not in raw_line:
            continue
        key, value = raw_line.split(":", 1)
        result[key.strip()] = value.strip().strip('"\'')
    return result


def validate(root: Path) -> list[str]:
    errors: list[str] = []
    skills_dir = root / ".agents" / "skills"
    if not skills_dir.is_dir():
        return [f"missing skills directory: {skills_dir}"]

    skill_dirs = sorted(path for path in skills_dir.iterdir() if path.is_dir())
    if not skill_dirs:
        return [f"no skill directories found under: {skills_dir}"]

    for skill_dir in skill_dirs:
        skill_name = skill_dir.name
        prefix = str(skill_dir.relative_to(root))
        if not NAME_RE.fullmatch(skill_name):
            errors.append(f"{prefix}: directory name must use lowercase kebab-case")

        skill_file = skill_dir / "SKILL.md"
        if not skill_file.is_file():
            errors.append(f"{prefix}: missing SKILL.md")
            continue

        try:
            text = skill_file.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            errors.append(f"{prefix}/SKILL.md: file must be UTF-8")
            continue

        metadata = parse_frontmatter(text)
        declared_name = metadata.get("name", "")
        description = metadata.get("description", "")
        if declared_name != skill_name:
            errors.append(
                f"{prefix}/SKILL.md: frontmatter name '{declared_name}' must match '{skill_name}'"
            )
        if len(description) < 20:
            errors.append(
                f"{prefix}/SKILL.md: description must explain capability and trigger (minimum 20 chars)"
            )
        if not text[end_of_frontmatter(text):].strip().startswith("#"):
            errors.append(f"{prefix}/SKILL.md: body must start with a Markdown heading")

    return errors


def end_of_frontmatter(text: str) -> int:
    if not text.startswith("---\n"):
        return 0
    end = text.find("\n---\n", 4)
    return end + 5 if end >= 0 else 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="repository root (default: inferred from script location)",
    )
    args = parser.parse_args()
    root = args.root.resolve()
    errors = validate(root)
    if errors:
        print("Agent skill validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"Agent skill validation passed: {root / '.agents' / 'skills'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
