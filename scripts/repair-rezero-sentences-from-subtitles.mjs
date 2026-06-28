import fs from 'node:fs'
import path from 'node:path'

const SUPABASE_URL = process.env.SUPABASE_URL ?? 'https://qoatvdvbuleamyzsaldp.supabase.co'
const SUPABASE_KEY = process.env.SUPABASE_PUBLISHABLE_KEY ?? process.env.SUPABASE_ANON_KEY
if (!SUPABASE_KEY) throw new Error('Set SUPABASE_ANON_KEY or SUPABASE_PUBLISHABLE_KEY before running this script.')

const root = process.cwd()
const subtitlesRoot = path.join(root, 'public', '字幕备份')
const outDir = path.join(root, 'scripts', 'generated')
const outSql = path.join(outDir, 'repair-rezero-sentences.sql')
const outReport = path.join(outDir, 'repair-rezero-sentences.report.json')

const dryRunEpisode = Number(process.argv.find((arg) => arg.startsWith('--episode='))?.split('=')[1] ?? 0)
const updateMinEpisode = numberArg('--update-min-episode=', 1)
const updateMaxEpisode = numberArg('--update-max-episode=', Number.MAX_SAFE_INTEGER)
const deleteMinEpisode = numberArg('--delete-min-episode=', 1)
const deleteMaxEpisode = numberArg('--delete-max-episode=', Number.MAX_SAFE_INTEGER)

const [sentences, subtitleLines] = await Promise.all([
  fetchAll('learning_sentences', {
    select: 'id,work_slug,episode,sort_order,source_line_no,ja_text,meaning_zh,audio_url,storage_path',
    work_slug: 'eq.re-zero',
    order: 'episode.asc,sort_order.asc',
  }),
  fetchAll('subtitle_lines', {
    select: 'work_slug,episode,line_no,start_time,end_time,ja_text,zh_text',
    work_slug: 'eq.re-zero',
    order: 'episode.asc,line_no.asc',
  }),
])

const scopedSentences = dryRunEpisode
  ? sentences.filter((row) => row.episode === dryRunEpisode)
  : sentences
const scopedSubtitleLines = dryRunEpisode
  ? subtitleLines.filter((row) => row.episode === dryRunEpisode)
  : subtitleLines

const subtitleLineByEpisodeAndNo = new Map(
  scopedSubtitleLines.map((line) => [`${line.episode}:${line.line_no}`, line]),
)
const scDialogueCache = new Map()
const sourceAudioByEpisodeAndText = new Set()
for (const row of scopedSentences) {
  if (hasOriginalAudio(row)) sourceAudioByEpisodeAndText.add(`${row.episode}:${normalizeJaKey(row.ja_text)}`)
}

const updates = []
const deletes = []
const unmatchedBadMeanings = []

for (const row of scopedSentences) {
  if (inRange(row.episode, deleteMinEpisode, deleteMaxEpisode) && shouldDeleteNoAudioShort(row, sourceAudioByEpisodeAndText)) {
    deletes.push({
      id: row.id,
      episode: row.episode,
      sort_order: row.sort_order,
      ja_text: row.ja_text,
      reason: deletionReason(row, sourceAudioByEpisodeAndText),
    })
    continue
  }

  if (!inRange(row.episode, updateMinEpisode, updateMaxEpisode)) continue
  if (!needsSubtitleMeaning(row.meaning_zh)) continue

  const matched = matchChineseSubtitle(row, subtitleLineByEpisodeAndNo, scDialogueCache)
  if (matched) {
    updates.push({
      id: row.id,
      episode: row.episode,
      sort_order: row.sort_order,
      source_line_no: row.source_line_no,
      ja_text: row.ja_text,
      before: row.meaning_zh,
      after: matched.text,
      subtitle_start_ms: matched.startMs,
      subtitle_end_ms: matched.endMs,
      overlap_ms: matched.overlapMs,
    })
  } else {
    unmatchedBadMeanings.push({
      id: row.id,
      episode: row.episode,
      sort_order: row.sort_order,
      source_line_no: row.source_line_no,
      ja_text: row.ja_text,
      meaning_zh: row.meaning_zh,
    })
  }
}

fs.mkdirSync(outDir, { recursive: true })
fs.writeFileSync(outSql, buildSql(updates, deletes), 'utf8')
fs.writeFileSync(outReport, `${JSON.stringify({ updates, deletes, unmatchedBadMeanings }, null, 2)}\n`, 'utf8')

console.log(`sentences scanned: ${scopedSentences.length}`)
console.log(`meaning updates from SC subtitles: ${updates.length}`)
console.log(`short no-original deletes: ${deletes.length}`)
console.log(`bad meanings without subtitle match: ${unmatchedBadMeanings.length}`)
console.log(`sql: ${outSql}`)
console.log(`report: ${outReport}`)

for (const item of updates.slice(0, 12)) {
  console.log(`UPDATE EP${item.episode} #${item.sort_order} ${JSON.stringify(item.ja_text)} -> ${JSON.stringify(item.after)}`)
}
for (const item of deletes.slice(0, 12)) {
  console.log(`DELETE EP${item.episode} #${item.sort_order} ${JSON.stringify(item.ja_text)} (${item.reason})`)
}

async function fetchAll(table, query) {
  const output = []
  const pageSize = 1000
  for (let offset = 0; ; offset += pageSize) {
    const params = new URLSearchParams({
      ...query,
      limit: String(pageSize),
      offset: String(offset),
    })
    const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${params.toString()}`, {
      headers: {
        apikey: SUPABASE_KEY,
        Authorization: `Bearer ${SUPABASE_KEY}`,
      },
    })
    if (!response.ok) throw new Error(`${table} fetch failed: ${response.status} ${await response.text()}`)
    const rows = await response.json()
    output.push(...rows)
    if (rows.length < pageSize) return output
  }
}

function matchChineseSubtitle(row, subtitleLineByEpisodeAndNo, scDialogueCache) {
  const sourceLine = subtitleLineByEpisodeAndNo.get(`${row.episode}:${row.source_line_no}`)
  if (!sourceLine) return null
  const directText = trustedDirectSubtitleText(sourceLine, row.episode, subtitleLineByEpisodeAndNo)

  const startMs = parseDbTime(sourceLine.start_time)
  const endMs = parseDbTime(sourceLine.end_time)
  if (startMs === null || endMs === null) {
    return directText ? { text: directText, startMs: null, endMs: null, overlapMs: null } : null
  }

  const dialogues = getScDialogues(row.episode, scDialogueCache)
  const candidates = dialogues
    .map((dialogue) => ({
      ...dialogue,
      overlapMs: Math.max(0, Math.min(endMs, dialogue.endMs) - Math.max(startMs, dialogue.startMs)),
    }))
    .filter((dialogue) => dialogue.overlapMs >= 120)
    .sort((left, right) => {
      if (right.overlapMs !== left.overlapMs) return right.overlapMs - left.overlapMs
      return Math.abs(midpoint(left) - midpoint({ startMs, endMs })) - Math.abs(midpoint(right) - midpoint({ startMs, endMs }))
    })

  const best = candidates[0]
  if (best?.overlapMs >= 1200) {
    return { text: best.text, startMs: best.startMs, endMs: best.endMs, overlapMs: best.overlapMs }
  }
  if (directText) return { text: directText, startMs: null, endMs: null, overlapMs: null }
  return best ? { text: best.text, startMs: best.startMs, endMs: best.endMs, overlapMs: best.overlapMs } : null
}

function trustedDirectSubtitleText(sourceLine, episode, subtitleLineByEpisodeAndNo) {
  const text = cleanPlainChinese(sourceLine.zh_text)
  if (!isUsableChineseSubtitle(text)) return null
  const previous = subtitleLineByEpisodeAndNo.get(`${episode}:${sourceLine.line_no - 1}`)
  const previousText = cleanPlainChinese(previous?.zh_text)
  if (previousText && normalizeChineseText(previousText) === normalizeChineseText(text)) return null
  return text
}

function getScDialogues(globalEpisode, cache) {
  if (cache.has(globalEpisode)) return cache.get(globalEpisode)
  const file = findScSubtitleFile(globalEpisode)
  const dialogues = file ? parseAssDialogues(fs.readFileSync(file, 'utf8')) : []
  cache.set(globalEpisode, dialogues)
  return dialogues
}

function findScSubtitleFile(globalEpisode) {
  const { season, seasonEpisode } = mapReZeroEpisode(globalEpisode)
  const dir = path.join(subtitlesRoot, `S${String(season).padStart(2, '0')}`)
  if (!fs.existsSync(dir)) return null
  const needle = `S${String(season).padStart(2, '0')}E${String(seasonEpisode).padStart(2, '0')}`
  return fs.readdirSync(dir)
    .filter((name) => name.endsWith('.SC.ass') && name.includes(needle))
    .map((name) => path.join(dir, name))[0] ?? null
}

function mapReZeroEpisode(globalEpisode) {
  if (globalEpisode <= 25) return { season: 1, seasonEpisode: globalEpisode }
  if (globalEpisode <= 50) return { season: 2, seasonEpisode: globalEpisode - 25 }
  return { season: 3, seasonEpisode: globalEpisode - 50 }
}

function parseAssDialogues(content) {
  return content
    .split(/\r?\n/u)
    .filter((line) => line.startsWith('Dialogue:'))
    .map(parseAssDialogue)
    .filter(Boolean)
    .filter((line) => line.style === 'Default' || line.style === 'Comment')
    .map((line) => ({
      startMs: parseAssTime(line.start),
      endMs: parseAssTime(line.end),
      text: cleanAssText(line.text),
    }))
    .filter((line) => line.startMs !== null && line.endMs !== null && hasHan(line.text) && !hasKana(line.text))
}

function parseAssDialogue(line) {
  const payload = line.slice('Dialogue:'.length).trimStart()
  const parts = payload.split(',')
  if (parts.length < 10) return null
  return {
    start: parts[1],
    end: parts[2],
    style: parts[3],
    text: parts.slice(9).join(','),
  }
}

function cleanAssText(text) {
  return text
    .replace(/\{[^}]*\}/gu, '')
    .replace(/\\[Nnh]/gu, ' ')
    .replace(/\s+/gu, ' ')
    .trim()
}

function cleanPlainChinese(text) {
  return stringValue(text).replace(/\s+/gu, ' ').trim()
}

function isUsableChineseSubtitle(text) {
  return Boolean(text) && hasHan(text) && !hasKana(text) && !needsSubtitleMeaning(text)
}

function normalizeChineseText(text) {
  return stringValue(text).replace(/[，。！？!?、\s　]/gu, '')
}

function needsSubtitleMeaning(value) {
  const text = stringValue(value)
  if (!text) return true
  if (hasKana(text)) return true
  return /(?:围绕|剧情|核心说明|关键台词|场面|幼年|情绪台词|用[“"][^”"]+[”"]直说|说明。|台词。)/u.test(text)
}

function shouldDeleteNoAudioShort(row, sourceAudioByEpisodeAndText) {
  if (hasOriginalAudio(row)) return false
  if (sourceAudioByEpisodeAndText.has(`${row.episode}:${normalizeJaKey(row.ja_text)}`)) return true
  const length = normalizedJapaneseLength(row.ja_text)
  if (length <= 8) return true
  return false
}

function deletionReason(row, sourceAudioByEpisodeAndText) {
  const length = normalizedJapaneseLength(row.ja_text)
  if (sourceAudioByEpisodeAndText.has(`${row.episode}:${normalizeJaKey(row.ja_text)}`)) return `duplicate with source audio, len=${length}`
  return `no source audio, len=${length}`
}

function hasOriginalAudio(row) {
  if (stringValue(row.audio_url) || stringValue(row.storage_path)) return true
  const id = stringValue(row.id)
  if (/^re-zero-s\d{2}e\d{2}-sentence-\d+$/u.test(id)) return true
  if (/^rezero_s03e\d+_v\d+_sent_\d+$/u.test(id)) return true
  return false
}

function normalizedJapaneseLength(value) {
  return normalizeJaKey(value).length
}

function normalizeJaKey(value) {
  return stringValue(value)
    .replace(/（[^）]*）/gu, '')
    .replace(/[「」『』【】\[\]（）()]/gu, '')
    .replace(/[、。！？!?…・･\-\s　]/gu, '')
    .trim()
}

function parseDbTime(value) {
  const match = stringValue(value).match(/^(\d{2}):(\d{2}):(\d{2}),(\d{3})$/u)
  if (!match) return null
  return (((Number(match[1]) * 60 + Number(match[2])) * 60 + Number(match[3])) * 1000) + Number(match[4])
}

function parseAssTime(value) {
  const match = stringValue(value).match(/^(\d+):(\d{2}):(\d{2})\.(\d{2})$/u)
  if (!match) return null
  return (((Number(match[1]) * 60 + Number(match[2])) * 60 + Number(match[3])) * 1000) + (Number(match[4]) * 10)
}

function midpoint({ startMs, endMs }) {
  return (startMs + endMs) / 2
}

function hasKana(value) {
  return /[ぁ-んァ-ヶ]/u.test(value)
}

function hasHan(value) {
  return /[\p{Script=Han}]/u.test(value)
}

function stringValue(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function buildSql(updates, deletes) {
  const statements = [
    '-- Generated by scripts/repair-rezero-sentences-from-subtitles.mjs',
    'begin;',
    ...updates.map((row) => (
      `update public.learning_sentences set meaning_zh = ${sqlString(row.after)} where id = ${sqlString(row.id)};`
    )),
    ...deletes.map((row) => (
      `delete from public.learning_sentences where id = ${sqlString(row.id)};`
    )),
    'commit;',
    '',
  ]
  return statements.join('\n')
}

function sqlString(value) {
  return `'${String(value).replace(/'/gu, "''")}'`
}

function numberArg(prefix, fallback) {
  const raw = process.argv.find((arg) => arg.startsWith(prefix))?.slice(prefix.length)
  return raw ? Number(raw) : fallback
}

function inRange(value, min, max) {
  return value >= min && value <= max
}
