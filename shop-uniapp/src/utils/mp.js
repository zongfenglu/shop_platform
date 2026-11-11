/**
 * 微信小程序里不要在模板或组件里直接写 uni.xxx。
 * Vue 会编成 _ctx.uni，运行时是 undefined，一点击就报 showToast / navigateTo。
 */
export function mp() {
  const g = typeof globalThis !== 'undefined' ? globalThis : {}
  return g.wx || g['uni'] || {}
}

export function mpToast(title, icon = 'none') {
  const api = mp()
  if (typeof api.showToast === 'function') api.showToast({ title, icon })
}

export function mpNavigateTo(url) {
  const api = mp()
  if (typeof api.navigateTo === 'function') api.navigateTo({ url })
}

export function mpSwitchTab(url) {
  const api = mp()
  if (typeof api.switchTab === 'function') api.switchTab({ url })
}

export function mpPreviewImage(urls, current) {
  const api = mp()
  if (typeof api.previewImage === 'function') {
    api.previewImage({ urls, current: current || urls[0] })
  }
}

export function mpMakePhoneCall(phoneNumber) {
  const api = mp()
  if (typeof api.makePhoneCall === 'function') api.makePhoneCall({ phoneNumber })
}

export function mpSetBackgroundColor(color) {
  const api = mp()
  if (typeof api.setBackgroundColor === 'function') {
    api.setBackgroundColor({
      backgroundColor: color,
      backgroundColorTop: color,
      backgroundColorBottom: color,
    })
  }
}
