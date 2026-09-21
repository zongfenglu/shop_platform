<script setup>
const items = [
  ['如何成为分销员', '在分享赚钱页面点击立即加入，填写相关信息即可成为分销员，审核通过后即可开始分享商品。'],
  ['如何获得佣金', '好友通过您的分享链接或图片购买商品并确认收货后，您将获得相应的佣金奖励。'],
  ['佣金如何结算', '佣金会在订单确认收货后自动结算，您可以在佣金明细中查看收入和结算状态。'],
  ['提现规则', '支持微信钱包提现，最低提现金额和到账时间以平台设置为准。'],
  ['其他问题', '如有疑问，请联系在线客服，我们会尽快协助处理。'],
]
</script>
<template><view class="page"><view class="tabs"><text class="active">分销说明</text><text>常见问题</text></view><view v-for="(item,index) in items" :key="item[0]" class="faq"><view class="faq-title">{{ index + 1 }}. {{ item[0] }}</view><view class="faq-body">{{ item[1] }}</view></view><view class="service"><view class="service-icon">☎</view><view><view class="service-title">联系客服</view><view class="service-sub">工作时间：9:00 - 22:00</view></view><text>›</text></view></view></template>
<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #f5f9fd; color: #202b38; }.tabs { display: flex; gap: 64rpx; padding: 8rpx 18rpx 22rpx; font-size: 25rpx; }.tabs text { padding-bottom: 14rpx; color: #7f8b98; }.tabs .active { color: #1677e8; border-bottom: 4rpx solid #1677e8; }.faq { margin-bottom: 18rpx; padding: 26rpx; border-radius: 16rpx; background: #fff; box-shadow: 0 5rpx 18rpx rgba(53,83,118,.04); }.faq-title { font-size: 25rpx; font-weight: 700; }.faq-body { margin-top: 14rpx; color: #7b8895; font-size: 22rpx; line-height: 1.7; }.service { display: flex; align-items: center; gap: 16rpx; margin-top: 26rpx; padding: 26rpx; border-radius: 18rpx; background: #e5f3ff; }.service-icon { width: 64rpx; height: 64rpx; border-radius: 50%; color: #1677e8; background: #fff; text-align: center; line-height: 64rpx; font-size: 28rpx; }.service-title { font-size: 25rpx; font-weight: 700; }.service-sub { margin-top: 7rpx; color: #8295a6; font-size: 20rpx; }.service>text { margin-left: auto; color: #1677e8; font-size: 36rpx; }
</style>
