const { chromium } = require('playwright')
const BASE = 'http://localhost:5175'
;(async () => {
  const browser = await chromium.launch()
  const page = await (await browser.newContext({ viewport: { width: 420, height: 860 } })).newPage()
  await page.goto(BASE + '/#/pages/index/index')
  await page.waitForTimeout(4000)
  // 用 uni 自己的 API 写入，再看 localStorage 里到底长什么样
  const out = await page.evaluate(() => {
    uni.setStorageSync('shop_client_shop_id', '2084461863957803010')
    const dump = {}
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      dump[k] = localStorage.getItem(k)
    }
    return { dump, readBack: uni.getStorageSync('shop_client_shop_id') }
  })
  console.log('localStorage 内容:', JSON.stringify(out.dump, null, 2))
  console.log('uni.getStorageSync 读回:', JSON.stringify(out.readBack))
  await browser.close()
})()
