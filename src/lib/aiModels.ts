export type GatewayModel = 'gemini-3.1-flash-lite' | 'gemini-3.5-flash' | 'deepseek-v4-flash' | 'deepseek-v4-pro' | 'grok-4.3'

export const aiGatewayModels: { id: GatewayModel; label: string }[] = [
  { id: 'gemini-3.1-flash-lite', label: 'Gemini 3.1 Flash Lite' },
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash' },
  { id: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash' },
  { id: 'deepseek-v4-pro', label: 'DeepSeek V4 Pro' },
  { id: 'grok-4.3', label: 'Grok 4.3' },
]
