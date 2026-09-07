const { chromium } = require('playwright')
const SHOP_ID = '2084461863957803010'
const BASE = 'http://localhost:5175'

;(async () => {
  const browser = await chromium.launch()
  const page = await (await browser.newContext({ viewport: { width: 420, height: 860 } })).newPage()

  page.on('console', (m) => console.log('[console.' + m.type() + ']', m.text().slice(0, 300)))
  page.on('pageerror', (e) => console.log('[pageerror]', e.message.slice(0, 300)))
  page.on('request', (r) => {
    if (r.url().includes('/api/')) console.log('[req]', r.method(), r.url(), 'X-Shop-Id=' + (r.headers()['x-shop-id'] || '(none)'))
  })
  page.on('response', async (r) => {
    if (r.url().includes('/api/')) {
      const body = await r.text().catch(() => '')
      console.log('[res]', r.status(), r.url().replace(BASE, ''), '=>', body.slice(0, 300))
    }
  })

  await page.goto(BASE + '/')
  const login = await page.evaluate(async (shopId) => {
    const r = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Shop-Id': shopId },
      body: JSON.stringify({ mobile: '13800001111' }),
    })
    return (await r.json()).data
  }, SHOP_ID)
  await page.evaluate(([shopId, l]) => {
    localStorage.setItem('shop_client_shop_id', shopId)
    localStorage.setItem('shop_client_token', l.token)
    localStorage.setItem('shop_client_user', JSON.stringify({ userId: l.userId, nickname: l.nickname }))
  }, [SHOP_ID, login])

  console.log('\n--- 打开首页 ---')
  await page.goto(BASE + '/#/pages/index/index')
  await page.waitForTimeout(5000)
  console.log('storage shopId =', await page.evaluate(() => localStorage.getItem('shop_client_shop_id')))
  await browser.close()
})()
