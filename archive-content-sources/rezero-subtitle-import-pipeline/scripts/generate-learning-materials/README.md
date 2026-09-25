# 学习材料 CSV 生成器

从已有 subtitle_lines 日文台词生成可导入 Supabase 的学习材料 CSV。

## 环境要求

- Python 3.10+（仅标准库）
- 已运行 python preprocess.py

## 使用方法

python scripts/generate-learning-materials/generate.py

可选 OPENAI_API_KEY 启用 AI 增强。

## Supabase 导入顺序

1. works, episodes, subtitle_lines, subtitle_chunks
2. output/learning/learning_schema.sql
3. learning_vocab_items.csv
4. learning_vocab_occurrences.csv
5. learning_grammar_points.csv
6. learning_sentences.csv
7. learning_exercises.csv
8. episode_learning_plans.csv
