"""Live check of every AI / RAG endpoint the app calls, with the app's request bodies.

usage: python scripts/probe-ai.py [model] [case ...]
The account (AJL_EMAIL / AJL_PASSWORD) comes from the environment, else from the repo's gitignored .dev.vars.
Run it before a release that touches anything networked; every line should say 200.
"""
import json, os, sys, time, urllib.request, http.cookiejar, pathlib

for line in (pathlib.Path(__file__).resolve().parents[2] / '.dev.vars').read_text(encoding='utf-8-sig').splitlines():
    k, _, v = line.partition('=')
    if k.startswith('AJL_'):
        os.environ.setdefault(k.strip(), v.strip())

B = 'https://anime-japanese-lab.ishallnotwant123.workers.dev'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140 Safari/537.36'
MODEL = sys.argv[1] if len(sys.argv) > 1 else 'gemini-3.5-flash-lite'
jar = http.cookiejar.CookieJar()
op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

def call(path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(B + path, data=data, headers={'User-Agent': UA, 'Content-Type': 'application/json'})
    t = time.time()
    try:
        r = op.open(req, timeout=150)
        return r.status, r.read().decode('utf-8', 'replace'), time.time() - t
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'replace'), time.time() - t
    except Exception as e:
        return -1, repr(e), time.time() - t

print('login', call('/api/auth/login', {'email': os.environ['AJL_EMAIL'], 'password': os.environ['AJL_PASSWORD']})[0])
dev = 'device-00000000-0000-0000-0000-000000000000'
cases = [
    ('explain', '/api/ai/explain', {'deviceId': dev, 'model': MODEL, 'reasoningEffort': 'high', 'kind': 'vocab', 'text': '負け犬', 'context': 'Re:ゼロ 第1話'}),
    ('quick-feedback', '/api/ai/quick-feedback', {'prompt': '選んで', 'sentence': '行って___', 'chosen': 'みる', 'answer': 'しまう', 'point': 'てしまう', 'formula': 'て形+しまう', 'learned': ['てしまう']}),
    ('correct-sentence', '/api/ai/correct-sentence', {'targetType': 'grammar', 'targetId': 'x', 'targetLabel': 'てしまう', 'sentence': '宿題を忘れてしまった', 'model': MODEL, 'deviceId': dev}),
    ('furigana/batch', '/api/ai/furigana/batch', {'targetType': 'sentence', 'items': [{'targetId': 'a', 'text': '本気でやばい'}]}),
    ('deep-dive', '/api/ai/sentence-deep-dive', {'workSlug': 'rezero', 'episode': 7, 'lineNo': 3, 'jaText': 'どうしてこんなことを…', 'zhText': '为什么要做这种事', 'model': MODEL, 'reasoningEffort': 'high', 'deviceId': dev}),
    ('character-profile', '/api/ai/character-profile', {'workSlug': 'rezero', 'characterKey': 'rem', 'characterName': 'レム', 'model': MODEL, 'reasoningEffort': 'high'}),
    ('rag/search', '/api/rag/search', {'query': '下定决心的台词', 'workSlug': 'rezero', 'topK': 8, 'deviceId': dev, 'analyze': False}),
    ('rag/search+analyze', '/api/rag/search', {'query': '下定决心的台词', 'workSlug': 'rezero', 'topK': 8, 'deviceId': dev, 'analyze': True}),
    ('rag/suggest', '/api/rag/suggest-training-query', {'workSlug': 'rezero', 'model': MODEL, 'deviceId': dev}),
]
only = set(sys.argv[2:])
for name, path, body in cases:
    if only and name not in only:
        continue
    st, txt, dt = call(path, body)
    print(f'--- {name}: {st} ({dt:.1f}s)')
    print(txt[:700].replace('\n', ' '))
