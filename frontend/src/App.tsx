import axios, { AxiosError } from 'axios'
import { QRCodeSVG } from 'qrcode.react'
import { CalendarDays, LogOut, QrCode, Sparkles } from 'lucide-react'
import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, Navigate, NavLink, Route, BrowserRouter as Router, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import toast, { Toaster } from 'react-hot-toast'
import agendaSnapshot from './data/official-agenda.json'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'
const TOKEN_KEY = 'jprime.token'
const THEME_KEY = 'jprime.theme'

type AuthUser = { id: string; publicId: string; fullName: string; email?: string; profileCompleted: boolean }
type AuthResponse = { token: string; user: AuthUser }
type Lookup = { id: string; name: string }
type TalkStats = { interestedCount: number; attendedCount: number; wantToDiscussCount: number; missedWantRecapCount: number }
type Talk = { id: string; title: string; speaker?: string; description?: string; hall: string; startTime: string; endTime: string; tags: string[]; stats?: TalkStats; officialId?: number; talkLevel?: string; beginnerFriendly?: boolean; format?: string; audience?: string; takeaways?: string[]; sourceUrl?: string }
type AgendaSnapshotItem = Omit<Talk, 'speaker' | 'description' | 'talkLevel' | 'format' | 'audience' | 'takeaways' | 'sourceUrl' | 'tags'> & {
  speaker: string | null
  description: string | null
  talkLevel: string | null
  format: string | null
  audience?: string | null
  takeaways?: string[] | null
  sourceUrl: string | null
  tags?: string[] | null
}
type Profile = { id: string; publicId: string; fullName: string; email?: string; roleTitle?: string; company?: string; bio?: string; linkedinUrl?: string; githubUrl?: string; profilePhotoUrl?: string; profileCompleted: boolean; publicProfileEnabled?: boolean; contactInfoVisibleAfterMatch?: boolean; interests: string[]; goals: string[]; talksToDiscuss: string[] }
type SharedTalk = { talkId: string; title: string; reason: string }
type Candidate = { userId: string; publicId: string; fullName: string; roleTitle?: string; company?: string; bio?: string; score: number; sharedInterests: string[]; sharedGoals: string[]; sharedTalks: SharedTalk[]; icebreaker: string }
type MatchSummary = { matchId: string; fullName: string; roleTitle?: string; company?: string; sharedInterests: string[]; sharedTalks: SharedTalk[]; icebreaker: string; meetingStatus: string }
type MatchDetail = { matchId: string; otherUser: Profile; sharedInterests: string[]; sharedGoals: string[]; sharedTalks: SharedTalk[]; icebreakers: string[]; meeting?: Meeting | null }
type Meeting = { id: string; title: string; location: string; startTime: string; endTime: string; note?: string; topic?: string; status?: string }
type MeetingSummary = Meeting & { matchId: string; createdByCurrentUser: boolean; otherUserId: string; otherUserName: string; otherUserRoleTitle?: string; otherUserCompany?: string }
type Recommendation = { talkId: string; title: string; reason: string; score: number }
type RecommendationResponse = { message: string; recommendations: Recommendation[]; suggestedProfileTags: string[] }
type DiscussionAttendee = { userId: string; publicId: string; fullName: string; roleTitle?: string; company?: string; bio?: string; matched: boolean }
type TalkDiscussion = { talkId: string; title: string; speaker?: string; hall: string; startTime: string; attendeeCount: number; attendees: DiscussionAttendee[] }
type ForumTopic = { id: string; title: string; body: string; category: string; createdAt: string; authorName: string; authorRoleTitle?: string; authorCompany?: string; commentCount: number }
type ForumComment = { id: string; topicId: string; body: string; createdAt: string; authorName: string; authorRoleTitle?: string; authorCompany?: string }

const api = axios.create({ baseURL: API_BASE_URL })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function errorMessage(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>
  return axiosError.response?.data?.message ?? 'Something went wrong'
}

const OFFICIAL_AGENDA: Talk[] = (agendaSnapshot as AgendaSnapshotItem[]).map((talk) => ({
  ...talk,
  speaker: talk.speaker ?? undefined,
  description: talk.description ?? undefined,
  talkLevel: talk.talkLevel ?? undefined,
  format: talk.format ?? undefined,
  audience: talk.audience ?? undefined,
  takeaways: talk.takeaways ?? undefined,
  sourceUrl: talk.sourceUrl ?? undefined,
  tags: talk.tags ?? [],
}))

type AuthContextValue = {
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  refresh: () => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthContext missing')
  return value
}

function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setUser(null)
      setLoading(false)
      return
    }
    try {
      setUser((await api.get<AuthUser>('/auth/me')).data)
    } catch {
      localStorage.removeItem(TOKEN_KEY)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void refresh() }, [])

  const applyAuth = (data: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, data.token)
    setUser(data.user)
  }

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    refresh,
    login: async (email, password) => applyAuth((await api.post<AuthResponse>('/auth/login', { email, password })).data),
    register: async (fullName, email, password) => applyAuth((await api.post<AuthResponse>('/auth/register', { fullName, email, password })).data),
    logout: () => {
      localStorage.removeItem(TOKEN_KEY)
      setUser(null)
    },
  }), [user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function Shell({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return (
    <div className="theme-shell mx-auto min-h-screen max-w-7xl pb-24 md:pb-0">
      <header className="theme-header sticky top-0 z-10 border-b px-5 py-4 md:px-8">
        <div className="flex items-center justify-between gap-6">
          <Link to={user ? '/discover' : '/join/jprime'} className="jprime-brand shrink-0">
            <img className="jprime-logo" src="https://jprime.io/images/jprime-small.png" alt="jPrime" />
            <span>
              <span className="theme-title block text-lg">jPrime Connect</span>
              <span className="theme-muted hidden text-xs sm:block">Meet the right people between the talks.</span>
            </span>
          </Link>
          <div className="flex items-center gap-3">
            {user && <DesktopNav />}
            <ThemeToggle />
          </div>
        </div>
      </header>
      <main className="px-5 py-6 md:px-8 md:py-10">{children}</main>
      {user && <BottomNav />}
    </div>
  )
}

function ThemeToggle() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const stored = localStorage.getItem(THEME_KEY)
    if (stored === 'light' || stored === 'dark') return stored
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  return <button type="button" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} className="theme-button-secondary rounded-md border px-3 py-2 text-sm">
    {theme === 'dark' ? 'Light' : 'Dark'}
  </button>
}

function DesktopNav() {
  const items = [['/discover', 'Discover'], ['/agenda', 'Agenda'], ['/discussions', 'Discussions'], ['/forum', 'Forum'], ['/assistant', 'Assistant'], ['/matches', 'Matches'], ['/meetings', 'Meetings'], ['/profile', 'Profile']]
  return <nav className="hidden items-center gap-2 md:flex">
    {items.map(([href, label]) => <NavLink key={href} to={href} className={({ isActive }) => `theme-button-ghost rounded-md px-3 py-2 text-sm ${isActive ? 'jprime-nav-active' : ''}`}>{label}</NavLink>)}
  </nav>
}

function BottomNav() {
  const items = [['/discover', 'Discover'], ['/agenda', 'Agenda'], ['/discussions', 'Rooms'], ['/forum', 'Forum'], ['/assistant', 'Assistant'], ['/matches', 'Matches'], ['/meetings', 'Meetings'], ['/profile', 'Profile']]
  return <nav className="theme-header fixed bottom-0 left-1/2 z-20 grid w-full max-w-2xl -translate-x-1/2 grid-cols-8 border-t px-2 py-2 text-xs md:hidden">
    {items.map(([href, label]) => <NavLink key={href} to={href} className={({ isActive }) => `theme-button-ghost rounded-md px-2 py-2 text-center ${isActive ? 'jprime-nav-active' : ''}`}>{label}</NavLink>)}
  </nav>
}

function Button({ children, variant = 'primary', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' }) {
  const cls = variant === 'primary' ? 'theme-button-primary' : variant === 'secondary' ? 'theme-button-secondary border' : 'theme-button-ghost border'
  return <button {...props} className={`rounded-md px-4 py-2.5 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${cls} ${props.className ?? ''}`}>{children}</button>
}

function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`theme-card rounded-md border p-5 ${className}`}>{children}</section>
}

function Chip({ children, active = false, onClick }: { children: ReactNode; active?: boolean; onClick?: () => void }) {
  return <button type="button" onClick={onClick} className={`rounded-md border px-3 py-1.5 text-sm ${active ? 'theme-chip-active' : 'theme-chip'}`}>{children}</button>
}

function Field(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`theme-field w-full rounded-md border px-3 py-2.5 focus:outline-none ${props.className ?? ''}`} />
}

function TextArea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={`theme-field min-h-24 w-full rounded-md border px-3 py-2.5 focus:outline-none ${props.className ?? ''}`} />
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <Shell><p>Loading...</p></Shell>
  if (!user) return <Navigate to={`/login?returnTo=${encodeURIComponent(location.pathname)}`} replace />
  if (!user.profileCompleted && location.pathname !== '/onboarding') return <Navigate to="/onboarding" replace />
  return <>{children}</>
}

function ChipList({ title, values }: { title: string; values: string[] }) {
  if (!values.length) return null
  return <div><h3 className="theme-title mb-2 text-sm">{title}</h3><div className="flex flex-wrap gap-2">{values.map((value) => <span key={value} className="theme-soft rounded-md border px-2.5 py-1 text-sm">{value}</span>)}</div></div>
}

function LandingPage() {
  const { user } = useAuth()
  if (user?.profileCompleted) return <Navigate to="/discover" replace />
  if (user) return <Navigate to="/onboarding" replace />
  return <Shell><div className="grid gap-6 lg:grid-cols-[1.35fr_0.65fr] lg:items-start">
    <Card className="jprime-hero-card lg:min-h-[420px]">
      <p className="jprime-kicker mb-4">jPrime event companion</p>
      <h1 className="theme-title max-w-3xl text-5xl">Meet the right people between the talks.</h1>
      <p className="theme-copy mt-5 max-w-2xl text-lg leading-8">Scan. Join. Pick your interests. Discover attendees with similar technologies, goals, and lectures worth discussing.</p>
      <div className="mt-6 grid grid-cols-2 gap-3"><Link to="/register"><Button className="w-full">Join now</Button></Link><Link to="/login"><Button variant="secondary" className="w-full">Login</Button></Link></div>
    </Card>
    <Card className="lg:sticky lg:top-28"><h2 className="theme-title mb-3 text-xl">How it works</h2>{['Create your profile', 'Select interests and talks', 'Swipe through attendees', 'Match and meet during breaks'].map((step, index) => <p key={step} className="theme-border theme-copy border-t py-3">{index + 1}. {step}</p>)}</Card>
  </div></Shell>
}

function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const params = new URLSearchParams(useLocation().search)
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (mode === 'register' && password !== confirm) return toast.error('Passwords do not match')
    setBusy(true)
    try {
      if (mode === 'register') await register(fullName, email, password)
      else await login(email, password)
      navigate(params.get('returnTo') ?? (mode === 'register' ? '/onboarding' : '/discover'))
    } catch (error) {
      toast.error(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }
  return <Shell><div className="mx-auto max-w-lg"><Card><h1 className="theme-title mb-1 text-3xl">{mode === 'register' ? 'Join jPrime Connect' : 'Welcome back'}</h1><p className="theme-muted mb-6">Use your event profile to discover people worth meeting.</p><form onSubmit={submit} className="space-y-3">
    {mode === 'register' && <Field required placeholder="Full name" value={fullName} onChange={(e) => setFullName(e.target.value)} />}
    <Field required type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
    <Field required type="password" minLength={8} placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
    {mode === 'register' && <Field required type="password" minLength={8} placeholder="Confirm password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />}
    <Button disabled={busy} className="w-full">{mode === 'register' ? 'Create account' : 'Login'}</Button>
  </form><Link className="theme-link mt-5 block text-center text-sm font-medium" to={mode === 'register' ? '/login' : '/register'}>{mode === 'register' ? 'Already registered? Login' : 'New here? Join now'}</Link></Card></div></Shell>
}

function OnboardingPage() {
  const { refresh } = useAuth()
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [interests, setInterests] = useState<Lookup[]>([])
  const [goals, setGoals] = useState<Lookup[]>([])
  const [talks, setTalks] = useState<Talk[]>([])
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [selectedGoals, setSelectedGoals] = useState<string[]>([])
  const [talkSelections, setTalkSelections] = useState<Record<string, string | undefined>>({})
  const [profile, setProfile] = useState({ fullName: '', roleTitle: '', company: '', bio: '', linkedinUrl: '', githubUrl: '' })
  useEffect(() => { void Promise.all([api.get<Lookup[]>('/interests'), api.get<Lookup[]>('/goals'), api.get<Talk[]>('/talks'), api.get<Profile>('/users/me')]).then(([i, g, t, p]) => { setInterests(i.data); setGoals(g.data); setTalks(t.data); setProfile((old) => ({ ...old, fullName: p.data.fullName })) }) }, [])
  const toggle = (list: string[], id: string, setter: (value: string[]) => void) => setter(list.includes(id) ? list.filter((x) => x !== id) : [...list, id])
  const toggleTalk = (talkId: string, status: string) => setTalkSelections((current) => ({ ...current, [talkId]: current[talkId] === status ? undefined : status }))
  const finish = async () => {
    try {
      await api.put('/users/me', { ...profile, interestIds: selectedInterests, goalIds: selectedGoals })
      await api.put('/users/me/talk-selections', { selections: Object.entries(talkSelections).filter(([, status]) => status).map(([talkId, status]) => ({ talkId, statuses: [status] })) })
      await refresh()
      navigate('/discover')
    } catch (error) { toast.error(errorMessage(error)) }
  }
  const statuses = ['INTERESTED', 'ATTENDED', 'WANT_TO_DISCUSS', 'MISSED_WANT_RECAP']
  return <Shell><div className="mb-5"><p className="theme-link text-sm font-medium">Step {step + 1} of 4</p><h1 className="theme-title text-3xl">{['Profile', 'Interests', 'Looking For', 'Talks'][step]}</h1></div><Card>
    {step === 0 && <div className="space-y-3"><Field placeholder="Full name" value={profile.fullName} onChange={(e) => setProfile({ ...profile, fullName: e.target.value })} /><Field placeholder="Role/title" value={profile.roleTitle} onChange={(e) => setProfile({ ...profile, roleTitle: e.target.value })} /><Field placeholder="Company optional" value={profile.company} onChange={(e) => setProfile({ ...profile, company: e.target.value })} /><TextArea placeholder="Short bio" value={profile.bio} onChange={(e) => setProfile({ ...profile, bio: e.target.value })} /><Field placeholder="LinkedIn URL optional" value={profile.linkedinUrl} onChange={(e) => setProfile({ ...profile, linkedinUrl: e.target.value })} /><Field placeholder="GitHub URL optional" value={profile.githubUrl} onChange={(e) => setProfile({ ...profile, githubUrl: e.target.value })} /></div>}
    {step === 1 && <div className="flex flex-wrap gap-2">{interests.map((item) => <Chip key={item.id} active={selectedInterests.includes(item.id)} onClick={() => toggle(selectedInterests, item.id, setSelectedInterests)}>{item.name}</Chip>)}</div>}
    {step === 2 && <div className="flex flex-wrap gap-2">{goals.map((item) => <Chip key={item.id} active={selectedGoals.includes(item.id)} onClick={() => toggle(selectedGoals, item.id, setSelectedGoals)}>{item.name}</Chip>)}</div>}
    {step === 3 && <div className="grid gap-4 lg:grid-cols-2">{talks.map((talk) => <div key={talk.id} className="theme-card rounded-md border p-4"><h3 className="theme-title">{talk.title}</h3><p className="theme-muted text-sm">{talk.speaker} · {talk.hall}</p><div className="mt-3 flex flex-wrap gap-2">{statuses.map((status) => <Chip key={status} active={talkSelections[talk.id] === status} onClick={() => toggleTalk(talk.id, status)}>{status.replaceAll('_', ' ')}</Chip>)}</div></div>)}</div>}
  </Card><div className="mt-5 grid grid-cols-2 gap-3"><Button variant="ghost" disabled={step === 0} onClick={() => setStep(step - 1)}>Back</Button>{step < 3 ? <Button onClick={() => setStep(step + 1)}>Next</Button> : <Button onClick={finish}>Start discovering</Button>}</div></Shell>
}

function DiscoverPage() {
  const [candidates, setCandidates] = useState<Candidate[]>([])
  const [loading, setLoading] = useState(true)
  const [swipeAnimation, setSwipeAnimation] = useState<'SKIP' | 'MAYBE_LATER' | 'CONNECT' | null>(null)
  const current = candidates[0]
  useEffect(() => { void api.get<Candidate[]>('/discover').then((r) => setCandidates(r.data)).finally(() => setLoading(false)) }, [])
  const swipe = async (action: 'SKIP' | 'MAYBE_LATER' | 'CONNECT') => {
    if (!current || swipeAnimation) return
    setSwipeAnimation(action)
    try {
      const { data } = await api.post<{ matched: boolean }>('/swipes', { toUserId: current.userId, action })
      toast.success(data.matched ? 'Mutual match created!' : action === 'CONNECT' ? 'Connection sent' : 'Saved')
    } catch (error) {
      setSwipeAnimation(null)
      toast.error(errorMessage(error))
      return
    }
    window.setTimeout(() => {
      setCandidates((items) => items.slice(1))
      setSwipeAnimation(null)
    }, 320)
  }
  const cardAnimationClass = swipeAnimation === 'SKIP' ? 'discover-card-skip' : swipeAnimation === 'MAYBE_LATER' ? 'discover-card-maybe' : swipeAnimation === 'CONNECT' ? 'discover-card-connect' : ''
  return <Shell><div className="mx-auto max-w-4xl">
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="theme-title text-3xl">Discover</h1>
        <p className="theme-muted mt-1">Choose Connect, Maybe, or Skip for each attendee.</p>
      </div>
      {!loading && <div className="theme-soft rounded-md border px-3 py-2 text-sm font-medium">{candidates.length} {candidates.length === 1 ? 'person' : 'people'} left</div>}
    </div>
    {loading ? <Card>Loading attendees...</Card> : !current ? <Card><p className="theme-title text-lg">No more people for now.</p><p className="theme-muted">Try updating your interests or selected talks.</p></Card> : <div className="discover-card-stage"><Card className={`discover-swipe-card space-y-5 ${cardAnimationClass}`}>
      {swipeAnimation && <div className={`discover-action-badge ${swipeAnimation === 'CONNECT' ? 'connect' : swipeAnimation === 'MAYBE_LATER' ? 'maybe' : 'skip'}`}>{swipeAnimation === 'CONNECT' ? 'Connect' : swipeAnimation === 'MAYBE_LATER' ? 'Maybe Later' : 'Skip'}</div>}
      <div className="flex items-start justify-between gap-4"><div><h2 className="theme-title text-3xl">{current.fullName}</h2><p className="theme-copy">{current.roleTitle} {current.company && `at ${current.company}`}</p></div><div className="theme-soft rounded-md border px-3 py-2 text-xl font-medium">{current.score}%</div></div>
      <p className="theme-copy">{current.bio}</p>
      <div className="grid gap-5 lg:grid-cols-2"><ChipList title="Shared interests" values={current.sharedInterests} /><ChipList title="Shared goals" values={current.sharedGoals} /></div>
      <div><h3 className="theme-title mb-2">Shared talks</h3>{current.sharedTalks.map((talk) => <p key={talk.talkId} className="theme-soft mb-2 rounded-md border p-3 text-sm">{talk.title}<br /><span className="theme-muted">{talk.reason}</span></p>)}</div>
      <div className="theme-soft rounded-md border p-4"><p className="theme-muted text-sm">Suggested opener</p><p className="theme-title">{current.icebreaker}</p></div>
      <div className="grid grid-cols-3 gap-2"><Button disabled={!!swipeAnimation} variant="ghost" onClick={() => swipe('SKIP')}>Skip</Button><Button disabled={!!swipeAnimation} variant="secondary" onClick={() => swipe('MAYBE_LATER')}>Maybe</Button><Button disabled={!!swipeAnimation} onClick={() => swipe('CONNECT')}>Connect</Button></div>
    </Card></div>}
  </div></Shell>
}

function AgendaPage() {
  const talks = OFFICIAL_AGENDA
  const [selectedTalk, setSelectedTalk] = useState<Talk | null>(null)
  const [selectedDay, setSelectedDay] = useState<string | null>(null)
  const days = Array.from(new Set(talks.map((talk) => talk.startTime?.slice(0, 10)).filter(Boolean)))
  useEffect(() => { if (!selectedDay && days.length > 0) setSelectedDay(days[0]) }, [days, selectedDay])
  const formatTime = (value: string) => new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  const formatDay = (value: string) => new Date(`${value}T00:00:00`).toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' })
  const visibleDays = selectedDay ? [selectedDay] : days
  return <Shell><div className="mb-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between"><div><h1 className="theme-title text-3xl">Agenda</h1><p className="theme-muted mt-1">Official jPrime PWA schedule: Hall A, Hall B, and Workshops & Deep Dives.</p></div><div className="flex flex-wrap gap-2">{days.map((day) => <Chip key={day} active={selectedDay === day} onClick={() => setSelectedDay(day)}>{formatDay(day)}</Chip>)}</div></div>{talks.length === 0 && <Card>No agenda sessions found.</Card>}<div className="space-y-8">{visibleDays.map((day) => {
    const dayTalks = talks.filter((talk) => talk.startTime?.startsWith(day))
    const slots = Array.from(new Set(dayTalks.map((talk) => talk.startTime))).sort()
    return <section key={day}>
      <div className="theme-card mb-4 rounded-md border px-5 py-4">
        <p className="theme-muted text-xs font-medium uppercase tracking-[0.12em]">Day schedule</p>
        <h2 className="theme-title text-2xl">{formatDay(day)}</h2>
      </div>
      <div className="theme-card overflow-x-auto rounded-md border">
        <div className="theme-soft grid min-w-[980px] grid-cols-[6rem_1fr_1fr_1fr] border-b text-sm font-medium">
          <div className="px-4 py-3">Time</div>
          <div className="theme-border border-l px-4 py-3">Hall A</div>
          <div className="theme-border border-l px-4 py-3">Hall B</div>
          <div className="theme-border border-l px-4 py-3">Workshops & Deep Dives</div>
        </div>
        {slots.map((slot) => {
          const hallA = dayTalks.find((talk) => talk.startTime === slot && talk.hall === 'Hall A')
          const hallB = dayTalks.find((talk) => talk.startTime === slot && talk.hall === 'Hall B')
          const workshops = dayTalks.find((talk) => talk.startTime === slot && talk.hall === 'Workshops & Deep Dives')
          return <div key={slot} className="theme-border grid min-w-[980px] grid-cols-[6rem_1fr_1fr_1fr] border-b last:border-b-0">
            <div className="theme-soft px-4 py-5 text-sm font-medium">{formatTime(slot)}</div>
            <AgendaSlot talk={hallA} onSelect={setSelectedTalk} />
            <AgendaSlot talk={hallB} onSelect={setSelectedTalk} />
            <AgendaSlot talk={workshops} onSelect={setSelectedTalk} />
          </div>
        })}
      </div>
    </section>
  })}</div>{selectedTalk && <AgendaDetail talk={selectedTalk} onClose={() => setSelectedTalk(null)} />}</Shell>
}

function AgendaSlot({ talk, onSelect }: { talk?: Talk; onSelect: (talk: Talk) => void }) {
  if (!talk) {
    return <div className="theme-border theme-muted border-l px-4 py-5 text-sm">-</div>
  }
  return <button type="button" onClick={() => onSelect(talk)} className="theme-border theme-cell border-l px-4 py-5 text-left transition focus:outline-none">
    <h3 className="theme-title">{talk.title}</h3>
    {talk.speaker && <p className="theme-muted mt-1 text-sm">{talk.speaker}</p>}
    <div className="mt-3 flex flex-wrap gap-2">
      <span className="theme-chip rounded-md border px-2 py-1 text-xs">{new Date(talk.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - {new Date(talk.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
      {talk.beginnerFriendly && <span className="theme-success rounded-md border px-2 py-1 text-xs font-medium">Beginner friendly</span>}
      {talk.talkLevel && !talk.beginnerFriendly && <span className="theme-soft rounded-md border px-2 py-1 text-xs">{talk.talkLevel}</span>}
    </div>
  </button>
}

function AgendaDetail({ talk, onClose }: { talk: Talk; onClose: () => void }) {
  const time = `${new Date(talk.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${new Date(talk.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  return <div className="theme-overlay fixed inset-0 z-30 grid place-items-center p-4" onClick={onClose}>
    <div className="theme-modal max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-md border p-6" onClick={(event) => event.stopPropagation()}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="theme-muted text-xs font-medium uppercase tracking-[0.12em]">{talk.hall} · {time}</p>
          <h2 className="theme-title mt-2 text-3xl">{talk.title}</h2>
          {talk.speaker && <p className="theme-copy mt-2">{talk.speaker}</p>}
        </div>
        <button type="button" onClick={onClose} className="theme-button-secondary rounded-md border px-3 py-1 text-sm">Close</button>
      </div>
      <div className="mt-5 flex flex-wrap gap-2">
        {talk.talkLevel && <span className="theme-soft rounded-md border px-3 py-1.5 text-sm">{talk.talkLevel}</span>}
        <span className={`rounded-md border px-3 py-1.5 text-sm ${talk.beginnerFriendly ? 'theme-success' : 'theme-soft'}`}>{talk.beginnerFriendly ? 'Beginner friendly' : 'Best with prior experience'}</span>
        {talk.format && <span className="theme-soft rounded-md border px-3 py-1.5 text-sm">{talk.format}</span>}
      </div>
      {talk.description ? <p className="theme-copy mt-6 whitespace-pre-line leading-7">{talk.description}</p> : <p className="theme-copy mt-6 leading-7">This is a conference schedule item from the official jPrime app.</p>}
      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        {talk.audience && <div className="theme-soft rounded-md border p-4"><h3 className="theme-title">Good for</h3><p className="theme-copy mt-2 text-sm">{talk.audience}</p></div>}
        {talk.takeaways && talk.takeaways.length > 0 && <div className="theme-soft rounded-md border p-4"><h3 className="theme-title">What you will get</h3><ul className="theme-copy mt-2 space-y-1 text-sm">{talk.takeaways.map((item) => <li key={item}>• {item}</li>)}</ul></div>}
      </div>
      {talk.tags.length > 0 && <div className="mt-6"><ChipList title="Topics" values={talk.tags} /></div>}
      {talk.sourceUrl && <a href={talk.sourceUrl} target="_blank" rel="noreferrer" className="theme-button-primary mt-6 inline-flex rounded-md px-4 py-2.5 text-sm font-medium">Open official jPrime page</a>}
    </div>
  </div>
}

function DiscussionsPage() {
  const [rooms, setRooms] = useState<TalkDiscussion[]>([])
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    void api.get<TalkDiscussion[]>('/talk-discussions')
      .then((response) => setRooms(response.data))
      .catch((error) => toast.error(errorMessage(error)))
      .finally(() => setLoading(false))
  }, [])
  const formatTalkTime = (value: string) => new Date(value).toLocaleString([], { weekday: 'short', hour: '2-digit', minute: '2-digit' })
  return <Shell><div className="mb-5"><h1 className="theme-title text-3xl">Talk discussion rooms</h1><p className="theme-muted mt-1">Find attendees who marked a talk as “want to discuss”.</p></div>{loading ? <Card>Loading discussion rooms...</Card> : rooms.length === 0 ? <Card>No one has marked talks for discussion yet.</Card> : <div className="grid gap-4 lg:grid-cols-2">{rooms.map((room) => <Card key={room.talkId} className="space-y-4"><div><p className="theme-muted text-sm">{formatTalkTime(room.startTime)} · {room.hall}</p><h2 className="theme-title text-xl">{room.title}</h2>{room.speaker && <p className="theme-muted text-sm">{room.speaker}</p>}</div><p className="theme-soft inline-flex rounded-md border px-2.5 py-1 text-sm">{room.attendeeCount} want to discuss</p><div className="space-y-3">{room.attendees.map((attendee) => <div key={attendee.userId} className="theme-soft rounded-md border p-3"><div className="flex items-start justify-between gap-3"><div><h3 className="theme-title">{attendee.fullName}</h3><p className="theme-muted text-sm">{attendee.roleTitle}{attendee.company && ` at ${attendee.company}`}</p></div>{attendee.matched && <span className="theme-success rounded-md border px-2 py-1 text-xs">Matched</span>}</div>{attendee.bio && <p className="theme-copy mt-2 text-sm">{attendee.bio}</p>}<Link to={`/connect/${attendee.publicId}`}><Button variant="secondary" className="mt-3 w-full">{attendee.matched ? 'Open profile' : 'Connect'}</Button></Link></div>)}</div></Card>)}</div>}</Shell>
}

function ForumPage() {
  const [topics, setTopics] = useState<ForumTopic[]>([])
  const [selectedTopicId, setSelectedTopicId] = useState<string | null>(null)
  const [comments, setComments] = useState<ForumComment[]>([])
  const [topicForm, setTopicForm] = useState({ title: '', category: 'General', body: '' })
  const [commentBody, setCommentBody] = useState('')
  const [loading, setLoading] = useState(true)
  const selectedTopic = topics.find((topic) => topic.id === selectedTopicId) ?? topics[0]

  const loadTopics = async () => {
    setLoading(true)
    try {
      const { data } = await api.get<ForumTopic[]>('/forum/topics')
      setTopics(data)
      setSelectedTopicId((current) => current ?? data[0]?.id ?? null)
    } catch (error) { toast.error(errorMessage(error)) }
    finally { setLoading(false) }
  }

  const loadComments = async (topicId: string) => {
    try { setComments((await api.get<ForumComment[]>(`/forum/topics/${topicId}/comments`)).data) }
    catch (error) { toast.error(errorMessage(error)) }
  }

  useEffect(() => { void loadTopics() }, [])
  useEffect(() => { if (selectedTopic?.id) void loadComments(selectedTopic.id) }, [selectedTopic?.id])

  const createTopic = async () => {
    try {
      const { data } = await api.post<ForumTopic>('/forum/topics', topicForm)
      setTopicForm({ title: '', category: 'General', body: '' })
      await loadTopics()
      setSelectedTopicId(data.id)
      toast.success('Forum topic created')
    } catch (error) { toast.error(errorMessage(error)) }
  }

  const createComment = async () => {
    if (!selectedTopic) return
    try {
      await api.post(`/forum/topics/${selectedTopic.id}/comments`, { body: commentBody })
      setCommentBody('')
      await loadComments(selectedTopic.id)
      await loadTopics()
    } catch (error) { toast.error(errorMessage(error)) }
  }

  const authorLine = (item: { authorName: string; authorRoleTitle?: string; authorCompany?: string }) => `${item.authorName}${item.authorRoleTitle ? ` · ${item.authorRoleTitle}` : ''}${item.authorCompany ? ` at ${item.authorCompany}` : ''}`
  return <Shell><div className="mb-5"><h1 className="theme-title text-3xl">Forum</h1><p className="theme-muted mt-1">Create topics, ask questions, and continue jPrime discussions.</p></div><div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]"><div className="space-y-4"><Card className="space-y-3"><h2 className="theme-title text-xl">Start a theme</h2><Field placeholder="Title" value={topicForm.title} onChange={(e) => setTopicForm({ ...topicForm, title: e.target.value })} /><Field placeholder="Category" value={topicForm.category} onChange={(e) => setTopicForm({ ...topicForm, category: e.target.value })} /><TextArea placeholder="What should people discuss?" value={topicForm.body} onChange={(e) => setTopicForm({ ...topicForm, body: e.target.value })} /><Button onClick={createTopic} className="w-full">Create theme</Button></Card>{loading ? <Card>Loading forum...</Card> : topics.map((topic) => <button key={topic.id} type="button" onClick={() => setSelectedTopicId(topic.id)} className={`theme-card w-full rounded-md border p-4 text-left ${selectedTopic?.id === topic.id ? 'outline outline-2 outline-[var(--primary)]' : ''}`}><p className="theme-muted text-sm">{topic.category} · {topic.commentCount} comments</p><h2 className="theme-title">{topic.title}</h2><p className="theme-copy mt-2 line-clamp-2 text-sm">{topic.body}</p></button>)}</div><div>{selectedTopic ? <Card className="space-y-4"><div><p className="theme-muted text-sm">{selectedTopic.category}</p><h2 className="theme-title text-2xl">{selectedTopic.title}</h2><p className="theme-muted text-sm">{authorLine(selectedTopic)}</p><p className="theme-copy mt-4">{selectedTopic.body}</p></div><div className="theme-border border-t pt-4"><h3 className="theme-title mb-3">Comments</h3><div className="space-y-3">{comments.length === 0 ? <p className="theme-muted text-sm">No comments yet. Be the first to reply.</p> : comments.map((comment) => <div key={comment.id} className="theme-soft rounded-md border p-3"><p className="theme-muted text-sm">{authorLine(comment)}</p><p className="theme-copy mt-1">{comment.body}</p></div>)}</div></div><div className="theme-border border-t pt-4"><TextArea placeholder="Write a reply" value={commentBody} onChange={(e) => setCommentBody(e.target.value)} /><Button onClick={createComment} className="mt-3 w-full">Post comment</Button></div></Card> : <Card>Select a forum topic.</Card>}</div></div></Shell>
}

function AssistantPage() {
  const [prompt, setPrompt] = useState('Recommend talks for a backend Java developer interested in AI.')
  const [message, setMessage] = useState('')
  const [recommendations, setRecommendations] = useState<Recommendation[]>([])
  const ask = async () => {
    try {
      const { data } = await api.post<RecommendationResponse>('/assistant/recommend-talks', { prompt })
      setMessage(data.message)
      setRecommendations(data.recommendations)
    }
    catch (error) { toast.error(errorMessage(error)) }
  }
  return <Shell><h1 className="theme-title mb-5 text-3xl">Ask the jPrime Companion</h1><Card className="space-y-4"><TextArea value={prompt} onChange={(e) => setPrompt(e.target.value)} /><Button onClick={ask} className="w-full"><Sparkles className="mr-2 inline h-4 w-4" /> Ask assistant</Button>{['I like Java and Spring. What should I attend?', 'I want to discuss AI agents with people.', 'Where is the coffee area?'].map((p) => <Chip key={p} onClick={() => setPrompt(p)}>{p}</Chip>)}</Card><div className="mt-4 space-y-3">{message && <Card><p className="theme-copy">{message}</p></Card>}{recommendations.map((r) => <Card key={r.talkId}><h2 className="theme-title">{r.title}</h2><p className="theme-link text-sm font-medium">{r.score}% relevant</p><p className="theme-copy mt-2">{r.reason}</p></Card>)}</div></Shell>
}

function MatchesPage() {
  const [matches, setMatches] = useState<MatchSummary[]>([])
  useEffect(() => { void api.get<MatchSummary[]>('/matches').then((r) => setMatches(r.data)) }, [])
  return <Shell><h1 className="theme-title mb-5 text-3xl">Matches</h1>{matches.length === 0 ? <Card>No matches yet. Connect with people in Discover.</Card> : <div className="space-y-4">{matches.map((m) => <Card key={m.matchId}><h2 className="theme-title text-xl">You matched with {m.fullName}</h2><p className="theme-muted">{m.roleTitle} {m.company && `at ${m.company}`}</p><ChipList title="Shared" values={m.sharedInterests} /><p className="theme-copy mt-3 text-sm">{m.icebreaker}</p><p className="theme-muted mt-2 text-xs">Meeting: {m.meetingStatus}</p><Link to={`/matches/${m.matchId}`}><Button className="mt-4 w-full">Open</Button></Link></Card>)}</div>}</Shell>
}

function MatchDetailPage() {
  const { matchId } = useParams()
  const [match, setMatch] = useState<MatchDetail | null>(null)
  useEffect(() => { void api.get<MatchDetail>(`/matches/${matchId}`).then((r) => setMatch(r.data)) }, [matchId])
  if (!match) return <Shell><Card>Loading match...</Card></Shell>
  return <Shell><Card className="space-y-4"><h1 className="theme-title text-3xl">{match.otherUser.fullName}</h1><p className="theme-copy">{match.otherUser.roleTitle} {match.otherUser.company && `at ${match.otherUser.company}`}</p><p className="theme-copy">{match.otherUser.bio}</p><ChipList title="Shared interests" values={match.sharedInterests} /><ChipList title="Shared goals" values={match.sharedGoals} /><div><h3 className="theme-title">Icebreakers</h3>{match.icebreakers.map((i) => <p key={i} className="theme-soft mt-2 rounded-md border p-3">{i}</p>)}</div><div className="theme-soft rounded-md border p-4"><h3 className="theme-title">Contact unlocked</h3><p>{match.otherUser.email}</p><p>{match.otherUser.linkedinUrl}</p><p>{match.otherUser.githubUrl}</p></div>{match.meeting ? <p className="theme-success rounded-md border p-4">Meeting: {match.meeting.title} · {match.meeting.location}</p> : <Link to={`/matches/${match.matchId}/meeting`}><Button className="w-full">Plan meeting</Button></Link>}</Card></Shell>
}

function MeetingPlannerPage() {
  const { matchId } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: 'Meet at jPrime', location: 'In front of Hall B', startTime: '2026-06-04T14:30', endTime: '2026-06-04T14:45', topic: 'Spring AI discussion', note: '' })
  const submit = async () => {
    try { await api.post(`/matches/${matchId}/meetings`, form); toast.success('Meeting planned'); navigate(`/matches/${matchId}`) }
    catch (error) { toast.error(errorMessage(error)) }
  }
  return <Shell><h1 className="theme-title mb-5 text-3xl">Plan meeting</h1><Card className="space-y-3"><Field value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /><div className="flex flex-wrap gap-2">{['In front of Hall A', 'In front of Hall B', 'Coffee area', 'Sponsor booths', 'Entrance', 'Lunch area'].map((l) => <Chip key={l} active={form.location === l} onClick={() => setForm({ ...form, location: l })}>{l}</Chip>)}</div><Field type="datetime-local" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /><Field type="datetime-local" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /><Field value={form.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })} /><TextArea placeholder="Note" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /><Button onClick={submit} className="w-full"><CalendarDays className="mr-2 inline h-4 w-4" /> Create meeting</Button></Card></Shell>
}

function MeetingsPage() {
  const [meetings, setMeetings] = useState<MeetingSummary[]>([])
  const [loading, setLoading] = useState(true)
  const load = async () => {
    setLoading(true)
    try { setMeetings((await api.get<MeetingSummary[]>('/meetings')).data) }
    catch (error) { toast.error(errorMessage(error)) }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [])
  const respond = async (meetingId: string, status: 'ACCEPTED' | 'DECLINED') => {
    try {
      await api.patch(`/meetings/${meetingId}/status`, { status })
      toast.success(status === 'ACCEPTED' ? 'Meeting accepted' : 'Meeting declined')
      await load()
    } catch (error) { toast.error(errorMessage(error)) }
  }
  const statusLabel = (status?: string) => status ? status.charAt(0) + status.slice(1).toLowerCase() : 'Pending'
  const formatRange = (meeting: MeetingSummary) => `${new Date(meeting.startTime).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })} - ${new Date(meeting.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  return <Shell><h1 className="theme-title mb-5 text-3xl">Meetings</h1>{loading ? <Card>Loading meetings...</Card> : meetings.length === 0 ? <Card>No meetings planned yet. Open a match and plan one.</Card> : <div className="grid gap-4 lg:grid-cols-2">{meetings.map((meeting) => <Card key={meeting.id} className="space-y-4"><div className="flex items-start justify-between gap-4"><div><h2 className="theme-title text-xl">{meeting.title}</h2><p className="theme-muted">{meeting.otherUserName}{meeting.otherUserRoleTitle && ` · ${meeting.otherUserRoleTitle}`}{meeting.otherUserCompany && ` at ${meeting.otherUserCompany}`}</p></div><span className={`rounded-md border px-2.5 py-1 text-xs ${meeting.status === 'ACCEPTED' ? 'theme-success' : 'theme-soft'}`}>{statusLabel(meeting.status)}</span></div><div className="theme-soft rounded-md border p-3 text-sm"><p>{formatRange(meeting)}</p><p>{meeting.location}</p>{meeting.topic && <p className="theme-muted mt-1">{meeting.topic}</p>}{meeting.note && <p className="theme-muted mt-1">{meeting.note}</p>}</div><div className="grid grid-cols-3 gap-2"><Link to={`/matches/${meeting.matchId}`}><Button variant="ghost" className="w-full">Match</Button></Link><Button variant="secondary" disabled={meeting.status === 'DECLINED'} onClick={() => respond(meeting.id, 'DECLINED')}>Decline</Button><Button disabled={meeting.status === 'ACCEPTED'} onClick={() => respond(meeting.id, 'ACCEPTED')}>Accept</Button></div></Card>)}</div>}</Shell>
}

function ProfilePage() {
  const { logout, user, refresh } = useAuth()
  const [profile, setProfile] = useState<Profile | null>(null)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [showQr, setShowQr] = useState(false)
  const [interests, setInterests] = useState<Lookup[]>([])
  const [goals, setGoals] = useState<Lookup[]>([])
  const [form, setForm] = useState({ fullName: '', roleTitle: '', company: '', bio: '', linkedinUrl: '', githubUrl: '', profilePhotoUrl: '', publicProfileEnabled: true, contactInfoVisibleAfterMatch: true })
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [selectedGoals, setSelectedGoals] = useState<string[]>([])
  const loadProfile = async () => {
    const { data } = await api.get<Profile>('/users/me')
    setProfile(data)
    setForm({
      fullName: data.fullName ?? '',
      roleTitle: data.roleTitle ?? '',
      company: data.company ?? '',
      bio: data.bio ?? '',
      linkedinUrl: data.linkedinUrl ?? '',
      githubUrl: data.githubUrl ?? '',
      profilePhotoUrl: data.profilePhotoUrl ?? '',
      publicProfileEnabled: data.publicProfileEnabled ?? true,
      contactInfoVisibleAfterMatch: data.contactInfoVisibleAfterMatch ?? true,
    })
    setSelectedInterests(interests.filter((item) => data.interests.includes(item.name)).map((item) => item.id))
    setSelectedGoals(goals.filter((item) => data.goals.includes(item.name)).map((item) => item.id))
  }
  useEffect(() => { void Promise.all([api.get<Lookup[]>('/interests'), api.get<Lookup[]>('/goals')]).then(([i, g]) => { setInterests(i.data); setGoals(g.data) }) }, [])
  useEffect(() => { if (interests.length || goals.length) void loadProfile(); else void api.get<Profile>('/users/me').then((r) => setProfile(r.data)) }, [interests, goals])
  const toggle = (list: string[], id: string, setter: (value: string[]) => void) => setter(list.includes(id) ? list.filter((x) => x !== id) : [...list, id])
  const save = async () => {
    setSaving(true)
    try {
      const { data } = await api.put<Profile>('/users/me', { ...form, interestIds: selectedInterests, goalIds: selectedGoals })
      setProfile(data)
      await refresh()
      setEditing(false)
      toast.success('Profile updated')
    } catch (error) { toast.error(errorMessage(error)) }
    finally { setSaving(false) }
  }
  const url = `${window.location.origin}/connect/${profile?.publicId ?? user?.publicId}`
  return <Shell><div className="mb-5 flex items-center justify-between gap-4"><h1 className="theme-title text-3xl">Profile</h1>{profile && <Button variant={editing ? 'ghost' : 'secondary'} onClick={() => setEditing(!editing)}>{editing ? 'Cancel' : 'Edit profile'}</Button>}</div>{profile && <Card className="space-y-5">{editing ? <div className="space-y-4"><div className="grid gap-3 lg:grid-cols-2"><Field placeholder="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /><Field placeholder="Role/title" value={form.roleTitle} onChange={(e) => setForm({ ...form, roleTitle: e.target.value })} /><Field placeholder="Company" value={form.company} onChange={(e) => setForm({ ...form, company: e.target.value })} /><Field placeholder="Profile photo URL" value={form.profilePhotoUrl} onChange={(e) => setForm({ ...form, profilePhotoUrl: e.target.value })} /><Field placeholder="LinkedIn URL" value={form.linkedinUrl} onChange={(e) => setForm({ ...form, linkedinUrl: e.target.value })} /><Field placeholder="GitHub URL" value={form.githubUrl} onChange={(e) => setForm({ ...form, githubUrl: e.target.value })} /></div><TextArea placeholder="Bio" value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} /><div><h3 className="theme-title mb-2 text-sm">Interests</h3><div className="flex flex-wrap gap-2">{interests.map((item) => <Chip key={item.id} active={selectedInterests.includes(item.id)} onClick={() => toggle(selectedInterests, item.id, setSelectedInterests)}>{item.name}</Chip>)}</div></div><div><h3 className="theme-title mb-2 text-sm">Looking for</h3><div className="flex flex-wrap gap-2">{goals.map((item) => <Chip key={item.id} active={selectedGoals.includes(item.id)} onClick={() => toggle(selectedGoals, item.id, setSelectedGoals)}>{item.name}</Chip>)}</div></div><label className="theme-copy flex items-center gap-2 text-sm"><input type="checkbox" checked={form.publicProfileEnabled} onChange={(e) => setForm({ ...form, publicProfileEnabled: e.target.checked })} /> Public profile visible</label><label className="theme-copy flex items-center gap-2 text-sm"><input type="checkbox" checked={form.contactInfoVisibleAfterMatch} onChange={(e) => setForm({ ...form, contactInfoVisibleAfterMatch: e.target.checked })} /> Show contact info after match</label><Button disabled={saving} onClick={save} className="w-full">{saving ? 'Saving...' : 'Save profile'}</Button></div> : <><h2 className="theme-title text-2xl">{profile.fullName}</h2>{profile.profilePhotoUrl && <img src={profile.profilePhotoUrl} alt={profile.fullName} className="h-24 w-24 rounded-md object-cover" />}<p className="theme-copy">{profile.roleTitle} {profile.company && `at ${profile.company}`}</p><p className="theme-copy">{profile.bio}</p><div className="grid gap-2 text-sm"><p className="theme-muted">LinkedIn: {profile.linkedinUrl || 'Not set'}</p><p className="theme-muted">GitHub: {profile.githubUrl || 'Not set'}</p><p className="theme-muted">Public profile: {profile.publicProfileEnabled === false ? 'Hidden' : 'Visible'}</p><p className="theme-muted">Contact after match: {profile.contactInfoVisibleAfterMatch === false ? 'Hidden' : 'Visible'}</p></div><ChipList title="Interested in" values={profile.interests} /><ChipList title="Looking for" values={profile.goals} /><ChipList title="Talks to discuss" values={profile.talksToDiscuss} /><Button onClick={() => setShowQr(true)} className="w-full"><QrCode className="mr-2 inline h-4 w-4" /> Show My QR</Button><Button variant="ghost" onClick={logout} className="w-full"><LogOut className="mr-2 inline h-4 w-4" /> Logout</Button></>}</Card>}{showQr && <div className="theme-overlay fixed inset-0 z-30 grid place-items-center p-5" onClick={() => setShowQr(false)}><Card><h2 className="theme-title text-xl">My jPrime Connect QR</h2><p className="theme-copy mb-4">Let someone scan this to connect with you.</p><div className="inline-block rounded-md bg-white p-3"><QRCodeSVG value={url} size={240} bgColor="#ffffff" fgColor="#111111" /></div><p className="theme-muted mt-4 break-all text-xs">{url}</p></Card></div>}</Shell>
}

function PublicConnectPage() {
  const { publicId } = useParams()
  const { user } = useAuth()
  const [profile, setProfile] = useState<Profile | null>(null)
  const navigate = useNavigate()
  useEffect(() => { void api.get<Profile>(`/users/public/${publicId}`).then((r) => setProfile(r.data)).catch(() => toast.error('Profile not found')) }, [publicId])
  const connect = async (action: string) => {
    if (!user) { navigate(`/login?returnTo=/connect/${publicId}`); return }
    if (profile?.id === user.id) { toast.error('You cannot connect with yourself'); return }
    const { data } = await api.post<{ matched: boolean }>('/swipes', { toUserId: profile?.id, action })
    toast.success(data.matched ? 'Mutual match created!' : 'Connection saved')
    navigate('/discover')
  }
  return <Shell>{profile ? <Card className="space-y-4"><h1 className="theme-title text-3xl">Connect with {profile.fullName}?</h1><p className="theme-copy">{profile.roleTitle} {profile.company && `at ${profile.company}`}</p><p className="theme-copy">{profile.bio}</p><ChipList title="Interests" values={profile.interests} /><ChipList title="Looking for" values={profile.goals} />{!user && <p className="theme-soft rounded-md border p-3">Register or login to connect with this person.</p>}<div className="grid grid-cols-2 gap-3"><Button variant="secondary" onClick={() => connect('MAYBE_LATER')}>Maybe Later</Button><Button onClick={() => connect('CONNECT')}>Connect</Button></div></Card> : <Card>Loading profile...</Card>}</Shell>
}

function App() {
  return <Router><AuthProvider><Toaster position="top-center" toastOptions={{ style: { background: 'var(--surface)', color: 'var(--text)', border: '1px solid var(--border)', borderRadius: '6px' } }} /><Routes>
    <Route path="/" element={<Navigate to="/join/jprime" replace />} />
    <Route path="/join/jprime" element={<LandingPage />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/connect/:publicId" element={<PublicConnectPage />} />
    <Route path="/onboarding" element={<ProtectedRoute><OnboardingPage /></ProtectedRoute>} />
    <Route path="/discover" element={<ProtectedRoute><DiscoverPage /></ProtectedRoute>} />
    <Route path="/agenda" element={<ProtectedRoute><AgendaPage /></ProtectedRoute>} />
    <Route path="/discussions" element={<ProtectedRoute><DiscussionsPage /></ProtectedRoute>} />
    <Route path="/forum" element={<ProtectedRoute><ForumPage /></ProtectedRoute>} />
    <Route path="/assistant" element={<ProtectedRoute><AssistantPage /></ProtectedRoute>} />
    <Route path="/matches" element={<ProtectedRoute><MatchesPage /></ProtectedRoute>} />
    <Route path="/meetings" element={<ProtectedRoute><MeetingsPage /></ProtectedRoute>} />
    <Route path="/matches/:matchId" element={<ProtectedRoute><MatchDetailPage /></ProtectedRoute>} />
    <Route path="/matches/:matchId/meeting" element={<ProtectedRoute><MeetingPlannerPage /></ProtectedRoute>} />
    <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
  </Routes></AuthProvider></Router>
}

export default App
