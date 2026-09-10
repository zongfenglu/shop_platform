<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createAddress, getAddress, updateAddress } from '@/api'
// #ifdef H5
import { pcaTextArr } from 'element-china-area-data'
// #endif

/**
 * 新增/编辑收货地址。带 id 参数即编辑，否则新增。
 *
 * 省市区用 uni 内置 picker 的 region 模式（各端都有原生实现），不自己搓三级联动 ——
 * 后端 user_address 的 province/city/region 就是三个独立字段，与 picker 的三段值一一对应。
 * 注意：库里还没有 region 字典表（文档三 §3.1 规划了 region 表但未落地），
 * 所以这里存的是 picker 返回的文本，不是行政区划码；等 region 表落地后再补编码字段。
 */
const form = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  region: '',
  detail: '',
  isDefault: false,
})
const editingId = ref('')
const saving = ref(false)
/** 编辑已有的默认地址时不允许在这里取消默认（后端也会忽略），改默认请去列表页设别的地址 */
const lockDefault = ref(false)

// #ifdef H5
const h5Province = ref('')
const h5City = ref('')
const h5Region = ref('')
const h5Cities = computed(() =>
  pcaTextArr.find((item) => item.value === h5Province.value)?.children || [])
const h5Regions = computed(() =>
  h5Cities.value.find((item) => item.value === h5City.value)?.children || [])

function syncH5Region(province, city, region) {
  h5Province.value = pcaTextArr.some((item) => item.value === province) ? province : ''
  h5City.value = h5Cities.value.some((item) => item.value === city) ? city : ''
  h5Region.value = h5Regions.value.some((item) => item.value === region) ? region : ''
}

function onH5ProvinceChange() {
  h5City.value = ''
  h5Region.value = ''
  form.value.province = h5Province.value
  form.value.city = ''
  form.value.region = ''
}

function onH5CityChange() {
  h5Region.value = ''
  form.value.city = h5City.value
  form.value.region = ''
}

function onH5RegionChange() {
  form.value.region = h5Region.value
}
// #endif

onLoad(async (query) => {
  if (!query?.id) {
    uni.setNavigationBarTitle({ title: '新增收货地址' })
    return
  }
  editingId.value = query.id // 雪花 id，保持字符串
  uni.setNavigationBarTitle({ title: '编辑收货地址' })
  try {
    const data = await getAddress(editingId.value)
    form.value = {
      name: data.name || '',
      phone: data.phone || '',
      province: data.province || '',
      city: data.city || '',
      region: data.region || '',
      detail: data.detail || '',
      isDefault: !!data.isDefault,
    }
    // #ifdef H5
    syncH5Region(form.value.province, form.value.city, form.value.region)
    // #endif
    lockDefault.value = !!data.isDefault
  } catch (e) {
    uni.showToast({ title: '地址不存在', icon: 'none' })
  }
})

function onRegionChange(e) {
  const [province, city, region] = e.detail.value
  form.value.province = province
  form.value.city = city
  form.value.region = region
}

function validate() {
  if (!form.value.name.trim()) return '请填写收货人'
  if (!/^1[3-9]\d{9}$/.test(form.value.phone.trim())) return '手机号格式不正确'
  if (!form.value.province || !form.value.city || !form.value.region) return '请选择省市区'
  if (!form.value.detail.trim()) return '请填写详细地址'
  return null
}

async function onSave() {
  const err = validate()
  if (err) {
    uni.showToast({ title: err, icon: 'none' })
    return
  }
  if (saving.value) return
  saving.value = true
  const payload = {
    name: form.value.name.trim(),
    phone: form.value.phone.trim(),
    province: form.value.province || undefined,
    city: form.value.city || undefined,
    region: form.value.region || undefined,
    detail: form.value.detail.trim(),
    isDefault: form.value.isDefault,
  }
  try {
    if (editingId.value) {
      await updateAddress(editingId.value, payload)
    } else {
      await createAddress(payload)
    }
    uni.showToast({ title: '已保存' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
    // request.js 已统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="card">
      <view class="field">
        <text class="label">收货人</text>
        <input v-model="form.name" class="input" placeholder="姓名" maxlength="64" />
      </view>
      <view class="field">
        <text class="label">手机号</text>
        <input v-model="form.phone" class="input" type="number" placeholder="11 位手机号" maxlength="11" />
      </view>
      <!-- #ifdef H5 -->
      <view class="field region-field">
        <text class="label">所在地区</text>
        <view class="region-selects">
          <select v-model="h5Province" class="region-select" aria-label="省份" @change="onH5ProvinceChange">
            <option value="">请选择省</option>
            <option v-for="item in pcaTextArr" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <select v-model="h5City" class="region-select" aria-label="城市" :disabled="!h5Province" @change="onH5CityChange">
            <option value="">请选择市</option>
            <option v-for="item in h5Cities" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <select v-model="h5Region" class="region-select" aria-label="区县" :disabled="!h5City" @change="onH5RegionChange">
            <option value="">请选择区</option>
            <option v-for="item in h5Regions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </view>
      </view>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <picker mode="region" :value="[form.province, form.city, form.region]" @change="onRegionChange">
        <view class="field">
          <text class="label">所在地区</text>
          <text class="input" :class="{ placeholder: !form.province }">
            {{ form.province ? `${form.province} ${form.city} ${form.region}` : '请选择省市区' }}
          </text>
          <text class="arrow">›</text>
        </view>
      </picker>
      <!-- #endif -->
      <view class="field align-top">
        <text class="label">详细地址</text>
        <textarea v-model="form.detail" class="textarea" placeholder="街道、楼牌号等" maxlength="255" />
      </view>
      <view class="field">
        <text class="label">设为默认</text>
        <switch :checked="form.isDefault" :disabled="lockDefault" @change="form.isDefault = $event.detail.value" />
      </view>
      <view v-if="lockDefault" class="hint">当前已是默认地址；如需更换，请在地址列表把其它地址设为默认。</view>
    </view>

    <view class="bottom-spacer" />
    <view class="bar">
      <button class="bar-btn" :disabled="saving" @click="onSave">{{ saving ? '保存中…' : '保存' }}</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 20rpx;
  min-height: 100vh;
}
.card {
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.08);
  border-radius: 18rpx;
  padding: 8rpx 24rpx;
}
.field {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 26rpx 0;
  border-bottom: 2rpx solid rgba(11, 11, 11, 0.06);
}
.field.align-top {
  align-items: flex-start;
}
.label {
  font-size: 27rpx;
  color: #4a4844;
  width: 140rpx;
  flex-shrink: 0;
}
.input {
  flex: 1;
  font-size: 27rpx;
}
.input.placeholder {
  color: #b5b3ad;
}
.textarea {
  flex: 1;
  font-size: 27rpx;
  height: 140rpx;
  width: 100%;
}
.arrow {
  color: #b5b3ad;
  font-size: 32rpx;
}
.region-field { align-items: flex-start; }
.region-selects {
  flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 14rpx;
}
.region-select {
  box-sizing: border-box; width: 100%; height: 64rpx; padding: 0 18rpx;
  border: 2rpx solid rgba(11, 11, 11, 0.12); border-radius: 10rpx;
  background: #fff; color: #2b2a27; font-size: 26rpx;
}
.region-select:disabled { color: #b5b3ad; background: #f5f4f1; }
.hint {
  font-size: 23rpx;
  color: #898781;
  line-height: 1.6;
  padding: 18rpx 0 24rpx;
}
.bottom-spacer {
  height: 140rpx;
}
.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-top: 2rpx solid rgba(11, 11, 11, 0.08);
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.bar-btn {
  background: #d4380d;
  color: #fff;
  border-radius: 38rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  margin: 0;
}
</style>
