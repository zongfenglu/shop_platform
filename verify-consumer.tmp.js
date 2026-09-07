const { chromium } = require('playwright')

const SHOP_ID = '2084461863957803010'
const GOODS_MULTI = '2085564771189211138'
const BASE = 'http://localhost:5175'

;(async () => {
  const browser = await chromium.launch()
  const ctx = await browser.newContext({ viewport: { width: 420, height: 860 } })
  const page = await ctx.newPage()

  const errors = []
  page.on('console', (m) => {
    if (m.type() === 'error') errors.push(m.text())
  })
  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))

  // 先设好 shopId + 登录态（H5 dev 模式下 request.js 会把它当 X-Shop-Id 发出去）
  await page.goto(BASE + '/')
  const login = await page.evaluate(async (shopId) => {
    const r = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Shop-Id': shopId },
      body: JSON.stringify({ mobile: '13800001111' }),
    })
    return (await r.json()).data
  }, SHOP_ID)
  await page.evaluate(
    ([shopId, l]) => {
      localStorage.setItem('shop_client_shop_id', shopId)
      localStorage.setItem('shop_client_token', l.token)
      localStorage.setItem('shop_client_user', JSON.stringify({ userId: l.userId, nickname: l.nickname }))
    },
    [SHOP_ID, login]
  )

  const shot = async (name) => {
    await page.screenshot({ path: `shots/${name}.png`, fullPage: true })
    console.log('  → shots/' + name + '.png')
  }

  // 每个页面都整页加载 + reload：只改 hash 不会重新执行 onMounted/onLoad，
  // 页面会停在上一个页面的数据上，测出来的结果没有意义。
  //
  // 本工程的 H5 路由是 history 模式（uni 默认），所有页面都是**不带 # 的**真实路径。
  // 用 /#/pages/xxx 这种 hash 形式会静默停在当前页面上，元素数全为 0 ——
  // 看起来像页面渲染失败，其实是根本没跳过去。
  const open = async (route) => {
    await page.goto(BASE + route)
    await page.reload()
    await page.waitForTimeout(3200)
  }

  console.log('\n=== 首页 ===')
  await open('/pages/index/index')
  console.log('  商品卡片数:', await page.locator('.goods-card').count())
  console.log('  分类导航数:', await page.locator('.nav-item').count())
  console.log('  首个商品:', (await page.locator('.goods-card .goods-name').first().textContent().catch(() => '(无)')).trim())
  console.log('  首个价格:', (await page.locator('.goods-card .price').first().textContent().catch(() => '(无)')).trim())
  await shot('01-home')

  console.log('\n=== 商品列表/分类页 ===')
  await open('/pages/goods/list')
  console.log('  列表行数:', await page.locator('.goods-row').count())
  console.log('  分类 chip:', await page.locator('.cat-chip').count())
  await shot('02-goods-list')
  // 切到销量排序
  await page.locator('.sort-item', { hasText: '销量' }).click()
  await page.waitForTimeout(1500)
  console.log('  切"销量"后行数:', await page.locator('.goods-row').count())

  console.log('\n=== 商品详情（多规格 3色3码）===')
  await open(`/pages/goods/detail?id=${GOODS_MULTI}`)
  console.log('  标题:', (await page.locator('.name').first().textContent()).trim())
  console.log('  价格显示:', (await page.locator('.price').first().textContent()).trim())
  await shot('03-goods-detail')

  // 打开规格弹层
  await page.locator('.tap-row').click()
  await page.waitForTimeout(900)
  const groups = await page.locator('.spec-group').count()
  console.log('  规格组数:', groups)
  for (let i = 0; i < groups; i++) {
    const g = page.locator('.spec-group').nth(i)
    console.log(
      '    ',
      (await g.locator('.spec-name').textContent()).trim(),
      '=>',
      (await g.locator('.spec-chip').allTextContents()).map((t) => t.trim()).join(' / ')
    )
  }
  await shot('04-spec-popup')

  // 选「蓝色」→ L 应置灰（蓝色/L 库存 0）
  await page.locator('.spec-group').nth(0).locator('.spec-chip', { hasText: '蓝色' }).click()
  await page.waitForTimeout(700)
  const sizeGroup = page.locator('.spec-group').nth(1)
  for (const label of ['S', 'M', 'L']) {
    const chip = sizeGroup.locator('.spec-chip', { hasText: new RegExp(`^${label}$`) })
    const cls = await chip.getAttribute('class')
    console.log(`  选蓝色后 尺码「${label}」:`, cls.includes('disabled') ? '置灰 ✓' : '可选')
  }
  await shot('05-blue-selected')

  // 选 M → 价格应联动为 ¥104.00，库存 15
  await sizeGroup.locator('.spec-chip', { hasText: /^M$/ }).click()
  await page.waitForTimeout(700)
  console.log('  蓝色/M 联动价格:', (await page.locator('.popup-price .price').textContent()).trim())
  console.log('  蓝色/M 联动库存:', (await page.locator('.popup-price .small').textContent()).trim())
  await shot('06-blue-m-price')

  // 加入购物车
  await page.locator('.popup-confirm').click()
  await page.waitForTimeout(2000)
  await shot('07-added-to-cart')

  console.log('\n=== 购物车 ===')
  await open('/pages/cart/index')
  console.log('  购物车行数:', await page.locator('.row').count())
  const names = await page.locator('.row .name').allTextContents()
  const specs = await page.locator('.row .spec').allTextContents()
  console.log('  商品:', names.map((n) => n.trim()).join(' | '))
  console.log('  规格:', specs.map((s) => s.trim()).join(' | '))
  await shot('08-cart')

  // 全选 → 合计应变化
  await page.locator('.bar-check').click()
  await page.waitForTimeout(800)
  console.log('  全选后合计:', (await page.locator('.bar-total-price').textContent()).trim())
  console.log('  结算按钮:', (await page.locator('.bar-btn.buy').textContent()).trim())
  await shot('09-cart-all-checked')

  console.log('\n=== 地址列表 ===')
  await open('/pages/address/list')
  console.log('  地址数:', await page.locator('.card').count())
  console.log('  首条:', (await page.locator('.card .detail').first().textContent().catch(() => '(无)')).trim())
  await shot('10-address-list')

  console.log('\n=== 结算页（从购物车勾选进入）===')
  await open('/pages/cart/index')
  await page.locator('.bar-check').click()
  await page.waitForTimeout(600)
  await page.locator('.bar-btn.buy').click()
  await page.waitForTimeout(3000)
  console.log('  URL:', page.url().split('#')[1])
  console.log('  收货人:', (await page.locator('.addr-name').textContent().catch(() => '(无地址)')).trim())
  console.log('  商品行数:', await page.locator('.goods-row').count())
  const fees = await page.locator('.fee-row').allTextContents()
  fees.forEach((f) => console.log('  费用行:', f.replace(/\s+/g, ' ').trim()))
  console.log('  应付:', (await page.locator('.bar-price').textContent()).trim())
  await shot('11-checkout')

  console.log('\n=== 控制台错误 ===')
  if (!errors.length) console.log('  无')
  else errors.slice(0, 12).forEach((e) => console.log('  !', e.slice(0, 200)))

  await browser.close()
})()
