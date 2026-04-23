<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import QRCode from 'qrcode'
import { buildPublicUrl, isLikelyLocalhostOrigin } from '@/utils/publicUrl'
import {
  getUserReservations,
  type Reservation,
  updateReservation,
  generateReservationQrcode,
  getReservationQrcodeStatus,
} from '@/api/reservation'

interface User {
  id?: number
  username: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const reservations = ref<Reservation[]>([])
const loading = ref(false)

const statusTab = ref<'all' | 'pending' | 'done' | 'cancelled'>('all')

const reservationDetail = ref<Reservation | null>(null)
const showReservationDetail = ref(false)

const showScan = ref(false)
const scanningReservation = ref<Reservation | null>(null)
const qrCanvas = ref<HTMLCanvasElement | null>(null)
let pollTimer: number | null = null
let lastUnauthorizedToastAt = 0

const isLoggedIn = computed(() => !!storedUser.value?.id)

const pendingReservations = computed(() =>
  reservations.value.filter((r) => r.status === 1)
)
const doneReservations = computed(() =>
  reservations.value.filter((r) => r.status === 2 || r.status === 3)
)
const cancelledReservations = computed(() =>
  reservations.value.filter((r) => r.status === 4 || r.status === 5)
)

const getStatusText = (status?: number) => {
  if (status === 1) return '待签到'
  if (status === 2) return '已签到'
  if (status === 3) return '已完成'
  if (status === 4) return '已取消'
  if (status === 5) return '已违约'
  return '未知'
}

const getStatusClass = (status?: number) => {
  if (status === 1) return 'status-pending'
  if (status === 2 || status === 3) return 'status-done'
  if (status === 4 || status === 5) return 'status-cancelled'
  return 'status-unknown'
}

const getTitle = (item: Reservation) => {
  if (item.resourceName) return item.resourceName
  if (item.resourceType === 1) return `教室预约 #${item.id ?? '-'}`
  if (item.resourceType === 2) return `图书馆预约 #${item.id ?? '-'}`
  return `预约 #${item.id ?? '-'}`
}

const loadFromStorage = () => {
  const raw = localStorage.getItem('currentUser')
  if (raw) {
    try {
      storedUser.value = JSON.parse(raw)
    } catch {
      storedUser.value = null
    }
  }
}

const loadReservations = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await getUserReservations(storedUser.value.id)
    const maybeData = (res as unknown as { data?: unknown }).data
    if (Array.isArray(maybeData)) {
      reservations.value = maybeData as Reservation[]
    } else if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
      reservations.value = (maybeData as { data: Reservation[] }).data
    } else {
      reservations.value = []
    }
  } catch (e) {
    console.error(e)
    reservations.value = []
    showToast('加载预约记录失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openReservationDetail = (item: Reservation) => {
  reservationDetail.value = item
  showReservationDetail.value = true
}

const goRoomCollab = (item: Reservation) => {
  if (!item.id) return
  router.push(`/reservation/room-collab/${item.id}`)
}

const goCampusNav = (item: Reservation) => {
  const targetName = item.buildingName || item.resourceName || ''
  router.push({
    path: '/campus-nav',
    query: {
      targetName,
      reservationId: item.id ? String(item.id) : undefined,
    },
  })
}

const cancelReservation = async (item: Reservation) => {
  const ok = await showConfirmDialog({
    title: '取消预约',
    message: '确定要取消该预约吗？'
  }).catch(() => false)
  if (!ok) return
  if (!item.id) return
  try {
    await updateReservation({ ...item, status: 4 })
    showToast('已取消预约')
    await loadReservations()
    statusTab.value = 'cancelled'
  } catch (e) {
    console.error(e)
    showToast('取消失败，请稍后重试')
  }
}

const startPolling = (code: string) => {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  pollTimer = window.setInterval(async () => {
    try {
      const res = await getReservationQrcodeStatus(code)
      if (res.code !== 200) return
      const data = res.data ?? ({} as any)
      const msg = data.message
      const ok = data.ok
      if (ok && data.success) {
        showToast('签到成功')
        await loadReservations()
        statusTab.value = 'done'
        cancelScan()
        return
      }
      if (msg === '二维码已过期' || msg === '二维码无效/已使用' || msg === '二维码无效') {
        showToast(msg)
        cancelScan()
        return
      }
      if (msg === '非授权设备禁止扫码') {
        const now = Date.now()
        if (now - lastUnauthorizedToastAt > 5000) {
          showToast(msg)
          lastUnauthorizedToastAt = now
        }
      }
    } catch (e) {
      console.error(e)
    }
  }, 1500)
}

const startScan = async (item: Reservation) => {
  if (!item.id) return
  scanningReservation.value = item
  showScan.value = true

  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }

  try {
    const res = await generateReservationQrcode(item.id)
    if (res.code !== 200 || !res.data?.code) {
      showToast(res.message || '二维码生成失败')
      cancelScan()
      return
    }
    await nextTick()
    if (qrCanvas.value) {
      const base = import.meta.env.BASE_URL
      const basePath = base.endsWith('/') ? base : base + '/'
      if (isLikelyLocalhostOrigin()) {
        showToast('当前二维码域名是 localhost，手机不可访问；请配置 VITE_PUBLIC_WEB_ORIGIN')
      }
      const url = buildPublicUrl(`${basePath}scan-reservation?code=${encodeURIComponent(res.data.code)}`)
      await QRCode.toCanvas(qrCanvas.value, url, { errorCorrectionLevel: 'M', margin: 1 })
    }
    startPolling(res.data.code)
  } catch (e: unknown) {
    console.error(e)
    showToast('二维码生成失败')
    cancelScan()
  }
}

const cancelScan = () => {
  showScan.value = false
  scanningReservation.value = null
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  loadFromStorage()
  const query = new URLSearchParams(window.location.search)
  const tab = query.get('tab')
  if (tab === 'pending' || tab === 'done' || tab === 'cancelled' || tab === 'all') {
    statusTab.value = tab
  }
  if (storedUser.value?.id) {
    await loadReservations()
  }
})
</script>

<template>
  <div class="profile-reservation">
    <van-nav-bar title="我的预约" left-arrow @click-left="goBack" />

    <div v-if="isLoggedIn" class="page">
      <div class="status-tag-bar">
        <div
          class="status-tag-item"
          :class="{ active: statusTab === 'all' }"
          @click="statusTab = 'all'"
        >
          全部
        </div>
        <div
          class="status-tag-item"
          :class="{ active: statusTab === 'pending' }"
          @click="statusTab = 'pending'"
        >
          待签到
        </div>
        <div
          class="status-tag-item"
          :class="{ active: statusTab === 'done' }"
          @click="statusTab = 'done'"
        >
          已完成
        </div>
        <div
          class="status-tag-item"
          :class="{ active: statusTab === 'cancelled' }"
          @click="statusTab = 'cancelled'"
        >
          已取消
        </div>
      </div>

      <van-loading v-if="loading" size="24px" class="loading" vertical>加载中...</van-loading>

      <template v-else>
        <van-empty v-if="reservations.length === 0" description="暂无预约记录" />

        <div v-else-if="statusTab === 'all'">
          <div
            v-for="item in reservations"
            :key="item.id"
            class="booking-item"
            @click="openReservationDetail(item)"
          >
            <div class="booking-header">
              <div class="booking-info">
                <div class="booking-title">{{ getTitle(item) }}</div>
                <div class="booking-subtitle">
                  {{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}
                </div>
              </div>
              <div class="booking-status" :class="getStatusClass(item.status)">
                {{ getStatusText(item.status) }}
              </div>
            </div>
            <div class="booking-detail">
              <div class="detail-row">
                <span class="detail-label">用途：</span>
                <span>{{ item.purpose || '自习/讨论' }}</span>
              </div>
              <div class="detail-row" v-if="item.checkinTime">
                <span class="detail-label">签到时间：</span>
                <span>{{ item.checkinTime }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="statusTab === 'pending'">
          <div v-if="pendingReservations.length">
            <div
              v-for="item in pendingReservations"
              :key="item.id"
              class="booking-item"
              @click="openReservationDetail(item)"
            >
              <div class="booking-header">
                <div class="booking-info">
                  <div class="booking-title">{{ getTitle(item) }}</div>
                  <div class="booking-subtitle">
                    {{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}
                  </div>
                </div>
                <div class="booking-status status-pending">待签到</div>
              </div>
              <div class="booking-detail">
                <div class="detail-row">
                  <span class="detail-label">预约日期：</span>
                  <span>{{ item.reservationDate }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">预约时段：</span>
                  <span>{{ item.startTime }}-{{ item.endTime }}</span>
                </div>
              </div>
              <div class="booking-actions">
                <button
                  v-if="item.resourceType === 1"
                  class="action-btn btn-primary"
                  @click.stop="goCampusNav(item)"
                >
                  导航
                </button>
                <button class="action-btn btn-secondary" @click.stop="startScan(item)">扫码签到</button>
                <button class="action-btn btn-danger" @click.stop="cancelReservation(item)">取消预约</button>
              </div>
            </div>
          </div>
          <div v-else class="empty-text">暂无待签到预约</div>
        </div>

        <div v-else-if="statusTab === 'done'">
          <div v-if="doneReservations.length">
            <div
              v-for="item in doneReservations"
              :key="item.id"
              class="booking-item"
              @click="openReservationDetail(item)"
            >
              <div class="booking-header">
                <div class="booking-info">
                  <div class="booking-title">{{ getTitle(item) }}</div>
                  <div class="booking-subtitle">
                    {{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}
                  </div>
                </div>
                <div class="booking-status status-done">{{ getStatusText(item.status) }}</div>
              </div>
              <div class="booking-detail">
                <div class="detail-row">
                  <span class="detail-label">用途：</span>
                  <span>{{ item.purpose || '自习/讨论' }}</span>
                </div>
                <div class="detail-row" v-if="item.checkinTime">
                  <span class="detail-label">签到时间：</span>
                  <span>{{ item.checkinTime }}</span>
                </div>
              </div>
              <div class="booking-actions">
                <button
                  v-if="item.resourceType === 1"
                  class="action-btn btn-secondary"
                  @click.stop="goRoomCollab(item)"
                >
                  查看详情
                </button>
              </div>
            </div>
          </div>
          <div v-else class="empty-text">暂无已完成预约</div>
        </div>

        <div v-else>
          <div v-if="cancelledReservations.length">
            <div
              v-for="item in cancelledReservations"
              :key="item.id"
              class="booking-item"
              @click="openReservationDetail(item)"
            >
              <div class="booking-header">
                <div class="booking-info">
                  <div class="booking-title">{{ getTitle(item) }}</div>
                  <div class="booking-subtitle">
                    {{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}
                  </div>
                </div>
                <div class="booking-status status-cancelled">已取消</div>
              </div>
              <div class="booking-detail">
                <div class="detail-row">
                  <span class="detail-label">用途：</span>
                  <span>{{ item.purpose || '自习/讨论' }}</span>
                </div>
              </div>
              <div class="booking-actions">
                <button class="action-btn btn-secondary" @click.stop="$router.push('/reservation')">去预约</button>
              </div>
            </div>
          </div>
          <div v-else class="empty-text">暂无已取消预约</div>
        </div>
      </template>
    </div>

    <van-empty v-else description="请先登录后查看预约记录" />

    <van-popup v-model:show="showReservationDetail" round position="bottom" :style="{ height: '60%' }">
      <div class="detail-wrapper" v-if="reservationDetail">
        <div class="detail-title">预约详情</div>
        <div class="detail-row">地点：{{ getTitle(reservationDetail) }}</div>
        <div class="detail-row">日期：{{ reservationDetail.reservationDate }}</div>
        <div class="detail-row">时间：{{ reservationDetail.startTime }} - {{ reservationDetail.endTime }}</div>
        <div class="detail-row">用途：{{ reservationDetail.purpose || '自习/讨论' }}</div>
        <div class="detail-row">状态：{{ getStatusText(reservationDetail.status) }}</div>
        <div class="detail-row" v-if="reservationDetail.checkinTime">签到时间：{{ reservationDetail.checkinTime }}</div>
      </div>
    </van-popup>

    <van-popup v-model:show="showScan" round :style="{ width: '86%', maxWidth: '360px' }">
      <div class="scan-modal-inner">
        <div class="scan-title">签到</div>
        <div class="scan-subtitle">请使用已授权设备扫描下方二维码</div>
        <div class="scan-box">
          <canvas ref="qrCanvas" class="qr-canvas" />
        </div>
        <div class="scan-tips">核销成功后页面将自动更新签到状态</div>
        <button class="scan-cancel-btn" @click="cancelScan">取消扫码</button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-reservation {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page {
  padding: 16px;
}

.status-tag-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 12px;
}

.status-tag-item {
  padding: 8px 16px;
  border-radius: 16px;
  background-color: #f5f7fa;
  font-size: 14px;
  white-space: nowrap;
}

.status-tag-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.loading {
  margin-top: 24px;
}

.booking-item {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.booking-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.booking-subtitle {
  font-size: 12px;
  color: #909399;
}

.booking-status {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.status-pending {
  background-color: #fef7e0;
  color: #e6a23c;
}

.status-done {
  background-color: #f0f9ff;
  color: #4a90e2;
}

.status-cancelled {
  background-color: #f9f0f5;
  color: #9061f9;
}

.status-unknown {
  background-color: #e5e7eb;
  color: #6b7280;
}

.booking-detail {
  margin: 8px 0 12px;
  font-size: 14px;
  color: #666666;
}

.detail-row {
  margin-bottom: 4px;
}

.detail-label {
  color: #909399;
  margin-right: 4px;
}

.booking-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #f5f7fa;
  padding-top: 8px;
}

.action-btn {
  padding: 6px 12px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
}

.btn-secondary {
  background-color: #f5f7fa;
  color: #666666;
}

.btn-primary {
  background-color: #4a90e2;
  color: #ffffff;
}

.btn-danger {
  background-color: #f56c6c;
  color: #ffffff;
}

.empty-text {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.detail-wrapper {
  padding: 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.detail-wrapper .detail-row {
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
}

.scan-modal-inner {
  padding: 18px 16px 16px;
  text-align: center;
}

.scan-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.scan-subtitle {
  font-size: 12px;
  color: #8b8b8b;
  margin-bottom: 12px;
}

.scan-box {
  width: 240px;
  height: 240px;
  border-radius: 14px;
  border: 2px solid #6aa8eb;
  background: #f8fbff;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-canvas {
  width: 210px;
  height: 210px;
  display: block;
  margin: 0 auto;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #4a90e2;
  animation: scanMove 2s linear infinite;
}

@keyframes scanMove {
  0% {
    top: 0;
  }
  100% {
    top: 100%;
  }
}

.scan-tips {
  font-size: 13px;
  color: #606266;
  margin-bottom: 14px;
}

.scan-cancel-btn {
  min-width: 110px;
  padding: 8px 18px;
  border-radius: 10px;
  border: 1px solid #d9e4f2;
  background-color: #f7faff;
  font-size: 13px;
  color: #4a90e2;
  font-weight: 600;
}
</style>

