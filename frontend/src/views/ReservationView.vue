<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import QRCode from 'qrcode'
import { buildPublicUrl, isLikelyLocalhostOrigin } from '@/utils/publicUrl'
import { getAvailableClassrooms, type Classroom } from '@/api/classroom'
import { getBuildingList, type Building } from '@/api/building'
import { getLibraryList, type Library } from '@/api/library'
import {
  createReservation,
  type ReservationPayload,
  getUserReservations,
  type Reservation,
  updateReservation,
  generateReservationQrcode,
  getReservationQrcodeStatus,
} from '@/api/reservation'
import { getSeatsByLibraryAndFloor, type LibrarySeat } from '@/api/librarySeat'

const router = useRouter()

const statusTab = ref<'booking' | 'pending' | 'checked' | 'cancelled'>('booking')
const dateTab = ref<'today' | 'tomorrow' | 'dayAfter'>('today')
const areaTab = ref<'classroom' | 'seminar' | 'library'>('classroom')

const buildings = ref<Building[]>([])
const normalClassrooms = ref<Classroom[]>([])
const seminarRooms = ref<Classroom[]>([])
const libraries = ref<Library[]>([])

const reservations = ref<Reservation[]>([])

const loading = ref(false)
const seatModalShow = ref(false)
const seatFloor = ref(1)
const seatLibraryId = ref<number | null>(null)
const seatList = ref<LibrarySeat[]>([])
const selectedSeatId = ref<number | null>(null)

const showScan = ref(false)
const scanningReservation = ref<Reservation | null>(null)
const qrCanvas = ref<HTMLCanvasElement | null>(null)
let pollTimer: number | null = null
let lastUnauthorizedToastAt = 0

const storedUser = ref<{ id: number } | null>(null)

const today = new Date()
const tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000)
const dayAfter = new Date(today.getTime() + 2 * 24 * 60 * 60 * 1000)

const formatDate = (d: Date) => {
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${m}/${day}`
}

const formatDisplayDate = (value?: string) => {
  if (!value) return ''
  const match = value.match(/^(\d{4}-\d{2}-\d{2})/)
  return match ? match[1] : value.replace('T', ' ')
}

const formatDisplayTime = (value?: string) => {
  if (!value) return ''
  const normalized = value.includes('T') ? (value.split('T')[1] ?? '') : value
  return normalized.length >= 5 ? normalized.slice(0, 5) : normalized
}

const formatDisplayTimeRange = (start?: string, end?: string) => {
  const startText = formatDisplayTime(start)
  const endText = formatDisplayTime(end)
  if (startText && endText) return `${startText}-${endText}`
  return startText || endText
}

const formatDisplayDateTime = (value?: string) => {
  if (!value) return ''
  return value.replace('T', ' ')
}

const todayText = computed(() => formatDate(today))
const tomorrowText = computed(() => formatDate(tomorrow))
const dayAfterText = computed(() => formatDate(dayAfter))

const isLoggedIn = computed(() => !!storedUser.value?.id)

const pendingReservations = computed(() =>
  reservations.value.filter((r) => r.status === 1)
)
const checkedReservations = computed(() =>
  reservations.value.filter((r) => r.status === 2 || r.status === 3)
)
const cancelledReservations = computed(() =>
  reservations.value.filter((r) => r.status === 4 || r.status === 5)
)

const buildingCards = computed(() => {
  const source = areaTab.value === 'classroom' ? normalClassrooms.value : seminarRooms.value
  const countMap = new Map<number, number>()
  source.forEach((c) => {
    if (!c.buildingId) return
    countMap.set(c.buildingId, (countMap.get(c.buildingId) ?? 0) + 1)
  })
  return buildings.value
    .filter((b) => !!b.id && countMap.has(b.id))
    .map((b) => ({
      ...b,
      classroomCount: countMap.get(b.id!) ?? 0,
    }))
})

const sortClassroomsByFloor = (list: Classroom[]) => {
  return [...list].sort((a, b) => {
    const floorA = a.floor ?? Number.MAX_SAFE_INTEGER
    const floorB = b.floor ?? Number.MAX_SAFE_INTEGER
    if (floorA !== floorB) return floorA - floorB
    return (a.roomNumber || a.name || '').localeCompare((b.roomNumber || b.name || ''), 'zh-Hans-CN', {
      numeric: true,
      sensitivity: 'base',
    })
  })
}

const loadClassroomsByType = async (type: number): Promise<Classroom[]> => {
  const res = await getAvailableClassrooms(type)
  const maybeData = (res as unknown as { data?: unknown }).data
  if (Array.isArray(maybeData)) return sortClassroomsByFloor(maybeData as Classroom[])
  if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
    return sortClassroomsByFloor((maybeData as { data: Classroom[] }).data)
  }
  return []
}

const loadLibraries = async () => {
  const res = await getLibraryList()
  const maybeData = (res as unknown as { data?: unknown }).data
  if (Array.isArray(maybeData)) return (maybeData as Library[])
  if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
    return (maybeData as { data: Library[] }).data
  }
  return []
}

const loadBuildings = async () => {
  const res = await getBuildingList()
  const maybeData = (res as unknown as { data?: unknown }).data
  if (Array.isArray(maybeData)) return maybeData as Building[]
  if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
    return (maybeData as { data: Building[] }).data
  }
  return []
}

const loadUserFromStorage = () => {
  const raw = localStorage.getItem('currentUser')
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as { id: number }
      storedUser.value = parsed
    } catch {
      storedUser.value = null
    }
  }
}

const loadUserReservations = async () => {
  if (!storedUser.value?.id) return
  const res = await getUserReservations(storedUser.value.id)
  const maybeData = (res as unknown as { data?: unknown }).data
  if (Array.isArray(maybeData)) {
    reservations.value = maybeData as Reservation[]
  } else if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
    reservations.value = (maybeData as { data: Reservation[] }).data
  } else {
    reservations.value = []
  }
}

const loadAll = async () => {
  try {
    loading.value = true
    loadUserFromStorage()
    const [normal, seminar, libraryList, buildingList] = await Promise.all([
      loadClassroomsByType(1),
      loadClassroomsByType(2),
      loadLibraries(),
      loadBuildings(),
    ])
    buildings.value = buildingList
    normalClassrooms.value = normal
    seminarRooms.value = seminar
    libraries.value = libraryList
    await loadUserReservations()
  } catch (e) {
    console.error(e)
    buildings.value = []
    normalClassrooms.value = []
    seminarRooms.value = []
    libraries.value = []
  } finally {
    loading.value = false
  }
}

const getCurrentDateString = () => {
  const base =
    dateTab.value === 'today'
      ? today
      : dateTab.value === 'tomorrow'
        ? tomorrow
        : dayAfter
  return `${base.getFullYear()}-${`${base.getMonth() + 1}`.padStart(2, '0')}-${`${base
    .getDate()
    .toString()
    .padStart(2, '0')}`}`
}

const buildReservationPayload = (
  params: Omit<ReservationPayload, 'userId' | 'reservationDate' | 'duration'>
): ReservationPayload | null => {
  if (!storedUser.value?.id) {
    showToast('请先登录后再预约')
    return null
  }
  const start = params.startTime ?? ''
  const end = params.endTime ?? ''
  const [shStr = '0', smStr = '0'] = start.split(':')
  const [ehStr = '0', emStr = '0'] = end.split(':')
  const sh = Number(shStr)
  const sm = Number(smStr)
  const eh = Number(ehStr)
  const em = Number(emStr)
  const duration = (eh * 60 + em) - (sh * 60 + sm)
  return {
    ...params,
    userId: storedUser.value.id,
    reservationDate: getCurrentDateString(),
    duration
  }
}

const submitReservation = async (payload: ReservationPayload | null) => {
  if (!payload) return
  try {
    await createReservation(payload)
    showToast('预约成功')
    await loadUserReservations()
    statusTab.value = 'pending'
  } catch (e: unknown) {
    console.error(e)
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '预约失败，请稍后重试')
  }
}

// 预约页目前只作为教室详情入口使用，保留选中状态和具体预约逻辑在详情页中处理。

const openSeatModal = (library: Library, floor: number) => {
  seatLibraryId.value = library.id ?? null
  seatFloor.value = floor
  selectedSeatId.value = null
  seatModalShow.value = true
  if (seatLibraryId.value != null) {
    loadSeats()
  }
}

const loadSeats = async () => {
  if (seatLibraryId.value == null) return
  try {
    const res = await getSeatsByLibraryAndFloor(seatLibraryId.value, seatFloor.value)
    const maybeData = (res as unknown as { data?: unknown }).data
    if (Array.isArray(maybeData)) {
      seatList.value = maybeData as LibrarySeat[]
    } else if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
      seatList.value = (maybeData as { data: LibrarySeat[] }).data
    } else {
      seatList.value = []
    }
  } catch (e) {
    console.error(e)
    seatList.value = []
  }
}

const selectSeat = (seat: LibrarySeat) => {
  if (seat.status !== 1 || seat.realTimeStatus === 2) return
  selectedSeatId.value = seat.id ?? null
}

const confirmSeatReservation = async () => {
  if (!selectedSeatId.value || !seatLibraryId.value) {
    showToast('请先选择一个座位')
    return
  }
  const payload = buildReservationPayload({
    resourceType: 2,
    librarySeatId: selectedSeatId.value,
    startTime: '08:00',
    endTime: '12:00',
    purpose: '图书馆自习'
  })
  await submitReservation(payload)
  seatModalShow.value = false
}

const cancelReservation = async (item: Reservation) => {
  const ok = await showConfirmDialog({
    title: '取消预约',
    message: '确定要取消该预约吗？'
  }).catch(() => false)
  if (!ok) return
  try {
    const updated: Reservation = { ...item, status: 4 }
    await updateReservation(updated)
    showToast('已取消预约')
    await loadUserReservations()
  } catch (e) {
    console.error(e)
    showToast('取消失败，请稍后重试')
  }
}

const goToBuildingClassrooms = (buildingId?: number) => {
  if (!buildingId) return
  const type = areaTab.value === 'seminar' ? 2 : 1
  router.push(`/reservation/building/${buildingId}/${type}`)
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
      const data = res.data ?? { ok: false, message: '' }
      const msg = data.message
      const ok = data.ok
      if (ok && data.success) {
        showToast('签到成功')
        await loadUserReservations()
        statusTab.value = 'checked'
        cancelScan()
        return
      }
      if (msg === '二维码已过期' || msg === '二维码无效/已使用' || msg === '二维码无效') {
        showToast(msg)
        cancelScan()
        return
      }
      if (msg === '非授权设备禁止扫码') {
        // 不立即关闭，让用户继续等待授权设备扫码成功
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
    // 渲染二维码（二维码指向设备端扫码页）
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

const goRoomCollab = (item: Reservation) => {
  if (!item.id || item.resourceType !== 1) return
  router.push(`/reservation/room-collab/${item.id}`)
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="reservation">
    <van-nav-bar title="预约中心" left-arrow @click-left="router.back()" />

    <div class="page" v-if="isLoggedIn">
      <div class="page-title">预约中心</div>

      <div class="filter-panel">
        <div class="status-tag-bar">
          <div
            class="status-tag-item"
            :class="{ active: statusTab === 'booking' }"
            @click="statusTab = 'booking'"
          >
            预约
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
            :class="{ active: statusTab === 'checked' }"
            @click="statusTab = 'checked'"
          >
            已签到
          </div>
          <div
            class="status-tag-item"
            :class="{ active: statusTab === 'cancelled' }"
            @click="statusTab = 'cancelled'"
          >
            已取消
          </div>
        </div>

        <template v-if="statusTab === 'booking'">
          <div class="date-tag-bar">
            <div
              class="date-tag-item"
              :class="{ active: dateTab === 'today' }"
              @click="dateTab = 'today'"
            >
              <div>今天</div>
              <div>{{ todayText }}</div>
            </div>
            <div
              class="date-tag-item"
              :class="{ active: dateTab === 'tomorrow' }"
              @click="dateTab = 'tomorrow'"
            >
              <div>明天</div>
              <div>{{ tomorrowText }}</div>
            </div>
            <div
              class="date-tag-item"
              :class="{ active: dateTab === 'dayAfter' }"
              @click="dateTab = 'dayAfter'"
            >
              <div>后天</div>
              <div>{{ dayAfterText }}</div>
            </div>
          </div>

          <div class="area-tag-bar">
            <div
              class="area-tag-item"
              :class="{ active: areaTab === 'classroom' }"
              @click="areaTab = 'classroom'"
            >
              普通教室
            </div>
            <div
              class="area-tag-item"
              :class="{ active: areaTab === 'seminar' }"
              @click="areaTab = 'seminar'"
            >
              研讨室
            </div>
            <div
              class="area-tag-item"
              :class="{ active: areaTab === 'library' }"
              @click="areaTab = 'library'"
            >
              图书馆
            </div>
          </div>
        </template>
      </div>

      <div class="content-panel">
      <div v-if="statusTab === 'booking'">
        <div v-if="areaTab === 'classroom'" class="card-grid">
          <div v-for="b in buildingCards" :key="b.id" class="classroom-item">
            <div class="classroom-header">
              <div class="classroom-name">{{ b.name }}</div>
              <div class="classroom-status">空闲</div>
            </div>
            <div class="classroom-desc">
              {{ b.address || '教学楼' }} ｜ 共 {{ b.floorCount || '-' }} 层
            </div>
            <div class="classroom-footer">
              <div class="classroom-capacity">普通教室 {{ b.classroomCount }} 间</div>
              <button class="btn btn-small" @click="goToBuildingClassrooms(b.id)">查看教室</button>
            </div>
          </div>
          <div v-if="!loading && buildingCards.length === 0" class="empty-text">
            暂无可预约普通教室
          </div>
        </div>

        <div v-else-if="areaTab === 'seminar'" class="card-grid">
          <div v-for="b in buildingCards" :key="b.id" class="classroom-item">
            <div class="classroom-header">
              <div class="classroom-name">{{ b.name }}</div>
              <div class="classroom-status">空闲</div>
            </div>
            <div class="classroom-desc">
              {{ b.address || '教学楼' }} ｜ 共 {{ b.floorCount || '-' }} 层
            </div>
            <div class="classroom-footer">
              <div class="classroom-capacity">研讨室 {{ b.classroomCount }} 间</div>
              <button class="btn btn-small" @click="goToBuildingClassrooms(b.id)">查看教室</button>
            </div>
          </div>
          <div v-if="!loading && buildingCards.length === 0" class="empty-text">
            暂无可预约研讨室
          </div>
        </div>

        <div v-else class="card-grid">
          <div
            v-for="lib in libraries"
            :key="lib.id"
            class="classroom-item"
          >
            <div class="classroom-header">
              <div class="classroom-name">{{ lib.name }}</div>
              <div class="classroom-status">空闲</div>
            </div>
            <div class="classroom-desc">
              {{ lib.address || '校园图书馆' }} ｜ 共 {{ lib.floorCount || 5 }} 层
            </div>
            <div class="classroom-footer">
              <div class="classroom-capacity">图书馆选座</div>
              <button
                class="btn btn-small"
                @click="openSeatModal(lib, 1)"
              >
                选座位
              </button>
            </div>
          </div>
          <div v-if="!loading && libraries.length === 0" class="empty-text">
            暂无图书馆数据
          </div>
        </div>
      </div>

      <div v-else-if="statusTab === 'pending'">
        <div v-if="pendingReservations.length" class="card-grid">
          <div
            v-for="item in pendingReservations"
            :key="item.id"
            class="booking-item"
          >
            <div class="booking-header">
              <div class="booking-info">
                <div class="booking-title">
                  {{
                    item.resourceName ||
                    (item.resourceType === 1 ? `教室预约 #${item.id}` : `图书馆预约 #${item.id}`)
                  }}
                </div>
                <div class="booking-subtitle">
                  {{ formatDisplayDate(item.reservationDate) }} {{ formatDisplayTimeRange(item.startTime, item.endTime) }}
                </div>
              </div>
              <div class="booking-status status-pending">待签到</div>
            </div>
            <div class="booking-detail">
              <div class="detail-row">
                <span class="detail-label">预约日期：</span>
                <span>{{ formatDisplayDate(item.reservationDate) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">预约时段：</span>
                <span>{{ formatDisplayTimeRange(item.startTime, item.endTime) }}</span>
              </div>
            </div>
            <div class="booking-actions">
              <button class="action-btn btn-secondary scan-btn" @click.stop="startScan(item)">
                签到
              </button>
              <button class="action-btn btn-danger cancel-btn" @click.stop="cancelReservation(item)">
                取消预约
              </button>
            </div>
          </div>
        </div>
        <div v-else class="empty-text">暂无待签到预约</div>
      </div>

      <div v-else-if="statusTab === 'checked'">
        <div v-if="checkedReservations.length" class="card-grid">
          <div
            v-for="item in checkedReservations"
            :key="item.id"
            class="booking-item"
          >
            <div class="booking-header">
              <div class="booking-info">
                <div class="booking-title">
                  {{
                    item.resourceName ||
                    (item.resourceType === 1 ? `教室预约 #${item.id}` : `图书馆预约 #${item.id}`)
                  }}
                </div>
                <div class="booking-subtitle">
                  {{ formatDisplayDate(item.reservationDate) }} {{ formatDisplayTimeRange(item.startTime, item.endTime) }}
                </div>
              </div>
              <div class="booking-status status-checked">已签到</div>
            </div>
            <div class="booking-detail">
              <div class="detail-row">
                <span class="detail-label">预约日期：</span>
                <span>{{ formatDisplayDate(item.reservationDate) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">预约时段：</span>
                <span>{{ formatDisplayTimeRange(item.startTime, item.endTime) }}</span>
              </div>
              <div class="detail-row" v-if="(item as any).checkinTime">
                <span class="detail-label">签到时间：</span>
                <span>{{ formatDisplayDateTime((item as any).checkinTime) }}</span>
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
        <div v-else class="empty-text">暂无已签到预约</div>
      </div>

      <div v-else>
        <div v-if="cancelledReservations.length" class="card-grid">
          <div
            v-for="item in cancelledReservations"
            :key="item.id"
            class="booking-item"
          >
            <div class="booking-header">
              <div class="booking-info">
                <div class="booking-title">
                  {{
                    item.resourceName ||
                    (item.resourceType === 1 ? `教室预约 #${item.id}` : `图书馆预约 #${item.id}`)
                  }}
                </div>
                <div class="booking-subtitle">
                  {{ formatDisplayDate(item.reservationDate) }} {{ formatDisplayTimeRange(item.startTime, item.endTime) }}
                </div>
              </div>
              <div class="booking-status status-cancelled">已取消</div>
            </div>
            <div class="booking-detail">
              <div class="detail-row">
                <span class="detail-label">预约日期：</span>
                <span>{{ formatDisplayDate(item.reservationDate) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">预约时段：</span>
                <span>{{ formatDisplayTimeRange(item.startTime, item.endTime) }}</span>
              </div>
            </div>
            <div class="booking-actions">
              <button class="action-btn btn-secondary">
                重新预约
              </button>
            </div>
          </div>
        </div>
        <div v-else class="empty-text">暂无已取消预约</div>
      </div>
      </div>
    </div>

    <div v-else class="not-login">
      请先登录后再进行预约和查看记录。
    </div>

    <van-popup
      v-model:show="seatModalShow"
      round
      position="bottom"
      :style="{ height: '70%' }"
    >
      <div class="seat-container">
        <div class="library-logo">
          <h3>图书馆选座系统</h3>
        </div>
        <div class="floor-selector">
          <div
            v-for="floor in [1, 2, 3, 4, 5]"
            :key="floor"
            class="floor-item"
            :class="{ active: seatFloor === floor }"
            @click="
              () => {
                seatFloor = floor
                loadSeats()
              }
            "
          >
            {{ floor }}层
          </div>
        </div>
        <div class="seat-grid">
          <div
            v-for="seat in seatList"
            :key="seat.id"
            class="seat-item"
            :class="{
              occupied: seat.status !== 1 || seat.realTimeStatus === 2,
              selected: selectedSeatId === seat.id,
              available: seat.status === 1 && seat.realTimeStatus !== 2
            }"
            @click="selectSeat(seat)"
          >
            {{ seat.seatLabel }}
          </div>
        </div>
        <button class="btn" :disabled="!selectedSeatId" @click="confirmSeatReservation">
          立即预约
        </button>
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
.reservation {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page {
  padding: 16px;
}

.page-title {
  font-size: 20px;
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 16px;
}

.filter-panel {
  position: sticky;
  top: 46px;
  z-index: 12;
  margin-bottom: 14px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(6px);
}

.content-panel {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #edf2f8;
}

.status-tag-bar,
.date-tag-bar,
.area-tag-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 12px;
}

.status-tag-item,
.date-tag-item,
.area-tag-item {
  padding: 8px 16px;
  border-radius: 16px;
  background-color: #f5f7fa;
  font-size: 14px;
  color: #334155;
  font-weight: 500;
  white-space: nowrap;
}

.status-tag-item.active,
.date-tag-item.active,
.area-tag-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.classroom-item {
  padding: 16px;
  border-radius: 12px;
  background-color: #ffffff;
  margin-bottom: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.state-card {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 8px 0;
}

.state-item {
  cursor: pointer;
}

.state-left {
  display: flex;
  align-items: center;
}

.state-info h3 {
  font-size: 16px;
  color: #1a1a1a;
  font-weight: 600;
}

.state-info p {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.state-tag {
  width: 48px;
  height: 24px;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #ffffff;
  margin-right: 12px;
}

.tag-free {
  background-color: #4a90e2;
}

.tag-booked {
  background-color: #f7d060;
  color: #1a1a1a;
}

.tag-maintaining {
  background-color: #f59e0b;
  color: #ffffff;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.classroom-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.classroom-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  background-color: #f0f9ff;
  color: #4a90e2;
}

.classroom-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.classroom-time {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.time-slot {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  background-color: #f5f7fa;
  color: #333;
}

.time-slot.available {
  background-color: #ecf5ff;
  color: #4a90e2;
}

.time-slot.occupied {
  background-color: #e5e7eb;
  color: #9ca3af;
}

.time-slot.selected {
  background-color: #4a90e2;
  color: #ffffff;
}

.classroom-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.classroom-capacity {
  font-size: 12px;
  color: #909399;
}

.btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  border: none;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  background-color: #4a90e2;
}

.btn-small {
  height: 32px;
  font-size: 14px;
  width: auto;
  padding: 0 16px;
}

.btn-yellow {
  background-color: #f7d060;
  color: #1a1a1a;
}

.card {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f7fa;
}

.list-item:last-child {
  border-bottom: none;
}

.list-left {
  display: flex;
  align-items: center;
}

.list-info h3 {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.list-info p {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.status-tag {
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 12px;
  color: #ffffff;
  margin-right: 8px;
}

.status-yellow {
  background-color: #f7d060;
  color: #1a1a1a;
}

.status-green {
  background-color: #67c23a;
}

.status-gray {
  background-color: #909399;
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

.status-checked {
  background-color: #f0f9ff;
  color: #4a90e2;
}

.status-cancelled {
  background-color: #f9f0f5;
  color: #9061f9;
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

.btn-danger {
  background-color: #f56c6c;
  color: #ffffff;
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

.empty-text {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.not-login {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.seat-container {
  padding: 16px;
}

.library-logo {
  text-align: center;
  margin-bottom: 12px;
}

.floor-selector {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 12px;
}

.floor-item {
  padding: 8px 16px;
  border-radius: 8px;
  background-color: #f5f7fa;
  font-size: 14px;
  white-space: nowrap;
}

.floor-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.seat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.seat-item {
  height: 40px;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  color: #4b5563;
  background-color: #e5e7eb;
}

.seat-item.available {
  background-color: #e5e7eb;
  color: #4b5563;
}

.seat-item.occupied {
  background-color: #4b5563;
  color: #f3f4f6;
  cursor: not-allowed;
}

.seat-item.selected {
  background-color: #4a90e2;
  color: #ffffff;
  border: 2px solid #2f6fb8;
}

@media (min-width: 1024px) {
  .reservation {
    background:
      radial-gradient(circle at 10% -12%, rgba(74, 144, 226, 0.1), transparent 40%),
      #f3f6fb;
  }

  .page {
    max-width: 1180px;
    margin: 0 auto;
    padding: 22px 24px 24px;
  }

  .filter-panel {
    top: 54px;
    margin-bottom: 16px;
    padding: 14px;
    border-radius: 16px;
  }

  .content-panel {
    padding: 16px;
    border-radius: 16px;
  }

  .page-title {
    font-size: 24px;
    margin-bottom: 18px;
    letter-spacing: 0.02em;
  }

  .status-tag-bar,
  .date-tag-bar,
  .area-tag-bar {
    gap: 10px;
    flex-wrap: wrap;
    overflow-x: visible;
  }

  .status-tag-item,
  .date-tag-item,
  .area-tag-item {
    border: 1px solid transparent;
    transition: all 0.2s ease;
  }

  .card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 14px;
    align-content: start;
  }

  .classroom-item,
  .booking-item {
    margin-bottom: 0;
    border: 1px solid #e8edf5;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .booking-title,
  .classroom-name {
    font-size: 17px;
  }

  .classroom-desc,
  .booking-subtitle {
    font-size: 13px;
  }

  .booking-actions {
    padding-top: 10px;
  }

  .action-btn {
    padding: 7px 14px;
    border-radius: 9px;
  }

  .scan-modal-inner {
    padding: 22px 20px 18px;
  }

  .scan-box {
    width: 264px;
    height: 264px;
  }

  .qr-canvas {
    width: 224px;
    height: 224px;
  }

  .seat-container {
    max-width: 860px;
    margin: 0 auto;
    padding: 20px;
  }

  .seat-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 10px;
  }
}

@media (min-width: 1024px) and (hover: hover) {
  .status-tag-item:hover,
  .date-tag-item:hover,
  .area-tag-item:hover {
    border-color: #cfe1f8;
    background-color: #eef5ff;
    color: #4a90e2;
  }

  .classroom-item:hover,
  .booking-item:hover {
    transform: translateY(-2px);
    border-color: #cfe1f8;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
  }
}
</style>
