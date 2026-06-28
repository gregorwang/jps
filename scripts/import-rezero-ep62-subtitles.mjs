import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const workSlug = 're-zero'
const episode = 62
const apply = process.argv.includes('--apply')
const chineseMatchOffsetMs = -1000
const outDir = path.join(root, 'scripts', 'generated')
const outSql = path.join(outDir, 'import-rezero-ep62-subtitles.sql')
const outBatchDir = path.join(outDir, 'import-rezero-ep62-subtitles-batches')

const SUPABASE_URL = process.env.SUPABASE_URL ?? readWranglerValue('SUPABASE_URL') ?? 'https://qoatvdvbuleamyzsaldp.supabase.co'
const SUPABASE_KEY =
  process.env.SUPABASE_SERVICE_ROLE_KEY ??
  process.env.SUPABASE_PUBLISHABLE_KEY ??
  process.env.SUPABASE_ANON_KEY ??
  readWranglerValue('SUPABASE_PUBLISHABLE_KEY')

if (!SUPABASE_KEY) throw new Error('Set SUPABASE_SERVICE_ROLE_KEY, SUPABASE_PUBLISHABLE_KEY, or SUPABASE_ANON_KEY.')

const jaFile = path.join(
  root,
  'public',
  'Re_Zero kara Hajimeru Isekai Seikatsu 3rd Season',
  'Re_ゼロから始める異世界生活.新編集版.S03E62.レグルス・コルニアス.WEBRip.Netflix.ja[cc].srt',
)
const zhFile = path.join(
  root,
  'public',
  '字幕备份',
  'S03',
  'Re：从零开始的异世界生活.S03E12.2025.2160P.BDRIP.SC.ass',
)

const jaCues = parseSrt(fs.readFileSync(jaFile, 'utf8'))
const zhDialogues = parseAssDialogues(fs.readFileSync(zhFile, 'utf8'))
const rows = jaCues.map((cue, index) => ({
  work_slug: workSlug,
  episode,
  line_no: index + 1,
  start_time: cue.startTime,
  end_time: cue.endTime,
  ja_text: cue.text,
  zh_text: matchChinese(cue, zhDialogues),
}))

fs.mkdirSync(outDir, { recursive: true })
fs.writeFileSync(outSql, buildSql(rows), 'utf8')
writeBatchSqlFiles(rows)

const existing = await fetchAll('subtitle_lines', {
  select: 'line_no',
  work_slug: `eq.${workSlug}`,
  episode: `eq.${episode}`,
  order: 'line_no.asc',
})

console.log(`source ja cues: ${jaCues.length}`)
console.log(`source zh dialogues: ${zhDialogues.length}`)
console.log(`matched zh rows: ${rows.filter((row) => row.zh_text).length}`)
console.log(`existing subtitle_lines ${workSlug} EP${episode}: ${existing.length}`)
console.log(`sql: ${outSql}`)
console.log(`sql batches: ${outBatchDir}`)

for (const row of rows.slice(0, 8)) {
  console.log(`#${row.line_no} ${row.start_time} ${JSON.stringify(row.ja_text)} => ${JSON.stringify(row.zh_text)}`)
}

if (!apply) {
  console.log('dry run only; rerun with --apply to import')
} else {
  await request('DELETE', `/rest/v1/subtitle_lines?work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episode}`)

  const pageSize = 100
  for (let index = 0; index < rows.length; index += pageSize) {
    const chunk = rows.slice(index, index + pageSize)
    await request('POST', '/rest/v1/subtitle_lines', chunk)
    console.log(`inserted ${Math.min(index + pageSize, rows.length)}/${rows.length}`)
  }

  const imported = await fetchAll('subtitle_lines', {
    select: 'line_no',
    work_slug: `eq.${workSlug}`,
    episode: `eq.${episode}`,
    order: 'line_no.asc',
  })
  console.log(`imported subtitle_lines ${workSlug} EP${episode}: ${imported.length}`)
}

function parseSrt(content) {
  return content
    .replace(/^\uFEFF/u, '')
    .split(/\r?\n\r?\n/u)
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block) => {
      const lines = block.split(/\r?\n/u)
      const timeIndex = lines.findIndex((line) => line.includes('-->'))
      if (timeIndex < 0) return null
      const [startRaw, endRaw] = lines[timeIndex].split(/\s+-->\s+/u)
      const text = cleanSubtitleText(lines.slice(timeIndex + 1).join(' '))
      return {
        startTime: normalizeSrtTime(startRaw),
        endTime: normalizeSrtTime(endRaw),
        startMs: parseSrtTime(startRaw),
        endMs: parseSrtTime(endRaw),
        text,
      }
    })
    .filter((cue) => cue?.text && cue.startMs !== null && cue.endMs !== null)
}

function parseAssDialogues(content) {
  return content
    .split(/\r?\n/u)
    .filter((line) => line.startsWith('Dialogue:'))
    .map(parseAssDialogue)
    .filter(Boolean)
    .filter((line) => isChineseStyle(line.style))
    .map((line) => ({
      startMs: parseAssTime(line.start),
      endMs: parseAssTime(line.end),
      text: cleanSubtitleText(line.text),
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

function matchChinese(cue, dialogues) {
  const matchCue = {
    ...cue,
    startMs: cue.startMs + chineseMatchOffsetMs,
    endMs: cue.endMs + chineseMatchOffsetMs,
  }
  const best = dialogues
    .map((dialogue) => ({
      ...dialogue,
      overlapMs: Math.max(0, Math.min(matchCue.endMs, dialogue.endMs) - Math.max(matchCue.startMs, dialogue.startMs)),
    }))
    .filter((dialogue) => dialogue.overlapMs >= 450)
    .sort((left, right) => {
      if (right.overlapMs !== left.overlapMs) return right.overlapMs - left.overlapMs
      return Math.abs(midpoint(left) - midpoint(matchCue)) - Math.abs(midpoint(right) - midpoint(matchCue))
    })[0]
  return best?.text ?? ''
}

function isChineseStyle(style) {
  const normalized = String(style).trim().toLowerCase()
  return normalized === 'default' || normalized === 'up' || normalized === 'comment'
}

function cleanSubtitleText(text) {
  return String(text)
    .replace(/\{[^}]*\}/gu, '')
    .replace(/\\[Nnh]/gu, ' ')
    .replace(/[（）][^（）]*[））]/gu, '')
    .replace(/\s+/gu, ' ')
    .trim()
}

function normalizeSrtTime(value) {
  return String(value).trim().replace('.', ',')
}

function parseSrtTime(value) {
  const match = String(value).trim().match(/^(\d{2}):(\d{2}):(\d{2})[,.](\d{3})/u)
  if (!match) return null
  return (((Number(match[1]) * 60 + Number(match[2])) * 60 + Number(match[3])) * 1000) + Number(match[4])
}

function parseAssTime(value) {
  const match = String(value).trim().match(/^(\d+):(\d{2}):(\d{2})\.(\d{2})$/u)
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

async function fetchAll(table, query) {
  const output = []
  const pageSize = 1000
  for (let offset = 0; ; offset += pageSize) {
    const params = new URLSearchParams({
      ...query,
      limit: String(pageSize),
      offset: String(offset),
    })
    const rows = await request('GET', `/rest/v1/${table}?${params.toString()}`)
    output.push(...rows)
    if (rows.length < pageSize) return output
  }
}

async function request(method, pathName, body) {
  const response = await fetch(`${SUPABASE_URL}${pathName}`, {
    method,
    headers: {
      apikey: SUPABASE_KEY,
      Authorization: `Bearer ${SUPABASE_KEY}`,
      ...(body ? { 'Content-Type': 'application/json', Prefer: 'return=minimal' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) throw new Error(`${method} ${pathName} failed: ${response.status} ${await response.text()}`)
  if (response.status === 204) return []
  const text = await response.text()
  return text ? JSON.parse(text) : []
}

function readWranglerValue(name) {
  const file = path.join(root, 'wrangler.toml')
  if (!fs.existsSync(file)) return undefined
  const match = fs.readFileSync(file, 'utf8').match(new RegExp(`^${name}\\s*=\\s*"([^"]+)"`, 'mu'))
  return match?.[1]
}

function buildSql(rowsToImport) {
  return [
    '-- Generated by scripts/import-rezero-ep62-subtitles.mjs',
    'begin;',
    `delete from public.subtitle_lines where work_slug = ${sqlString(workSlug)} and episode = ${episode};`,
    buildInsertSql(rowsToImport),
    'commit;',
    '',
  ].join('\n')
}

function writeBatchSqlFiles(rowsToImport) {
  fs.rmSync(outBatchDir, { recursive: true, force: true })
  fs.mkdirSync(outBatchDir, { recursive: true })
  fs.writeFileSync(
    path.join(outBatchDir, '000-delete.sql'),
    [
      '-- Generated by scripts/import-rezero-ep62-subtitles.mjs',
      `delete from public.subtitle_lines where work_slug = ${sqlString(workSlug)} and episode = ${episode};`,
      '',
    ].join('\n'),
    'utf8',
  )
  const pageSize = 100
  for (let index = 0; index < rowsToImport.length; index += pageSize) {
    const batchNo = String((index / pageSize) + 1).padStart(3, '0')
    fs.writeFileSync(
      path.join(outBatchDir, `${batchNo}-insert.sql`),
      `${buildInsertSql(rowsToImport.slice(index, index + pageSize))}\n`,
      'utf8',
    )
  }
}

function buildInsertSql(rowsToImport) {
  const values = rowsToImport
    .map((row) => [
      sqlString(row.work_slug),
      String(row.episode),
      String(row.line_no),
      sqlString(row.start_time),
      sqlString(row.end_time),
      sqlString(row.ja_text),
      sqlString(row.zh_text),
    ].join(', '))
    .map((value) => `  (${value})`)
    .join(',\n')

  return [
    'insert into public.subtitle_lines (work_slug, episode, line_no, start_time, end_time, ja_text, zh_text)',
    'values',
    `${values};`,
  ].join('\n')
}

function sqlString(value) {
  return `'${String(value).replace(/'/gu, "''")}'`
}
