import { mkdir, writeFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const projectDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const outputDir = resolve(projectDir, 'src/static/icons')

const icons = {
  'badge-percent-violet': ['badge-percent', '#6854a5'],
  'calendar-check-2-green': ['calendar-check-2', '#287e62'],
  'check-circle-2-green': ['circle-check-big', '#287e62'],
  'chevron-right-light': ['chevron-right', '#cfd5d9'],
  'chevron-right-muted': ['chevron-right', '#969ca0'],
  'circle-dollar-sign-service': ['circle-dollar-sign', '#3c5968'],
  'coins-service': ['coins', '#3c5968'],
  'coins-green': ['coins', '#246d58'],
  'coins-muted': ['coins', '#858c90'],
  'coins-soft': ['coins', '#a1a7aa'],
  'crown-light': ['crown', '#cfd5d9'],
  'crown-service': ['crown', '#3c5968'],
  'gift-amber': ['gift', '#9a6817'],
  'headphones-blue': ['headphones', '#2a78d6'],
  'log-out-red': ['log-out', '#b8403a'],
  'map-pin-green': ['map-pin', '#287e62'],
  'package-blue': ['package', '#2e70b8'],
  'package-open-muted': ['package-open', '#898781'],
  'phone-green': ['phone', '#287e62'],
  'rotate-ccw-coral': ['rotate-ccw', '#c94d45'],
  'arrow-down-to-line-white': ['arrow-down-to-line', '#ffffff'],
  'clock-3-green': ['clock-3', '#287e62'],
  'receipt-text-green': ['receipt-text', '#246d58'],
  'receipt-text-muted': ['receipt-text', '#858c90'],
  'receipt-text-soft': ['receipt-text', '#a1a7aa'],
  'share-2-coral': ['share-2', '#c94d45'],
  'store-blue': ['store', '#2e70b8'],
  'ticket-amber': ['ticket', '#9a6817'],
  'user-round-white': ['user-round', '#ffffff'],
  'users-green': ['users', '#246d58'],
  'users-muted': ['users', '#858c90'],
  'users-soft': ['users', '#a1a7aa'],
  'wallet-cards-muted': ['wallet-cards', '#687076'],
  'wallet-cards-service': ['wallet-cards', '#3c5968'],
}

function escapeAttribute(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
}

function renderNode([tag, attributes, children]) {
  const attrs = Object.entries(attributes || {})
    .filter(([name]) => name !== 'key')
    .map(([name, value]) => `${name}="${escapeAttribute(value)}"`)
    .join(' ')
  if (children?.length) {
    return `<${tag}${attrs ? ` ${attrs}` : ''}>${children.map(renderNode).join('')}</${tag}>`
  }
  return `<${tag}${attrs ? ` ${attrs}` : ''}/>`
}

await mkdir(outputDir, { recursive: true })

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 96, height: 96 } })

try {
  for (const [assetName, [iconName, color]] of Object.entries(icons)) {
    const moduleUrl = new URL(`../node_modules/@lucide/vue/dist/esm/icons/${iconName}.mjs`, import.meta.url)
    const { __iconData } = await import(moduleUrl)
    const body = __iconData.node.map(renderNode).join('')
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${body}</svg>\n`
    await writeFile(resolve(outputDir, `${assetName}.svg`), svg, 'utf8')
    await page.setContent(`<style>html,body{margin:0;width:96px;height:96px;background:transparent}svg{display:block;width:96px;height:96px}</style>${svg}`)
    await page.screenshot({
      path: resolve(outputDir, `${assetName}.png`),
      clip: { x: 0, y: 0, width: 96, height: 96 },
      omitBackground: true,
    })
  }
} finally {
  await browser.close()
}
