"""Import report generation."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

from japanese_utils import has_kanji


@dataclass
class EpisodeStats:
    episode: int
    vocab_count: int = 0
    grammar_count: int = 0
    sentence_count: int = 0
    exercise_count: int = 0
    warnings: list[str] = field(default_factory=list)


def generate_report(
    path: Path,
    work_slug: str,
    work_name: str,
    episode_stats: list[EpisodeStats],
    vocab_items: list[dict],
    warnings: list[str],
    ai_mode: str,
    dup_ids: list[str],
) -> None:
    lines = [
        "# Anime Japanese Lab — 学习材料导入报告",
        "",
        f"生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        "",
        "## 处理作品",
        "",
        f"- work_slug: `{work_slug}`",
        f"- 作品名: {work_name}",
        f"- 生成模式: {ai_mode}",
        "",
        "## 处理集数",
        "",
        f"- 共 **{len(episode_stats)}** 集",
        "",
        "## 每集统计",
        "",
        "| 集数 | 词汇出现记录 | 语法点 | 跟读句 | 练习题 | 警告 |",
        "|------|-------------|--------|--------|--------|------|",
    ]
    for st in sorted(episode_stats, key=lambda x: x.episode):
        warn = "; ".join(st.warnings) if st.warnings else "—"
        lines.append(
            f"| {st.episode} | {st.vocab_count} | {st.grammar_count} | "
            f"{st.sentence_count} | {st.exercise_count} | {warn} |"
        )
    lines.extend(["", "## 词汇总览", "", f"- 高频词条目总数: **{len(vocab_items)}**", "", "## 质量检查", ""])
    missing_reading = [
        v["surface"] for v in vocab_items if has_kanji(v["surface"]) and not v.get("reading")
    ]
    if missing_reading:
        sample = ", ".join(missing_reading[:8])
        lines.append(f"- ⚠ 缺少假名的汉字词: **{len(missing_reading)}** 个（示例: {sample}）")
        warnings.append(f"{len(missing_reading)} 个词缺少假名")
    else:
        lines.append("- ✓ 未发现大量缺假名问题")
    if dup_ids:
        lines.append(f"- ⚠ 重复 ID: **{len(dup_ids)}** 个")
    else:
        lines.append("- ✓ 未发现重复 ID")
    lines.extend(["", "## 警告汇总", ""])
    if warnings:
        for w in warnings:
            lines.append(f"- ⚠ {w}")
    else:
        lines.append("- 无额外警告")
    lines.extend(
        [
            "",
            "## Supabase 导入顺序",
            "",
            "1. 确保已有: `works`, `episodes`, `subtitle_lines`, `subtitle_chunks`",
            "2. 执行: `output/learning/learning_schema.sql`",
            "3. 按顺序导入 CSV:",
            "   - `learning_vocab_items.csv`",
            "   - `learning_vocab_occurrences.csv`",
            "   - `learning_grammar_points.csv`",
            "   - `learning_sentences.csv`",
            "   - `learning_exercises.csv`",
            "   - `episode_learning_plans.csv`",
            "",
        ]
    )
    path.write_text("\n".join(lines), encoding="utf-8")
