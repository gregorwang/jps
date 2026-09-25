# Anime Japanese Lab Vectorize Ingest

See wrangler.toml and src/index.ts.

1. npm install
2. npm run probe:embedding
3. wrangler vectorize create anime-japanese-lab-subtitles --dimensions=1024 --metric=cosine
4. metadata indexes: work, episode, language
5. npx wrangler secret bulk .dev.vars && npm run deploy
6. INGEST_WORKER_URL + ADMIN_TOKEN: npm run upload:chunks

chunks.jsonl must be Japanese only. Worker URL deployed: https://anime-japanese-lab-vector-ingest.ishallnotwant123.workers.dev
