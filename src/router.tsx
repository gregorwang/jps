import {
  Link,
  Outlet,
  createRootRoute,
  createRoute,
  createRouter,
  lazyRouteComponent,
  useRouterState,
} from '@tanstack/react-router'
import { useEffect, useState } from 'react'
import {
  BookOpen,
  Brain,
  Captions,
  GraduationCap,
  MessageSquareText,
  History,
  Home,
  KeyRound,
  Library,
  Mic2,
  PenLine,
  PlayCircle,
  Sparkles,
  Users,
} from 'lucide-react'
import { useAuthMe } from './lib/auth'
import { useProgressSync } from './lib/progress'
import { readEpisodeScope, writeEpisodeScope } from './lib/episodeScope'

const rootRoute = createRootRoute({
  component: AppLayout,
})

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: lazyRouteComponent(() => import('./routes/HomePage'), 'HomePage'),
})

const worksRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works',
  component: lazyRouteComponent(() => import('./routes/WorksPage'), 'WorksPage'),
})

const episodeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode',
  component: lazyRouteComponent(() => import('./routes/EpisodePage'), 'EpisodePage'),
})

const vocabRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/vocab',
  component: lazyRouteComponent(() => import('./routes/VocabPage'), 'VocabPage'),
})

const grammarRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/grammar',
  component: lazyRouteComponent(() => import('./routes/GrammarPage'), 'GrammarPage'),
})

const sentencesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/sentences',
  component: lazyRouteComponent(() => import('./routes/SentencesPage'), 'SentencesPage'),
})

const practiceRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/practice',
  component: lazyRouteComponent(() => import('./routes/PracticePage'), 'PracticePage'),
})

const lessonRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/lesson',
  component: lazyRouteComponent(() => import('./routes/LessonPage'), 'LessonPage'),
})

const writingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/writing',
  component: lazyRouteComponent(() => import('./routes/WritingPracticePage'), 'WritingPracticePage'),
})

const episodeWritingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/writing',
  component: lazyRouteComponent(() => import('./routes/WritingPracticePage'), 'WritingPracticePage'),
})

const linguisticTrainingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/linguistic-training',
  component: lazyRouteComponent(() => import('./routes/LinguisticTrainingPage'), 'LinguisticTrainingPage'),
})

const episodeLinguisticsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/linguistics',
  component: lazyRouteComponent(() => import('./routes/EpisodeLinguisticsPage'), 'EpisodeLinguisticsPage'),
})

const subtitlesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/subtitles',
  component: lazyRouteComponent(() => import('./routes/SubtitlesPage'), 'SubtitlesPage'),
})

const sentenceDeepDiveRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/works/$workSlug/episodes/$episode/sentence',
  component: lazyRouteComponent(() => import('./routes/SentenceDeepDivePage'), 'SentenceDeepDivePage'),
})

const ragRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/rag',
  component: lazyRouteComponent(() => import('./routes/RagPage'), 'RagPage'),
})

const characterProfilesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/characters',
  component: lazyRouteComponent(() => import('./routes/CharacterProfilesPage'), 'CharacterProfilesPage'),
})

const correctionRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/correction',
  component: lazyRouteComponent(() => import('./routes/CorrectionPage'), 'CorrectionPage'),
})

const historyRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/history',
  component: lazyRouteComponent(() => import('./routes/HistoryPage'), 'HistoryPage'),
})

const historyDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/history/$kind/$id',
  component: lazyRouteComponent(() => import('./routes/HistoryDetailPage'), 'HistoryDetailPage'),
})

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: lazyRouteComponent(() => import('./routes/LoginPage'), 'LoginPage'),
})

const accountRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/account',
  component: lazyRouteComponent(() => import('./routes/AccountPage'), 'AccountPage'),
})

const routeTree = rootRoute.addChildren([
  indexRoute,
  worksRoute,
  episodeRoute,
  vocabRoute,
  grammarRoute,
  sentencesRoute,
  practiceRoute,
  lessonRoute,
  writingRoute,
  episodeWritingRoute,
  linguisticTrainingRoute,
  episodeLinguisticsRoute,
  subtitlesRoute,
  sentenceDeepDiveRoute,
  ragRoute,
  characterProfilesRoute,
  correctionRoute,
  historyRoute,
  historyDetailRoute,
  loginRoute,
  accountRoute,
])

const navActiveProps = { className: 'nav-link active' }
const exactActiveOptions = { exact: true } as const

export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
  scrollRestoration: true,
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

function AppLayout() {
  useAuthMe()
  useProgressSync()
  const [recentEpisodeScope, setRecentEpisodeScope] = useState(readEpisodeScope)
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const episodeMatch = pathname.match(/^\/works\/([^/]+)\/episodes\/([^/]+)/)
  const isLessonRoute = /^\/works\/[^/]+\/episodes\/[^/]+\/lesson/.test(pathname)
  useEffect(() => {
    if (!episodeMatch) return
    const nextScope = {
      workSlug: episodeMatch[1],
      episode: Number(episodeMatch[2]),
    }
    setRecentEpisodeScope((current) => (
      current.workSlug === nextScope.workSlug && current.episode === nextScope.episode ? current : nextScope
    ))
    writeEpisodeScope(nextScope)
  }, [episodeMatch?.[1], episodeMatch?.[2]])

  useEffect(() => {
    function handleEpisodeScopeChange(event: Event) {
      const detail = (event as CustomEvent<typeof recentEpisodeScope>).detail
      if (!detail?.workSlug || !detail.episode) return
      setRecentEpisodeScope((current) => (
        current.workSlug === detail.workSlug && current.episode === detail.episode ? current : detail
      ))
    }

    window.addEventListener('episode-scope-change', handleEpisodeScopeChange)
    return () => window.removeEventListener('episode-scope-change', handleEpisodeScopeChange)
  }, [])

  const currentEpisodeParams = {
    workSlug: episodeMatch?.[1] ?? recentEpisodeScope.workSlug,
    episode: episodeMatch?.[2] ?? String(recentEpisodeScope.episode),
  }
  const episodeLabel = episodeMatch ? `EP${currentEpisodeParams.episode.padStart(2, '0')}` : '单集'

  return (
    <div className={isLessonRoute ? 'app-shell lesson-app-shell' : 'app-shell'}>
      {isLessonRoute ? null : (
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">あ</span>
          <div>
            <strong>Anime Japanese Lab</strong>
            <small>Multi-work corpus lab</small>
          </div>
        </div>
        <nav className="nav-list" aria-label="主导航">
          <Link to="/" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Home size={18} />
            <span>今日</span>
          </Link>
          <Link to="/works" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Library size={18} />
            <span>作品</span>
          </Link>
          <Link to="/works/$workSlug/episodes/$episode" params={currentEpisodeParams} className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <BookOpen size={18} />
            <span>{episodeLabel}</span>
          </Link>
          <Link to="/works/$workSlug/episodes/$episode/vocab" params={currentEpisodeParams} className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Sparkles size={18} />
            <span>词</span>
          </Link>
          <Link to="/works/$workSlug/episodes/$episode/grammar" params={currentEpisodeParams} className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Brain size={18} />
            <span>语法</span>
          </Link>
          <Link to="/works/$workSlug/episodes/$episode/sentences" params={currentEpisodeParams} className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Mic2 size={18} />
            <span>跟读</span>
          </Link>
          <Link to="/works/$workSlug/episodes/$episode/writing" params={currentEpisodeParams} className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <PenLine size={18} />
            <span>手写</span>
          </Link>
          <Link to="/linguistic-training" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <PlayCircle size={18} />
            <span>作答</span>
          </Link>
          <Link to="/works/$workSlug/episodes/$episode/subtitles" params={currentEpisodeParams} className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Captions size={18} />
            <span>台词</span>
          </Link>
          <Link to="/rag" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <GraduationCap size={18} />
            <span>出题</span>
          </Link>
          <Link to="/characters" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <Users size={18} />
            <span>角色</span>
          </Link>
          <Link to="/correction" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <MessageSquareText size={18} />
            <span>批改</span>
          </Link>
          <Link to="/history" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <History size={18} />
            <span>历史</span>
          </Link>
          <Link to="/account" className="nav-link" activeProps={navActiveProps} activeOptions={exactActiveOptions}>
            <KeyRound size={18} />
            <span>账号</span>
          </Link>
        </nav>
      </aside>
      )}
      <main className={isLessonRoute ? 'main-panel lesson-main-panel' : 'main-panel'}>
        <Outlet />
      </main>
    </div>
  )
}
