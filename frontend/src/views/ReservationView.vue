<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import { getAvailableClassrooms, type Classroom } from '@/api/classroom'
import { getLibraryList, type Library } from '@/api/library'
import { createReservation, type ReservationPayload, getUserReservations, type Reservation } from '@/api/reservation'
import { getSeatsByLibraryAndFloor, type LibrarySeat } from '@/api/librarySeat'

const statusTab = ref<'booking' | 'pending' | 'checked' | 'cancelled'>('booking')
const dateTab = ref<'today' | 'tomorrow' | 'dayAfter'>('today')
const areaTab = ref<'classroom' | 'seminar' | 'library'>('classroom')

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

const storedUser = ref<{ id: number } | null>(null)

const today = new Date()
const tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000)
const dayAfter = new Date(today.getTime() + 2 * 24 * 60 * 60 * 1000)

const formatDate = (d: Date) => {
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${m}/${day}`
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

const loadClassroomsByType = async (type: number): Promise<Classroom[]> => {
  const res = await getAvailableClassrooms(type)
  const maybeData = (res as unknown as { data?: unknown }).data
  if (Array.isArray(maybeData)) return maybeData as Classroom[]
  if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
    return (maybeData as { data: Classroom[] }).data
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
    const [normal, seminar, libraryList] = await Promise.all([
      loadClassroomsByType(1),
      loadClassroomsByType(2),
      loadLibraries()
    ])
    normalClassrooms.value = normal
    seminarRooms.value = seminar
    libraries.value = libraryList
    await loadUserReservations()
  } catch (e) {
    console.error(e)
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
  const start = params.startTime
  const end = params.endTime
  const [sh, sm] = start.split(':').map(Number)
  const [eh, em] = end.split(':').map(Number)
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
  } catch (e) {
    console.error(e)
    showToast('预约失败，请稍后重试')
  }
}

const handleQuickReserveClassroom = async (classroom: Classroom) => {
  const payload = buildReservationPayload({
    resourceType: 1,
    classroomId: classroom.id!,
    startTime: '14:00',
    endTime: '16:00',
    purpose: '教室自习/研讨'
  })
  await submitReservation(payload)
}

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
    await createReservation({ ...(item as any), status: 4 })
    showToast('已取消预约')
    await loadUserReservations()
  } catch (e) {
    console.error(e)
    showToast('取消失败，请稍后重试')
  }
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="reservation">
    <van-nav-bar title="预约中心" />

    <div class="page">
      <div class="page-title">预约中心</div>

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

      <div v-if="statusTab === 'booking'">
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

        <div v-if="areaTab === 'classroom'">
          <div
            v-for="room in normalClassrooms"
            :key="room.id"
            class="classroom-item"
          >
            <div class="classroom-header">
              <div class="classroom-name">{{ room.name }}</div>
              <div class="classroom-status">空闲</div>
            </div>
            <div class="classroom-desc">
              {{ room.floor }}层 ｜ 容量 {{ room.capacity || '-' }} 人
            </div>
            <div class="classroom-footer">
              <div class="classroom-capacity">教室预约</div>
              <button class="btn btn-small" @click="handleQuickReserveClassroom(room)">
                立即预约
              </button>
            </div>
          </div>
          <div v-if="!loading && normalClassrooms.length === 0" class="empty-text">
            暂无可预约普通教室
          </div>
        </div>

        <div v-else-if="areaTab === 'seminar'">
          <div
            v-for="room in seminarRooms"
            :key="room.id"
            class="classroom-item"
          >
            <div class="classroom-header">
              <div class="classroom-name">{{ room.name }}</div>
              <div class="classroom-status">空闲</div>
            </div>
            <div class="classroom-desc">
              {{ room.floor }}层 ｜ 容量 {{ room.capacity || '-' }} 人
            </div>
            <div class="classroom-footer">
              <div class="classroom-capacity">研讨室预约</div>
              <button class="btn btn-small" @click="handleQuickReserveClassroom(room)">
                立即预约
              </button>
            </div>
          </div>
          <div v-if="!loading && seminarRooms.length === 0" class="empty-text">
            暂无可预约研讨室
          </div>
        </div>

        <div v-else>
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
        <div class="card" v-if="pendingReservations.length">
          <div
            v-for="item in pendingReservations"
            :key="item.id"
            class="list-item"
          >
            <div class="list-left">
              <div class="status-tag status-yellow">待签到</div>
              <div class="list-info">
                <h3>预约ID {{ item.id }}</h3>
                <p>{{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}</p>
              </div>
            </div>
            <div class="list-right">
              <button class="btn btn-small" @click="cancelReservation(item)">取消预约</button>
            </div>
          </div>
        </div>
        <div v-else class="empty-text">暂无待签到预约</div>
      </div>

      <div v-else-if="statusTab === 'checked'">
        <div class="card" v-if="checkedReservations.length">
          <div
            v-for="item in checkedReservations"
            :key="item.id"
            class="list-item"
          >
            <div class="list-left">
              <div class="status-tag status-green">已签到</div>
              <div class="list-info">
                <h3>预约ID {{ item.id }}</h3>
                <p>{{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}</p>
              </div>
            </div>
            <div class="list-right">
              <button class="btn btn-small btn-yellow">评价</button>
            </div>
          </div>
        </div>
        <div v-else class="empty-text">暂无已签到预约</div>
      </div>

      <div v-else>
        <div class="card" v-if="cancelledReservations.length">
          <div
            v-for="item in cancelledReservations"
            :key="item.id"
            class="list-item"
          >
            <div class="list-left">
              <div class="status-tag status-gray">已取消</div>
              <div class="list-info">
                <h3>预约ID {{ item.id }}</h3>
                <p>{{ item.reservationDate }} {{ item.startTime }}-{{ item.endTime }}</p>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-text">暂无已取消预约</div>
      </div>
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

.empty-text {
  margin-top: 24px;
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
  color: #ffffff;
}

.seat-item.available {
  background-color: #4a90e2;
}

.seat-item.occupied {
  background-color: #f56c6c;
}

.seat-item.selected {
  background-color: #67c23a;
  border: 2px solid #4caf50;
}
</style>
