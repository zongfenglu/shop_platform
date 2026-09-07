const { chromium } = require('playwright');
const path = require('path');

const shotDir = path.join(__dirname, 'shots');
const files = ['diy-editor.html', 'diy-page-list.html', 'diy-tabbar.html'];

(async () => {
  const browser = await chromium.launch({ args: ['--no-sandbox'] });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  for (const f of files) {
    const filePath = path.join(__dirname, 'docs', 'prototype', 'store', f);
    const fileUrl = 'file:///' + filePath.split(path.sep).join('/');
    await page.goto(fileUrl, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(shotDir, 'proto-' + f.replace('.html', '.png')), fullPage: true });
    console.log('shot: proto-' + f.replace('.html', '.png'));
  }
  await browser.close();
})().catch(e => { console.error('FATAL', e); process.exit(1); });
