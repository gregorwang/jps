import fs from 'node:fs'
import path from 'node:path'

const SUPABASE_URL = process.env.SUPABASE_URL ?? 'https://qoatvdvbuleamyzsaldp.supabase.co'
const SUPABASE_KEY = process.env.SUPABASE_PUBLISHABLE_KEY ?? process.env.SUPABASE_ANON_KEY
if (!SUPABASE_KEY) throw new Error('Set SUPABASE_PUBLISHABLE_KEY or SUPABASE_ANON_KEY')

const root = process.cwd()
const outDir = path.join(root, 'scripts', 'generated')
const outReport = path.join(outDir, 'repair-rezero-learning-sentence-translations.report.json')
const outSql = path.join(outDir, 'repair-rezero-learning-sentence-translations.sql')
const dryRun = !process.argv.includes('--apply')
const onlyBad = process.argv.includes('--only-bad')
const minEpisode = numberArg('--min-episode=', 1)
const maxEpisode = numberArg('--max-episode=', 66)

const [sentences, subtitleLines] = await Promise.all([
  fetchAll('learning_sentences', {
    select: 'id,episode,sort_order,source_line_no,ja_text,meaning_zh',
    work_slug: 'eq.re-zero',
    order: 'episode.asc,sort_order.asc',
  }),
  fetchAll('subtitle_lines', {
    select: 'episode,line_no,start_time,end_time,ja_text,zh_text',
    work_slug: 'eq.re-zero',
    order: 'episode.asc,line_no.asc',
  }),
])

const scopedSentences = sentences.filter((row) => row.episode >= minEpisode && row.episode <= maxEpisode)
const dbLinesByEpisode = groupBy(subtitleLines, (line) => line.episode)
const localChineseByEpisode = loadLocalChineseSubtitles()

const updates = []
const unchanged = []
const misses = []
const suspicious = []

for (const sentence of scopedSentences) {
  if (onlyBad && !needsRepair(sentence.meaning_zh)) {
    unchanged.push({ id: sentence.id, episode: sentence.episode, reason: 'already_ok' })
    continue
  }

  const manualBeforeMatch = manualTranslation(sentence)
  if (manualBeforeMatch) {
    updates.push(buildUpdate(sentence, manualBeforeMatch, {
      source: 'manual_fallback',
      startLine: sentence.source_line_no,
      endLine: sentence.source_line_no,
      score: 1,
      zhSource: 'manual_fallback',
    }))
    continue
  }

  if (sentence.episode <= 25) {
    const sanitized = sanitizeExistingMeaning(sentence.meaning_zh)
    if (sanitized && sanitized !== stringValue(sentence.meaning_zh) && isUsableChinese(sanitized)) {
      updates.push(buildUpdate(sentence, sanitized, {
        source: 'existing_meaning_zh',
        startLine: sentence.source_line_no,
        endLine: sentence.source_line_no,
        score: 1,
        zhSource: 'sanitize_existing_meaning',
      }))
      continue
    }
  }

  const source = findJapaneseMatch(sentence, dbLinesByEpisode.get(sentence.episode) ?? [])
  if (!source) {
    const manual = manualTranslation(sentence)
    if (manual) {
      updates.push(buildUpdate(sentence, manual, {
        source: 'manual_fallback',
        startLine: sentence.source_line_no,
        endLine: sentence.source_line_no,
        score: 1,
        zhSource: 'manual_fallback',
      }))
      continue
    }

    const sanitized = sanitizeExistingMeaning(sentence.meaning_zh)
    if (sanitized && sanitized !== stringValue(sentence.meaning_zh) && isUsableChinese(sanitized)) {
      updates.push(buildUpdate(sentence, sanitized, {
        source: 'existing_meaning_zh',
        startLine: sentence.source_line_no,
        endLine: sentence.source_line_no,
        score: 1,
        zhSource: 'sanitize_existing_meaning',
      }))
    } else {
      misses.push(compactMiss(sentence, 'no_ja_match'))
    }
    continue
  }

  const translation = pickTranslation(source.lines, localChineseByEpisode.get(sentence.episode) ?? [], sentence.episode > 25)
  if (!translation) {
    const manual = manualTranslation(sentence)
    if (manual) {
      updates.push(buildUpdate(sentence, manual, {
        source: 'manual_fallback',
        startLine: source.startLine,
        endLine: source.endLine,
        score: source.score,
        zhSource: 'manual_fallback',
      }))
      continue
    }

    const sanitized = sanitizeExistingMeaning(sentence.meaning_zh)
    if (sanitized && sanitized !== stringValue(sentence.meaning_zh) && isUsableChinese(sanitized)) {
      updates.push(buildUpdate(sentence, sanitized, {
        source: 'existing_meaning_zh',
        startLine: sentence.source_line_no,
        endLine: sentence.source_line_no,
        score: 1,
        zhSource: 'sanitize_existing_meaning',
      }))
    } else {
      misses.push(compactMiss(sentence, 'no_zh_match', source))
    }
    continue
  }

  if (!isUsableChinese(translation.text)) {
    suspicious.push({ ...compactMiss(sentence, 'unusable_zh', source), candidate: translation.text })
    continue
  }

  const before = stringValue(sentence.meaning_zh)
  if (normalizeChinese(before) === normalizeChinese(translation.text)) {
    unchanged.push({ id: sentence.id, episode: sentence.episode, reason: 'same_translation' })
    continue
  }

  updates.push(buildUpdate(sentence, translation.text, {
    source: source.source,
    startLine: source.startLine,
    endLine: source.endLine,
    score: source.score,
    zhSource: translation.source,
  }))
}

const summary = summarize(scopedSentences, updates, unchanged, misses, suspicious)

fs.mkdirSync(outDir, { recursive: true })
fs.writeFileSync(outReport, `${JSON.stringify({ summary, updates, unchanged, misses, suspicious }, null, 2)}\n`, 'utf8')
fs.writeFileSync(outSql, buildSql(updates), 'utf8')

console.log(JSON.stringify(summary, null, 2))
console.log(`report: ${outReport}`)
console.log(`sql: ${outSql}`)

if (!dryRun) {
  await applyUpdates(updates)
  console.log(`applied updates: ${updates.length}`)
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

async function applyUpdates(rows) {
  const chunkSize = 500
  for (let index = 0; index < rows.length; index += chunkSize) {
    const chunk = rows.slice(index, index + chunkSize)
    const sql = buildSql(chunk)
    const response = await fetch(`${SUPABASE_URL}/rest/v1/rpc/exec_sql`, {
      method: 'POST',
      headers: {
        apikey: SUPABASE_KEY,
        Authorization: `Bearer ${SUPABASE_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ query: sql }),
    })
    if (!response.ok) throw new Error(`apply failed: ${response.status} ${await response.text()}`)
  }
}

function findJapaneseMatch(sentence, dbLines) {
  const target = normalizeJapanese(sentence.ja_text)
  if (!target) return null

  const sources = [
    { name: 'db', lines: dbLines },
  ]

  let best = null
  for (const source of sources) {
    const candidate = findInLines(target, source.lines, source.name)
    if (!candidate) continue
    if (!best || candidate.score > best.score || span(candidate) < span(best)) {
      best = candidate
    }
  }
  return best
}

function findInLines(target, lines, source) {
  let best = null
  for (let start = 0; start < lines.length; start += 1) {
    let combined = ''
    const selected = []
    for (let end = start; end < Math.min(lines.length, start + 12); end += 1) {
      const normalizedLine = normalizeJapanese(lines[end].ja_text)
      if (!normalizedLine) continue
      combined += normalizedLine
      selected.push(lines[end])

      const score = matchScore(target, combined)
      if (score >= 0.9) {
        const candidate = {
          source,
          lines: selected.slice(),
          startLine: selected[0].line_no,
          endLine: selected[selected.length - 1].line_no,
          score,
        }
        if (!best || candidate.score > best.score || span(candidate) < span(best)) {
          best = candidate
        }
      }

      if (combined.length > target.length * 1.35) break
    }
  }
  return best
}

function pickTranslation(dbLines, localChineseLines, allowDbFallback) {
  const timeRange = timeRangeFromLines(dbLines)
  if (timeRange) {
    const localText = uniqueChinese(bestLocalChinesePerDbLine(dbLines, localChineseLines))
    if (isUsableChinese(localText)) return { text: localText, source: 'local_sc_ass_time_overlap' }
  }

  if (allowDbFallback) {
    const dbText = uniqueChinese(dbLines.map((line) => line.zh_text))
    if (isUsableChinese(dbText)) return { text: dbText, source: 'db_zh_text' }
  }
  return null
}

function bestLocalChinesePerDbLine(dbLines, localChineseLines) {
  const output = []
  for (const dbLine of dbLines) {
    const range = timeRangeFromLines([dbLine])
    if (!range) continue
    const best = localChineseLines
      .map((line) => ({
        ...line,
        overlap: overlapMs(range, line),
        distance: Math.abs(midpoint(range) - midpoint(line)),
      }))
      .filter((line) => line.overlap >= 120)
      .sort((left, right) => {
        if (right.overlap !== left.overlap) return right.overlap - left.overlap
        return left.distance - right.distance
      })[0]
    if (best) output.push(best.text)
  }
  return output
}

function buildUpdate(sentence, after, match) {
  return {
    id: sentence.id,
    episode: sentence.episode,
    sort_order: sentence.sort_order,
    source_line_no: sentence.source_line_no,
    ja_text: sentence.ja_text,
    before: stringValue(sentence.meaning_zh),
    after,
    source: match.source,
    lines: [match.startLine, match.endLine],
    ja_score: match.score,
    zh_source: match.zhSource,
  }
}

function loadLocalChineseSubtitles() {
  const result = new Map()
  const rootDir = path.join(root, 'public', '字幕备份')
  for (const season of [1, 2, 3]) {
    const dir = path.join(rootDir, `S${String(season).padStart(2, '0')}`)
    if (!fs.existsSync(dir)) continue
    for (const filename of fs.readdirSync(dir)) {
      if (!filename.endsWith('.SC.ass')) continue
      const episodeMatch = filename.match(/S\d{2}E(\d{2})/u)
      if (!episodeMatch) continue
      const seasonEpisode = Number(episodeMatch[1])
      const globalEpisode = season === 1 ? seasonEpisode : season === 2 ? seasonEpisode + 25 : seasonEpisode + 50
      const file = path.join(dir, filename)
      result.set(globalEpisode, parseAssDialogues(fs.readFileSync(file, 'utf8')))
    }
  }
  return result
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
    .filter((line) => line.startMs !== null && line.endMs !== null && isUsableChinese(line.text))
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

function timeRangeFromLines(lines) {
  const starts = lines.map((line) => parseDbTime(line.start_time)).filter((value) => value !== null)
  const ends = lines.map((line) => parseDbTime(line.end_time)).filter((value) => value !== null)
  if (!starts.length || !ends.length) return null
  return { startMs: Math.min(...starts), endMs: Math.max(...ends) }
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

function overlapMs(left, right) {
  return Math.max(0, Math.min(left.endMs, right.endMs) - Math.max(left.startMs, right.startMs))
}

function midpoint(range) {
  return (range.startMs + range.endMs) / 2
}

function uniqueChinese(values) {
  const parts = []
  let previous = ''
  for (const value of values) {
    const text = stringValue(value).replace(/\s+/gu, ' ').trim()
    if (!text || !hasHan(text) || hasKana(text)) continue
    const key = normalizeChinese(text)
    if (!key || key === previous) continue
    parts.push(text)
    previous = key
  }
  return parts.join(' ')
}

function matchScore(target, candidate) {
  if (target === candidate) return 1
  if (target.includes(candidate)) return candidate.length / target.length
  if (candidate.includes(target)) return target.length / candidate.length
  return 0
}

function normalizeJapanese(value) {
  return stringValue(value)
    .replace(/\([^)]*\)|\uFF08[^\uFF09]*\uFF09/gu, '')
    .replace(/[\s\u3000\u300C\u300D\u300E\u300F\u3010\u3011[\]\uFF08\uFF09(),.。、\u3001!?！？\u2026\u30FB\uFF65\-\u2014\u2015~\uFF5E\u201C\u201D"'\u2019]/gu, '')
    .toLowerCase()
}

function normalizeChinese(value) {
  return stringValue(value).replace(/[，。！？!?、\s\u3000]/gu, '')
}

function needsRepair(value) {
  const text = stringValue(value)
  if (!text) return true
  if (hasKana(text)) return true
  if (!hasHan(text)) return true
  return /(?:这句用于理解当前场景|围绕|剧情|核心说明|关键台词|场面|用[“"][^”"]+[”"]直说|说明。|台词。|谈到|表达感谢|在道歉|在请求对方|追问或确认信息|推动当前场面|判断、情绪或行动)/u.test(text)
}

function sanitizeExistingMeaning(value) {
  let text = stringValue(value)
  if (!text) return ''
  const replacements = [
    [/ナツキ[･・]スバル/gu, '菜月昴'],
    [/スバルきゅん/gu, '昴亲'],
    [/スバル君/gu, '昴'],
    [/スバル/gu, '昴'],
    [/エミリアたん/gu, '爱蜜莉雅碳'],
    [/エミリア/gu, '爱蜜莉雅'],
    [/フェルト/gu, '菲鲁特'],
    [/フェリス/gu, '菲利斯'],
    [/フェリ(?:酱|ちゃん)?/gu, '菲利斯'],
    [/レム/gu, '雷姆'],
    [/ラム/gu, '拉姆'],
    [/ユリウス/gu, '由里乌斯'],
    [/ユーリ/gu, '尤里'],
    [/ロズワール/gu, '罗兹瓦尔'],
    [/ベアトリス/gu, '碧翠丝'],
    [/ベア子/gu, '贝蒂'],
    [/リンガ/gu, '林格'],
    [/竜殊/gu, '龙珠'],
    [/巫女/gu, '巫女'],
    [/バルス/gu, '巴鲁斯'],
    [/ペテルギウス[･・]ロマネコンティ/gu, '培提其乌斯·罗曼尼康帝'],
    [/ペテルギウス/gu, '培提其乌斯'],
    [/ヴィルヘルム/gu, '威尔海姆'],
    [/クルシュ/gu, '库珥修'],
    [/ゲート/gu, '门'],
    [/エルザ/gu, '艾尔莎'],
    [/ガーフィール/gu, '加菲尔'],
    [/リューズ/gu, '琉兹'],
    [/オットー/gu, '奥托'],
    [/プリシラ/gu, '普莉希拉'],
    [/アナスタシア/gu, '安娜塔西亚'],
    [/ヨシュア/gu, '约书亚'],
    [/ラインハルト/gu, '莱因哈鲁特'],
    [/シリウス/gu, ' Sirius '],
    [/オーケー/gu, 'OK'],
    [/デス/gu, '的说'],
    [/です/gu, '的说'],
    [/八方塞がり/gu, '四面楚歌'],
    [/夜払い/gu, '夜袭'],
  ]
  for (const [pattern, replacement] of replacements) {
    text = text.replace(pattern, replacement)
  }
  return text.replace(/\s+/gu, ' ').trim()
}

function manualTranslation(sentence) {
  const byId = new Map([
    ['re-zero-s01e08-sentence-084', '我现在走投无路、四面楚歌。'],
    ['re-zero-s01e15-sentence-085', '昴！'],
    ['re-zero-s01e20-sentence-008', '夜袭要来了，请闭上眼睛。'],
    ['re-zero-s01e23-sentence-072', '去死吧……的说！'],
    ['re-zero-s01e23-sentence-079', '……也已经太迟了的说。'],
    ['re-zero-s02e17-sentence-031', '（爱蜜莉雅）偷偷摸摸……哈！呼！'],
    ['re-zero-s02e17-sentence-032', '呀～！唰唰！'],
    ['re-zero-s02e23-sentence-204', '喝啊！哈！'],
    ['re-zero-s02e23-sentence-205', '呜……呃……呃！'],
    ['re-zero-sentence-ep050-095', '贝蒂万岁！昴～！'],
    ['re-zero-s02e25-sentence-067', '昴～！'],
    ['re-zero-sentence-ep050-097', '昴～！哈哈哈哈！真是的，哈哈哈哈……'],
    ['re-zero-s02e25-sentence-107', '那真是太让人高兴了。'],
    ['re-zero-s02e25-sentence-286', '呃……OK。'],
  ])
  return byId.get(sentence.id) ?? ''
}

function isUsableChinese(value) {
  const text = stringValue(value)
  return Boolean(text) && hasHan(text) && !hasKana(text) && !/本字幕由|仅供学习交流|请勿用作商业/u.test(text)
}

function compactMiss(sentence, reason, source = null) {
  return {
    id: sentence.id,
    episode: sentence.episode,
    sort_order: sentence.sort_order,
    source_line_no: sentence.source_line_no,
    reason,
    ja_text: sentence.ja_text,
    meaning_zh: sentence.meaning_zh,
    lines: source ? [source.startLine, source.endLine] : undefined,
    ja_score: source?.score,
  }
}

function summarize(rows, updates, unchanged, misses, suspicious) {
  const byEpisode = {}
  for (const row of rows) {
    byEpisode[row.episode] ??= { total: 0, updates: 0, unchanged: 0, misses: 0, suspicious: 0 }
    byEpisode[row.episode].total += 1
  }
  for (const row of updates) byEpisode[row.episode].updates += 1
  for (const row of unchanged) byEpisode[row.episode].unchanged += 1
  for (const row of misses) byEpisode[row.episode].misses += 1
  for (const row of suspicious) byEpisode[row.episode].suspicious += 1
  return {
    dryRun,
    onlyBad,
    minEpisode,
    maxEpisode,
    totalSentences: rows.length,
    updates: updates.length,
    unchanged: unchanged.length,
    misses: misses.length,
    suspicious: suspicious.length,
    byEpisode,
    sampleUpdates: updates.slice(0, 20),
    sampleMisses: misses.slice(0, 20),
    sampleSuspicious: suspicious.slice(0, 20),
  }
}

function buildSql(rows) {
  if (!rows.length) return '-- no updates\n'
  const values = rows.map((row) => `(${sqlString(row.id)}, ${sqlString(row.after)})`).join(',\n')
  return [
    'with updates(id, meaning_zh) as (',
    `  values\n${values}`,
    ')',
    'update public.learning_sentences as target',
    'set meaning_zh = updates.meaning_zh',
    'from updates',
    'where target.id = updates.id',
    'returning target.id, target.episode, target.sort_order, target.meaning_zh;',
    '',
  ].join('\n')
}

function groupBy(items, keyFn) {
  const map = new Map()
  for (const item of items) {
    const key = keyFn(item)
    map.set(key, [...(map.get(key) ?? []), item])
  }
  return map
}

function span(match) {
  return match.endLine - match.startLine
}

function hasKana(value) {
  return /[\u3041-\u3093\u30A1-\u30F6]/u.test(value)
}

function hasHan(value) {
  return /\p{Script=Han}/u.test(value)
}

function stringValue(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function sqlString(value) {
  return `'${String(value).replace(/'/gu, "''")}'`
}

function numberArg(prefix, fallback) {
  const raw = process.argv.find((arg) => arg.startsWith(prefix))?.slice(prefix.length)
  return raw ? Number(raw) : fallback
}
