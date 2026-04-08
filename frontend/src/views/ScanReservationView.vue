<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { scanReservationQrcode } from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const code = ref<string>('')
const deviceUidKey = 'scanDeviceUid'

const deviceUid = ref<string>('')

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

const resultMessage = ref<string>('正在校验中...')

onMounted(async () => {
  code.value = route.query.code ? String(route.query.code) : ''
  if (!code.value) {
    resultMessage.value = '二维码无效'
    return
  }
  deviceUid.value = getOrCreateDeviceUid()
  try {
    const res = await scanReservationQrcode(code.value, deviceUid.value)
    if (res.code !== 200) {
      resultMessage.value = res.message || '校验失败'
      showToast(res.message || '校验失败')
      return
    }
    resultMessage.value = res.data?.message || '校验完成'
    showToast(resultMessage.value)
  } catch (e: unknown) {
    console.error(e)
    resultMessage.value = '校验失败，请稍后重试'
    showToast('校验失败')
  }
})
</script>

<template>
  <div class="scan-reservation-page">
    <van-nav-bar title="预约核销结果" left-arrow @click-left="router.back()" />
    <div class="content">
      <div class="card">
        <div class="title">处理结果</div>
        <div class="message">{{ resultMessage }}</div>
        <div class="tip">完成后可关闭页面或返回。</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scan-reservation-page {
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
  margin-bottom: 10px;
}
.message {
  font-size: 14px;
  color: #4a90e2;
  line-height: 1.6;
  word-break: break-word;
  margin-bottom: 12px;
}
.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>

