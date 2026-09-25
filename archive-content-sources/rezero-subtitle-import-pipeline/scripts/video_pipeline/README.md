# Video Pipeline

从双语 ASS 解析时间轴，匹配 learning_sentences.csv，用 ffmpeg 截取跟读视频片段。

## 准备

1. python scripts/generate-learning-materials/generate.py  # 已有 learning_sentences.csv
2. 放入 input/videos/ 和 input/subtitles/
3. 安装 ffmpeg 并加入 PATH（截取时需要）

## Dry-run

`ash
python scripts/video_pipeline/video_pipeline.py run \
  --episode-id re0_s3e07 \
  --video input/videos/re0_s3e07.mkv \
  --subtitle input/subtitles/ReZero_S3E07_EP57.bilingual.ass \
  --learning-sentences output/learning/learning_sentences.csv \
  --out-dir output/video_pipeline/re0_s3e07 \
  --dry-run
`

## 正式截取

去掉 --dry-run。可加 --pre-roll-ms 300 --post-roll-ms 300

## 输出

- subtitle_index.csv
- sentence_clip_manifest.csv / .json
- clips/re0_s3e07_sentence_001.mp4 ...

## ffmpeg 安装 (Windows)

winget install Gyan.FFmpeg

无 ffmpeg 时用 --dry-run 仍可生成 manifest。

## match_status

matched | unmatched | skipped | clip_failed
