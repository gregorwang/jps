"""ffmpeg / ffprobe helpers for clip extraction and hardsub burn-in."""

from __future__ import annotations

import json
import shutil
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path

from sub_burn import write_clip_ass


@dataclass
class ProbeResult:
    ok: bool
    duration_ms: int | None
    error: str


@dataclass
class ClipResult:
    ok: bool
    error: str


@dataclass(frozen=True)
class EncodeOptions:
    """Tunable ffmpeg encode settings for clip generation."""

    preset: str = "veryfast"
    crf: int = 23
    scale_height: int = 1080
    video_codec: str = "libx264"
    workers: int = 3

    def summary(self) -> str:
        scale = f"{self.scale_height}p" if self.scale_height > 0 else "source"
        return f"{self.video_codec} preset={self.preset} crf={self.crf} scale={scale} workers={self.workers}"


def find_tool(name: str) -> str | None:
    return shutil.which(name)


def require_ffmpeg() -> tuple[str, str]:
    ffmpeg = find_tool("ffmpeg")
    ffprobe = find_tool("ffprobe")
    if not ffmpeg or not ffprobe:
        raise RuntimeError(
            "未找到 ffmpeg 或 ffprobe。请安装后加入 PATH。\n"
            "Windows: winget install Gyan.FFmpeg\n"
            "跳过截取可加 --dry-run"
        )
    return ffmpeg, ffprobe


def probe_video(path: Path) -> ProbeResult:
    ffprobe = find_tool("ffprobe")
    if not ffprobe:
        return ProbeResult(False, None, "ffprobe not found in PATH")
    if not path.exists():
        return ProbeResult(False, None, f"video not found: {path}")
    cmd = [ffprobe, "-v", "error", "-show_entries", "format=duration", "-of", "json", str(path)]
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
        if proc.returncode != 0:
            return ProbeResult(False, None, proc.stderr.strip() or "ffprobe failed")
        data = json.loads(proc.stdout)
        duration = float(data.get("format", {}).get("duration", 0))
        return ProbeResult(True, int(round(duration * 1000)), "")
    except (json.JSONDecodeError, ValueError, OSError) as exc:
        return ProbeResult(False, None, str(exc))


def ms_to_seconds(ms: int) -> str:
    return f"{ms / 1000:.3f}"


def build_vf_filter(ass_filter: str | None, scale_height: int) -> str | None:
    parts: list[str] = []
    if ass_filter:
        parts.append(ass_filter)
    if scale_height > 0:
        parts.append(f"scale=-2:{scale_height}")
    if not parts:
        return None
    return ",".join(parts)


def build_video_encode_args(opts: EncodeOptions) -> list[str]:
    if opts.video_codec == "h264_nvenc":
        preset = opts.preset if opts.preset.startswith("p") else "p4"
        return ["-c:v", "h264_nvenc", "-preset", preset, "-rc", "vbr", "-cq", str(opts.crf)]
    preset = opts.preset if opts.preset not in {"p1", "p2", "p3", "p4", "p5", "p6", "p7"} else "veryfast"
    return ["-c:v", "libx264", "-preset", preset, "-crf", str(opts.crf)]


def extract_clip(
    video_path: Path,
    output_path: Path,
    start_ms: int,
    end_ms: int,
    ffmpeg_bin: str | None = None,
    *,
    ja_text: str = "",
    zh_text: str = "",
    subtitle_start_ms: int | None = None,
    subtitle_end_ms: int | None = None,
    burn_subtitles: bool = True,
    ass_dir: Path | None = None,
    encode: EncodeOptions | None = None,
) -> ClipResult:
    ffmpeg = ffmpeg_bin or find_tool("ffmpeg")
    if not ffmpeg:
        return ClipResult(False, "ffmpeg not found")

    opts = encode or EncodeOptions()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    clip_duration_ms = end_ms - start_ms
    ass_filter: str | None = None
    ass_cwd: Path | None = None

    if burn_subtitles and ja_text:
        sub_start = (subtitle_start_ms or start_ms) - start_ms
        sub_end = (subtitle_end_ms or end_ms) - start_ms
        ass_path = (ass_dir or output_path.parent / "ass") / f"{output_path.stem}.ass"
        write_clip_ass(ass_path, ja_text, zh_text, sub_start, sub_end, clip_duration_ms)
        ass_filter = f"ass={ass_path.name}"
        ass_cwd = ass_path.parent

    vf_filter = build_vf_filter(ass_filter, opts.scale_height)
    video_abs = video_path.resolve()
    output_abs = output_path.resolve()

    # -ss/-to BEFORE -i: libass ass filter uses source timeline; seek-first keeps
    # clip-relative ASS times (0..duration) aligned with decoded frames.
    cmd = [
        ffmpeg, "-y",
        "-ss", ms_to_seconds(start_ms),
        "-to", ms_to_seconds(end_ms),
        "-i", str(video_abs),
        "-map", "0:v:0", "-map", "0:a:0?",
        "-sn", "-dn",
    ]
    if vf_filter:
        cmd += ["-vf", vf_filter]
    cmd += build_video_encode_args(opts)
    cmd += ["-c:a", "aac", "-movflags", "+faststart", str(output_abs)]

    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            check=False,
            cwd=str(ass_cwd) if ass_cwd else None,
        )
        if proc.returncode != 0:
            err = (proc.stderr or proc.stdout or "ffmpeg failed")[-800:]
            return ClipResult(False, err)
        if not output_path.exists() or output_path.stat().st_size == 0:
            return ClipResult(False, "output file missing or empty")
        return ClipResult(True, "")
    except OSError as exc:
        return ClipResult(False, str(exc))


def _clip_job(
    video_path: Path,
    out_dir: Path,
    row: dict,
    ffmpeg: str,
    *,
    burn_subtitles: bool,
    ass_dir: Path | None,
    encode: EncodeOptions,
) -> tuple[dict, ClipResult]:
    row = dict(row)
    clip_out = out_dir / row["clip_path"]
    start_ms = int(row["clip_start_ms"])
    end_ms = int(row["clip_end_ms"])
    sub_start = int(row["subtitle_start_ms"]) if row.get("subtitle_start_ms") != "" else start_ms
    sub_end = int(row["subtitle_end_ms"]) if row.get("subtitle_end_ms") != "" else end_ms
    result = extract_clip(
        video_path, clip_out, start_ms, end_ms, ffmpeg,
        ja_text=row.get("ja_text", ""),
        zh_text=row.get("zh_text", ""),
        subtitle_start_ms=sub_start,
        subtitle_end_ms=sub_end,
        burn_subtitles=burn_subtitles,
        ass_dir=ass_dir,
        encode=encode,
    )
    return row, result


def generate_clips(
    video_path: Path,
    out_dir: Path,
    manifest_rows: list[dict],
    *,
    burn_subtitles: bool = True,
    encode: EncodeOptions | None = None,
) -> tuple[int, int, list[dict]]:
    ffmpeg, _ = require_ffmpeg()
    opts = encode or EncodeOptions()
    ass_dir = out_dir / "ass" if burn_subtitles else None
    if ass_dir:
        ass_dir.mkdir(parents=True, exist_ok=True)

    jobs = [
        row for row in manifest_rows
        if row.get("match_status") == "matched" and row.get("clip_path")
    ]
    results_by_path: dict[str, tuple[dict, ClipResult]] = {}

    workers = max(1, opts.workers)
    if workers == 1 or len(jobs) <= 1:
        for row in jobs:
            updated_row, result = _clip_job(
                video_path, out_dir, row, ffmpeg,
                burn_subtitles=burn_subtitles, ass_dir=ass_dir, encode=opts,
            )
            results_by_path[row["clip_path"]] = (updated_row, result)
    else:
        with ThreadPoolExecutor(max_workers=workers) as pool:
            futures = {
                pool.submit(
                    _clip_job, video_path, out_dir, row, ffmpeg,
                    burn_subtitles=burn_subtitles, ass_dir=ass_dir, encode=opts,
                ): row["clip_path"]
                for row in jobs
            }
            for future in as_completed(futures):
                updated_row, result = future.result()
                results_by_path[futures[future]] = (updated_row, result)

    generated = failed = 0
    updated: list[dict] = []
    for row in manifest_rows:
        if row.get("clip_path") not in results_by_path:
            updated.append(row)
            continue
        row, result = results_by_path[row["clip_path"]]
        if result.ok:
            generated += 1
            row["clip_path"] = row["clip_path"].replace("\\", "/")
            if burn_subtitles:
                row["match_reason"] = f"{row.get('match_reason', '')};hardsub:yes".strip(";")
        else:
            failed += 1
            row["match_status"] = "clip_failed"
            row["match_reason"] = f"{row.get('match_reason', '')};clip_error:{result.error[:200]}"
            row["clip_path"] = ""
        updated.append(row)

    return generated, failed, updated
