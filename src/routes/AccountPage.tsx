import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from '@tanstack/react-router'
import { KeyRound, LogOut, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { aiGatewayModels, type GatewayModel } from '../lib/aiModels'
import { setPreferredGatewayModel, usePreferredGatewayModel } from '../lib/aiPreferences'
import { changeOwnerPassword, claimCurrentDevice, logoutOwner, useAuthMe } from '../lib/auth'
import { getDeviceId } from '../lib/progress'
import { reasoningEfforts, setPreferredReasoningEffort, usePreferredReasoningEffort, type ReasoningEffort } from '../lib/reasoningPreferences'
import { setPreferredJapaneseVoice, usePreferredJapaneseVoice } from '../lib/voicePreferences'
import { japaneseVoices, type JapaneseVoice } from '../server/tts'

export function AccountPage() {
  const authQuery = useAuthMe()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [message, setMessage] = useState('')
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const preferredModel = usePreferredGatewayModel()
  const preferredReasoningEffort = usePreferredReasoningEffort()
  const preferredVoice = usePreferredJapaneseVoice()
  const user = authQuery.data?.user ?? null
  const deviceId = getDeviceId()

  const claimMutation = useMutation({
    mutationFn: claimCurrentDevice,
    onSuccess: async (result) => {
      setMessage(`已合并：progress ${result.merged.progress}，corrections ${result.merged.corrections}，AI ${result.merged.aiInteractions}`)
      await queryClient.invalidateQueries()
    },
    onError: (error) => setMessage(error instanceof Error ? error.message : '合并失败'),
  })

  const logoutMutation = useMutation({
    mutationFn: logoutOwner,
    onSuccess: async () => {
      await queryClient.invalidateQueries()
      await navigate({ to: '/login' })
    },
  })

  const passwordMutation = useMutation({
    mutationFn: () => changeOwnerPassword(oldPassword, newPassword),
    onSuccess: () => {
      setOldPassword('')
      setNewPassword('')
      setMessage('密码已更新，其他旧 session 已清理。')
    },
    onError: (error) => setMessage(error instanceof Error ? error.message : '密码更新失败'),
  })

  return (
    <section className="page-stack narrow-page">
      <PageHeader
        eyebrow="Account"
        title="同步状态"
        description="登录后用 owner userId 跨设备读取进度；未登录时继续使用当前 deviceId。"
      />

      <section className="source-preview">
        <p className="eyebrow">当前状态</p>
        <strong>{user ? user.email : '未登录'}</strong>
        <span>deviceId: {deviceId}</span>
        <span>{user ? '今日复习、历史记录和批改会优先按 userId 读取。' : '当前仍在 anonymous device-level 模式。'}</span>
        <div className="card-actions">
          {user ? (
            <>
              <button className="icon-button secondary" type="button" disabled={claimMutation.isPending} onClick={() => claimMutation.mutate()}>
                <RefreshCw size={18} />
                <span>{claimMutation.isPending ? '合并中' : '合并当前设备进度'}</span>
              </button>
              <button className="icon-button secondary" type="button" disabled={logoutMutation.isPending} onClick={() => logoutMutation.mutate()}>
                <LogOut size={18} />
                <span>{logoutMutation.isPending ? '退出中' : '退出登录'}</span>
              </button>
            </>
          ) : (
            <Link className="primary-action" to="/login">
              <KeyRound size={18} />
              <span>登录</span>
            </Link>
          )}
        </div>
        {message ? <p className="error-text">{message}</p> : null}
      </section>

      <section className="source-preview">
        <p className="eyebrow">AI 教练设置</p>
        <strong>默认讲解模型</strong>
        <span>词汇、语法、读空气和批改默认使用这里的模型；学习卡片里不再重复显示模型下拉框。</span>
        <select
          className="model-select"
          value={preferredModel}
          onChange={(event) => setPreferredGatewayModel(event.target.value as GatewayModel)}
        >
          {aiGatewayModels.map((item) => (
            <option key={item.id} value={item.id}>
              {item.label}
            </option>
          ))}
        </select>
        {preferredModel === 'grok-4.3' ? (
          <>
            <strong>Grok 推理强度</strong>
            <span>仅 Grok 4.3 生效；高强度更稳，低强度响应更轻。</span>
            <select
              className="model-select"
              value={preferredReasoningEffort}
              onChange={(event) => setPreferredReasoningEffort(event.target.value as ReasoningEffort)}
            >
              {reasoningEfforts.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.label}
                </option>
              ))}
            </select>
          </>
        ) : null}
      </section>

      <section className="source-preview">
        <p className="eyebrow">TTS 设置</p>
        <strong>默认日语人声</strong>
        <span>所有播放按钮默认使用这里的人声；默认是微软七海，不再在每张卡片里重复选择。</span>
        <select
          className="model-select"
          value={preferredVoice}
          onChange={(event) => setPreferredJapaneseVoice(event.target.value as JapaneseVoice)}
        >
          {japaneseVoices.map((item) => (
            <option key={item.id} value={item.id}>
              {item.label}
            </option>
          ))}
        </select>
      </section>

      {user ? (
        <form className="correction-form auth-form">
          <p className="eyebrow">修改密码</p>
          <input type="password" value={oldPassword} onChange={(event) => setOldPassword(event.target.value)} placeholder="旧密码" autoComplete="current-password" />
          <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} placeholder="新密码，至少 6 位" autoComplete="new-password" />
          <button className="icon-button secondary" type="button" disabled={passwordMutation.isPending} onClick={() => passwordMutation.mutate()}>
            <KeyRound size={18} />
            <span>{passwordMutation.isPending ? '更新中' : '更新密码'}</span>
          </button>
        </form>
      ) : null}
    </section>
  )
}
