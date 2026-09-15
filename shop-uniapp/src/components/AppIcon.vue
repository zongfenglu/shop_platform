<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 24 },
})

const dimensions = computed(() => {
  const value = typeof props.size === 'number' ? `${props.size}px` : props.size
  return { width: value, height: value }
})
</script>

<template>
  <image v-if="name === 'arrow-down-to-line-white'" class="app-icon" src="/static/icons/arrow-down-to-line-white.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'badge-percent-violet'" class="app-icon" src="/static/icons/badge-percent-violet.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'calendar-check-2-green'" class="app-icon" src="/static/icons/calendar-check-2-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'check-circle-2-green'" class="app-icon" src="/static/icons/check-circle-2-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'chevron-right-light'" class="app-icon" src="/static/icons/chevron-right-light.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'chevron-right-muted'" class="app-icon" src="/static/icons/chevron-right-muted.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'circle-dollar-sign-service'" class="app-icon" src="/static/icons/circle-dollar-sign-service.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'clock-3-green'" class="app-icon" src="/static/icons/clock-3-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'coins-green'" class="app-icon" src="/static/icons/coins-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'coins-muted'" class="app-icon" src="/static/icons/coins-muted.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'coins-service'" class="app-icon" src="/static/icons/coins-service.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'coins-soft'" class="app-icon" src="/static/icons/coins-soft.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'crown-light'" class="app-icon" src="/static/icons/crown-light.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'crown-service'" class="app-icon" src="/static/icons/crown-service.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'gift-amber'" class="app-icon" src="/static/icons/gift-amber.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'headphones-blue'" class="app-icon" src="/static/icons/headphones-blue.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'log-out-red'" class="app-icon" src="/static/icons/log-out-red.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'map-pin-green'" class="app-icon" src="/static/icons/map-pin-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'package-blue'" class="app-icon" src="/static/icons/package-blue.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'package-open-muted'" class="app-icon" src="/static/icons/package-open-muted.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'phone-green'" class="app-icon" src="/static/icons/phone-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'receipt-text-green'" class="app-icon" src="/static/icons/receipt-text-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'receipt-text-muted'" class="app-icon" src="/static/icons/receipt-text-muted.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'receipt-text-soft'" class="app-icon" src="/static/icons/receipt-text-soft.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'rotate-ccw-coral'" class="app-icon" src="/static/icons/rotate-ccw-coral.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'share-2-coral'" class="app-icon" src="/static/icons/share-2-coral.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'store-blue'" class="app-icon" src="/static/icons/store-blue.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'ticket-amber'" class="app-icon" src="/static/icons/ticket-amber.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'user-round-white'" class="app-icon" src="/static/icons/user-round-white.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'users-green'" class="app-icon" src="/static/icons/users-green.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'users-muted'" class="app-icon" src="/static/icons/users-muted.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'users-soft'" class="app-icon" src="/static/icons/users-soft.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'wallet-cards-muted'" class="app-icon" src="/static/icons/wallet-cards-muted.png" :style="dimensions" mode="aspectFit" />
  <image v-else-if="name === 'wallet-cards-service'" class="app-icon" src="/static/icons/wallet-cards-service.png" :style="dimensions" mode="aspectFit" />
  <view v-else class="app-icon" :style="dimensions" />
</template>

<style scoped>
.app-icon {
  display: block;
  flex: 0 0 auto;
}
</style>
