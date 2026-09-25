"""Load supabase CSV inputs."""

from __future__ import annotations

import csv
from dataclasses import dataclass, field
from pathlib import Path

from japanese_utils import normalize_ja


@dataclass
class SubtitleLine:
    work_slug: str
    work_display_name: str
    episode: int
    line_no: int
    ja_text: str
    zh_text: str
    usable: bool


@dataclass
class EpisodeMeta:
    id: str
    work_slug: str
    episode: int


def load_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def load_subtitle_lines(supabase_dir: Path) -> list[SubtitleLine]:
    rows = load_csv(supabase_dir / "subtitle_lines.csv")
    lines: list[SubtitleLine] = []
    for r in rows:
        usable = str(r.get("usable_for_analysis", "true")).lower() == "true"
        lines.append(
            SubtitleLine(
                work_slug=r["work_slug"],
                work_display_name=r.get("work_display_name", ""),
                episode=int(r["episode"]),
                line_no=int(r["line_no"]),
                ja_text=normalize_ja(r.get("ja_text", "")),
                zh_text=(r.get("zh_text") or "").strip(),
                usable=usable,
            )
        )
    return lines


def load_episodes(supabase_dir: Path) -> list[EpisodeMeta]:
    rows = load_csv(supabase_dir / "episodes.csv")
    return [
        EpisodeMeta(id=r["id"], work_slug=r["work_slug"], episode=int(r["episode"]))
        for r in rows
    ]
