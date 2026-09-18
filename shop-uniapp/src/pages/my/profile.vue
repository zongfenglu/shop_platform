<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { bindWechatPhone, getMyProfile, updateMyProfile, uploadMyAvatar } from '@/api'
import { getLoginUser, getToken, setLoginUser, setToken } from '@/utils/request'

const member = ref(null)
const nickname = ref('')
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const bindingPhone = ref(false)

onLoad(async () => {
  if (!getToken()) {
    uni.redirectTo({ url: '/pages/my/login' })
    return
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    const result = await getMyProfile()
    member.value = result.member
    nickname.value = result.member?.nickname || ''
  } finally {
    loading.value = false
  }
}

function syncCachedUser(next) {
  const cached = {
    ...(getLoginUser() || {}),
    userId: next.id,
    nickname: next.nickname,
    mobile: next.mobile || '',
    avatar: next.avatar || '',
  }
  setLoginUser(cached)
}

async function uploadAvatarPath(filePath) {
  if (!filePath || uploading.value) return
  uploading.value = true
  try {
    const next = await uploadMyAvatar(filePath)
    member.value = next
    syncCachedUser(next)
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } finally {
    uploading.value = false
  }
}

function onChooseAvatar(event) {
  uploadAvatarPath(event?.detail?.avatarUrl)
}

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (result) => uploadAvatarPath(result.tempFilePaths?.[0]),
  })
}

async function saveProfile() {
  const value = String(nickname.value || '').trim()
  if (!value) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  if (value.length > 32) {
    uni.showToast({ title: '昵称不能超过32个字符', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const next = await updateMyProfile({ nickname: value })
    member.value = next
    nickname.value = next.nickname
    syncCachedUser(next)
    uni.showToast({ title: '资料已保存', icon: 'success' })
  } finally {
    saving.value = false
  }
}

async function onGetPhoneNumber(event) {
  const code = event?.detail?.code
  if (!code) {
    if (!String(event?.detail?.errMsg || '').includes('deny')) {
      uni.showToast({ title: '未取得手机号授权', icon: 'none' })
    }
    return
  }
  if (bindingPhone.value) return
  bindingPhone.value = true
  try {
    const result = await bindWechatPhone(code)
    setToken(result.token)
    member.value = { ...member.value, mobile: result.mobile || '' }
    syncCachedUser(member.value)
    uni.showToast({ title: '手机号已绑定', icon: 'success' })
  } finally {
    bindingPhone.value = false
  }
}

function maskMobile(value) {
  return String(value || '').replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中...</view>
    <template v-else-if="member">
      <view class="card profile-card">
        <!-- #ifdef MP-WEIXIN -->
        <button class="avatar-button" open-type="chooseAvatar" :disabled="uploading" @chooseavatar="onChooseAvatar">
          <image v-if="member.avatar" class="avatar" :src="member.avatar" mode="aspectFill" />
          <view v-else class="avatar placeholder"><AppIcon name="user-round-white" :size="34" /></view>
          <view class="avatar-action">{{ uploading ? '上传中...' : '更换头像' }}</view>
        </button>
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <button class="avatar-button" :disabled="uploading" @click="chooseImage">
          <image v-if="member.avatar" class="avatar" :src="member.avatar" mode="aspectFill" />
          <view v-else class="avatar placeholder"><AppIcon name="user-round-white" :size="34" /></view>
          <view class="avatar-action">{{ uploading ? '上传中...' : '更换头像' }}</view>
        </button>
        <!-- #endif -->
      </view>

      <view class="card form-card">
        <view class="form-row">
          <text class="label">昵称</text>
          <input v-model="nickname" class="input" type="nickname" maxlength="32" placeholder="请输入昵称" />
        </view>
        <view class="form-row phone-row">
          <text class="label">手机号</text>
          <text class="phone-value">{{ member.mobile ? maskMobile(member.mobile) : '未绑定' }}</text>
          <!-- #ifdef MP-WEIXIN -->
          <button
            class="phone-button"
            open-type="getPhoneNumber"
            :disabled="bindingPhone"
            @getphonenumber="onGetPhoneNumber"
          >{{ bindingPhone ? '获取中...' : (member.mobile ? '更换' : '微信授权获取') }}</button>
          <!-- #endif -->
        </view>
      </view>

      <button class="save-button" :disabled="saving || uploading" @click="saveProfile">
        {{ saving ? '保存中...' : '保存资料' }}
      </button>
    </template>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #f4f5f3; color: #20262e; }
.empty { padding: 120rpx 0; text-align: center; color: #92989c; font-size: 24rpx; }
.card { background: #fff; border: 1rpx solid #e4e6e3; border-radius: 16rpx; }
.profile-card { padding: 44rpx 24rpx 36rpx; display: flex; justify-content: center; }
.avatar-button { width: 190rpx; margin: 0; padding: 0; display: flex; flex-direction: column; align-items: center; background: transparent; line-height: 1.4; }
.avatar-button::after { border: 0; }
.avatar { width: 136rpx; height: 136rpx; border-radius: 50%; background: #eef1f0; }
.placeholder { display: flex; align-items: center; justify-content: center; background: #ed6a5a; color: #fff; }
.avatar-action { margin-top: 16rpx; color: #2a78d6; font-size: 23rpx; }
.form-card { margin-top: 20rpx; padding: 0 26rpx; }
.form-row { min-height: 104rpx; display: flex; align-items: center; gap: 20rpx; border-bottom: 1rpx solid #eceeeb; }
.form-row:last-child { border-bottom: 0; }
.label { width: 110rpx; flex-shrink: 0; color: #454b50; font-size: 25rpx; }
.input { flex: 1; min-width: 0; height: 82rpx; text-align: right; color: #20262e; font-size: 25rpx; }
.phone-row { flex-wrap: nowrap; }
.phone-value { flex: 1; text-align: right; color: #7d858a; font-size: 23rpx; }
.phone-button { min-width: 116rpx; height: 58rpx; margin: 0; padding: 0 18rpx; border: 1rpx solid #cfd5d2; border-radius: 8rpx; background: #fff; color: #2a78d6; font-size: 21rpx; line-height: 56rpx; }
.phone-button::after { border: 0; }
.save-button { width: 100%; height: 82rpx; margin-top: 30rpx; border-radius: 12rpx; background: #2a78d6; color: #fff; font-size: 27rpx; line-height: 82rpx; }
.save-button::after { border: 0; }
</style>
