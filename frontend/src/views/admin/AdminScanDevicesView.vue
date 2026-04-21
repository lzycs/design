<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue'
import QRCode from 'qrcode'
import { showConfirmDialog, showToast } from 'vant'
import { buildPublicUrl, getPublicWebOrigin, isLikelyLocalhostOrigin } from '@/utils/publicUrl'
import {
  generateAdminPairToken,
  getAdminScanDevices,
  updateAdminScanDeviceEnabled,
  deleteAdminScanDevice,
  type AdminScanDevice,
} from '@/api/admin'

const pairToken = ref<string>('')
const pairExpiredTime = ref<string>('')
const loadingToken = ref(false)
const devices = ref<AdminScanDevice[]>([])
const loadingDevices = ref(false)
let autoRefreshTimer: number | null = null

const qrCanvas = ref<HTMLCanvasElement | null>(null)

const scanDeviceUrl = computed(() => {
  if (!pairToken.value) return ''
  const base = import.meta.env.BASE_URL
  const basePath = base.endsWith('/') ? base : base + '/'
  const expireAt = pairExpiredTime.value ? `&expireAt=${encodeURIComponent(pairExpiredTime.value)}` : ''
  return buildPublicUrl(`${basePath}scan-device?token=${encodeURIComponent(pairToken.value)}${expireAt}`)
})

const renderQr = async () => {
  if (!qrCanvas.value || !scanDeviceUrl.value) return
  await QRCode.toCanvas(qrCanvas.value, scanDeviceUrl.value, { errorCorrectionLevel: 'M', margin: 1 })
}

const genToken = async () => {
  loadingToken.value = true
  try {
    if (isLikelyLocalhostOrigin()) {
      showToast('当前二维码域名是 localhost，手机不可访问；请配置 VITE_PUBLIC_WEB_ORIGIN 为电脑局域网地址')
    }
    const res = await generateAdminPairToken()
    if (res.code !== 200) {
      showToast(res.message || '生成失败')
      return
    }
    pairToken.value = res.data?.token || ''
    pairExpiredTime.value = res.data?.expiredTime || ''
    await nextTick()
    await renderQr()
  } catch (e: unknown) {
    console.error(e)
    showToast('生成失败')
  } finally {
    loadingToken.value = false
  }
}

const loadDevices = async (silent = false) => {
  if (!silent) {
    loadingDevices.value = true
  }
  try {
    const res = await getAdminScanDevices()
    if (res.code !== 200) {
      if (!silent) {
        showToast(res.message || '加载失败')
      }
      return
    }
    devices.value = res.data ?? []
  } catch (e: unknown) {
    console.error(e)
    if (!silent) {
      devices.value = []
    }
  } finally {
    if (!silent) {
      loadingDevices.value = false
    }
  }
}

const startAutoRefresh = () => {
  if (autoRefreshTimer !== null) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
  // 后台列表自动刷新：保证手机授权成功后无需手动刷新
  autoRefreshTimer = window.setInterval(() => {
    loadDevices(true)
  }, 3000)
}

const toggleEnabled = async (d: AdminScanDevice, nextEnabled: number) => {
  if (!d.id) return
  try {
    const res = await updateAdminScanDeviceEnabled(d.id, { enabled: nextEnabled, deviceName: d.deviceName })
    if (res.code !== 200) {
      showToast(res.message || '操作失败')
      return
    }
    await loadDevices()
    showToast('已更新')
  } catch (e: unknown) {
    console.error(e)
    showToast('操作失败')
  }
}

const removeDevice = async (d: AdminScanDevice) => {
  if (!d.id) return
  const ok = await showConfirmDialog({
    title: '删除设备',
    message: `确定删除设备「${d.deviceName || d.deviceUid || d.id}」吗？`,
  }).catch(() => false)
  if (!ok) return
  try {
    const res = await deleteAdminScanDevice(d.id)
    if (res.code !== 200) {
      showToast(res.message || '删除失败')
      return
    }
    showToast('删除成功')
    await loadDevices()
  } catch (e: unknown) {
    console.error(e)
    showToast('删除失败')
  }
}

onMounted(() => {
  loadDevices()
  startAutoRefresh()
  // 不自动生成 token，避免管理员误触；需要时点击按钮生成
})

onBeforeUnmount(() => {
  if (autoRefreshTimer !== null) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
})
</script>

<template>
  <div>
    <div class="page-header">
      <div style="width: 24px"></div>
      <div class="page-header-title">设备授权</div>
      <div style="width: 24px"></div>
    </div>

    <div class="content-area">
      <div class="admin-card">
        <div class="section-title">授权设备配对二维码（一次性）</div>
        <div class="hint">管理员生成后，授权设备端扫码页面会自动绑定该设备。</div>
        <div class="hint">当前扫码域名：{{ getPublicWebOrigin() }}</div>
        <div class="btn-row">
          <button class="action-btn primary-btn" :disabled="loadingToken" @click="genToken">
            {{ loadingToken ? '生成中...' : '生成授权二维码' }}
          </button>
        </div>
        <div v-if="pairToken" class="qr-wrap">
          <canvas ref="qrCanvas" class="qr-canvas" />
          <div class="qr-desc">扫码完成后即可核销用户端预约二维码</div>
        </div>
        <div v-else class="empty-desc">尚未生成二维码</div>
      </div>

      <div class="admin-card">
        <div class="section-title">已授权设备</div>
        <div class="hint">可禁用某台设备，禁用后将无法扫码核销预约。</div>

        <div v-if="loadingDevices" class="list-loading">加载中...</div>

        <div v-else-if="devices.length === 0" class="empty-desc">暂无授权设备</div>

        <div v-else class="device-list">
          <div v-for="d in devices" :key="d.id" class="device-item">
            <div class="device-info">
              <div class="device-name">{{ d.deviceName || '未命名设备' }}</div>
              <div class="device-uid">{{ d.deviceUid }}</div>
            </div>
            <div class="device-actions">
              <button
                v-if="d.enabled === 1"
                class="action-btn danger-btn"
                @click="toggleEnabled(d, 0)"
              >
                禁用
              </button>
              <button
                v-else
                class="action-btn primary-btn"
                @click="toggleEnabled(d, 1)"
              >
                启用
              </button>
              <button class="action-btn ghost-danger-btn" @click="removeDevice(d)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.content-area {
  padding: 16px 20px;
}
.page-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #4a90e2;
  color: #fff;
  position: sticky;
  top: 0;
  z-index: 30;
}
.page-header-title {
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  text-align: center;
}
.admin-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
}
.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}
.hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 12px;
}
.btn-row {
  display: flex;
  gap: 12px;
}
.action-btn {
  padding: 10px 14px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  cursor: pointer;
  font-weight: 600;
}
.primary-btn {
  background-color: #4a90e2;
  color: #fff;
}
.danger-btn {
  background-color: #f56c6c;
  color: #fff;
}
.qr-wrap {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.qr-canvas {
  width: 210px;
  height: 210px;
  image-rendering: pixelated;
}
.qr-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.empty-desc {
  font-size: 13px;
  color: #909399;
  padding: 20px 0;
  text-align: center;
}
.device-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 12px;
  border: 1px solid #f5f7fa;
  border-radius: 10px;
}
.device-info {
  min-width: 0;
}
.device-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}
.device-uid {
  font-size: 12px;
  color: #909399;
  word-break: break-all;
  max-width: 180px;
}
.device-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}
.ghost-danger-btn {
  background-color: #fff5f5;
  color: #d03050;
  border: 1px solid #ffd6d6;
}
.list-loading {
  font-size: 13px;
  color: #909399;
  padding: 16px 0;
  text-align: center;
}
</style>

