type Env = {
  ASSETS: Fetcher
  SUBTITLE_RAG_WORKER?: Fetcher
  AI: Ai
  VECTORIZE: VectorizeIndex
  SUPABASE_URL: string
  SUPABASE_PUBLISHABLE_KEY: string
  SUPABASE_SERVICE_ROLE_KEY?: string
  OWNER_EMAIL?: string
  TTS_WORKER_URL: string
  VECTORIZE_INDEX: string
  SUBTITLE_RAG_WORKER_URL?: string
  AI_GATEWAY_BASE_URL: string
  CF_AIG_TOKEN: string
}

const EMBEDDING_MODEL = '@cf/baai/bge-m3'
const SUBTITLE_RAG_WORKER_URL = 'https://anime-japanese-lab-vector-ingest.ishallnotwant123.workers.dev'
const defaultGatewayModel = 'gemini-3.1-flash-lite'
const cacheSchemaVersion = 'v6'
const sessionCookieName = 'ajl_session'
const sessionMaxAgeSeconds = 60 * 60 * 24 * 30
const passwordIterations = 100_000

type GatewayModel = 'gemini-3.1-flash-lite' | 'gemini-3.5-flash' | 'deepseek-v4-flash' | 'deepseek-v4-pro' | 'grok-4.3'
type ReasoningEffort = 'low' | 'medium' | 'high'
type RagWorkSlug = 'rezero' | 'k-on'
type SubtitleRagMatch = {
  id: string
  score: number
  metadata?: {
    work?: string
    episode?: number
    chunk_no?: number
    start_time?: string
    end_time?: string
    language?: string
    source?: string
    text?: string
  }
}

type SubtitleRagQueryResponse = {
  ok: boolean
  matches?: SubtitleRagMatch[]
  error?: string
}

const ragWorkSlugs = new Set<RagWorkSlug>(['rezero', 'k-on'])

const gatewayModels: { id: GatewayModel; label: string }[] = [
  { id: 'gemini-3.1-flash-lite', label: 'Gemini 3.1 Flash Lite' },
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash' },
  { id: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash' },
  { id: 'deepseek-v4-pro', label: 'DeepSeek V4 Pro' },
  { id: 'grok-4.3', label: 'Grok 4.3' },
]

type WorkRow = {
  id: string
  slug: string
  display_name: string
  episode_count: number
}

type EpisodeRow = {
  id: string
  work_slug: string
  work_display_name: string
  episode: number
  total_cues: number
  ja_lines: number
  zh_lines: number
  usable_ja_lines: number
  chunk_count: number
  usable_as_main_corpus: boolean
}

type PlanRow = {
  id: string
  work_slug: string
  episode: number
  vocab_item_ids: string[]
  handwriting_vocab_ids: string[]
  shadowing_sentence_ids: string[]
  grammar_point_ids: string[]
  exercise_ids: string[]
  notes: string
}

type ReviewState = 'known' | 'fuzzy' | 'unknown' | 'good' | 'ok' | 'bad'
type ProgressItemType = 'vocab' | 'grammar' | 'sentence' | 'exercise' | 'unknown'
type WritingSubmitReason = 'empty' | 'too_short' | 'too_far' | 'low_coverage' | 'skipped'

type AiCacheRow = {
  cache_key: string
  result_payload: unknown
  updated_at?: string
}

const allowedReviewStates = new Set<ReviewState>(['known', 'fuzzy', 'unknown', 'good', 'ok', 'bad'])
const allowedProgressItemTypes = new Set<ProgressItemType>(['vocab', 'grammar', 'sentence', 'exercise', 'unknown'])
const allowedCacheKinds = new Set([
  'explain:vocab',
  'explain:grammar',
  'explain:sentence',
  'explain:linguistic',
  'sentence_deep_dive',
  'sentence_correction',
  'rag_air',
  'furigana',
])

type RubySegment = {
  text: string
  reading?: string
}

type AuthUser = {
  id: string
  email: string
}

type AuthContext = {
  user: AuthUser | null
  sessionTokenHash: string | null
}

type AppUserRow = {
  id: string
  email: string
  password_hash: string
  password_salt: string
  password_iterations: number
  created_at: string
  updated_at: string
}

const loginFailures = new Map<string, { count: number; resetAt: number }>()

const jsonHeaders = {
  'Content-Type': 'application/json; charset=utf-8',
  'Cache-Control': 'public, max-age=30',
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)
    if (url.pathname.startsWith('/api/')) {
      try {
        return await handleApi(request, env, url)
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Unknown error'
        return json({ error: { message } }, 500)
      }
    }

    return serveAssetOrSpaFallback(request, env)
  },
}

async function serveAssetOrSpaFallback(request: Request, env: Env) {
  const url = new URL(request.url)
  const accept = request.headers.get('Accept') ?? ''
  if (
    (request.method === 'GET' || request.method === 'HEAD') &&
    accept.includes('text/html') &&
    !url.pathname.includes('.')
  ) {
    return env.ASSETS.fetch(new Request(new URL('/', url), request))
  }

  try {
    return await env.ASSETS.fetch(request)
  } catch (error) {
    if (request.method !== 'GET' && request.method !== 'HEAD') throw error
    if (!accept.includes('text/html') && url.pathname.includes('.')) throw error
    return env.ASSETS.fetch(new Request(new URL('/index.html', url), request))
  }
}

async function handleApi(request: Request, env: Env, url: URL) {
  if (request.method !== 'GET' && request.method !== 'POST') {
    return json({ error: { message: 'Method not allowed' } }, 405)
  }

  const parts = url.pathname.split('/').filter(Boolean)
  const workSlug = parts[2]
  const episodeNo = Number(parts[4])

  if (url.pathname === '/api/config') {
    return json({
      ttsWorkerUrl: env.TTS_WORKER_URL,
      vectorizeIndex: env.VECTORIZE_INDEX,
      aiGatewayModels: gatewayModels,
    })
  }

  if (url.pathname === '/api/ai/models') {
    return json(gatewayModels)
  }

  if (url.pathname === '/api/linguistic-exercises') {
    return handleLinguisticExercises(request, env, url)
  }

  if (url.pathname === '/api/auth/register-owner' && request.method === 'POST') {
    return handleRegisterOwner(request, env)
  }

  if (url.pathname === '/api/auth/login' && request.method === 'POST') {
    return handleLogin(request, env)
  }

  if (url.pathname === '/api/auth/logout' && request.method === 'POST') {
    return handleLogout(request, env)
  }

  if (url.pathname === '/api/auth/me' && request.method === 'GET') {
    return handleMe(request, env)
  }

  if (url.pathname === '/api/auth/change-password' && request.method === 'POST') {
    return handleChangePassword(request, env)
  }

  if (url.pathname === '/api/auth/claim-device' && request.method === 'POST') {
    return handleClaimDevice(request, env)
  }

  if (url.pathname === '/api/works') {
    const rows = await supabase<WorkRow[]>(env, '/rest/v1/works?select=*&order=display_name.asc')
    return json(rows.map(mapWork))
  }

  if (parts[0] === 'api' && parts[1] === 'works' && workSlug && parts[3] === 'episodes' && parts.length === 4) {
    const rows = await supabase<EpisodeRow[]>(
      env,
      `/rest/v1/episodes?select=*&work_slug=eq.${encodeURIComponent(workSlug)}&order=episode.asc`,
    )
    return json(rows.map(mapEpisode))
  }

  if (parts[0] === 'api' && parts[1] === 'works' && workSlug && parts[3] === 'episodes' && Number.isFinite(episodeNo)) {
    if (parts.length === 5) {
      const rows = await supabase<EpisodeRow[]>(
        env,
        `/rest/v1/episodes?select=*&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&limit=1`,
      )
      return json(rows[0] ? mapEpisode(rows[0]) : null)
    }

    if (parts[5] === 'plan') {
      const rows = await supabase<PlanRow[]>(
        env,
        `/rest/v1/episode_learning_plans?select=*&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&limit=1`,
      )
      return json(rows[0] ? mapPlan(rows[0]) : null)
    }

    if (parts[5] === 'vocab') {
      const mode = url.searchParams.get('mode') === 'handwriting' ? 'handwriting' : 'vocab'
      const rows = await listEpisodeVocabRows(env, workSlug, episodeNo, mode)
      return json(rows.map(mapVocab))
    }

    if (parts[5] === 'grammar') {
      const rows = await supabase<unknown[]>(
        env,
        `/rest/v1/learning_grammar_points?select=*&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&order=sort_order.asc&limit=30`,
      )
      return json(rows.map(mapGrammar))
    }

    if (parts[5] === 'sentences') {
      const rows = await supabase<unknown[]>(
        env,
        `/rest/v1/learning_sentences?select=*&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&order=sort_order.asc&limit=30`,
      )
      return json(rows.map(mapSentence))
    }

    if (parts[5] === 'exercises') {
      const rows = await supabase<unknown[]>(
        env,
        `/rest/v1/linguistic_exercise_drafts?select=*&status=eq.published&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&order=source_line_no.asc,id.asc&limit=30`,
      )
      return json(rows.map(mapExercise))
    }

    if (parts[5] === 'subtitles') {
      const rows = await supabase<unknown[]>(
        env,
        `/rest/v1/subtitle_lines?select=line_no,start_time,end_time,ja_text,zh_text&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&order=line_no.asc&limit=500`,
      )
      return json(rows.map(mapSubtitle))
    }
  }

  if (url.pathname === '/api/rag/search' && request.method === 'POST') {
    return handleRagSearch(request, env)
  }

  if (url.pathname === '/api/rag/suggest-training-query' && request.method === 'POST') {
    return handleRagSuggestTrainingQuery(request, env)
  }

  if (url.pathname === '/api/rag/generate-question' && request.method === 'POST') {
    return handleRagGenerateQuestion(request, env)
  }

  if (url.pathname === '/api/rag/generate-questions' && request.method === 'POST') {
    return handleRagGenerateQuestions(request, env)
  }

  if (url.pathname === '/api/ai/explain' && request.method === 'POST') {
    return handleAiExplain(request, env)
  }

  if (url.pathname === '/api/ai/furigana') {
    return handleFurigana(request, env, url)
  }

  if (url.pathname === '/api/ai/sentence-deep-dive' && request.method === 'POST') {
    return handleSentenceDeepDive(request, env)
  }

  if (url.pathname === '/api/ai/character-profile' && request.method === 'POST') {
    return handleCharacterProfile(request, env)
  }

  if (url.pathname === '/api/ai/correct-sentence' && request.method === 'POST') {
    return handleSentenceCorrection(request, env)
  }

  if (url.pathname === '/api/progress' && request.method === 'GET') {
    return handleProgressList(request, env, url)
  }

  if (url.pathname === '/api/progress' && request.method === 'POST') {
    return handleProgressUpsert(request, env)
  }

  if (url.pathname === '/api/writing/stats' && request.method === 'GET') {
    return handleWritingStats(request, env, url)
  }

  if (url.pathname === '/api/writing/submit' && request.method === 'POST') {
    return handleWritingSubmit(request, env)
  }

  if (url.pathname === '/api/review/today' && request.method === 'GET') {
    return handleTodayReview(request, env, url)
  }

  if (url.pathname === '/api/history' && request.method === 'GET') {
    return handleHistory(request, env, url)
  }

  if (url.pathname === '/api/history/detail' && request.method === 'GET') {
    return handleHistoryDetail(request, env, url)
  }

  return json({ error: { message: 'Not found' } }, 404)
}

async function handleLinguisticExercises(request: Request, env: Env, url: URL) {
  const workSlug = normalizeWorkSlugAlias(url.searchParams.get('workSlug')?.trim())
  const episodeParam = url.searchParams.get('episode')
  const episode = episodeParam ? Number(episodeParam) : NaN
  const status = normalizeLinguisticStatus(url.searchParams.get('status'))
  const hasScopedQuery = Boolean(workSlug) || Number.isFinite(episode)
  const wantsDrafts = status === 'all' || status === 'draft'
  const auth = wantsDrafts ? await getAuthContext(request, env) : { user: null, sessionTokenHash: null }
  const canReadDrafts = Boolean(auth.user && normalizeEmail(auth.user.email) === normalizeEmail(env.OWNER_EMAIL) && env.SUPABASE_SERVICE_ROLE_KEY)
  if (status === 'draft' && !canReadDrafts) {
    return json({ error: { message: 'Draft linguistic exercises require owner access' } }, 403)
  }
  const statusFilter = wantsDrafts && canReadDrafts
    ? status === 'draft' ? 'status=eq.draft' : 'status=in.(draft,published)'
    : 'status=eq.published'
  const select = [
    'id',
    'batch_id',
    'work_slug',
    'episode',
    'source_line_no',
    'source_id',
    'ja_text',
    'zh_text',
    'domain',
    'phenomenon_key',
    'question_type',
    'prompt',
    'options',
    'answer',
    'hint',
    'basic_explanation_zh',
    'deep_explanation_zh',
    'anime_context_note_zh',
    'caution_note_zh',
    'difficulty',
    'quality_score',
    'status',
  ].join(',')
  const filters = [`select=${select}`, statusFilter]
  if (workSlug) filters.push(`work_slug=eq.${encodeURIComponent(workSlug)}`)
  if (Number.isFinite(episode)) filters.push(`episode=eq.${episode}`)
  const order = hasScopedQuery ? 'source_line_no.asc,id.asc' : 'episode.asc,domain.asc,phenomenon_key.asc,source_line_no.asc,id.asc'
  filters.push(`order=${order}`)
  filters.push(`limit=${hasScopedQuery ? '200' : '500'}`)

  const path = `/rest/v1/linguistic_exercise_drafts?${filters.join('&')}`
  const rows = wantsDrafts && canReadDrafts
    ? await supabaseAdmin<unknown[]>(env, 'GET', path)
    : await supabase<unknown[]>(env, path)
  const phenomena = await listLinguisticPhenomena(env, rows)
  return json(rows.map((row) => mapLinguisticExercise(row, phenomena)))
}

function normalizeLinguisticStatus(value: string | null) {
  if (value === 'draft') return 'draft'
  if (value === 'published') return 'published'
  if (value === 'all') return 'all'
  return value ? 'published' : 'published'
}

function normalizeWorkSlugAlias(value?: string) {
  if (!value) return ''
  return value === 'rezero' ? 're-zero' : value
}

async function listLinguisticPhenomena(env: Env, rows: unknown[]) {
  const keys = [...new Set(rows
    .map((row) => readString(row as Record<string, unknown>, 'phenomenon_key'))
    .filter(Boolean))]
  if (keys.length === 0) return new Map<string, Record<string, unknown>>()

  const inList = keys.map((key) => `"${key.replace(/"/g, '\\"')}"`).join(',')
  const path = `/rest/v1/linguistic_phenomena?select=phenomenon_key,domain,name_ja,name_zh,short_definition_zh&phenomenon_key=in.(${encodeURIComponent(inList)})`
  const rowsByKey = await supabase<Record<string, unknown>[]>(env, path).catch((error) => {
    console.error('Failed to load linguistic phenomena', error)
    return []
  })
  return new Map(rowsByKey.map((row) => [readString(row, 'phenomenon_key'), row]))
}

async function handleRegisterOwner(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as { email?: string; password?: string } | null
  const ownerEmail = normalizeEmail(env.OWNER_EMAIL)
  const email = normalizeEmail(body?.email)
  const password = body?.password ?? ''

  if (!ownerEmail) return json({ error: { message: 'OWNER_EMAIL is not configured' } }, 500)
  if (email !== ownerEmail) return json({ error: { message: 'Only OWNER_EMAIL can be initialized' } }, 403)
  if (!isValidPassword(password)) return json({ error: { message: 'Password must be at least 6 characters' } }, 400)

  const existing = await supabaseAdmin<AppUserRow[]>(env, 'GET', '/rest/v1/app_users?select=id&limit=1')
  if (existing.length > 0) return json({ error: { message: 'Owner is already initialized' } }, 409)

  const salt = randomBase64Url(16)
  const passwordHash = await hashPassword(password, salt, passwordIterations)
  const userId = crypto.randomUUID()
  await supabaseAdmin(env, 'POST', '/rest/v1/app_users', {
    id: userId,
    email,
    password_hash: passwordHash,
    password_salt: salt,
    password_iterations: passwordIterations,
  })

  return json({ user: { id: userId, email } }, 201)
}

async function handleLogin(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as { email?: string; password?: string; deviceHint?: string } | null
  const email = normalizeEmail(body?.email)
  const password = body?.password ?? ''
  const ownerEmail = normalizeEmail(env.OWNER_EMAIL)
  const rateKey = `${clientIp(request)}:${email || 'unknown'}`

  if (!ownerEmail) return json({ error: { message: 'OWNER_EMAIL is not configured' } }, 500)
  if (isLoginRateLimited(rateKey)) return json({ error: { message: 'Too many failed login attempts' } }, 429)
  if (email !== ownerEmail || !password) {
    recordLoginFailure(rateKey)
    return json({ error: { message: 'Invalid email or password' } }, 401)
  }

  const users = await supabaseAdmin<AppUserRow[]>(
    env,
    'GET',
    `/rest/v1/app_users?select=*&email=eq.${encodeURIComponent(email)}&limit=1`,
  )
  const user = users[0]
  if (!user || !(await verifyPassword(password, user.password_salt, user.password_iterations, user.password_hash))) {
    recordLoginFailure(rateKey)
    return json({ error: { message: 'Invalid email or password' } }, 401)
  }

  clearLoginFailure(rateKey)
  const token = randomBase64Url(32)
  const tokenHash = await sha256Hex(token)
  const expiresAt = new Date(Date.now() + sessionMaxAgeSeconds * 1000).toISOString()
  await supabaseAdmin(env, 'POST', '/rest/v1/app_sessions', {
    session_token_hash: tokenHash,
    user_id: user.id,
    expires_at: expiresAt,
    user_agent: request.headers.get('User-Agent')?.slice(0, 500) ?? null,
    device_hint: body?.deviceHint?.slice(0, 120) ?? null,
  })

  return jsonWithHeaders({ user: publicUser(user) }, 200, {
    'Set-Cookie': sessionCookie(token, sessionMaxAgeSeconds),
  })
}

async function handleLogout(request: Request, env: Env) {
  const token = readCookie(request, sessionCookieName)
  if (token) {
    const tokenHash = await sha256Hex(token)
    await supabaseAdmin(env, 'DELETE', `/rest/v1/app_sessions?session_token_hash=eq.${encodeURIComponent(tokenHash)}`).catch(() => undefined)
  }
  return jsonWithHeaders({ ok: true }, 200, { 'Set-Cookie': clearSessionCookie() })
}

async function handleMe(request: Request, env: Env) {
  const auth = await getAuthContext(request, env)
  return json({ user: auth.user })
}

async function handleChangePassword(request: Request, env: Env) {
  const auth = await requireAuth(request, env)
  if (auth instanceof Response) return auth
  const body = (await request.json().catch(() => null)) as { oldPassword?: string; newPassword?: string } | null
  const oldPassword = body?.oldPassword ?? ''
  const newPassword = body?.newPassword ?? ''
  if (!isValidPassword(newPassword)) return json({ error: { message: 'New password must be at least 6 characters' } }, 400)

  const users = await supabaseAdmin<AppUserRow[]>(
    env,
    'GET',
    `/rest/v1/app_users?select=*&id=eq.${encodeURIComponent(auth.user.id)}&limit=1`,
  )
  const user = users[0]
  if (!user || !(await verifyPassword(oldPassword, user.password_salt, user.password_iterations, user.password_hash))) {
    return json({ error: { message: 'Old password is incorrect' } }, 401)
  }

  const salt = randomBase64Url(16)
  const passwordHash = await hashPassword(newPassword, salt, passwordIterations)
  await supabaseAdmin(env, 'PATCH', `/rest/v1/app_users?id=eq.${encodeURIComponent(user.id)}`, {
    password_hash: passwordHash,
    password_salt: salt,
    password_iterations: passwordIterations,
    updated_at: new Date().toISOString(),
  })
  if (auth.sessionTokenHash) {
    await supabaseAdmin(
      env,
      'DELETE',
      `/rest/v1/app_sessions?user_id=eq.${encodeURIComponent(user.id)}&session_token_hash=neq.${encodeURIComponent(auth.sessionTokenHash)}`,
    ).catch(() => undefined)
  }

  return json({ ok: true })
}

async function handleClaimDevice(request: Request, env: Env) {
  const auth = await requireAuth(request, env)
  if (auth instanceof Response) return auth
  const body = (await request.json().catch(() => null)) as { deviceId?: string } | null
  const deviceId = body?.deviceId ?? ''
  if (!isValidDeviceId(deviceId)) return json({ error: { message: 'deviceId is invalid' } }, 400)

  const progressRows = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/user_progress?select=*&device_id=eq.${encodeURIComponent(deviceId)}`,
  )
  let progressMerged = 0
  for (const row of progressRows) {
    const itemId = readString(row, 'item_id')
    if (!itemId) continue
    const existingRows = await supabaseAdmin<Record<string, unknown>[]>(
      env,
      'GET',
      `/rest/v1/user_progress?select=*&user_id=eq.${encodeURIComponent(auth.user.id)}&item_id=eq.${encodeURIComponent(itemId)}&limit=1`,
    )
    const existing = existingRows[0]
    if (existing) {
      if (shouldReplaceProgress(existing, row)) {
        await supabaseAdmin(env, 'PATCH', `/rest/v1/user_progress?user_id=eq.${encodeURIComponent(auth.user.id)}&item_id=eq.${encodeURIComponent(itemId)}`, progressPatch(row, auth.user.id, deviceId))
      }
      if (readString(existing, 'device_id') !== deviceId) {
        await supabaseAdmin(env, 'DELETE', `/rest/v1/user_progress?device_id=eq.${encodeURIComponent(deviceId)}&item_id=eq.${encodeURIComponent(itemId)}`).catch(() => undefined)
      }
    } else {
      await supabaseAdmin(env, 'PATCH', `/rest/v1/user_progress?device_id=eq.${encodeURIComponent(deviceId)}&item_id=eq.${encodeURIComponent(itemId)}`, {
        user_id: auth.user.id,
        updated_at: new Date().toISOString(),
      })
    }
    progressMerged += 1
  }

  const corrections = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/sentence_correction_history?select=id&device_id=eq.${encodeURIComponent(deviceId)}`,
  )
  await supabaseAdmin(env, 'PATCH', `/rest/v1/sentence_correction_history?device_id=eq.${encodeURIComponent(deviceId)}`, {
    user_id: auth.user.id,
  }).catch(() => undefined)

  const interactions = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/ai_interaction_history?select=id&device_id=eq.${encodeURIComponent(deviceId)}`,
  ).catch(() => [])
  await supabaseAdmin(env, 'PATCH', `/rest/v1/ai_interaction_history?device_id=eq.${encodeURIComponent(deviceId)}`, {
    user_id: auth.user.id,
  }).catch(() => undefined)

  const writingStats = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/writing_practice_stats?select=*&device_id=eq.${encodeURIComponent(deviceId)}`,
  ).catch(() => [])
  let writingStatsMerged = 0
  for (const row of writingStats) {
    const itemId = readString(row, 'item_id')
    if (!itemId) continue
    const existingRows = await supabaseAdmin<Record<string, unknown>[]>(
      env,
      'GET',
      `/rest/v1/writing_practice_stats?select=*&user_id=eq.${encodeURIComponent(auth.user.id)}&item_id=eq.${encodeURIComponent(itemId)}&limit=1`,
    ).catch(() => [])
    const existing = existingRows[0]
    if (existing) {
      await supabaseAdmin(env, 'PATCH', `/rest/v1/writing_practice_stats?id=eq.${encodeURIComponent(readString(existing, 'id'))}`, {
        completed_count: readNumber(existing, 'completed_count') + readNumber(row, 'completed_count'),
        last_practiced_at: maxIsoDate(readString(existing, 'last_practiced_at'), readString(row, 'last_practiced_at')),
        updated_at: new Date().toISOString(),
      }).catch(() => undefined)
      await supabaseAdmin(env, 'DELETE', `/rest/v1/writing_practice_stats?id=eq.${encodeURIComponent(readString(row, 'id'))}`).catch(() => undefined)
    } else {
      await supabaseAdmin(env, 'PATCH', `/rest/v1/writing_practice_stats?id=eq.${encodeURIComponent(readString(row, 'id'))}`, {
        user_id: auth.user.id,
        updated_at: new Date().toISOString(),
      }).catch(() => undefined)
    }
    writingStatsMerged += 1
  }

  const writingSubmissions = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/writing_practice_submissions?select=id&device_id=eq.${encodeURIComponent(deviceId)}`,
  ).catch(() => [])
  await supabaseAdmin(env, 'PATCH', `/rest/v1/writing_practice_submissions?device_id=eq.${encodeURIComponent(deviceId)}`, {
    user_id: auth.user.id,
  }).catch(() => undefined)

  return json({
    ok: true,
    merged: {
      progress: progressMerged,
      corrections: corrections.length,
      aiInteractions: interactions.length,
      writingStats: writingStatsMerged,
      writingSubmissions: writingSubmissions.length,
    },
  })
}

async function handleAiExplain(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | { kind?: 'vocab' | 'sentence' | 'grammar' | 'linguistic'; text?: string; context?: string; model?: GatewayModel; reasoningEffort?: ReasoningEffort; deviceId?: string }
    | null

  const text = body?.text?.trim()
  if (!text) {
    return json({ error: { message: 'text is required' } }, 400)
  }

  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const payload = { kind: body?.kind ?? 'vocab', text, context: body?.context ?? '', model, reasoningEffort }
  const cacheKey = await hashPayload(`ai-explain-${cacheSchemaVersion}`, payload)
  const stored = await readAiCache(env, cacheKey)
  if (stored) {
    await recordAiInteraction(request, env, {
      deviceId: body?.deviceId,
      cacheKey,
      cacheKind: `explain:${payload.kind}`,
      model,
      resultPayload: stored,
    })
    return json(stored)
  }

  const edgeCacheKey = await buildCacheKey(`ai-explain-${cacheSchemaVersion}`, JSON.stringify(payload))
  const cache = (caches as unknown as { default: Cache }).default
  const cached = await cache.match(edgeCacheKey)
  if (cached) return cached

  const explanation = payload.kind === 'linguistic'
    ? await callAiGateway(
        env,
        model,
        '你是 Anime Japanese Lab 的日语语言学训练老师。必须只使用简体中文回答。只基于用户给出的题目、选项、用户答案、正确答案和解释分析，不引用外部作品，不编造剧情，不把 TTS 当作音系学标准发音依据。',
        `题目材料：\n${payload.context}\n\n请严格按这些栏目回答：${explainSections(payload.kind).join('、')}。重点解释用户为什么被错误选项吸引、错误选项具体错在哪里、正确答案为什么成立、下次遇到同类语言现象怎么判断。`,
        { maxTokens: 1400, temperature: 0.2, reasoningEffort },
      )
    : await callAiGateway(
        env,
        model,
        '你是 Anime Japanese Lab 的日语老师。必须只使用简体中文回答。只基于用户给出的日文文本和上下文讲解，不引用外部作品，不编造例句，禁止拆字或词源解释。只讲现代日语学习用法、语气、现实可用性和注意点。',
        `类型：${payload.kind}\n文本：${text}\n上下文：${payload.context}\n\n请用简体中文做日语学习精讲，并严格按这些栏目输出：${explainSections(payload.kind).join('、')}。不要解释词源。`,
        { maxTokens: 1200, temperature: 0.2, reasoningEffort },
      )
  const result = structuredTextResult('AI 精讲', explanation, explainSections(payload.kind))
  await writeAiCache(env, {
    cacheKey,
    cacheKind: `explain:${payload.kind}`,
    model,
    inputPayload: payload,
    resultPayload: result,
  })
  await recordAiInteraction(request, env, {
    deviceId: body?.deviceId,
    cacheKey,
    cacheKind: `explain:${payload.kind}`,
    model,
    resultPayload: result,
  })
  const response = json(result)
  response.headers.set('Cache-Control', 'public, max-age=604800')
  await cache.put(edgeCacheKey, response.clone())
  return response
}

async function handleFurigana(request: Request, env: Env, url: URL) {
  const input = request.method === 'GET'
    ? {
        targetType: url.searchParams.get('targetType') ?? undefined,
        targetId: url.searchParams.get('targetId') ?? undefined,
        text: url.searchParams.get('text') ?? undefined,
      }
    : (await request.json().catch(() => null)) as { targetType?: string; targetId?: string; text?: string } | null

  if (!input) return json({ error: { message: 'Invalid JSON' } }, 400)
  const targetType = normalizeFuriganaTargetType(input.targetType)
  const targetId = input.targetId?.trim() ?? ''
  const text = input.text ?? ''
  if (!targetId) return json({ error: { message: 'targetId is required' } }, 400)
  if (!text.trim()) return json({ error: { message: 'text is required' } }, 400)
  if (text.length > 500) return json({ error: { message: 'text is too long' } }, 400)

  const textHash = await sha256Hex(text)
  const cacheInput = { feature: 'furigana', targetType, targetId, textHash }
  const cacheKey = await hashPayload(`furigana-${cacheSchemaVersion}`, cacheInput)
  const cached = await readAiCache(env, cacheKey)
  const cachedSegments = readRubySegments(cached)
  if (cachedSegments && validateRubySegments(cachedSegments, text).ok) {
    return json(cached)
  }
  if (request.method === 'GET') {
    return json({ ruby_segments: null, cached: false })
  }

  const rawText = await callAiGateway(
    env,
    defaultGatewayModel,
    [
      'You generate Japanese furigana for language learners.',
      'Return only strict JSON. No markdown, no explanation, no translation.',
      'Do not rewrite, omit, add, normalize, or translate the original Japanese text.',
      'Only annotate text spans that contain kanji. Do not add readings to pure kana, punctuation, spaces, or Latin text.',
    ].join(' '),
    [
      'Return a JSON array of segments for this exact Japanese sentence.',
      'Each segment must be {"text":"原文片段"} or {"text":"含汉字片段","reading":"かな"}.',
      'reading may contain only hiragana, katakana, or the long vowel mark ー.',
      'Concatenating every text field must equal the original sentence exactly.',
      `Original sentence: ${text}`,
    ].join('\n'),
    { maxTokens: 700, temperature: 0, reasoningEffort: 'low' },
  )
  const parsedSegments = parseRubySegments(rawText)
  const validation = validateRubySegments(parsedSegments, text)
  if (!validation.ok) {
    return json({ error: { message: validation.message } }, 422)
  }

  const result = {
    ruby_segments: parsedSegments,
    cachedAt: new Date().toISOString(),
  }
  await writeAiCache(env, {
    cacheKey,
    cacheKind: 'furigana',
    model: defaultGatewayModel,
    sourceId: `${targetType}:${targetId}`,
    inputPayload: {
      ...cacheInput,
      text,
    },
    resultPayload: result,
    mustPersist: true,
  })
  return json(result)
}

function explainSections(kind: string) {
  if (kind === 'linguistic') return ['你为什么会选错', '错误选项的问题', '正确答案为什么成立', '下次判断方法']
  if (kind === 'grammar') return ['核心意思', '这句里的用法', '语气', '现实可用性', '相近表达', '学习建议']
  if (kind === 'sentence') return ['字面意思', '句子结构', '语气', '现实可用性', '跟读重点', '学习建议']
  return ['意思', '词性/用法', '语气', '现实可用性', '容易误解', '学习建议']
}

async function handleSentenceDeepDive(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | {
        workSlug?: string
        episode?: number
        lineNo?: number
        jaText?: string
        zhText?: string
        model?: GatewayModel
        reasoningEffort?: ReasoningEffort
        deviceId?: string
      }
    | null

  const jaText = body?.jaText?.trim()
  if (!jaText) return json({ error: { message: 'jaText is required' } }, 400)

  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const payload = {
    workSlug: body?.workSlug ?? 'k-on',
    episode: body?.episode ?? 1,
    lineNo: body?.lineNo ?? 0,
    jaText,
    zhText: body?.zhText ?? '',
    model,
    reasoningEffort,
  }
  const cacheKey = await hashPayload(`sentence-deep-dive-${cacheSchemaVersion}`, payload)
  const cached = await readAiCache(env, cacheKey)
  if (cached) {
    await recordAiInteraction(request, env, {
      deviceId: body?.deviceId,
      cacheKey,
      cacheKind: 'sentence_deep_dive',
      model,
      sourceId: `${payload.workSlug}:ep${payload.episode}:line${payload.lineNo}`,
      workSlug: payload.workSlug,
      episode: payload.episode,
      resultPayload: cached,
    })
    return json(cached)
  }

  const text = await callAiGateway(
    env,
    model,
    '你是 Anime Japanese Lab 的日语语言学精读老师。只使用简体中文，必须只基于给定台词和上下文分析。输出要结构化，不能泛泛翻译。',
    `台词：${jaText}\n中文对照：${payload.zhText}\n作品：${payload.workSlug} EP${payload.episode} line ${payload.lineNo}\n\n请严格按这些栏目精读：字面意思、词法拆解、句法结构、助词说明、句末语气、角色心理、现实可用性、相近表达对比。每栏给出简洁但具体的说明。`,
    { maxTokens: 1800, temperature: 0.2, reasoningEffort },
  )
  const result = structuredTextResult('单句精读', text, [
    '字面意思',
    '词法拆解',
    '句法结构',
    '助词说明',
    '句末语气',
    '角色心理',
    '现实可用性',
    '相近表达对比',
  ])
  await writeAiCache(env, {
    cacheKey,
    cacheKind: 'sentence_deep_dive',
    model,
    sourceId: `${payload.workSlug}:ep${payload.episode}:line${payload.lineNo}`,
    workSlug: payload.workSlug,
    episode: payload.episode,
    inputPayload: payload,
    resultPayload: result,
  })
  await recordAiInteraction(request, env, {
    deviceId: body?.deviceId,
    cacheKey,
    cacheKind: 'sentence_deep_dive',
    model,
    sourceId: `${payload.workSlug}:ep${payload.episode}:line${payload.lineNo}`,
    workSlug: payload.workSlug,
    episode: payload.episode,
    resultPayload: result,
  })
  return json(result)
}

async function handleCharacterProfile(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | { workSlug?: string; characterKey?: string; characterName?: string; model?: GatewayModel; reasoningEffort?: ReasoningEffort; regenerate?: boolean }
    | null
  const workSlug = body?.workSlug ?? 'k-on'
  const ragWorkSlug = normalizeRagWorkSlug(workSlug)
  const characterKey = body?.characterKey ?? 'yui'
  const characterName = body?.characterName ?? '唯'
  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)

  if (!body?.regenerate) {
    const existing = await supabase<Record<string, unknown>[]>(
      env,
      `/rest/v1/character_language_profiles?select=result_payload,source_payload,updated_at&work_slug=eq.${encodeURIComponent(workSlug)}&character_key=eq.${encodeURIComponent(characterKey)}&model=eq.${encodeURIComponent(model)}&limit=1`,
    ).catch(() => [])
    if (existing[0]?.result_payload) return json(withProfileMetadata(existing[0].result_payload, existing[0], model))
  }

  const query = `${characterName} 口癖 语气 角色心理 日文台词`
  const vector = await embedQuery(env.AI, query)
  const result = await env.VECTORIZE.query(vector, {
    topK: 5,
    returnMetadata: 'all',
    filter: { language: 'ja', work: ragWorkSlug },
  })
  const sources = (result.matches ?? []).map((match) => {
    const metadata = (match.metadata ?? {}) as Record<string, unknown>
    return {
    id: match.id,
    score: match.score,
      work: readString(metadata, 'work'),
      episode: readNumber(metadata, 'episode'),
      chunkNo: readNumber(metadata, 'chunk_no'),
      startTime: readString(metadata, 'start_time'),
      endTime: readString(metadata, 'end_time'),
      text: readString(metadata, 'text'),
    }
  })
  const text = await callAiGateway(
    env,
    model,
    '你是日语角色语言画像分析助手。只基于给定检索结果做谨慎画像，必须标注这是第一版占位分析，不要声称角色识别完全准确。',
    `角色：${characterName}\n作品：${workSlug}\nRAG work：${ragWorkSlug}\n检索来源 JSON：${JSON.stringify(sources).slice(0, 9000)}\n\n请输出：常见口癖、句末倾向、礼貌度、情绪表达、吐槽/被吐槽模式、典型场景、学习价值、局限。`,
    { maxTokens: 1600, temperature: 0.2, reasoningEffort },
  )
  const profile = {
    ...structuredTextResult(`${characterName} 的语言画像`, text, [
    '常见口癖',
    '句末倾向',
    '礼貌度',
    '情绪表达',
    '吐槽/被吐槽模式',
    '典型场景',
    '学习价值',
    '局限',
    ]),
    model,
    cachedAt: new Date().toISOString(),
    sources,
  }
  await supabaseWrite(env, '/rest/v1/character_language_profiles?on_conflict=work_slug,character_key,model', {
    work_slug: workSlug,
    character_key: characterKey,
    model,
    result_payload: profile,
    source_payload: { sources },
    updated_at: new Date().toISOString(),
  })
  return json(profile)
}

function withProfileMetadata(resultPayload: unknown, row: Record<string, unknown>, model: GatewayModel) {
  const result = typeof resultPayload === 'object' && resultPayload ? resultPayload as Record<string, unknown> : {}
  const sourcePayload = typeof row.source_payload === 'object' && row.source_payload ? row.source_payload as Record<string, unknown> : {}
  return {
    ...result,
    model: typeof result.model === 'string' ? result.model : model,
    cachedAt: readString(row, 'updated_at'),
    sources: Array.isArray(result.sources) ? result.sources : Array.isArray(sourcePayload.sources) ? sourcePayload.sources : [],
  }
}

async function handleSentenceCorrection(request: Request, env: Env) {
  const auth = await getAuthContext(request, env)
  const body = (await request.json().catch(() => null)) as
    | {
        deviceId?: string
        targetType?: 'vocab' | 'grammar' | 'free'
        targetId?: string
        targetLabel?: string
        sentence?: string
        workSlug?: string
        episode?: number
        model?: GatewayModel
        reasoningEffort?: ReasoningEffort
      }
    | null
  const sentence = body?.sentence?.trim()
  if (!sentence) return json({ error: { message: 'sentence is required' } }, 400)
  if (body?.deviceId && !isValidDeviceId(body.deviceId)) return json({ error: { message: 'deviceId is invalid' } }, 400)
  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const payload = {
    targetType: body?.targetType ?? 'free',
    targetId: body?.targetId ?? '',
    targetLabel: body?.targetLabel ?? '',
    sentence,
    model,
    reasoningEffort,
  }
  const cacheKey = await hashPayload(`sentence-correction-${cacheSchemaVersion}`, payload)
  const cached = await readAiCache(env, cacheKey)
  if (cached) {
    await writeCorrectionHistory(env, auth.user, body, payload, model, sentence, cacheKey, cached)
    return json(cached)
  }

  const text = await callAiGateway(
    env,
    model,
    '你是日语造句批改老师。只使用简体中文。批改要具体、直接，指出自然度和语气，不要鼓励空话。',
    `目标类型：${payload.targetType}\n目标词/语法：${payload.targetLabel || payload.targetId || '自由造句'}\n用户造句：${sentence}\n\n请返回：语法是否正确、自然度、语气是否合适、更自然改写、用法提醒、评分（100分制）。`,
    { maxTokens: 1400, temperature: 0.2, reasoningEffort },
  )
  const correction = structuredTextResult('造句批改', text, ['语法是否正确', '自然度', '语气', '改写', '用法提醒', '评分'])
  await writeAiCache(env, {
    cacheKey,
    cacheKind: 'sentence_correction',
    model,
    sourceId: payload.targetId,
    workSlug: body?.workSlug,
    episode: body?.episode,
    inputPayload: payload,
    resultPayload: correction,
  })
  await writeCorrectionHistory(env, auth.user, body, payload, model, sentence, cacheKey, correction)
  return json(correction)
}

async function buildCacheKey(prefix: string, value: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  const hash = [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
  return new Request(`https://anime-japanese-lab.cache/${prefix}/${hash}`)
}

async function handleRagSearch(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | { query?: string; workSlug?: string; episode?: number; topK?: number; model?: GatewayModel; reasoningEffort?: ReasoningEffort; deviceId?: string }
    | null

  const query = body?.query?.trim()
  if (!query) {
    return json({ error: { message: 'query is required' } }, 400)
  }
  const workSlug = body?.workSlug?.trim()
  if (!workSlug) {
    return json({ error: { message: 'workSlug is required' } }, 400)
  }
  if (!isRagWorkSlug(workSlug)) {
    return json({ error: { message: 'workSlug must be one of: rezero, k-on' } }, 400)
  }
  const episode = typeof body?.episode === 'number' && Number.isFinite(body.episode) ? body.episode : undefined
  const topK = Math.min(Math.max(body?.topK ?? 5, 1), 50)

  const result = await searchSubtitleChunks(env, {
    query,
    work: workSlug,
    episode,
    topK,
  })

  const sources = await Promise.all(
    (result.matches ?? [])
      .filter((match) => readNumber((match.metadata ?? {}) as Record<string, unknown>, 'chunk_no') > 0)
      .map(async (match) => {
      const metadata = (match.metadata ?? {}) as Record<string, unknown>
      const work = readString(metadata, 'work')
      const episode = readNumber(metadata, 'episode')
      const chunkNo = readNumber(metadata, 'chunk_no')
      const chunkRows = await supabase<Record<string, unknown>[]>(
        env,
        `/rest/v1/subtitle_chunks?select=*&work_slug=eq.${encodeURIComponent(work)}&episode=eq.${episode}&chunk_no=eq.${chunkNo}&limit=1`,
      )
      const chunk = chunkRows[0]
      const startLine = chunk ? readNumber(chunk, 'start_line') : 0
      const endLine = chunk ? readNumber(chunk, 'end_line') : 0
      const lines =
        startLine > 0 && endLine > 0
          ? await supabase<Record<string, unknown>[]>(
              env,
              `/rest/v1/subtitle_lines?select=line_no,start_time,end_time,ja_text,zh_text&work_slug=eq.${encodeURIComponent(work)}&episode=eq.${episode}&line_no=gte.${startLine}&line_no=lte.${endLine}&order=line_no.asc&limit=20`,
            )
          : []

      return {
        id: match.id,
        score: match.score,
        work,
        episode,
        chunkNo,
        startTime: readString(metadata, 'start_time'),
        endTime: readString(metadata, 'end_time'),
        text: readString(metadata, 'text'),
        lines: lines.map(mapSubtitle),
      }
    }),
  )

  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const cacheKey = await hashPayload(`rag-air-${cacheSchemaVersion}`, {
    query,
    workSlug,
    episode: episode ?? 0,
    topK,
    model,
    reasoningEffort,
  })
  const cachedAnalysis = await readAiCache(env, cacheKey)
  const analysis = cachedAnalysis ?? (await analyzeAir(env, query, sources, model, reasoningEffort))
  if (!cachedAnalysis) {
    await writeAiCache(env, {
      cacheKey,
      cacheKind: 'rag_air',
      model,
      workSlug,
      episode,
      inputPayload: { query, sources: sources.map((source) => ({ id: source.id, score: source.score })) },
      resultPayload: analysis,
    })
  }
  await recordAiInteraction(request, env, {
    deviceId: body?.deviceId,
    cacheKey,
    cacheKind: 'rag_air',
    model,
    workSlug,
    episode,
    resultPayload: analysis,
  })

  return json({
    query,
    sources,
    analysis,
  })
}

function isRagWorkSlug(value: string): value is RagWorkSlug {
  return ragWorkSlugs.has(value as RagWorkSlug)
}

function normalizeRagWorkSlug(value: string): RagWorkSlug {
  return value === 're-zero' || value === 'rezero' ? 'rezero' : 'k-on'
}

async function searchSubtitleChunks(env: Env, params: { query: string; work: RagWorkSlug; episode?: number; topK?: number }) {
  const { query, work, episode, topK = 5 } = params
  const filter: Record<string, string | number> = {
    work,
    language: 'ja',
  }
  if (episode != null) filter.episode = episode

  const init: RequestInit = {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, topK, filter }),
  }
  const workerUrl = (env.SUBTITLE_RAG_WORKER_URL ?? SUBTITLE_RAG_WORKER_URL).replace(/\/+$/u, '')
  const response = env.SUBTITLE_RAG_WORKER
    ? await env.SUBTITLE_RAG_WORKER.fetch(new Request('https://subtitle-rag-worker/query', init))
    : await fetch(`${workerUrl}/query`, init)
  const responseText = await response.text()
  const data = tryParseJson<SubtitleRagQueryResponse>(responseText)
  if (!response.ok || !data?.ok) {
    throw new Error(data?.error ?? `RAG query failed with HTTP ${response.status}: ${responseText.slice(0, 200)}`)
  }
  return { matches: data.matches ?? [] }
}

function tryParseJson<T>(text: string): T | null {
  try {
    return JSON.parse(text) as T
  } catch {
    return null
  }
}

function normalizeFuriganaTargetType(value: unknown) {
  if (typeof value !== 'string') return 'learning_sentence'
  const normalized = value.trim()
  if (!/^[a-z][a-z0-9_-]{0,63}$/u.test(normalized)) return 'learning_sentence'
  return normalized
}

function parseRubySegments(text: string): RubySegment[] {
  const cleaned = extractJsonArrayText(text)
    .trim()
    .replace(/^```(?:json)?\s*/u, '')
    .replace(/\s*```$/u, '')
    .trim()
  const parsed = tryParseJson<unknown>(cleaned)
  if (!Array.isArray(parsed)) return []
  return parsed.map((item) => {
    if (!item || typeof item !== 'object') return null
    const row = item as Record<string, unknown>
    const segmentText = typeof row.text === 'string' ? row.text : ''
    const reading = typeof row.reading === 'string' ? row.reading : undefined
    if (!segmentText) return null
    return reading ? { text: segmentText, reading } : { text: segmentText }
  }).filter((segment): segment is RubySegment => Boolean(segment))
}

function extractJsonArrayText(text: string) {
  const trimmed = text.trim()
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) return trimmed

  const start = trimmed.indexOf('[')
  const end = trimmed.lastIndexOf(']')
  if (start >= 0 && end > start) return trimmed.slice(start, end + 1)
  return trimmed
}

function readRubySegments(input: unknown): RubySegment[] | null {
  if (!input || typeof input !== 'object') return null
  const payload = input as Record<string, unknown>
  const segments = Array.isArray(payload.ruby_segments) ? payload.ruby_segments : []
  if (segments.length === 0) return null
  return segments.map((item) => {
    if (!item || typeof item !== 'object') return null
    const row = item as Record<string, unknown>
    const text = typeof row.text === 'string' ? row.text : ''
    const reading = typeof row.reading === 'string' ? row.reading : undefined
    if (!text) return null
    return reading ? { text, reading } : { text }
  }).filter((segment): segment is RubySegment => Boolean(segment))
}

function validateRubySegments(segments: RubySegment[], originalText: string): { ok: true } | { ok: false; message: string } {
  if (segments.length === 0) return { ok: false, message: 'ruby_segments is empty' }
  if (segments.map((segment) => segment.text).join('') !== originalText) {
    return { ok: false, message: 'ruby_segments must preserve the original sentence exactly' }
  }

  for (const segment of segments) {
    if (!segment.text) return { ok: false, message: 'segment text is required' }
    if (!segment.reading) continue
    if (!/^[\u3041-\u3096\u30A1-\u30FAー]+$/u.test(segment.reading)) {
      return { ok: false, message: 'reading must be kana only' }
    }
    if (!hasJapaneseKanji(segment.text)) {
      return { ok: false, message: 'pure kana or non-kanji text must not have reading' }
    }
  }

  return { ok: true }
}

function hasJapaneseKanji(value: string) {
  return /[\p{Script=Han}々〆ヵヶ]/u.test(value)
}

async function handleRagSuggestTrainingQuery(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | { workSlug?: string; model?: GatewayModel; reasoningEffort?: ReasoningEffort; deviceId?: string }
    | null
  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const workSlug = body?.workSlug?.trim()
  if (!workSlug) return json({ error: { message: 'workSlug is required' } }, 400)
  if (!isRagWorkSlug(workSlug)) return json({ error: { message: 'workSlug must be one of: rezero, k-on' } }, 400)
  const fallback = {
    query: '找一个角色没有直接说出口、但语气或反应里有潜台词的日常对话场景。',
    focus: '潜台词判断',
    reason: '适合作为读空气训练：学习者需要从语气、回应和上下文判断真实态度。',
  }

  try {
    const text = await callAiGateway(
      env,
      model,
      '你是 Anime Japanese Lab 的读空气训练策划。你只负责决定检索意图，不生成题目。必须输出严格 JSON，不要 Markdown，不要代码块。',
      `作品：${workSlug}

请为 RAG 字幕检索决定一个适合日语读空气训练的检索意图。不要让用户自己想关键词。
JSON 字段必须为：
{
  "query": "给向量检索用的中文自然语言问题，聚焦可训练片段",
  "focus": "训练目标，8个汉字以内",
  "reason": "为什么这个方向适合练习，一句话"
}

训练目标可从这些方向选择或自行概括：潜台词、委婉拒绝、句尾语气、自然回应、尴尬缓和、吐槽与被吐槽、关系距离、情绪变化。`,
      { maxTokens: 500, temperature: 0.7, reasoningEffort },
    )
    const parsed = parseJsonObject(text)
    const query = readString(parsed, 'query')
    return json({
      query: query || fallback.query,
      focus: readString(parsed, 'focus') || fallback.focus,
      reason: readString(parsed, 'reason') || fallback.reason,
    })
  } catch (error) {
    console.error(error)
    return json(fallback)
  }
}

async function handleRagGenerateQuestion(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | { source?: unknown; model?: GatewayModel; reasoningEffort?: ReasoningEffort; deviceId?: string }
    | null
  const source = body?.source as Record<string, unknown> | undefined
  if (!source) return json({ error: { message: 'source is required' } }, 400)

  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const context = JSON.stringify(source).slice(0, 9000)
  const text = await callAiGateway(
    env,
    model,
    '你是 Anime Japanese Lab 的读空气练习题生成器。只基于给定字幕场景生成题目，不编造剧情。必须输出严格 JSON，不要 Markdown，不要代码块。',
    `字幕场景 JSON：${context}

请生成 1 道读空气选择题，JSON 字段必须为：
{
  "sceneJa": "场景日文原文，保留换行",
  "sceneZh": "中文参考，保留换行",
  "question": "问题",
  "options": ["选项A", "选项B", "选项C", "选项D"],
  "answer": "正确选项文本，必须完全等于 options 之一",
  "explanation": "简短解释",
  "evidence": ["语气关键词或判断依据"],
  "targetLineNo": 123,
  "sceneLines": [{ "lineNo": 123, "speaker": "可空", "jaText": "日文台词", "zhText": "中文参考", "isTarget": true }],
  "source": { "work": "", "episode": 1, "chunkNo": 1, "startTime": "", "endTime": "" }
}

题目要考角色真实态度、潜台词、委婉程度、句尾语气、自然回应或翻译细微差别之一。`,
    { maxTokens: 1500, temperature: 0.3, reasoningEffort },
  )
  const candidate = normalizeAirQuestionCandidate(parseJsonObject(text), source)
  await recordAiInteraction(request, env, {
    deviceId: body?.deviceId,
    cacheKey: await hashPayload(`rag-air-question-${cacheSchemaVersion}`, { source, model, reasoningEffort }),
    cacheKind: 'rag_air',
    model,
    workSlug: readString(source, 'work'),
    episode: readNumber(source, 'episode'),
    sourceId: readString(source, 'id'),
    resultPayload: candidate,
  }).catch(() => undefined)
  return json(candidate)
}

async function handleRagGenerateQuestions(request: Request, env: Env) {
  const body = (await request.json().catch(() => null)) as
    | { sources?: unknown[]; model?: GatewayModel; reasoningEffort?: ReasoningEffort; deviceId?: string }
    | null
  const sources = Array.isArray(body?.sources)
    ? body.sources.filter((source): source is Record<string, unknown> => Boolean(source) && typeof source === 'object').slice(0, 5)
    : []
  if (sources.length === 0) return json({ error: { message: 'sources is required' } }, 400)

  const model = normalizeGatewayModel(body?.model)
  const reasoningEffort = normalizeReasoningEffort(body?.reasoningEffort)
  const context = JSON.stringify(sources).slice(0, 18000)
  const text = await callAiGateway(
    env,
    model,
    '你是 Anime Japanese Lab 的读空气练习题生成器。只基于给定字幕场景生成题目，不编造剧情。必须输出严格 JSON 数组，不要 Markdown，不要代码块。',
    `字幕场景数组 JSON：${context}

请为每个字幕场景各生成 1 道读空气选择题，按输入顺序输出 JSON 数组。数组每一项字段必须为：
{
  "sceneJa": "场景日文原文，保留换行",
  "sceneZh": "中文参考，保留换行",
  "question": "问题，必须指向某一句目标台词",
  "options": ["选项A", "选项B", "选项C", "选项D"],
  "answer": "正确选项文本，必须完全等于 options 之一",
  "explanation": "简短解释",
  "evidence": ["语气关键词或判断依据"],
  "targetLineNo": 123,
  "sceneLines": [{ "lineNo": 123, "speaker": "可空", "jaText": "日文台词", "zhText": "中文参考", "isTarget": true }],
  "source": { "work": "", "episode": 1, "chunkNo": 1, "startTime": "", "endTime": "" }
}

要求：
1. 每题必须高亮一个目标句，targetLineNo 要对应 sceneLines 里的 lineNo。
2. sceneLines 优先使用输入 lines，保留 lineNo/jaText/zhText；speaker 不确定可为空。
3. 题目要考角色真实态度、潜台词、委婉程度、句尾语气、自然回应或翻译细微差别之一。
4. 不要合并多个场景；不要输出解释性文字，只输出 JSON 数组。`,
    { maxTokens: 5200, temperature: 0.3, reasoningEffort },
  )
  const parsed = parseJsonArray(text)
  const candidates = sources.map((source, index) => normalizeAirQuestionCandidate(
    (parsed[index] && typeof parsed[index] === 'object' ? parsed[index] : {}) as Record<string, unknown>,
    source,
  ))

  await recordAiInteraction(request, env, {
    deviceId: body?.deviceId,
    cacheKey: await hashPayload(`rag-air-questions-${cacheSchemaVersion}`, { sources, model, reasoningEffort }),
    cacheKind: 'rag_air',
    model,
    workSlug: readString(sources[0], 'work'),
    episode: readNumber(sources[0], 'episode'),
    sourceId: sources.map((source) => readString(source, 'id')).join(',').slice(0, 500),
    resultPayload: candidates,
  }).catch(() => undefined)

  return json({ candidates })
}

async function handleProgressList(request: Request, env: Env, url: URL) {
  const scope = await getDataScope(request, env, url.searchParams.get('deviceId'))
  if (scope instanceof Response) return scope
  const rows = await supabase<Record<string, unknown>[]>(
    env,
    `/rest/v1/user_progress?select=*&${scope.query}&order=last_reviewed_at.desc&limit=500`,
  )
  return json(rows.map(mapProgress))
}

async function handleProgressUpsert(request: Request, env: Env) {
  const auth = await getAuthContext(request, env)
  const body = (await request.json().catch(() => null)) as
    | {
        deviceId?: string
        itemId?: string
        itemType?: string
        workSlug?: string
        episode?: number
        state?: ReviewState
        payload?: Record<string, unknown>
      }
    | null
  if (!body?.deviceId || !body.itemId || !body.state) {
    return json({ error: { message: 'deviceId, itemId and state are required' } }, 400)
  }
  if (!isValidDeviceId(body.deviceId)) return json({ error: { message: 'deviceId is invalid' } }, 400)
  if (!allowedReviewStates.has(body.state)) return json({ error: { message: 'state is invalid' } }, 400)
  const itemType = normalizeProgressItemType(body.itemType)
  if (!itemType) return json({ error: { message: 'itemType is invalid' } }, 400)
  const nextReviewOn = nextReviewDate(body.state)
  const row = {
    device_id: body.deviceId,
    user_id: auth.user?.id ?? null,
    item_id: body.itemId,
    item_type: itemType,
    work_slug: body.workSlug ?? null,
    episode: body.episode ?? null,
    state: body.state,
    next_review_on: nextReviewOn,
    last_reviewed_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
    payload: body.payload ?? {},
  }
  await supabaseWrite(
    env,
    auth.user ? '/rest/v1/user_progress?on_conflict=user_id,item_id' : '/rest/v1/user_progress?on_conflict=device_id,item_id',
    row,
  )
  return json(mapProgress(row))
}

async function handleWritingStats(request: Request, env: Env, url: URL) {
  const scope = await getDataScope(request, env, url.searchParams.get('deviceId'))
  if (scope instanceof Response) return scope
  const rows = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/writing_practice_stats?select=*&${scope.query}&order=last_practiced_at.desc&limit=500`,
  ).catch(() => [])
  return json(rows.map(mapWritingStats))
}

async function handleWritingSubmit(request: Request, env: Env) {
  const auth = await getAuthContext(request, env)
  const body = (await request.json().catch(() => null)) as
    | {
        deviceId?: string
        itemId?: string
        itemText?: string
        itemType?: string
        reading?: string
        romaji?: string
        meaningZh?: string
        workSlug?: string
        episode?: number
        passed?: boolean
        reason?: WritingSubmitReason
        metrics?: Record<string, unknown>
      }
    | null

  if (!body?.deviceId || !body.itemId || !body.itemText || typeof body.passed !== 'boolean') {
    return json({ error: { message: 'deviceId, itemId, itemText and passed are required' } }, 400)
  }
  if (!isValidDeviceId(body.deviceId)) return json({ error: { message: 'deviceId is invalid' } }, 400)
  if (body.itemType !== 'vocab') return json({ error: { message: 'itemType is invalid' } }, 400)

  const now = new Date().toISOString()
  const scopeId = auth.user?.id ?? body.deviceId
  const statsId = `${scopeId}:${body.itemId}`
  const metrics = normalizeWritingMetrics(body.metrics)
  const reason = normalizeWritingReason(body.reason, body.passed)

  await supabaseAdmin(env, 'POST', '/rest/v1/writing_practice_submissions?on_conflict=id', {
    id: crypto.randomUUID(),
    device_id: body.deviceId,
    user_id: auth.user?.id ?? null,
    item_id: body.itemId,
    item_text: body.itemText,
    item_type: body.itemType,
    work_slug: body.workSlug ?? null,
    episode: body.episode ?? null,
    passed: body.passed,
    failure_reason: body.passed ? null : reason,
    coverage_ratio: metrics.coverageRatio,
    off_target_ratio: metrics.offTargetRatio,
    stroke_length: metrics.strokeLength,
    duration_ms: metrics.durationMs,
    created_at: now,
  }).catch(() => undefined)

  if (!body.passed) {
    return json({
      itemId: body.itemId,
      completedCount: 0,
      lastPracticedAt: undefined,
    })
  }

  const existingRows = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/writing_practice_stats?select=*&id=eq.${encodeURIComponent(statsId)}&limit=1`,
  ).catch(() => [])
  const completedCount = readNumber(existingRows[0] ?? {}, 'completed_count') + 1
  const statsRow = {
    id: statsId,
    device_id: body.deviceId,
    user_id: auth.user?.id ?? null,
    item_id: body.itemId,
    item_text: body.itemText,
    item_type: body.itemType,
    reading: body.reading ?? null,
    romaji: body.romaji ?? null,
    meaning_zh: body.meaningZh ?? null,
    work_slug: body.workSlug ?? null,
    episode: body.episode ?? null,
    completed_count: completedCount,
    last_practiced_at: now,
    updated_at: now,
  }

  await supabaseAdmin(env, 'POST', '/rest/v1/writing_practice_stats?on_conflict=id', statsRow).catch(() => undefined)
  return json(mapWritingStats(statsRow))
}

async function handleTodayReview(request: Request, env: Env, url: URL) {
  const scope = await getDataScope(request, env, url.searchParams.get('deviceId'))
  if (scope instanceof Response) return scope
  const rows = await supabase<Record<string, unknown>[]>(
    env,
    `/rest/v1/user_progress?select=*&${scope.query}&state=in.(fuzzy,unknown,bad,ok)&order=next_review_on.asc&limit=80`,
  )
  const tasks = rows.map(mapReviewTask).sort(compareReviewTasks).slice(0, 30)
  const today = new Date().toISOString().slice(0, 10)
  return json({
    generatedAt: new Date().toISOString(),
    dueDate: today,
    tasks,
    groups: {
      vocab: tasks.filter((task) => task.itemType === 'vocab').length,
      grammar: tasks.filter((task) => task.itemType === 'grammar').length,
      sentence: tasks.filter((task) => task.itemType === 'sentence').length,
      exercise: tasks.filter((task) => task.itemType === 'exercise').length,
      other: tasks.filter((task) => !['vocab', 'grammar', 'sentence', 'exercise'].includes(task.itemType)).length,
    },
  })
}

async function handleHistory(request: Request, env: Env, url: URL) {
  const scope = await getDataScope(request, env, url.searchParams.get('deviceId'))
  if (scope instanceof Response) return scope

  const [corrections, interactions, profileRows] = await Promise.all([
    supabase<Record<string, unknown>[]>(
      env,
      `/rest/v1/sentence_correction_history?select=id,target_type,target_id,work_slug,episode,model,prompt_text,result_payload,created_at&${scope.query}&order=created_at.desc&limit=20`,
    ).catch(() => []),
    supabase<Record<string, unknown>[]>(
      env,
      `/rest/v1/ai_interaction_history?select=id,cache_key,cache_kind,model,work_slug,episode,source_id,result_payload,created_at&${scope.query}&order=created_at.desc&limit=30`,
    ).catch(() => []),
    supabase<Record<string, unknown>[]>(
      env,
      '/rest/v1/character_language_profiles?select=work_slug,character_key,model,result_payload,updated_at&order=updated_at.desc&limit=20',
    ).catch(() => []),
  ])

  return json({
    generatedAt: new Date().toISOString(),
    corrections: corrections.map(mapCorrectionHistory),
    ai: interactions.map(mapAiHistory),
    profiles: profileRows.map(mapProfileHistory),
  })
}

async function handleHistoryDetail(request: Request, env: Env, url: URL) {
  const scope = await getDataScope(request, env, url.searchParams.get('deviceId'))
  if (scope instanceof Response) return scope
  const type = url.searchParams.get('type') ?? ''
  const id = url.searchParams.get('id') ?? ''
  if (!id) return json({ error: { message: 'id is required' } }, 400)

  if (type === 'correction') {
    const rows = await supabase<Record<string, unknown>[]>(
      env,
      `/rest/v1/sentence_correction_history?select=*&id=eq.${encodeURIComponent(id)}&${scope.query}&limit=1`,
    ).catch(() => [])
    const row = rows[0]
    return json(row ? mapHistoryDetail('correction', row) : null)
  }

  if (type === 'ai') {
    const rows = await supabase<Record<string, unknown>[]>(
      env,
      `/rest/v1/ai_interaction_history?select=*&cache_key=eq.${encodeURIComponent(id)}&${scope.query}&limit=1`,
    ).catch(() => [])
    const row = rows[0]
    return json(row ? mapHistoryDetail('ai', row) : null)
  }

  if (type === 'profile') {
    const [workSlug, characterKey, model] = id.split(':')
    const rows = await supabase<Record<string, unknown>[]>(
      env,
      `/rest/v1/character_language_profiles?select=*&work_slug=eq.${encodeURIComponent(workSlug ?? '')}&character_key=eq.${encodeURIComponent(characterKey ?? '')}&model=eq.${encodeURIComponent(model ?? '')}&limit=1`,
    ).catch(() => [])
    const row = rows[0]
    return json(row ? mapHistoryDetail('profile', row) : null)
  }

  return json({ error: { message: 'type is invalid' } }, 400)
}

async function analyzeAir(env: Env, query: string, sources: unknown[], model: GatewayModel, reasoningEffort: ReasoningEffort) {
  if (sources.length === 0) {
    return {
      title: '未找到相关字幕',
      summary: '该作品/该集未找到相关字幕。',
      bullets: ['没有进行无作品 filter 的全库 fallback。'],
    }
  }

  try {
    const context = formatRagContext(sources).slice(0, 9000)
    const text = await callAiGateway(
      env,
      model,
      '你是日语语言学和动漫对话语用分析助手。只基于给定字幕来源分析，不编造剧情。用简体中文输出，必须引用日文证据。',
      `用户问题：${query}\n\n字幕来源：\n${context}\n\n请按这些小节回答：表层对话、潜台词、角色心理、语言证据、相似场景、学习价值。`,
      { maxTokens: 1800, temperature: 0.2, reasoningEffort },
    )

    if (!text) throw new Error('Empty AI response')
    const bullets = text
      .split(/\n+/)
      .map((line) => line.trim())
      .filter(Boolean)
      .slice(0, 12)

    return {
      title: 'AI 读空气分析',
      summary: bullets[0] ?? text.slice(0, 180),
      bullets,
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    return {
      title: '检索结果',
      summary: '已返回相关字幕 chunk，但 AI 分析暂时失败。',
      bullets: [message],
    }
  }
}

function formatRagContext(sources: unknown[]) {
  return sources
    .map((source) => {
      const record = typeof source === 'object' && source ? source as Record<string, unknown> : {}
      const work = readString(record, 'work')
      const episode = readNumber(record, 'episode')
      const chunkNo = readNumber(record, 'chunkNo')
      const startTime = readString(record, 'startTime')
      const endTime = readString(record, 'endTime')
      const score = readNumber(record, 'score')
      const text = readString(record, 'text')
      return [
        `[来源: ${work} EP${String(episode).padStart(2, '0')} | ${startTime}-${endTime} | chunk ${chunkNo} | score ${score.toFixed(4)}]`,
        text,
      ].filter(Boolean).join('\n')
    })
    .join('\n\n')
}

function parseJsonObject(text: string): Record<string, unknown> {
  const cleaned = text
    .trim()
    .replace(/^```(?:json)?/u, '')
    .replace(/```$/u, '')
    .trim()
  try {
    const parsed = JSON.parse(cleaned)
    return typeof parsed === 'object' && parsed ? parsed as Record<string, unknown> : {}
  } catch {
    const match = cleaned.match(/\{[\s\S]*\}/u)
    if (!match) return {}
    try {
      const parsed = JSON.parse(match[0])
      return typeof parsed === 'object' && parsed ? parsed as Record<string, unknown> : {}
    } catch {
      return {}
    }
  }
}

function parseJsonArray(text: string): unknown[] {
  const cleaned = text
    .trim()
    .replace(/^```(?:json)?/u, '')
    .replace(/```$/u, '')
    .trim()
  try {
    const parsed = JSON.parse(cleaned)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    const match = cleaned.match(/\[[\s\S]*\]/u)
    if (!match) return []
    try {
      const parsed = JSON.parse(match[0])
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
}

function normalizeAirQuestionCandidate(input: Record<string, unknown>, source: Record<string, unknown>) {
  const rawOptions = Array.isArray(input.options) ? input.options.filter((option): option is string => typeof option === 'string') : []
  const options = rawOptions.slice(0, 4)
  const fallbackAnswer = options[0] ?? ''
  const answer = options.includes(readString(input, 'answer')) ? readString(input, 'answer') : fallbackAnswer
  const rawEvidence = Array.isArray(input.evidence) ? input.evidence.filter((item): item is string => typeof item === 'string') : []
  const lines = Array.isArray(source.lines) ? source.lines as Record<string, unknown>[] : []
  const inputSceneLines = Array.isArray(input.sceneLines) ? input.sceneLines as Record<string, unknown>[] : []
  const sceneLinesSource = inputSceneLines.length > 0 ? inputSceneLines : lines
  const sceneLines = sceneLinesSource
    .map((line, index) => ({
      lineNo: readNumber(line, 'lineNo', readNumber(line, 'line_no', index + 1)),
      speaker: readString(line, 'speaker'),
      jaText: readString(line, 'jaText', readString(line, 'ja_text')),
      zhText: readString(line, 'zhText', readString(line, 'zh_text')),
      isTarget: line.isTarget === true,
    }))
    .filter((line) => line.jaText)
  const sceneJa = readString(input, 'sceneJa') || lines.map((line) => readString(line, 'jaText')).filter(Boolean).join('\n') || readString(source, 'text')
  const sceneZh = readString(input, 'sceneZh') || lines.map((line) => readString(line, 'zhText')).filter(Boolean).join('\n')
  const sourceInfo = typeof input.source === 'object' && input.source ? input.source as Record<string, unknown> : {}
  const explicitTargetLineNo = readNumber(input, 'targetLineNo')
  const targetLineNo = explicitTargetLineNo || sceneLines.find((line) => line.isTarget)?.lineNo || undefined

  return {
    sceneJa,
    sceneZh,
    question: readString(input, 'question') || '这段对话里最自然的语气判断是什么？',
    options,
    answer,
    explanation: readString(input, 'explanation'),
    evidence: rawEvidence,
    targetLineNo,
    sceneLines: sceneLines.map((line) => ({
      ...line,
      isTarget: targetLineNo ? line.lineNo === targetLineNo || line.isTarget : line.isTarget,
    })),
    source: {
      work: readString(sourceInfo, 'work', readString(source, 'work')),
      episode: readNumber(sourceInfo, 'episode', readNumber(source, 'episode')),
      chunkNo: readNumber(sourceInfo, 'chunkNo', readNumber(source, 'chunkNo')),
      startTime: readString(sourceInfo, 'startTime', readString(source, 'startTime')),
      endTime: readString(sourceInfo, 'endTime', readString(source, 'endTime')),
      sourceId: readString(source, 'id'),
    },
  }
}

function structuredTextResult(title: string, text: string, preferredSections: string[]) {
  const lines = text
    .split(/\n+/)
    .map((line) => cleanupAiMarkdown(line.trim()))
    .filter(Boolean)
  return {
    title,
    summary: lines.find((line) => !preferredSections.some((section) => line.includes(section))) ?? lines[0] ?? text.slice(0, 180),
    sections: preferredSections.map((section) => ({
      title: section,
      body: extractSection(text, section),
    })),
    text,
  }
}

function extractSection(text: string, section: string) {
  const aliases = sectionAliases(section)
  for (const alias of aliases) {
    const inline = extractInlineLabeledSection(text, alias, aliases)
    if (inline) return inline
  }

  const lines = text.split(/\n/)
  let start = lines.findIndex((line) => aliases.some((alias) => isSectionHeading(line, alias)))
  if (start < 0) start = lines.findIndex((line) => aliases.some((alias) => line.includes(alias)))
  if (start < 0) return ''

  const collected: string[] = []
  for (const line of lines.slice(start + 1)) {
    if (isAnySectionHeading(line)) {
      break
    }
    collected.push(line)
  }

  return collected.join('\n').trim()
}

function isSectionHeading(line: string, alias: string) {
  const escapedAlias = alias.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(`^\\s*(#{1,6}\\s*)?(\\d+[.、]\\s*)?(\\*\\*)?${escapedAlias}(\\*\\*)?\\s*[：:]?\\s*$`, 'u').test(line)
}

function isAnySectionHeading(line: string) {
  return allAiSectionLabels().some((label) => isSectionHeading(line, label))
}

function allAiSectionLabels() {
  return [
    '核心意思',
    '这句里的用法',
    '意思',
    '词性/用法',
    '容易误解',
    '学习建议',
    '字面意思',
    '句子结构',
    '跟读重点',
    '词法拆解',
    '句法结构',
    '助词说明',
    '句末语气',
    '角色心理',
    '现实可用性',
    '相近表达对比',
    '常见口癖',
    '句末倾向',
    '礼貌度',
    '情绪表达',
    '吐槽/被吐槽模式',
    '典型场景',
    '学习价值',
    '局限',
    '语法是否正确',
    '自然度',
    '语气是否合适',
    '更自然改写',
    '改写',
    '用法提醒',
    '评分',
    '总结',
  ]
}

function cleanupAiMarkdown(line: string) {
  return line
    .replace(/^#{1,6}\s*/u, '')
    .replace(/^\d+[.、]\s*/u, '')
    .replace(/^\s*[-*•]\s+/u, '')
    .replace(/\*\*/g, '')
    .trim()
}

function sectionAliases(section: string) {
  const aliasMap: Record<string, string[]> = {
    语气: ['语气是否合适', '语气'],
    改写: ['更自然改写', '改写'],
  }
  return aliasMap[section] ?? [section]
}

function extractInlineLabeledSection(text: string, alias: string, aliases: string[]) {
  const knownLabels = [
    '字面意思',
    '词法拆解',
    '句法结构',
    '助词说明',
    '句末语气',
    '角色心理',
    '现实可用性',
    '相近表达对比',
    '语法是否正确',
    '自然度',
    '语气是否合适',
    '更自然改写',
    '用法提醒',
    '评分',
    '总结',
  ].filter((label) => !aliases.includes(label))
  const escapedAlias = alias.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const escapedLabels = knownLabels.map((label) => label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')
  const pattern = new RegExp(`\\*?\\s*\\*\\*${escapedAlias}\\*\\*[：:]\\s*([\\s\\S]*?)(?=\\n\\s*\\*?\\s*\\*\\*(?:${escapedLabels})\\*\\*[：:]|$)`, 'u')
  return text.match(pattern)?.[1]?.trim() ?? ''
}

function normalizeGatewayModel(model: unknown): GatewayModel {
  return gatewayModels.some((item) => item.id === model) ? (model as GatewayModel) : defaultGatewayModel
}

function normalizeReasoningEffort(value: unknown): ReasoningEffort {
  return value === 'low' || value === 'medium' || value === 'high' ? value : 'high'
}

async function getAuthContext(request: Request, env: Env): Promise<AuthContext> {
  const token = readCookie(request, sessionCookieName)
  if (!token) return { user: null, sessionTokenHash: null }
  const tokenHash = await sha256Hex(token)
  const sessions = await supabaseAdmin<Record<string, unknown>[]>(
    env,
    'GET',
    `/rest/v1/app_sessions?select=*&session_token_hash=eq.${encodeURIComponent(tokenHash)}&limit=1`,
  ).catch(() => [])
  const session = sessions[0]
  if (!session) return { user: null, sessionTokenHash: null }
  const expiresAt = Date.parse(readString(session, 'expires_at'))
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    await supabaseAdmin(env, 'DELETE', `/rest/v1/app_sessions?session_token_hash=eq.${encodeURIComponent(tokenHash)}`).catch(() => undefined)
    return { user: null, sessionTokenHash: null }
  }

  const userId = readString(session, 'user_id')
  const users = await supabaseAdmin<AppUserRow[]>(
    env,
    'GET',
    `/rest/v1/app_users?select=id,email,created_at,updated_at,password_hash,password_salt,password_iterations&id=eq.${encodeURIComponent(userId)}&limit=1`,
  ).catch(() => [])
  const user = users[0]
  if (!user) return { user: null, sessionTokenHash: null }
  await supabaseAdmin(env, 'PATCH', `/rest/v1/app_sessions?session_token_hash=eq.${encodeURIComponent(tokenHash)}`, {
    last_seen_at: new Date().toISOString(),
  }).catch(() => undefined)
  return { user: publicUser(user), sessionTokenHash: tokenHash }
}

async function requireAuth(request: Request, env: Env) {
  const auth = await getAuthContext(request, env)
  if (!auth.user) return json({ error: { message: 'Authentication required' } }, 401)
  return auth as AuthContext & { user: AuthUser }
}

function publicUser(user: AppUserRow): AuthUser {
  return { id: user.id, email: user.email }
}

function normalizeEmail(value: unknown) {
  return typeof value === 'string' ? value.trim().toLowerCase() : ''
}

function isValidPassword(value: string) {
  return value.length >= 6 && value.length <= 256
}

async function hashPassword(password: string, saltBase64Url: string, iterations: number) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits'])
  const bits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      hash: 'SHA-256',
      salt: base64UrlToBytes(saltBase64Url),
      iterations,
    },
    key,
    256,
  )
  return bytesToBase64Url(new Uint8Array(bits))
}

async function verifyPassword(password: string, salt: string, iterations: number, expectedHash: string) {
  const actualHash = await hashPassword(password, salt, iterations)
  return timingSafeEqual(actualHash, expectedHash)
}

function timingSafeEqual(a: string, b: string) {
  const aBytes = new TextEncoder().encode(a)
  const bBytes = new TextEncoder().encode(b)
  const length = Math.max(aBytes.length, bBytes.length)
  let diff = aBytes.length ^ bBytes.length
  for (let index = 0; index < length; index += 1) {
    diff |= (aBytes[index] ?? 0) ^ (bBytes[index] ?? 0)
  }
  return diff === 0
}

async function sha256Hex(value: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

function randomBase64Url(byteLength: number) {
  const bytes = new Uint8Array(byteLength)
  crypto.getRandomValues(bytes)
  return bytesToBase64Url(bytes)
}

function bytesToBase64Url(bytes: Uint8Array) {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

function base64UrlToBytes(value: string) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=')
  const binary = atob(base64)
  return Uint8Array.from(binary, (char) => char.charCodeAt(0))
}

function readCookie(request: Request, name: string) {
  const cookie = request.headers.get('Cookie') ?? ''
  return cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${name}=`))
    ?.slice(name.length + 1) ?? ''
}

function sessionCookie(token: string, maxAge: number) {
  return `${sessionCookieName}=${token}; Max-Age=${maxAge}; HttpOnly; Secure; SameSite=Lax; Path=/`
}

function clearSessionCookie() {
  return `${sessionCookieName}=; Max-Age=0; HttpOnly; Secure; SameSite=Lax; Path=/`
}

function clientIp(request: Request) {
  return request.headers.get('CF-Connecting-IP') ?? request.headers.get('X-Forwarded-For') ?? 'unknown'
}

function isLoginRateLimited(key: string) {
  const record = loginFailures.get(key)
  if (!record) return false
  if (record.resetAt <= Date.now()) {
    loginFailures.delete(key)
    return false
  }
  return record.count >= 5
}

function recordLoginFailure(key: string) {
  const existing = loginFailures.get(key)
  const resetAt = Date.now() + 15 * 60 * 1000
  loginFailures.set(key, {
    count: existing && existing.resetAt > Date.now() ? existing.count + 1 : 1,
    resetAt,
  })
}

function clearLoginFailure(key: string) {
  loginFailures.delete(key)
}

function progressPatch(row: Record<string, unknown>, userId: string, deviceId: string) {
  return {
    user_id: userId,
    device_id: deviceId,
    state: readString(row, 'state'),
    ease: readNumber(row, 'ease', 2),
    review_count: readNumber(row, 'review_count', 1),
    next_review_on: readString(row, 'next_review_on'),
    last_reviewed_at: readString(row, 'last_reviewed_at'),
    updated_at: new Date().toISOString(),
    payload: typeof row.payload === 'object' && row.payload ? row.payload : {},
  }
}

function shouldReplaceProgress(existing: Record<string, unknown>, incoming: Record<string, unknown>) {
  const existingTime = Date.parse(readString(existing, 'last_reviewed_at'))
  const incomingTime = Date.parse(readString(incoming, 'last_reviewed_at'))
  if (Number.isFinite(incomingTime) && Number.isFinite(existingTime) && incomingTime !== existingTime) {
    return incomingTime > existingTime
  }
  return reviewPriority(readString(incoming, 'state'), readString(incoming, 'item_type')) > reviewPriority(readString(existing, 'state'), readString(existing, 'item_type'))
}

async function callAiGateway(
  env: Env,
  model: GatewayModel,
  systemPrompt: string,
  userPrompt: string,
  options: { maxTokens: number; temperature: number; reasoningEffort?: ReasoningEffort },
): Promise<string> {
  const aiGatewayToken = env.CF_AIG_TOKEN?.replace(/^\uFEFF/u, '').trim()
  if (!aiGatewayToken) {
    throw new Error('CF_AIG_TOKEN is not configured')
  }

  if (model.startsWith('gemini-')) {
    const response = await fetch(`${env.AI_GATEWAY_BASE_URL}/compat/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'cf-aig-authorization': `Bearer ${aiGatewayToken}`,
        'cf-aig-skip-cache': 'false',
      },
      body: JSON.stringify({
        model: `google-ai-studio/${model}`,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt },
        ],
        max_tokens: options.maxTokens,
        temperature: options.temperature,
        stream: false,
      }),
    })

    if (!response.ok) throw new Error(`AI Gateway Gemini failed: ${response.status} ${await response.text()}`)
    const data = (await response.json()) as { choices?: { message?: { content?: string } }[] }
    return data.choices?.[0]?.message?.content?.trim() ?? ''
  }

  const isDeepSeek = model.startsWith('deepseek-')
  const response = await fetch(`${env.AI_GATEWAY_BASE_URL}/${isDeepSeek ? 'deepseek' : 'grok'}/chat/completions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'cf-aig-authorization': `Bearer ${aiGatewayToken}`,
      ...(isDeepSeek ? { 'cf-aig-byok-alias': 'deepseek' } : {}),
    },
    body: JSON.stringify({
      model,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt },
      ],
      temperature: model === 'grok-4.3' ? Math.max(options.temperature, 0.7) : options.temperature,
      max_tokens: options.maxTokens,
      ...(model === 'grok-4.3' ? { reasoning_effort: options.reasoningEffort ?? 'high' } : {}),
      stream: false,
    }),
  })

  if (!response.ok) throw new Error(`AI Gateway ${model} failed: ${response.status} ${await response.text()}`)
  const data = (await response.json()) as { choices?: { message?: { content?: string } }[] }
  return data.choices?.[0]?.message?.content?.trim() ?? ''
}

async function embedQuery(ai: Ai, query: string): Promise<number[]> {
  const result = (await ai.run(EMBEDDING_MODEL, { text: [query] })) as {
    data?: number[][]
    shape?: number[]
  }
  const vector = result.data?.[0]
  if (!vector) {
    throw new Error('Failed to generate query embedding')
  }
  return vector
}

async function supabase<T>(env: Env, path: string): Promise<T> {
  if (!env.SUPABASE_PUBLISHABLE_KEY) {
    throw new Error('SUPABASE_PUBLISHABLE_KEY is not configured')
  }

  const response = await fetch(`${env.SUPABASE_URL}${path}`, {
    headers: {
      apikey: env.SUPABASE_PUBLISHABLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_PUBLISHABLE_KEY}`,
    },
  })

  if (!response.ok) {
    throw new Error(`Supabase REST failed: ${response.status} ${await response.text()}`)
  }

  return response.json()
}

async function listEpisodeVocabRows(
  env: Env,
  workSlug: string,
  episodeNo: number,
  mode: 'vocab' | 'handwriting',
) {
  const planRows = await supabase<PlanRow[]>(
    env,
    `/rest/v1/episode_learning_plans?select=vocab_item_ids,handwriting_vocab_ids&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episodeNo}&limit=1`,
  )
  const plannedIds = mode === 'handwriting' ? planRows[0]?.handwriting_vocab_ids : planRows[0]?.vocab_item_ids
  if (plannedIds?.length) {
    const rows = await supabase<unknown[]>(
      env,
      `/rest/v1/learning_vocab_items?select=*&id=${encodeURIComponent(`in.(${plannedIds.map(quotePostgrestString).join(',')})`)}&limit=${plannedIds.length}`,
    )
    const order = new Map(plannedIds.map((id, index) => [id, index]))
    return rows.sort((left, right) => {
      const leftIndex = order.get(readString(left as Record<string, unknown>, 'id')) ?? Number.MAX_SAFE_INTEGER
      const rightIndex = order.get(readString(right as Record<string, unknown>, 'id')) ?? Number.MAX_SAFE_INTEGER
      return leftIndex - rightIndex
    })
  }

  return supabase<unknown[]>(
    env,
    `/rest/v1/learning_vocab_items?select=*&work_slug=eq.${encodeURIComponent(workSlug)}&order=total_occurrences.desc&limit=40`,
  )
}

function quotePostgrestString(value: string) {
  return `"${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`
}

async function supabaseWrite(env: Env, path: string, row: Record<string, unknown>) {
  if (!env.SUPABASE_PUBLISHABLE_KEY) {
    throw new Error('SUPABASE_PUBLISHABLE_KEY is not configured')
  }

  const response = await fetch(`${env.SUPABASE_URL}${path}`, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_PUBLISHABLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_PUBLISHABLE_KEY}`,
      'Content-Type': 'application/json',
      Prefer: 'resolution=merge-duplicates,return=minimal',
    },
    body: JSON.stringify(row),
  })

  if (!response.ok) {
    throw new Error(`Supabase REST write failed: ${response.status} ${await response.text()}`)
  }
}

async function supabaseAdmin<T = unknown>(
  env: Env,
  method: 'GET' | 'POST' | 'PATCH' | 'DELETE',
  path: string,
  row?: Record<string, unknown>,
): Promise<T> {
  if (!env.SUPABASE_SERVICE_ROLE_KEY) {
    throw new Error('SUPABASE_SERVICE_ROLE_KEY is not configured')
  }

  const init: RequestInit = {
    method,
    headers: {
      apikey: env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
      ...(row ? { 'Content-Type': 'application/json' } : {}),
      ...(method === 'POST' || method === 'PATCH' ? { Prefer: 'resolution=merge-duplicates,return=representation' } : {}),
    },
  }
  if (row) init.body = JSON.stringify(row)

  const response = await fetch(`${env.SUPABASE_URL}${path}`, init)

  if (!response.ok) {
    throw new Error(`Supabase admin REST failed: ${response.status} ${await response.text()}`)
  }

  if (response.status === 204) return undefined as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

async function readAiCache(env: Env, cacheKey: string) {
  const rows = await supabase<AiCacheRow[]>(
    env,
    `/rest/v1/ai_result_cache?select=result_payload&cache_key=eq.${encodeURIComponent(cacheKey)}&limit=1`,
  ).catch(() => [])
  return rows[0]?.result_payload
}

async function writeAiCache(
  env: Env,
  input: {
    cacheKey: string
    cacheKind: string
    model: GatewayModel
    sourceId?: string
    workSlug?: string
    episode?: number
    inputPayload: unknown
    resultPayload: unknown
    mustPersist?: boolean
  },
) {
  if (!allowedCacheKinds.has(input.cacheKind)) {
    throw new Error(`Invalid cache kind: ${input.cacheKind}`)
  }
  const inputHash = await hashPayload('input', input.inputPayload)
  const write = supabaseWrite(env, '/rest/v1/ai_result_cache?on_conflict=cache_key', {
      cache_key: input.cacheKey,
      cache_kind: input.cacheKind,
      model: input.model,
      work_slug: input.workSlug ?? null,
      episode: input.episode ?? null,
      source_id: input.sourceId ?? null,
      input_hash: inputHash,
      request_payload: input.inputPayload,
      result_payload: input.resultPayload,
      updated_at: new Date().toISOString(),
    })
  if (input.mustPersist) {
    await write
    return
  }
  await write.catch(() => undefined)
}

async function recordAiInteraction(
  request: Request,
  env: Env,
  input: {
    deviceId?: string
    cacheKey: string
    cacheKind: string
    model: GatewayModel
    sourceId?: string
    workSlug?: string
    episode?: number
    resultPayload: unknown
  },
) {
  if (!allowedCacheKinds.has(input.cacheKind)) return
  const auth = await getAuthContext(request, env)
  const deviceId = input.deviceId && isValidDeviceId(input.deviceId) ? input.deviceId : null
  if (!auth.user && !deviceId) return

  await supabaseWrite(env, '/rest/v1/ai_interaction_history?on_conflict=id', {
    id: auth.user ? `${auth.user.id}:${input.cacheKey}` : `${deviceId}:${input.cacheKey}`,
    device_id: deviceId,
    user_id: auth.user?.id ?? null,
    cache_key: input.cacheKey,
    cache_kind: input.cacheKind,
    model: input.model,
    work_slug: input.workSlug ?? null,
    episode: input.episode ?? null,
    source_id: input.sourceId ?? null,
    result_payload: input.resultPayload,
    created_at: new Date().toISOString(),
  }).catch(() => undefined)
}

async function writeCorrectionHistory(
  env: Env,
  user: AuthUser | null,
  body: {
    deviceId?: string
    targetType?: 'vocab' | 'grammar' | 'free'
    targetId?: string
    workSlug?: string
    episode?: number
  } | null,
  payload: { targetType: string; targetId: string },
  model: GatewayModel,
  sentence: string,
  cacheKey: string,
  resultPayload: unknown,
) {
  const deviceId = body?.deviceId && isValidDeviceId(body.deviceId) ? body.deviceId : null
  if (body?.deviceId && !deviceId) return
  if (!user && !deviceId) return
  await supabaseWrite(env, '/rest/v1/sentence_correction_history', {
    id: user ? `${user.id}:${cacheKey}` : cacheKey,
    device_id: deviceId,
    user_id: user?.id ?? null,
    target_type: payload.targetType,
    target_id: payload.targetId,
    work_slug: body?.workSlug,
    episode: body?.episode,
    model,
    prompt_text: sentence,
    result_payload: resultPayload,
  }).catch(() => undefined)
}

async function hashPayload(prefix: string, payload: unknown) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(JSON.stringify(payload)))
  const hash = [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${prefix}:${hash}`
}

function nextReviewDate(state: ReviewState) {
  const date = new Date()
  if (state === 'known' || state === 'good') date.setDate(date.getDate() + 7)
  if (state === 'fuzzy' || state === 'ok') date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

function maxIsoDate(a: string, b: string) {
  if (!a) return b || null
  if (!b) return a
  return a >= b ? a : b
}

function isValidDeviceId(value: string) {
  return /^device-[0-9a-fA-F-]{36}$/.test(value) || /^smoke-device[-\w]*$/.test(value)
}

function normalizeProgressItemType(value: unknown): ProgressItemType | null {
  const itemType = typeof value === 'string' ? value : 'unknown'
  return allowedProgressItemTypes.has(itemType as ProgressItemType) ? (itemType as ProgressItemType) : null
}

async function getDataScope(request: Request, env: Env, deviceId: string | null) {
  const auth = await getAuthContext(request, env)
  if (auth.user) {
    return {
      user: auth.user,
      deviceId: deviceId && isValidDeviceId(deviceId) ? deviceId : null,
      query: `user_id=eq.${encodeURIComponent(auth.user.id)}`,
    }
  }
  if (!deviceId) return json({ error: { message: 'deviceId is required' } }, 400)
  if (!isValidDeviceId(deviceId)) return json({ error: { message: 'deviceId is invalid' } }, 400)
  return {
    user: null,
    deviceId,
    query: `device_id=eq.${encodeURIComponent(deviceId)}`,
  }
}

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: jsonHeaders })
}

function jsonWithHeaders(data: unknown, status: number, headers: Record<string, string>) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...jsonHeaders,
      ...headers,
    },
  })
}

function mapWork(row: WorkRow) {
  return {
    id: row.id,
    slug: row.slug,
    displayName: row.display_name,
    episodeCount: row.episode_count,
  }
}

function mapEpisode(row: EpisodeRow) {
  return {
    id: row.id,
    workSlug: row.work_slug,
    workDisplayName: row.work_display_name,
    episode: row.episode,
    totalCues: row.total_cues,
    jaLines: row.ja_lines,
    zhLines: row.zh_lines,
    usableJaLines: row.usable_ja_lines,
    chunkCount: row.chunk_count,
    usableAsMainCorpus: row.usable_as_main_corpus,
  }
}

function mapPlan(row: PlanRow) {
  return {
    id: row.id,
    workSlug: row.work_slug,
    episode: row.episode,
    vocabCount: row.vocab_item_ids.length,
    handwritingCount: row.handwriting_vocab_ids.length,
    shadowingCount: row.shadowing_sentence_ids.length,
    grammarCount: row.grammar_point_ids.length,
    exerciseCount: row.exercise_ids.length,
    vocabItemIds: row.vocab_item_ids,
    handwritingVocabIds: row.handwriting_vocab_ids,
    notes: row.notes,
  }
}

function readString(row: Record<string, unknown>, key: string, fallback = '') {
  return typeof row[key] === 'string' ? row[key] : fallback
}

function readNumber(row: Record<string, unknown>, key: string, fallback = 0) {
  return typeof row[key] === 'number' ? row[key] : fallback
}

function readBoolean(row: Record<string, unknown>, key: string) {
  return row[key] === true
}

function mapVocab(input: unknown) {
  const row = input as Record<string, unknown>
  return {
    id: readString(row, 'id'),
    workSlug: readString(row, 'work_slug'),
    surface: readString(row, 'surface'),
    reading: readString(row, 'reading'),
    romaji: readString(row, 'romaji'),
    meaningZh: readString(row, 'meaning_zh'),
    pos: readString(row, 'pos'),
    jlptLevel: readString(row, 'jlpt_level'),
    suitableHandwriting: readBoolean(row, 'suitable_handwriting'),
    suitableShadowing: readBoolean(row, 'suitable_shadowing'),
    animeToneNote: readString(row, 'anime_tone_note'),
    realWorldNote: readString(row, 'real_world_note'),
    totalOccurrences: readNumber(row, 'total_occurrences'),
    episodeCount: readNumber(row, 'episode_count'),
  }
}

function mapGrammar(input: unknown) {
  const row = input as Record<string, unknown>
  return {
    id: readString(row, 'id'),
    pattern: readString(row, 'pattern'),
    functionZh: readString(row, 'function_zh'),
    jaExample: readString(row, 'ja_example'),
    explanationZh: readString(row, 'explanation_zh'),
    pragmaticsNote: readString(row, 'pragmatics_note'),
    realWorldNote: readString(row, 'real_world_note'),
    difficulty: readString(row, 'difficulty'),
    sourceLineNo: readNumber(row, 'source_line_no'),
  }
}

function mapSentence(input: unknown) {
  const row = input as Record<string, unknown>
  const toneTags = Array.isArray(row.tone_tags) ? row.tone_tags.filter((tag): tag is string => typeof tag === 'string') : []
  return {
    id: readString(row, 'id'),
    jaText: readString(row, 'ja_text'),
    romaji: readString(row, 'romaji'),
    meaningZh: readString(row, 'meaning_zh'),
    toneTags,
    difficulty: readString(row, 'difficulty'),
    sourceLineNo: readNumber(row, 'source_line_no'),
    audioUrl: readString(row, 'audio_url'),
    storagePath: readString(row, 'storage_path'),
  }
}

function mapExercise(input: unknown) {
  const row = input as Record<string, unknown>
  return {
    id: readString(row, 'id'),
    exerciseType: readString(row, 'question_type', readString(row, 'exercise_type')),
    prompt: readString(row, 'prompt'),
    answer: readAnswer(row),
    hint: readString(row, 'hint', readString(row, 'basic_explanation_zh')),
    difficulty: readString(row, 'difficulty'),
  }
}

function mapLinguisticExercise(input: unknown, phenomena = new Map<string, Record<string, unknown>>()) {
  const row = input as Record<string, unknown>
  const phenomenon = phenomena.get(readString(row, 'phenomenon_key'))
  const optionItems = readLinguisticOptions(row.options)
  return {
    id: readString(row, 'id'),
    batchId: readString(row, 'batch_id'),
    workSlug: readString(row, 'work_slug'),
    episode: readNumber(row, 'episode'),
    sourceId: readString(row, 'source_id'),
    sourceLineNo: readNumber(row, 'source_line_no'),
    jaText: readString(row, 'ja_text'),
    zhText: readString(row, 'zh_text'),
    domain: readString(row, 'domain'),
    phenomenonKey: readString(row, 'phenomenon_key'),
    questionType: readString(row, 'question_type'),
    prompt: readString(row, 'prompt'),
    options: optionItems.map((option) => option.label),
    optionItems,
    answer: readLinguisticAnswer(row),
    hint: readString(row, 'hint'),
    basicExplanationZh: readString(row, 'basic_explanation_zh'),
    deepExplanationZh: readString(row, 'deep_explanation_zh'),
    animeContextNoteZh: readString(row, 'anime_context_note_zh'),
    cautionNoteZh: readString(row, 'caution_note_zh'),
    difficulty: readString(row, 'difficulty'),
    qualityScore: readNumber(row, 'quality_score'),
    status: readString(row, 'status'),
    phenomenonNameZh: phenomenon ? readString(phenomenon, 'name_zh') : '',
    phenomenonNameJa: phenomenon ? readString(phenomenon, 'name_ja') : '',
    phenomenonDefinitionZh: phenomenon ? readString(phenomenon, 'short_definition_zh') : '',
  }
}

function readAnswer(row: Record<string, unknown>) {
  const answer = row.answer
  if (typeof answer === 'string') return answer
  if (!answer || typeof answer !== 'object') return ''

  const answerObject = answer as Record<string, unknown>
  if (typeof answerObject.answer_zh === 'string') return answerObject.answer_zh
  if (typeof answerObject.answer === 'string') return answerObject.answer

  const correctIndex = answerObject.correct_index
  const options = row.options
  if (typeof correctIndex === 'number' && Array.isArray(options) && typeof options[correctIndex] === 'string') {
    return options[correctIndex]
  }

  return ''
}

function readLinguisticAnswer(row: Record<string, unknown>) {
  const answer = row.answer
  const answerObject = answer && typeof answer === 'object' ? answer as Record<string, unknown> : {}
  const options = readLinguisticOptions(row.options)
  const correctIndex = typeof answerObject.correct_index === 'number' ? answerObject.correct_index : undefined
  const correctKey = readString(answerObject, 'correct_key')
  const keyedAnswer = correctKey ? options.find((option) => option.key === correctKey)?.label : undefined
  const indexedAnswer = correctIndex !== undefined ? options[correctIndex]?.label : undefined
  return {
    answerZh: typeof answerObject.answer_zh === 'string'
      ? answerObject.answer_zh
      : typeof answerObject.answer === 'string'
        ? answerObject.answer
        : keyedAnswer ?? indexedAnswer ?? readAnswer(row),
    correctIndex,
    correctKey,
    rationaleZh: readString(answerObject, 'rationale_zh'),
  }
}

function readLinguisticOptions(value: unknown) {
  if (!Array.isArray(value)) return []
  return value
    .map((option, index) => {
      if (typeof option === 'string') {
        return { key: String(index), label: option }
      }
      if (!option || typeof option !== 'object') return null
      const optionObject = option as Record<string, unknown>
      const label =
        readString(optionObject, 'label') ||
        readString(optionObject, 'text') ||
        readString(optionObject, 'value') ||
        readString(optionObject, 'answer') ||
        readString(optionObject, 'content')
      if (!label) return null
      return {
        key: readString(optionObject, 'key', readString(optionObject, 'id', String(index))),
        label,
      }
    })
    .filter((option): option is { key: string; label: string } => Boolean(option))
}

function mapSubtitle(input: unknown) {
  const row = input as Record<string, unknown>
  return {
    lineNo: readNumber(row, 'line_no'),
    startTime: readString(row, 'start_time'),
    endTime: readString(row, 'end_time'),
    jaText: readString(row, 'ja_text'),
    zhText: readString(row, 'zh_text'),
  }
}

function mapProgress(input: Record<string, unknown>) {
  return {
    deviceId: readString(input, 'device_id'),
    itemId: readString(input, 'item_id'),
    itemType: readString(input, 'item_type'),
    workSlug: readString(input, 'work_slug'),
    episode: readNumber(input, 'episode'),
    state: readString(input, 'state'),
    nextReviewOn: readString(input, 'next_review_on'),
    lastReviewedAt: readString(input, 'last_reviewed_at'),
    payload: typeof input.payload === 'object' && input.payload ? input.payload : {},
  }
}

function mapWritingStats(input: Record<string, unknown>) {
  return {
    itemId: readString(input, 'item_id'),
    completedCount: readNumber(input, 'completed_count'),
    lastPracticedAt: readString(input, 'last_practiced_at'),
  }
}

function normalizeWritingMetrics(input: Record<string, unknown> | undefined) {
  return {
    coverageRatio: clampMetric(readNumber(input ?? {}, 'coverageRatio')),
    offTargetRatio: clampMetric(readNumber(input ?? {}, 'offTargetRatio')),
    strokeLength: Math.max(0, readNumber(input ?? {}, 'strokeLength')),
    durationMs: Math.max(0, Math.round(readNumber(input ?? {}, 'durationMs'))),
  }
}

function clampMetric(value: number) {
  return Math.max(0, Math.min(1, value))
}

function normalizeWritingReason(reason: WritingSubmitReason | undefined, passed: boolean): WritingSubmitReason | null {
  if (passed) return null
  return reason && ['empty', 'too_short', 'too_far', 'low_coverage', 'skipped'].includes(reason) ? reason : 'too_far'
}

type ReviewTask = ReturnType<typeof mapProgress> & {
  priority: number
  route: string
  label: string
  due: boolean
}

function mapReviewTask(input: Record<string, unknown>): ReviewTask {
  const progress = mapProgress(input)
  const payload = progress.payload as Record<string, unknown>
  const label = typeof payload.label === 'string' ? payload.label : progress.itemId
  return {
    ...progress,
    label,
    priority: reviewPriority(progress.state, progress.itemType),
    route: reviewRoute(progress),
    due: !progress.nextReviewOn || progress.nextReviewOn <= new Date().toISOString().slice(0, 10),
  }
}

function reviewPriority(state: string, itemType: string) {
  const stateScore: Record<string, number> = {
    unknown: 100,
    bad: 95,
    fuzzy: 70,
    ok: 55,
  }
  const typeScore: Record<string, number> = {
    exercise: 8,
    vocab: 6,
    sentence: 4,
    grammar: 2,
  }
  return (stateScore[state] ?? 0) + (typeScore[itemType] ?? 0)
}

function compareReviewTasks(a: ReviewTask, b: ReviewTask) {
  if (a.due !== b.due) return a.due ? -1 : 1
  if (a.priority !== b.priority) return b.priority - a.priority
  return a.nextReviewOn.localeCompare(b.nextReviewOn)
}

function reviewRoute(progress: ReturnType<typeof mapProgress>) {
  const workSlug = progress.workSlug || 'k-on'
  const episode = progress.episode || 1
  if (progress.itemType === 'vocab') return `/works/${workSlug}/episodes/${episode}/vocab`
  if (progress.itemType === 'grammar') return `/works/${workSlug}/episodes/${episode}/grammar`
  if (progress.itemType === 'sentence') return `/works/${workSlug}/episodes/${episode}/sentences`
  if (progress.itemType === 'exercise') return `/works/${workSlug}/episodes/${episode}/practice`
  return `/works/${workSlug}/episodes/${episode}`
}

function mapCorrectionHistory(input: Record<string, unknown>) {
  const result = typeof input.result_payload === 'object' && input.result_payload ? input.result_payload as Record<string, unknown> : {}
  return {
    id: readString(input, 'id'),
    targetType: readString(input, 'target_type'),
    targetId: readString(input, 'target_id'),
    workSlug: readString(input, 'work_slug'),
    episode: readNumber(input, 'episode'),
    model: readString(input, 'model'),
    promptText: readString(input, 'prompt_text'),
    title: typeof result.title === 'string' ? result.title : '造句批改',
    summary: typeof result.summary === 'string' ? result.summary : '',
    createdAt: readString(input, 'created_at'),
  }
}

function mapAiHistory(input: Record<string, unknown>) {
  const result = typeof input.result_payload === 'object' && input.result_payload ? input.result_payload as Record<string, unknown> : {}
  return {
    id: readString(input, 'cache_key'),
    kind: readString(input, 'cache_kind'),
    model: readString(input, 'model'),
    workSlug: readString(input, 'work_slug'),
    episode: readNumber(input, 'episode'),
    sourceId: readString(input, 'source_id'),
    title: typeof result.title === 'string' ? result.title : readString(input, 'cache_kind'),
    summary: typeof result.summary === 'string' ? result.summary : '',
    updatedAt: readString(input, 'updated_at', readString(input, 'created_at')),
  }
}

function mapProfileHistory(input: Record<string, unknown>) {
  const result = typeof input.result_payload === 'object' && input.result_payload ? input.result_payload as Record<string, unknown> : {}
  return {
    id: `${readString(input, 'work_slug')}:${readString(input, 'character_key')}:${readString(input, 'model')}`,
    workSlug: readString(input, 'work_slug'),
    characterKey: readString(input, 'character_key'),
    model: readString(input, 'model'),
    title: typeof result.title === 'string' ? result.title : '角色语言画像',
    summary: typeof result.summary === 'string' ? result.summary : '',
    updatedAt: readString(input, 'updated_at'),
  }
}

function mapHistoryDetail(type: 'correction' | 'ai' | 'profile', input: Record<string, unknown>) {
  const result = typeof input.result_payload === 'object' && input.result_payload ? input.result_payload as Record<string, unknown> : {}
  return {
    type,
    id: type === 'ai' ? readString(input, 'cache_key') : type === 'profile'
      ? `${readString(input, 'work_slug')}:${readString(input, 'character_key')}:${readString(input, 'model')}`
      : readString(input, 'id'),
    title: typeof result.title === 'string' ? result.title : type === 'correction' ? '造句批改' : type === 'profile' ? '角色语言画像' : readString(input, 'cache_kind'),
    summary: typeof result.summary === 'string' ? result.summary : '',
    model: readString(input, 'model'),
    cacheKind: readString(input, 'cache_kind', type === 'correction' ? 'sentence_correction' : type),
    cacheStatus: type === 'profile' ? 'global-cache' : 'history-snapshot',
    workSlug: readString(input, 'work_slug'),
    episode: readNumber(input, 'episode'),
    sourceId: readString(input, 'source_id', readString(input, 'target_id')),
    promptText: readString(input, 'prompt_text'),
    createdAt: readString(input, 'created_at', readString(input, 'updated_at')),
    updatedAt: readString(input, 'updated_at', readString(input, 'created_at')),
    result,
  }
}
