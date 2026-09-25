"""Generate per-clip ASS for hardsub burn-in."""

from __future__ import annotations

from pathlib import Path

ASS_HEADER = """[Script Info]
Title: Anime Japanese Lab clip
ScriptType: v4.00+
WrapStyle: 2
ScaledBorderAndShadow: yes
PlayResX: 1920
PlayResY: 1080

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: JP_Upper,Yu Gothic UI,58,&H00FFFFFF,&H000000FF,&H00222222,&H96000000,0,0,0,0,100,100,0,0,1,3,0,2,80,80,130,1
Style: ZH_Lower,Microsoft JhengHei UI,52,&H00FFFBF0,&H000000FF,&H00222222,&H96000000,0,0,0,0,100,100,0,0,1,3,0,2,80,80,55,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""


def ms_to_ass_time(ms: int) -> str:
    ms = max(0, ms)
    h = ms // 3600000
    ms %= 3600000
    m = ms // 60000
    ms %= 60000
    s = ms // 1000
    cs = (ms % 1000) // 10
    return f"{h}:{m:02d}:{s:02d}.{cs:02d}"


def escape_ass_dialogue(text: str) -> str:
    return (text or "").replace("\n", "\\N")


def write_clip_ass(path: Path, ja_text: str, zh_text: str, sub_start_ms: int, sub_end_ms: int, clip_duration_ms: int) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    start = max(0, sub_start_ms)
    end = min(clip_duration_ms, max(sub_end_ms, start + 200))
    if end <= start:
        end = min(clip_duration_ms, start + 500)
    start_t = ms_to_ass_time(start)
    end_t = ms_to_ass_time(end)
    lines = [ASS_HEADER]
    ja = escape_ass_dialogue(ja_text)
    if ja:
        lines.append(f"Dialogue: 0,{start_t},{end_t},JP_Upper,,0,0,0,,{ja}")
    zh = escape_ass_dialogue(zh_text)
    if zh:
        lines.append(f"Dialogue: 0,{start_t},{end_t},ZH_Lower,,0,0,0,,{zh}")
    path.write_text("\n".join(lines), encoding="utf-8-sig")
    return path


def ffmpeg_ass_filter_path(path: Path) -> str:
    try:
        return path.resolve().relative_to(Path.cwd().resolve()).as_posix()
    except ValueError:
        return path.resolve().as_posix()