export type GatewayModel = 'gemini-3.5-flash-lite' | 'gemini-3.6-flash' | 'gemini-3.8-flash'

export const aiGatewayModels: { id: GatewayModel; label: string }[] = [
  { id: 'gemini-3.5-flash-lite', label: 'Gemini 3.5 Flash-Lite' },
  { id: 'gemini-3.6-flash', label: 'Gemini 3.6 Flash' },
  { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash' },
]
