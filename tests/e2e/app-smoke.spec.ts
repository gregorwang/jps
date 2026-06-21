import { expect, test } from '@playwright/test'

test('iPad landscape app routes render without blank screens', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /k-on EP01/i })).toBeVisible()
  await expect(page.locator('.review-task-list')).toBeVisible()

  await page.goto('/works/k-on/episodes/1/subtitles')
  await expect(page.getByRole('heading', { name: 'EP01 日文台词' })).toBeVisible()
  await expect(page.locator('.timeline-row').first()).toBeVisible()

  await page.goto('/works/k-on/episodes/1/sentence?lineNo=46')
  await expect(page.getByRole('heading', { name: /line 46/ })).toBeVisible()
  await expect(page.getByText('正確には廃部寸前ね')).toBeVisible()

  await page.goto('/works/k-on/episodes/1/practice')
  await expect(page.getByRole('heading', { name: /Canvas/ })).toBeVisible()
  await expect(page.locator('canvas.handwriting-canvas')).toBeVisible()

  await page.goto('/works/k-on/episodes/1/lesson')
  await expect(page.getByRole('heading', { name: '把中文意思和日文表达配起来' })).toBeVisible()
  await expect(page.locator('.sidebar')).toHaveCount(0)

  await page.goto('/characters')
  await expect(page.getByRole('heading', { name: /角色说话习惯/ })).toBeVisible()
  await expect(page.getByLabel('番剧')).toBeVisible()
  await expect(page.getByRole('button', { name: '唯' })).toBeVisible()

  await page.goto('/correction')
  await expect(page.getByRole('heading', { name: '词汇 / 语法点造句' })).toBeVisible()
  await expect(page.locator('textarea')).toBeVisible()

  await page.goto('/login')
  await expect(page.getByRole('heading', { name: '个人同步登录' })).toBeVisible()
  await expect(page.getByLabel('邮箱')).toBeVisible()

  await page.goto('/account')
  await expect(page.getByRole('heading', { name: '同步状态' })).toBeVisible()
  await expect(page.getByText(/deviceId:/)).toBeVisible()

  await page.goto('/history/ai/smoke-cache-key')
  await expect(page.getByRole('heading', { name: '记录详情' })).toBeVisible()
})

test('sidebar marks only the current section active', async ({ page }) => {
  await page.goto('/works/k-on/episodes/1/vocab')
  await expect(page.getByRole('heading', { name: 'EP01 本集词汇' })).toBeVisible()

  const activeLinks = page.locator('.nav-link.active')
  await expect(activeLinks).toHaveCount(1)
  await expect(activeLinks.first()).toContainText('词')
})

test('deployed service worker asset bypasses API paths and AI POST succeeds', async ({ request }) => {
  const serviceWorkerResponse = await request.get('https://anime-japanese-lab.ishallnotwant123.workers.dev/sw.js')
  expect(serviceWorkerResponse.ok()).toBe(true)
  const serviceWorkerText = await serviceWorkerResponse.text()
  expect(serviceWorkerText).toContain("url.pathname.startsWith('/api/')")
  expect(serviceWorkerText).toContain("if (url.pathname.startsWith('/api/')) return")

  const meResponse = await request.get('https://anime-japanese-lab.ishallnotwant123.workers.dev/api/auth/me')
  expect(meResponse.ok()).toBe(true)
  const meBody = await meResponse.json()
  expect(meBody.user).toBeNull()

  const logoutResponse = await request.post('https://anime-japanese-lab.ishallnotwant123.workers.dev/api/auth/logout')
  expect(logoutResponse.ok()).toBe(true)

  const response = await request.post('https://anime-japanese-lab.ishallnotwant123.workers.dev/api/ai/explain', {
    data: { kind: 'vocab', text: '大丈夫', context: '没事吧 / 没关系' },
  })

  expect(response.ok()).toBe(true)
  const body = await response.json()
  expect(body.title).toBe('AI 精讲')
  expect(body.sections.length).toBeGreaterThan(0)
})
