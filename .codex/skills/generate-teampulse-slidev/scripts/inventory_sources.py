#!/usr/bin/env python3
"""Inventory TeamPulse besoin and ADR files for Slidev generation."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

WEEK_RE = re.compile(r"\bW0*(\d{1,3})\b", re.IGNORECASE)
TICKET_RE = re.compile(r"\bW0*(\d{1,3})-T0*(\d{1,2})\b", re.IGNORECASE)


def normalize_week(value: str) -> str:
    match = WEEK_RE.search(value)
    if not match:
        raise ValueError(f"Cannot parse week code: {value}")
    return f"W{int(match.group(1)):03d}"


def normalize_ticket(value: str) -> str:
    match = TICKET_RE.search(value)
    if not match:
        raise ValueError(f"Cannot parse ticket code: {value}")
    return f"W{int(match.group(1)):03d}-T{int(match.group(2)):02d}"


def scan_markdown(root: Path, prefix: str, target_week: str | None) -> dict[str, list[dict[str, str]]]:
    result: dict[str, list[dict[str, str]]] = {}
    if not root.exists():
        return result

    for path in sorted(root.glob("W*/**/*.md")):
        ticket_match = TICKET_RE.search(path.name)
        week_match = WEEK_RE.search(str(path.parent))
        if ticket_match:
            code = normalize_ticket(ticket_match.group(0))
            week = code.split("-")[0]
        elif week_match:
            week = normalize_week(week_match.group(0))
            code = week
        else:
            continue
        if target_week and week != target_week:
            continue
        result.setdefault(week, []).append(
            {
                "code": code,
                "kind": prefix,
                "path": str(path),
            }
        )
    return result


def scan_adr(root: Path, target_week: str | None) -> dict[str, list[dict[str, str]]]:
    result: dict[str, list[dict[str, str]]] = {}
    if not root.exists():
        return result

    for path in sorted(root.glob("W*/**/*.md")):
        ticket_match = TICKET_RE.search(path.name)
        week_match = WEEK_RE.search(str(path.parent))
        if ticket_match:
            code = normalize_ticket(ticket_match.group(0))
            week = code.split("-")[0]
        elif week_match:
            week = normalize_week(week_match.group(0))
            code = week
        else:
            continue
        if target_week and week != target_week:
            continue
        result.setdefault(week, []).append(
            {
                "code": code,
                "kind": "adr",
                "path": str(path),
            }
        )
    return result


def merge_inventory(project_root: Path, target: str | None) -> dict[str, object]:
    target_week = None
    target_ticket = None
    if target:
        if "-T" in target.upper():
            target_ticket = normalize_ticket(target)
            target_week = target_ticket.split("-")[0]
        else:
            target_week = normalize_week(target)

    besoins = scan_markdown(project_root / "docs" / "besoins", "besoin", target_week)
    adrs = scan_adr(project_root / "docs" / "adr", target_week)
    weeks = sorted(set(besoins) | set(adrs))

    output_weeks = []
    for week in weeks:
        besoin_items = besoins.get(week, [])
        adr_items = adrs.get(week, [])
        tickets = sorted(
            {
                item["code"]
                for item in besoin_items + adr_items
                if "-T" in item["code"] and (not target_ticket or item["code"] == target_ticket)
            }
        )
        adr_codes = {item["code"] for item in adr_items}
        missing_adr = [code for code in tickets if code not in adr_codes]
        output_weeks.append(
            {
                "week": week,
                "tickets": tickets,
                "besoins": [item for item in besoin_items if not target_ticket or item["code"] == target_ticket],
                "adrs": [item for item in adr_items if not target_ticket or item["code"] == target_ticket],
                "missing_adr": missing_adr,
            }
        )

    roadmap = project_root / "docs" / "teampulse-roadmap-v9-code-infra-detaille.xlsx"
    return {
        "project_root": str(project_root),
        "target": target,
        "roadmap": str(roadmap) if roadmap.exists() else None,
        "weeks": output_weeks,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", default=".", help="TeamPulse repository root")
    parser.add_argument("--target", help="Optional week or ticket code, e.g. W001 or W001-T03")
    args = parser.parse_args()

    project_root = Path(args.project_root).resolve()
    inventory = merge_inventory(project_root, args.target)
    print(json.dumps(inventory, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
