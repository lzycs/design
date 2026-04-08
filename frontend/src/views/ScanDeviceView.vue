<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authorizeScanDevice } from '@/api/scanDevice'

const route = useRoute()
const router = useRouter()

const token = ref<string>('')
const authorizeState = ref<'pending' | 'success' | 'failed'>('pending')
const stateText = computed(() => {
  if (authorizeState.value === 'success') return '授权成功'
  if (authorizeState.value === 'failed') return '授权失败'
  return '正在完成设备授权'
})
const stateDesc = computed(() => {
  if (authorizeState.value === 'success') return '该设备已授权，可直接扫描用户端预约二维码。'
  if (authorizeState.value === 'failed') return '请返回重新扫码授权二维码，或联系管理员重新生成。'
  return '请保持该页面打开，授权完成后可扫描用户端预约二维码。'
})
const remainSeconds = ref<number | null>(null)
let countdownTimer: number | null = null

const deviceUidKey = 'scanDeviceUid'

const getOrCreateDeviceUid = () => {
  const raw = localStorage.getItem(deviceUidKey)
  if (raw) return raw
  const next =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `dev-${Math.random().toString(16).slice(2)}-${Date.now()}`
  localStorage.setItem(deviceUidKey, next)
  return next
}

onMounted(async () => {
  const t = route.query.token
  token.value = t ? String(t) : ''
  const expireAtRaw = route.query.expireAt ? String(route.query.expireAt) : ''
  if (expireAtRaw) {
    const expireAtMs = new Date(expireAtRaw).getTime()
    if (!Number.isNaN(expireAtMs)) {
      const updateRemain = () => {
        const left = Math.ceil((expireAtMs - Date.now()) / 1000)
        remainSeconds.value = Math.max(0, left)
      }
      updateRemain()
      countdownTimer = window.setInterval(updateRemain, 1000)
    }
  }

  if (!token.value) {
    authorizeState.value = 'failed'
    showToast('缺少授权 token')
    return
  }

  const deviceUid = getOrCreateDeviceUid()
  try {
    const res = await authorizeScanDevice(token.value, deviceUid)
    if (res.code !== 200) {
      authorizeState.value = 'failed'
      showToast(res.message || '授权失败')
      return
    }
    authorizeState.value = 'success'
    showToast('授权成功，可开始扫描预约二维码')
  } catch (e: unknown) {
    authorizeState.value = 'failed'
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '授权失败，请稍后重试')
  }
})

onBeforeUnmount(() => {
  if (countdownTimer !== null) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<template>
  <div class="scan-device-page">
    <van-nav-bar title="设备授权" left-arrow @click-left="router.back()" />
    <div class="content">
      <div class="card">
        <div class="title" :class="{ ok: authorizeState === 'success', fail: authorizeState === 'failed' }">{{ stateText }}</div>
        <div class="desc">{{ stateDesc }}</div>
        <div class="desc" v-if="remainSeconds !== null">配对码剩余时间：{{ remainSeconds }} 秒</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scan-device-page {
  background-color: #f5f7fa;
  min-height: 100vh;
}
.content {
  padding: 16px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.title.ok {
  color: #18a058;
}
.title.fail {
  color: #d03050;
}
.desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-top: 6px;
}
</style>

