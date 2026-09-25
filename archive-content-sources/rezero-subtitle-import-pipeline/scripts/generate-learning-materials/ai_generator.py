"""AI generator interface — rule-based default, optional OpenAI."""

from __future__ import annotations

import json
import os
import re
import urllib.error
import urllib.request
from abc import ABC, abstractmethod
from typing import Any


class AIGenerator(ABC):
    @abstractmethod
    def enrich_grammar(
        self, pattern: str, ja_example: str, zh_hint: str, function_zh: str
    ) -> dict[str, str]:
        ...

    @abstractmethod
    def enrich_sentence(self, ja_text: str, zh_hint: str) -> dict[str, Any]:
        ...


class RuleBasedGenerator(AIGenerator):
    def enrich_grammar(
        self, pattern: str, ja_example: str, zh_hint: str, function_zh: str
    ) -> dict[str, str]:
        tone = "口语" if re.search(r"(よ|ね|ちゃん)", ja_example) else "陈述"
        expl = f"此处使用「{pattern}」，表示{function_zh}。"
        if zh_hint:
            expl += f" 对照：{zh_hint[:60]}"
        return {
            "explanation_zh": expl,
            "pragmatics_note": f"语气偏{tone}，动漫日常对话中自然。",
            "real_world_note": "日常对话中可用",
        }

    def enrich_sentence(self, ja_text: str, zh_hint: str) -> dict[str, Any]:
        tags: list[str] = []
        if re.search(r"(よ|ね)$", ja_text):
            tags.append("语气助词")
        if re.search(r"(です|ます)", ja_text):
            tags.append("礼貌体")
        else:
            tags.append("日常体")
        if not tags:
            tags = ["日常"]
        return {
            "reading": "",
            "meaning_zh": zh_hint,
            "tone_tags": tags,
            "difficulty": "N5" if len(ja_text) <= 12 else "N4",
        }


class OpenAIGenerator(AIGenerator):
    def __init__(self, api_key: str | None = None, model: str = "gpt-4o-mini") -> None:
        self.api_key = api_key or os.environ.get("OPENAI_API_KEY", "")
        self.model = model
        self._fallback = RuleBasedGenerator()

    def _chat(self, system: str, user: str, retries: int = 2) -> dict[str, Any]:
        if not self.api_key:
            return {}
        payload = json.dumps(
            {
                "model": self.model,
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
                "response_format": {"type": "json_object"},
                "temperature": 0.3,
            }
        ).encode("utf-8")
        req = urllib.request.Request(
            "https://api.openai.com/v1/chat/completions",
            data=payload,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        for attempt in range(retries + 1):
            try:
                with urllib.request.urlopen(req, timeout=60) as resp:
                    body = json.loads(resp.read().decode())
                return json.loads(body["choices"][0]["message"]["content"])
            except (urllib.error.URLError, json.JSONDecodeError, KeyError, OSError):
                if attempt >= retries:
                    return {}
        return {}

    def enrich_grammar(
        self, pattern: str, ja_example: str, zh_hint: str, function_zh: str
    ) -> dict[str, str]:
        result = self._chat(
            "你是日语教师。只分析日文例句，中文仅作解释。返回 JSON: "
            '{"explanation_zh":"","pragmatics_note":"","real_world_note":""}',
            f"语法: {pattern}\n例句: {ja_example}\n功能: {function_zh}\n对照: {zh_hint}",
        )
        if not result:
            return self._fallback.enrich_grammar(pattern, ja_example, zh_hint, function_zh)
        return {
            "explanation_zh": str(result.get("explanation_zh", "")),
            "pragmatics_note": str(result.get("pragmatics_note", "")),
            "real_world_note": str(result.get("real_world_note", "")),
        }

    def enrich_sentence(self, ja_text: str, zh_hint: str) -> dict[str, Any]:
        result = self._chat(
            "你是日语教师。分析日文句子，中文仅作解释。返回 JSON: "
            '{"reading":"","meaning_zh":"","tone_tags":[],"difficulty":"N4"}',
            f"日文: {ja_text}\n对照: {zh_hint}",
        )
        if not result:
            return self._fallback.enrich_sentence(ja_text, zh_hint)
        return {
            "reading": str(result.get("reading", "")),
            "meaning_zh": str(result.get("meaning_zh", zh_hint)),
            "tone_tags": result.get("tone_tags") or ["日常"],
            "difficulty": str(result.get("difficulty", "N4")),
        }
