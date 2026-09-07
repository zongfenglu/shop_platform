<script>
export default {
  onLaunch() {
    // 小程序端：ext.json 注入的 shopId 在这里最早可读，request.js 会自动带上 X-Shop-Id
  },
}
</script>

<style>
/* 全局样式：token 与原型 docs/prototype/shared/style.css 保持一致 */
@import './styles/tokens.css';

page {
  background: #f9f9f7;
  color: #0b0b0b;
  font-size: 28rpx;
  font-family: system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>
