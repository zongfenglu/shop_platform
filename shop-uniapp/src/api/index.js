import { get, post, put, del } from '@/utils/request'

/**
 * 消费者端接口。对应 shop-client-api 的 /api/**。
 */

/** 手机号登录/注册（后端 loginOrRegister，一次调用即可） */
export function login(mobile) {
  return post('/api/auth/login', { mobile })
}

// ---- 商品浏览（ConsumerGoodsController）----

/** 分类树（只含 is_show=1 的分类，children 递归嵌套） */
export function getCategoryTree() {
  return get('/api/category')
}

/**
 * 商品列表。sort: default(综合) / sales(销量) / price_asc / price_desc。
 * 注意 price_asc/desc 是**分页内排序**（价格在 SKU 上，goods 表排不了），
 * 这是后端有意为之的取舍，见 ConsumerGoodsController#list 的注释。
 */
export function pageGoods(params) {
  return get('/api/goods', params)
}

/** 商品详情，含 specs 规格树 + skus 列表，供规格选择器联动 */
export function getGoodsDetail(id) {
  return get(`/api/goods/${id}`)
}

// ---- 购物车（ConsumerCartController）----

export function listCart() {
  return get('/api/cart')
}

export function getCartCount() {
  return get('/api/cart/count')
}

export function addToCart(skuId, quantity) {
  return post('/api/cart', { skuId, quantity })
}

export function updateCartQuantity(cartId, quantity) {
  return put(`/api/cart/${cartId}/quantity`, { quantity })
}

export function removeCartItems(cartIds) {
  return del('/api/cart', cartIds)
}

// ---- 收货地址簿（ConsumerAddressController）----

export function listAddresses() {
  return get('/api/address')
}

export function getDefaultAddress() {
  return get('/api/address/default')
}

export function getAddress(id) {
  return get(`/api/address/${id}`)
}

export function createAddress(payload) {
  return post('/api/address', payload)
}

export function updateAddress(id, payload) {
  return put(`/api/address/${id}`, payload)
}

export function deleteAddress(id) {
  return del(`/api/address/${id}`)
}

export function setDefaultAddress(id) {
  return put(`/api/address/${id}/default`)
}

/** 我的订单列表 */
export function pageOrders(params) {
  return get('/api/order', params)
}

export function getOrder(id) {
  return get(`/api/order/${id}`)
}

/** 物流轨迹；多包裹时传 packageId 查指定包裹 */
export function getOrderTracks(id, packageId) {
  return get(`/api/order/${id}/tracks`, packageId ? { packageId } : undefined)
}

export function confirmReceipt(id) {
  return post(`/api/order/${id}/confirm-receipt`)
}

export function cancelOrder(id) {
  return post(`/api/order/${id}/cancel`)
}

/** 发起订单支付；测试环境直接模拟成功，正式环境返回微信 H5 支付地址。 */
export function prepayOrder(id) {
  return post(`/api/pay/${id}/prepay`)
}

/** 下单前预览（算价：优惠分摊、运费） */
export function previewCheckout(payload) {
  return post('/api/checkout/preview', payload)
}

export function submitCheckout(payload) {
  return post('/api/checkout/submit', payload)
}

/** 可选自提门店列表，用于结算页 deliveryType=pickup 时选择门店 */
export function listOfflineStores() {
  return get('/api/offline-stores')
}

/** 商品评价列表 */
export function getGoodsComments(goodsId, params) {
  return get(`/api/goods/${goodsId}/comments`, params)
}

/** 发布商品评价 */
export function publishComment(payload) {
  return post('/api/comment', payload)
}

/**
 * 售后。对应 ConsumerAfterSaleController（/api/after-sale）。
 * type: refund_only仅退款 / return_refund退货退款
 */
export function applyAfterSale(payload) {
  return post('/api/after-sale', payload)
}

export function getAfterSale(id) {
  return get(`/api/after-sale/${id}`)
}

export function listAfterSaleByOrder(orderId) {
  return get(`/api/after-sale/order/${orderId}`)
}

export function returnShippedAfterSale(id, payload) {
  return post(`/api/after-sale/${id}/return-shipped`, payload)
}

export function closeAfterSale(id) {
  return post(`/api/after-sale/${id}/close`)
}

// ---- 会员资产与充值（ConsumerMemberController / ConsumerRechargeController）----

/** 我的资料与资产（余额/积分/成长值/等级/消费统计） */
export function getMyProfile() {
  return get('/api/member/me')
}

/** 我的余额变动明细 */
export function getMyBalanceLogs(params) {
  return get('/api/member/balance-logs', params)
}

/** 我的积分变动明细 */
export function getMyPointsLogs(params) {
  return get('/api/member/points-logs', params)
}

/** 会员等级说明（全部等级 + 我的当前等级 + 我的成长值） */
export function getMyGrades() {
  return get('/api/member/grades')
}

/** 上架的充值方案 */
export function getRechargePlans() {
  return get('/api/recharge/plans')
}

/** 创建充值订单，返回充值单（含 orderNo） */
export function createRechargeOrder(planId) {
  return post('/api/recharge/orders', { planId })
}

/** 充值支付（模拟支付入账；接入微信支付后替换为 prepay + 回调） */
export function payRechargeOrder(id) {
  return post(`/api/recharge/orders/${id}/pay`)
}

// ---- 优惠券（ConsumerCouponController）----

/** 领券中心：当前可领取的券 */
export function getReceivableCoupons() {
  return get('/api/coupons/receivable')
}

/** 领取一张券 */
export function receiveCoupon(couponId) {
  return post(`/api/coupons/${couponId}/receive`)
}

/** 我的优惠券，status: null(全部)/unused/used/expired */
export function getMyCoupons(status) {
  return get('/api/coupons/mine', status ? { status } : {})
}

// ---- 秒杀/限时折扣（ConsumerSeckillController）----

/** 今日场次及状态（upcoming/ongoing/ended） */
export function getSeckillSessions() {
  return get('/api/seckill/sessions')
}

/** 今日有效活动列表 */
export function listSeckillActives() {
  return get('/api/seckill/actives')
}

/** 活动详情 + 商品列表（含剩余量） */
export function getSeckillActive(id) {
  return get(`/api/seckill/actives/${id}`)
}

// ---- 拼团（ConsumerGroupController）----

/** 上架拼团活动列表 */
export function listGroupActives() {
  return get('/api/group/actives')
}

/** 拼团活动详情（商品 + 各 SKU 拼团价） */
export function getGroupActive(id) {
  return get(`/api/group/actives/${id}`)
}

/** 拼团进度（已参团/成团人数/状态/截止） */
export function getGroupRecord(recordId) {
  return get(`/api/group/records/${recordId}`)
}

// ---- 砍价（ConsumerBargainController）----

/** 上架砍价活动列表 */
export function listBargainActives() {
  return get('/api/bargain/actives')
}

/** 砍价活动详情（商品 + 底价） */
export function getBargainActive(id) {
  return get(`/api/bargain/actives/${id}`)
}

/** 发起砍价 */
export function startBargain(activeId, skuId) {
  return post('/api/bargain/start', { activeId, skuId })
}

/** 好友助力砍一刀 */
export function helpBargain(recordId) {
  return post(`/api/bargain/help/${recordId}`)
}

/** 砍价进度 */
export function getBargainRecord(recordId) {
  return get(`/api/bargain/records/${recordId}`)
}

// ---- 签到（ConsumerSignController）----

/** 签到状态（连续天数/本月签到/今日是否已签/奖励规则） */
export function signStatus() {
  return get('/api/sign/status')
}

/** 每日签到，返回 {earnedPoints, dayNumber} */
export function dailySign() {
  return post('/api/sign/daily')
}

/** 补签昨日 */
export function makeupSign(date) {
  return post('/api/sign/makeup', { date })
}

// ---- 积分商城（ConsumerPointsMallController）----

/** 在架兑换项列表（公开，无需登录） */
export function listPointsGoods() {
  return get('/api/points-mall/goods')
}

/** 创建兑换单（仅校验，返回 unpaid 记录） */
export function redeemPointsGoods(goodsId) {
  return post('/api/points-mall/exchanges', { goodsId })
}

/** 支付兑换单 */
export function payPointsExchange(id) {
  return post(`/api/points-mall/exchanges/${id}/pay`)
}

// ---- 分销（ConsumerDealerController）----

/** 分销设置（是否开启/佣金比例/最低提现） */
export function getDealerSetting() {
  return get('/api/dealer/setting')
}

/** 申请成为分销商 */
export function applyDealer() {
  return post('/api/dealer/apply')
}

/** 我的分销资料 */
export function getMyDealer() {
  return get('/api/dealer/me')
}

/** 我的佣金明细 */
export function listDealerOrders(status) {
  return get('/api/dealer/orders', status ? { status } : {})
}

/** 我的下级团队 */
export function listMyDealerTeam() {
  return get('/api/dealer/team')
}

/** 申请提现 */
export function applyWithdraw(amount, method, accountInfo) {
  return post('/api/dealer/withdraw', { amount, method, accountInfo })
}

/** 我的提现记录 */
export function listMyWithdraws() {
  return get('/api/dealer/withdraws')
}

// ---- 装修（ConsumerDiyController）----

export function getDiyHome() {
  return get('/api/diy/home')
}

export function getDiyPage(id) {
  return get(`/api/diy/page/${id}`)
}

export function getDiyTabbar() {
  return get('/api/diy/tabbar')
}

export function getDiyCategoryPage() {
  return get('/api/diy/category-page')
}
