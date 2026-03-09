<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getClassroomDetail, type ClassroomDetailVO } from '@/api/classroom'
import { getBuildingById, type Building } from '@/api/building'
import {
  getClassroomSlots,
  type ClassroomSlotStatus,
  createReservation,
  type ReservationPayload
} from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const detail = ref<ClassroomDetailVO | null>(null)
const classroom = computed(() => detail.value?.classroom ?? null)
const building = ref<Building | null>(null)
const loading = ref(false)

const dateTab = ref<'today' | 'tomorrow' | 'dayAfter'>('today')
const slots = ref<ClassroomSlotStatus[]>([])
const selectedLabel = ref<string | null>(null)

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

/** 综合评分：历史评分的平均值 */
const averageScore = computed(() => {
  const v = detail.value?.averageScore
  if (v == null) return null
  return typeof v === 'number' ? v : Number(v)
})
const totalReviews = computed(() => detail.value?.totalReviews ?? 0)
/** 热度星级 1-5 */
const popularityStars = computed(() => Math.min(5, Math.max(1, detail.value?.popularityStars ?? 0)))

const getCurrentDate = () => {
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

const loadUserFromStorage = () => {
  const raw = localStorage.getItem('currentUser')
  if (raw) {
    try {
      storedUser.value = JSON.parse(raw)
    } catch {
      storedUser.value = null
    }
  }
}

const loadClassroom = async () => {
  const idParam = route.params.id
  const id = Number(idParam)
  if (!id) {
    showToast('教室信息不存在')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await getClassroomDetail(id)
    const maybe = (res as unknown as { data?: ClassroomDetailVO }).data
    detail.value = maybe ?? null
    if (detail.value?.classroom?.buildingId) {
      const bRes = await getBuildingById(detail.value.classroom.buildingId)
      building.value = (bRes as unknown as { data?: Building }).data ?? null
    }
    await loadSlots()
  } finally {
    loading.value = false
  }
}

const loadSlots = async () => {
  if (!classroom.value?.id) return
  try {
    const res = await getClassroomSlots(classroom.value.id, getCurrentDate())
    const maybe = (res as unknown as { data?: unknown }).data
    if (Array.isArray(maybe)) {
      const list = maybe as ClassroomSlotStatus[]
      const map = new Map<string, ClassroomSlotStatus>()
      list.forEach((s) => {
        if (!map.has(s.label)) {
          map.set(s.label, s)
        }
      })
      slots.value = Array.from(map.values())
    } else if (maybe && Array.isArray((maybe as { data?: unknown }).data)) {
      const list = (maybe as { data: ClassroomSlotStatus[] }).data
      const map = new Map<string, ClassroomSlotStatus>()
      list.forEach((s) => {
        if (!map.has(s.label)) {
          map.set(s.label, s)
        }
      })
      slots.value = Array.from(map.values())
    } else {
      slots.value = []
    }
  } catch (e) {
    console.error(e)
    slots.value = []
  }
}

const toggleTime = (slot: ClassroomSlotStatus) => {
  if (slot.status !== 'available') return
  selectedLabel.value = selectedLabel.value === slot.label ? null : slot.label
}

const buildReservationPayload = (slot: ClassroomSlotStatus): ReservationPayload | null => {
  if (!storedUser.value?.id || !classroom.value?.id) {
    showToast('请先登录后再预约')
    return null
  }
  const start = slot.startTime ?? ''
  const end = slot.endTime ?? ''
  const [shStr = '0', smStr = '0'] = start.split(':')
  const [ehStr = '0', emStr = '0'] = end.split(':')
  const sh = Number(shStr)
  const sm = Number(smStr)
  const eh = Number(ehStr)
  const em = Number(emStr)
  const duration = (eh * 60 + em) - (sh * 60 + sm)
  return {
    userId: storedUser.value.id,
    resourceType: 1,
    classroomId: classroom.value.id,
    reservationDate: getCurrentDate(),
    startTime: start,
    endTime: end,
    duration,
    purpose: '教室自习/研讨'
  }
}

const confirmReserve = async () => {
  if (!selectedLabel.value) {
    showToast('请先选择预约时段')
    return
  }
  const slot = slots.value.find((s) => s.label === selectedLabel.value && s.status === 'available')
  if (!slot) {
    showToast('当前时段不可预约')
    return
  }
  const payload = buildReservationPayload(slot)
  if (!payload) return
  try {
    await createReservation(payload)
    showToast('预约成功')
    await loadSlots()
  } catch (e) {
    console.error(e)
    showToast('预约失败，请稍后重试')
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadUserFromStorage()
  loadClassroom()
})
</script>

<template>
  <div class="detail-page">
    <van-nav-bar title="教室详情" left-arrow @click-left="goBack" />

    <div class="page-inner" v-if="classroom">
      <div class="classroom-header">
        <div class="classroom-info">
          <h2>{{ classroom.name }}</h2>
          <p>
            <i class="icon iconfont icon-lou" />
            {{ building?.name || '教学楼' }}{{ classroom.floor ? `${classroom.floor}层` : '' }} |
            {{ classroom.type === 2 ? '研讨室' : '普通教室' }} |
            {{ classroom.capacity || '-' }}座
          </p>
          <p>
            <i class="icon iconfont icon-rexian" />
            热度：<span class="star-row">
              <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= popularityStars }">★</span>
            </span>
          </p>
          <p>
            <i class="icon iconfont icon-rexian" />
            综合评分：{{ averageScore != null ? averageScore.toFixed(1) : '暂无' }} 分（{{ totalReviews }} 条评价）
          </p>
        </div>
        <div class="classroom-state">
          {{ slots.some((s) => s.status === 'available') ? '空闲' : '约满' }}
        </div>
      </div>

      <div class="card book-card">
        <div class="book-title">预约时段选择</div>

        <div class="date-picker">
          <div
            class="date-item"
            :class="{ active: dateTab === 'today' }"
            @click="
              () => {
                dateTab = 'today'
                selectedLabel = null
                loadSlots()
              }
            "
          >
            <span>今日</span>
            <span>{{ todayText }}</span>
          </div>
          <div
            class="date-item"
            :class="{ active: dateTab === 'tomorrow' }"
            @click="
              () => {
                dateTab = 'tomorrow'
                selectedLabel = null
                loadSlots()
              }
            "
          >
            <span>明日</span>
            <span>{{ tomorrowText }}</span>
          </div>
          <div
            class="date-item"
            :class="{ active: dateTab === 'dayAfter' }"
            @click="
              () => {
                dateTab = 'dayAfter'
                selectedLabel = null
                loadSlots()
              }
            "
          >
            <span>后天</span>
            <span>{{ dayAfterText }}</span>
          </div>
        </div>

        <div class="time-picker">
          <div
            v-for="slot in slots"
            :key="slot.label"
            class="time-item"
            :class="{
              active: selectedLabel === slot.label,
              disabled: slot.status === 'occupied'
            }"
            @click="toggleTime(slot)"
          >
            {{ slot.label }}
          </div>
        </div>

        <div class="tip-text">
          温馨提示：每人每周最多预约4次，单次最长4小时
        </div>

        <button class="btn btn-yellow" @click="confirmReserve">
          确认预约
        </button>
      </div>
    </div>

    <div v-else class="empty-text">
      正在加载教室信息...
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-inner {
  padding: 16px;
}

.card {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 16px;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.classroom-info h2 {
  font-size: 20px;
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 8px;
}

.classroom-info p {
  font-size: 14px;
  color: #909399;
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.classroom-info p .icon {
  font-size: 14px;
  margin-right: 4px;
}

.star-row {
  display: inline-flex;
  gap: 2px;
}
.star-row .star {
  color: #e0e0e0;
  font-size: 16px;
}
.star-row .star.filled {
  color: #f7d060;
}

.classroom-state {
  width: 80px;
  height: 32px;
  border-radius: 16px;
  background-color: #4a90e2;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  justify-content: center;
  align-items: center;
}

.book-card .book-title {
  font-size: 16px;
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 16px;
}

.date-picker {
  display: flex;
  overflow-x: auto;
  gap: 8px;
  margin-bottom: 24px;
  padding-bottom: 8px;
}

.date-item {
  min-width: 72px;
  height: 72px;
  border-radius: 8px;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.date-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.date-item span:first-child {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.time-picker {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.time-item {
  height: 36px;
  border-radius: 8px;
  background-color: #f5f7fa;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  color: #333333;
}

.time-item.disabled {
  background-color: #e5e7eb;
  color: #9ca3af;
}

.time-item.active {
  background-color: #4a90e2;
  color: #ffffff;
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

.btn-yellow {
  background-color: #f7d060;
  color: #1a1a1a;
}

.tip-text {
  font-size: 12px;
  color: #909399;
  margin-bottom: 16px;
}

.empty-text {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}
</style>

