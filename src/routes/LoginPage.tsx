import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from '@tanstack/react-router'
import { KeyRound, LogIn } from 'lucide-react'
import { useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { claimCurrentDevice, loginOwner, registerOwner } from '../lib/auth'

export function LoginPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  const loginMutation = useMutation({
    mutationFn: () => loginOwner(email, password),
    onSuccess: async () => {
      await claimCurrentDevice().catch(() => undefined)
      await queryClient.invalidateQueries()
      await navigate({ to: '/account' })
    },
    onError: (error) => setMessage(error instanceof Error ? error.message : '登录失败'),
  })

  const initMutation = useMutation({
    mutationFn: () => registerOwner(email, password),
    onSuccess: () => setMessage('Owner 已初始化，现在可以登录。'),
    onError: (error) => setMessage(error instanceof Error ? error.message : '初始化失败'),
  })

  return (
    <section className="page-stack narrow-page">
      <PageHeader
        eyebrow="Owner login"
        title="个人同步登录"
        description="只允许 OWNER_EMAIL 初始化和登录。未登录时仍使用本设备匿名进度。"
      />

      <form className="correction-form auth-form">
        <label htmlFor="owner-email">邮箱</label>
        <input id="owner-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" />
        <label htmlFor="owner-password">密码</label>
        <input
          id="owner-password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
        />
        <button className="primary-action" type="button" disabled={loginMutation.isPending} onClick={() => loginMutation.mutate()}>
          <LogIn size={18} />
          <span>{loginMutation.isPending ? '登录中' : '登录并同步本设备'}</span>
        </button>
        <Link className="icon-button secondary" to="/account">
          <KeyRound size={18} />
          <span>查看当前账号状态</span>
        </Link>
        {message ? <p className="error-text">{message}</p> : null}
      </form>

      <details className="source-preview">
        <summary>首次 owner 初始化</summary>
        <span>仅当数据库还没有 owner 且邮箱等于 Worker 环境变量 OWNER_EMAIL 时可用。</span>
        <button className="icon-button secondary" type="button" disabled={initMutation.isPending} onClick={() => initMutation.mutate()}>
          <KeyRound size={18} />
          <span>{initMutation.isPending ? '初始化中' : '初始化 owner'}</span>
        </button>
      </details>
    </section>
  )
}
