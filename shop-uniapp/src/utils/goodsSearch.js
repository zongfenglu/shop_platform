const PENDING_SEARCH_KEY = 'shop_pending_goods_search'

/** 从任意页面进入底部 Tab 的商品页，并把本次关键词交给商品页消费。 */
export function openGoodsSearch(keyword = '') {
  uni.setStorageSync(PENDING_SEARCH_KEY, JSON.stringify({ keyword: String(keyword || '').trim() }))
  uni.switchTab({ url: '/pages/goods/list' })
}

export function consumeGoodsSearch() {
  try {
    const raw = uni.getStorageSync(PENDING_SEARCH_KEY)
    if (!raw) return null
    uni.removeStorageSync(PENDING_SEARCH_KEY)
    const parsed = JSON.parse(raw)
    return { keyword: String(parsed?.keyword || '').trim() }
  } catch (e) {
    uni.removeStorageSync(PENDING_SEARCH_KEY)
    return null
  }
}

