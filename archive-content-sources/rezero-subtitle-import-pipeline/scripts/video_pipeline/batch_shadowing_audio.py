#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from ass_parser import read_text_auto
from clipper import ms_to_seconds, probe_video, require_ffmpeg
from matcher import normalize_for_match

RE_SRT_TAGS = re.compile(r"<[^>]+>")
MAX_BACK_GAP_MS = 12000
MAX_FWD_GAP_MS = 2500
PRE_ROLL_MS = 150
POST_ROLL_MS = 200
R2_PATH_TEMPLATE = "shadowing-audio/re-zero/s01e01/{sentence_id}.mp3"


def srt_time_to_ms(raw: str) -> int:
    raw = raw.strip().replace(",", ".")
    hh, mm, rest = raw.split(":")
    ss, _, frac = rest.partition(".")
    ms = int(hh) * 3600000 + int(mm) * 60000 + int(ss) * 1000
    if frac:
        ms += int(frac.ljust(3, "0")[:3])
    return ms


def parse_srt(path: Path) -> dict[int, dict]:
    content = read_text_auto(path)
    blocks = re.split(r"\n\s*\n", content.strip(), flags=re.MULTILINE)
    cues: dict[int, dict] = {}
    line_no = 0
    for block in blocks:
        rows = [row.strip() for row in block.splitlines() if row.strip()]
        if len(rows) < 2:
            continue
        time_idx = 1 if rows[0].isdigit() else 0
        if time_idx >= len(rows) or "-->" not in rows[time_idx]:
            continue
        start_raw, end_raw = [part.strip() for part in rows[time_idx].split("-->")]
        text = RE_SRT_TAGS.sub("", "\n".join(rows[time_idx + 1 :])).strip()
        if not text:
            continue
        line_no += 1
        cues[line_no] = {
            "line_no": line_no,
            "start_ms": srt_time_to_ms(start_raw),
            "end_ms": srt_time_to_ms(end_raw),
            "text": text,
        }
    return cues


def is_meta_cue(text: str) -> bool:
    raw = text.strip()
    if re.match(r"^[\(（][^)）]{1,40}[\)）]\s*$", raw):
        return True
    norm = normalize_for_match(raw)
    if not norm:
        return True
    return False


def is_rhetorical_prefix(text: str) -> bool:
    if is_meta_cue(text):
        return False
    raw = text.strip()
    norm = normalize_for_match(raw)
    if not norm:
        return False
    if re.search(r"^(フフ|ふふ|はっ|ハァ|あっ)", norm):
        return False
    if raw.endswith("\u2026") or raw.endswith("..."):
        return True
    if len(norm) <= 5 and not re.search(r"[!！?？]$", raw):
        return True
    return False


def combined_norm(cues: dict[int, dict], start_line: int, end_line: int) -> str:
    parts = [normalize_for_match(cues[i]["text"]) for i in range(start_line, end_line + 1)]
    return "".join(parts)


def expand_range(cues: dict[int, dict], source_line_no: int, ja_text: str) -> tuple[int, int, str]:
    if source_line_no not in cues:
        raise KeyError(f"source_line_no {source_line_no} not in SRT")
    start_line = end_line = source_line_no
    reason_parts = [f"anchor:{source_line_no}"]

    while start_line > 1:
        prev_line = start_line - 1
        prev = cues[prev_line]
        curr = cues[start_line]
        gap = curr["start_ms"] - prev["end_ms"]
        if gap > MAX_BACK_GAP_MS:
            break
        if is_meta_cue(prev["text"]):
            break
        if is_rhetorical_prefix(prev["text"]):
            start_line = prev_line
            reason_parts.append(f"back:{prev_line}")
        else:
            break

    sent_norm = normalize_for_match(ja_text)
    fwd_extra = 0
    while fwd_extra < 2:
        combo = combined_norm(cues, start_line, end_line)
        if sent_norm in combo or combo in sent_norm:
            break
        if SequenceMatcher_ratio(sent_norm, combo) >= 0.92:
            break
        next_line = end_line + 1
        if next_line not in cues:
            break
        curr = cues[end_line]
        nxt = cues[next_line]
        gap = nxt["start_ms"] - curr["end_ms"]
        if gap > MAX_FWD_GAP_MS:
            break
        nxt_norm = normalize_for_match(nxt["text"])
        if not nxt_norm or (nxt_norm not in sent_norm and sent_norm not in nxt_norm):
            break
        end_line = next_line
        fwd_extra += 1
        reason_parts.append(f"fwd:{next_line}")

    return start_line, end_line, ";".join(reason_parts)


def SequenceMatcher_ratio(a: str, b: str) -> float:
    from difflib import SequenceMatcher
    return SequenceMatcher(None, a, b).ratio()


def extract_audio(ffmpeg: str, video: Path, out: Path, start_ms: int, end_ms: int) -> tuple[bool, str]:
    out.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        ffmpeg, "-y",
        "-ss", ms_to_seconds(start_ms),
        "-to", ms_to_seconds(end_ms),
        "-i", str(video.resolve()),
        "-vn", "-acodec", "libmp3lame", "-q:a", "2",
        str(out.resolve()),
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        return False, (proc.stderr or proc.stdout or "ffmpeg failed")[-400:]
    if not out.exists() or out.stat().st_size == 0:
        return False, "empty output"
    return True, ""


def run(args: argparse.Namespace) -> int:
    video = Path(args.video)
    srt = Path(args.srt)
    out_dir = Path(args.out_dir)
    sentences = json.loads(Path(args.sentences).read_text(encoding="utf-8-sig"))
    cues = parse_srt(srt)
    ffmpeg, _ = require_ffmpeg()
    probe = probe_video(video)
    if not probe.ok:
        print(probe.error)
        return 1

    manifest = []
    ok_n = fail_n = 0

    for item in sorted(sentences, key=lambda x: x["sort_order"]):
        sid = item["id"]
        ja = item["ja_text"]
        src = int(item["source_line_no"])
        try:
            s_line, e_line, expand_reason = expand_range(cues, src, ja)
        except KeyError as exc:
            manifest.append({"sentence_id": sid, "status": "failed", "error": str(exc)})
            fail_n += 1
            print(f"[FAIL] {sid}: {exc}")
            continue

        start_ms = cues[s_line]["start_ms"]
        end_ms = cues[e_line]["end_ms"]
        clip_start = max(0, start_ms - PRE_ROLL_MS)
        clip_end = min(probe.duration_ms or end_ms + POST_ROLL_MS, end_ms + POST_ROLL_MS)
        duration_ms = clip_end - clip_start
        out_file = out_dir / f"{sid}.mp3"
        ok, err = extract_audio(ffmpeg, video, out_file, clip_start, clip_end)
        row = {
            "sentence_id": sid,
            "sort_order": item["sort_order"],
            "ja_text": ja,
            "source_line_no": src,
            "srt_line_start": s_line,
            "srt_line_end": e_line,
            "clip_start_ms": clip_start,
            "clip_end_ms": clip_end,
            "duration_ms": duration_ms,
            "timing_source": expand_reason,
            "file_name": out_file.name,
            "storage_path": R2_PATH_TEMPLATE.format(sentence_id=sid),
            "status": "ok" if ok else "failed",
            "error": err,
        }
        manifest.append(row)
        if ok:
            ok_n += 1
            print(f"[{item['sort_order']:02d}] OK {duration_ms}ms {sid} lines {s_line}-{e_line}")
        else:
            fail_n += 1
            print(f"[{item['sort_order']:02d}] FAIL {sid}: {err[:100]}")

    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "audio_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nDone: {ok_n} ok, {fail_n} failed -> {out_dir}")
    return 0 if fail_n == 0 else 1


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--video", required=True)
    p.add_argument("--srt", required=True)
    p.add_argument("--sentences", required=True)
    p.add_argument("--out-dir", required=True)
    sys.exit(run(p.parse_args()))

