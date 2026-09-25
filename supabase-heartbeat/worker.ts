type Env = {
  APP: Fetcher
  SUPABASE_SERA_URL: string
  SUPABASE_SERA_PUBLISHABLE_KEY: string
}

export default {
  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    await Promise.all([
      keepJpsActive(env),
      keepSeraActive(env),
    ])
    console.log('Supabase heartbeats completed')
  },
}

async function keepJpsActive(env: Env): Promise<void> {
  const response = await env.APP.fetch('https://supabase-heartbeat.internal/api/works', {
    headers: {
      'Cache-Control': 'no-store',
    },
  })

  if (!response.ok) {
    throw new Error(`jps heartbeat failed through the app API: ${response.status} ${await response.text()}`)
  }

  await response.arrayBuffer()
}

async function keepSeraActive(env: Env): Promise<void> {
  const response = await fetch(`${env.SUPABASE_SERA_URL}/rest/v1/rpc/keep_alive_probe`, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_SERA_PUBLISHABLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERA_PUBLISHABLE_KEY}`,
      'Cache-Control': 'no-store',
      'Content-Type': 'application/json',
    },
    body: '{}',
  })

  if (!response.ok) {
    throw new Error(`sera heartbeat failed: ${response.status} ${await response.text()}`)
  }

  await response.arrayBuffer()
}
