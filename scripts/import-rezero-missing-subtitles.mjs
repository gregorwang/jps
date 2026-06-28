import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const projectRef = process.env.SUPABASE_REF ?? 'qoatvdvbuleamyzsaldp'
const supabaseUrl = process.env.SUPABASE_URL ?? 'https://qoatvdvbuleamyzsaldp.supabase.co'
const accessToken = process.env.SUPABASE_ACCESS_TOKEN
const apply = process.argv.includes('--apply')
const replaceAll = process.argv.includes('--replace-all')
const onlyArg = process.argv.find((arg) => arg.startsWith('--episodes='))?.slice('--episodes='.length)
const onlyEpisodes = onlyArg ? new Set(onlyArg.split(',').map((value) => Number(value.trim())).filter(Number.isFinite)) : null

if (!accessToken) throw new Error('Set SUPABASE_ACCESS_TOKEN')

const serviceKey = await getServiceRoleKey()
const localEpisodes = discoverLocalEpisodes()
const existingCounts = await getExistingSubtitleCounts()
const selectedEpisodes = localEpisodes
  .filter((episode) => !onlyEpisodes || onlyEpisodes.has(episode.episode))
  .map((episode) => ({
    ...episode,
    existingCount: existingCounts.get(episode.episode) ?? 0,
  }))
  .filter((episode) => replaceAll || episode.existingCount === 0 || episode.existingCount < episode.rows.length)

console.log(`local SRT-backed episodes: ${localEpisodes.length}`)
console.log(`episodes selected: ${selectedEpisodes.length}`)
for (const episode of selectedEpisodes) {
  console.log(
    `EP${String(episode.episode).padStart(2, '0')} local=${episode.rows.length} existing=${episode.existingCount} zh=${episode.rows.filter((row) => row.zh_text).length} offset=${episode.offsetMs}ms`,
  )
}

if (!apply) {
  console.log('dry run only; rerun with --apply to import selected episodes')
} else {
  for (const episode of selectedEpisodes) {
    await importEpisode(episode)
  }
  const afterCounts = await getExistingSubtitleCounts()
  console.log('import complete')
  for (const episode of selectedEpisodes) {
    console.log(`EP${String(episode.episode).padStart(2, '0')} now=${afterCounts.get(episode.episode) ?? 0}`)
  }
}

function discoverLocalEpisodes() {
  const jaFiles = [
    ...findFiles(path.join(root, 'public'), /\.srt$/iu),
    ...findFiles(path.join(root, 'srt', 'Re_Zero kara Hajimeru Isekai Seikatsu'), /\.srt$/iu),
  ]
    .map((file) => ({ file, episode: episodeFromJapaneseSrt(file) }))
    .filter((entry) => entry.episode != null)
    .sort((left, right) => left.episode - right.episode)

  const zhFiles = new Map(
    findFiles(path.join(root, 'public', '字幕备份'), /\.SC\.ass$/iu)
      .map((file) => ({ file, episode: episodeFromChineseAss(file) }))
      .filter((entry) => entry.episode != null)
      .map((entry) => [entry.episode, entry.file]),
  )

  return jaFiles.map(({ file, episode }) => {
    const zhFile = zhFiles.get(episode)
    const jaCues = parseSrt(fs.readFileSync(file, 'utf8'))
    const zhDialogues = zhFile ? parseAssDialogues(fs.readFileSync(zhFile, 'utf8')) : []
    const offsetMs = zhDialogues.length ? chooseBestOffset(jaCues, zhDialogues) : 0
    const rows = jaCues.map((cue, index) => ({
      work_slug: 're-zero',
      episode,
      line_no: index + 1,
      start_time: cue.startTime,
      end_time: cue.endTime,
      ja_text: cue.text,
      zh_text: matchChinese(cue, zhDialogues, offsetMs),
    }))
    return { episode, jaFile: file, zhFile, offsetMs, rows }
  })
}

function episodeFromJapaneseSrt(file) {
  const normalized = file.replaceAll('\\', '/')
  const match = normalized.match(/S(01|02|03)E(\d{2})/iu)
  if (!match) return null
  const season = Number(match[1])
  const number = Number(match[2])
  if (season === 1) return number
  if (season === 2) return 25 + number
  if (season === 3) return number >= 51 ? number : 50 + number
  return null
}

function episodeFromChineseAss(file) {
  const normalized = file.replaceAll('\\', '/')
  const match = normalized.match(/字幕备份\/S(01|02|03)\/.*S\1E(\d{2})/iu)
  if (!match) return null
  const season = Number(match[1])
  const number = Number(match[2])
  if (season === 1) return number
  if (season === 2) return 25 + number
  if (season === 3) return 50 + number
  return null
}

async function importEpisode(episode) {
  console.log(`importing EP${String(episode.episode).padStart(2, '0')} rows=${episode.rows.length}`)
  await request('DELETE', `/rest/v1/subtitle_lines?work_slug=eq.re-zero&episode=eq.${episode.episode}`)

  for (const chunk of chunks(episode.rows, 500)) {
    await request('POST', '/rest/v1/subtitle_lines', chunk)
  }

  const zhLines = episode.rows.filter((row) => row.zh_text).length
  const episodeRow = {
    id: `re-zero-ep${String(episode.episode).padStart(2, '0')}`,
    work_slug: 're-zero',
    work_display_name: 'Re:ゼロから始める異世界生活',
    episode: episode.episode,
    total_cues: episode.rows.length,
    ja_lines: episode.rows.length,
    zh_lines: zhLines,
    usable_ja_lines: episode.rows.length,
    chunk_count: Math.ceil(episode.rows.length / 40),
    usable_as_main_corpus: true,
  }
  const existingEpisode = await request(
    'GET',
    `/rest/v1/episodes?select=id&work_slug=eq.re-zero&episode=eq.${episode.episode}&limit=1`,
  )
  if (existingEpisode[0]?.id) {
    await request('PATCH', `/rest/v1/episodes?id=eq.${encodeURIComponent(existingEpisode[0].id)}`, episodeRow)
  } else {
    await request('POST', '/rest/v1/episodes', episodeRow)
  }
}

async function getServiceRoleKey() {
  const response = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/api-keys`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!response.ok) throw new Error(`api-keys failed: ${response.status} ${await response.text()}`)
  const keys = await response.json()
  const service = keys.find((key) => key.name === 'service_role' || key.id === 'service_role')
  if (!service?.api_key) throw new Error('service_role key not found')
  return service.api_key
}

async function getExistingSubtitleCounts() {
  const rows = await fetchAll('subtitle_lines', {
    select: 'episode,line_no',
    work_slug: 'eq.re-zero',
    order: 'episode.asc,line_no.asc',
  })
  const counts = new Map()
  for (const row of rows) counts.set(row.episode, (counts.get(row.episode) ?? 0) + 1)
  return counts
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

async function request(method, pathName, body, options = {}) {
  const response = await fetch(`${supabaseUrl}${pathName}`, {
    method,
    headers: {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      ...(body ? { 'Content-Type': 'application/json', Prefer: options.prefer ?? 'return=minimal' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) throw new Error(`${method} ${pathName} failed: ${response.status} ${await response.text()}`)
  if (response.status === 204) return []
  const text = await response.text()
  return text ? JSON.parse(text) : []
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
    .filter((line) => isChineseDialogueStyle(line.style))
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

function isChineseDialogueStyle(style) {
  const normalized = String(style).trim().toLowerCase()
  return normalized === 'default' || normalized === 'comment' || normalized === 'up'
}

function chooseBestOffset(cues, dialogues) {
  let best = { offset: 0, score: -1 }
  for (let offset = -3000; offset <= 3000; offset += 250) {
    let score = 0
    for (const cue of cues) {
      const match = bestOverlap(cue, dialogues, offset)
      if (match >= 450) score += Math.min(match, 2500)
    }
    if (score > best.score) best = { offset, score }
  }
  return best.offset
}

function matchChinese(cue, dialogues, offsetMs) {
  const adjusted = { startMs: cue.startMs + offsetMs, endMs: cue.endMs + offsetMs }
  const best = dialogues
    .map((dialogue) => ({
      ...dialogue,
      overlapMs: overlap(adjusted, dialogue),
    }))
    .filter((dialogue) => dialogue.overlapMs >= 450)
    .sort((left, right) => {
      if (right.overlapMs !== left.overlapMs) return right.overlapMs - left.overlapMs
      return Math.abs(midpoint(left) - midpoint(adjusted)) - Math.abs(midpoint(right) - midpoint(adjusted))
    })[0]
  return best?.text ?? ''
}

function bestOverlap(cue, dialogues, offsetMs) {
  const adjusted = { startMs: cue.startMs + offsetMs, endMs: cue.endMs + offsetMs }
  return dialogues.reduce((best, dialogue) => Math.max(best, overlap(adjusted, dialogue)), 0)
}

function overlap(left, right) {
  return Math.max(0, Math.min(left.endMs, right.endMs) - Math.max(left.startMs, right.startMs))
}

function midpoint({ startMs, endMs }) {
  return (startMs + endMs) / 2
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

function hasKana(value) {
  return /[ぁ-んァ-ヶ]/u.test(value)
}

function hasHan(value) {
  return /[\p{Script=Han}]/u.test(value)
}

function findFiles(dir, pattern) {
  if (!fs.existsSync(dir)) return []
  const results = []
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) results.push(...findFiles(fullPath, pattern))
    else if (pattern.test(entry.name)) results.push(fullPath)
  }
  return results
}

function chunks(items, size) {
  const output = []
  for (let index = 0; index < items.length; index += size) output.push(items.slice(index, index + size))
  return output
}
