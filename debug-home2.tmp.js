const { chromium } = require('playwright')
const SHOP_ID = '2084461863957803010'
const BASE = 'http://localhost:5175'
;(async () => {
  const browser = await chromium.launch()
  const ctx = await browser.newContext({ viewport: { width: 420, height: 860 } })
  const page = await ctx.newPage()
  page.on('request', (r) => {
    if (r.url().includes('/api/') && !r.url().includes('.js'))
      console.log('[req]', r.method(), r.url().replace(BASE, ''), 'X-Shop-Id=' + (r.headers()['x-shop-id'] || '(none)'))
  })
  page.on('response', async (r) => {
    if (r.url().includes('/api/') && !r.url().includes('.js'))
      console.log('[res]', r.status(), r.url().replace(BASE, ''), '=>', (await r.text().catch(() => '')).slice(0, 160))
  })

  // 先落地 storage，再整页加载首页（不能只改 hash —— 那样不会重新执行 onMounted）
  await page.goto(BASE + '/')
  await page.evaluate((s) => localStorage.setItem('shop_client_shop_id', s), SHOP_ID)
  console.log('--- import.meta.env.DEV 是否为 true ---')
  console.log('  DEV =', await page.evaluate(() => (typeof __DEV_FLAG__ !== 'undefined' ? __DEV_FLAG__ : 'n/a')))

  console.log('\n--- 整页加载 #/pages/index/index ---')
  await page.goto(BASE + '/#/pages/index/index')
  await page.reload()
  await page.waitForTimeout(5000)
  console.log('  商品卡片数:', await page.locator('.goods-card').count())
  await browser.close()
})()
