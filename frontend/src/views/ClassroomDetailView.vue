<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getClassroomDetail, type ClassroomDetailVO } from '@/api/classroom'
import { getApprovedClassroomFeedback, type FeedbackItem } from '@/api/feedback'
import { getBuildingById, type Building } from '@/api/building'
import {
  getClassroomSlots,
  type ClassroomSlotStatus,
  createReservation,
  getReservationLimits,
  type ReservationPayload,
  type ReservationLimitVO,
} from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const detail = ref<ClassroomDetailVO | null>(null)
const classroom = computed(() => detail.value?.classroom ?? null)
const building = ref<Building | null>(null)
const loading = ref(false)
const approvedFeedbackList = ref<FeedbackItem[]>([])

const dateTab = ref<'today' | 'tomorrow' | 'dayAfter'>('today')
const slots = ref<ClassroomSlotStatus[]>([])
/** 已选时段 label，支持多选（须为时间上连续的一段） */
const selectedLabels = ref<string[]>([])
/** 选时段时的校验说明（页内提示，避免误触确认后才看到 Toast） */
const selectionHint = ref('')

const storedUser = ref<{ id: number } | null>(null)

const reservationLimits = ref<ReservationLimitVO | null>(null)

const formatDurationRule = (minutes: number) => {
  if (minutes % 60 === 0) return `${minutes / 60}小时`
  return `${minutes}分钟`
}

const ruleTipText = computed(() => {
  const L = reservationLimits.value
  if (!L) return '温馨提示：预约须遵守平台规则（加载规则失败时可稍后重试）'
  return `温馨提示：可勾选多个相邻时段合并为一次预约；合并后单次最长 ${formatDurationRule(L.maxDurationMinutes)}`
})

const loadReservationLimits = async () => {
  try {
    const res = await getReservationLimits()
    if (res.code === 200 && res.data) {
      const d = res.data
      reservationLimits.value = {
        maxPerWeek: Number(d.maxPerWeek ?? 4),
        maxDurationMinutes: Number(d.maxDurationMinutes ?? 240),
      }
    }
  } catch {
    reservationLimits.value = null
  }
}

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
const approvedReviewCount = computed(() => approvedFeedbackList.value.length)
const hasApprovedFeedback = computed(() => approvedFeedbackList.value.length > 0)
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
    if (detail.value?.classroom?.id) {
      await loadApprovedFeedback(detail.value.classroom.id)
    } else {
      approvedFeedbackList.value = []
    }
    if (detail.value?.classroom?.buildingId) {
      const bRes = await getBuildingById(detail.value.classroom.buildingId)
      building.value = (bRes as unknown as { data?: Building }).data ?? null
    }
    await loadSlots()
  } finally {
    loading.value = false
  }
}

const loadApprovedFeedback = async (classroomId: number) => {
  try {
    const res = await getApprovedClassroomFeedback(classroomId)
    approvedFeedbackList.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error(e)
    approvedFeedbackList.value = []
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
    selectedLabels.value = []
    selectionHint.value = ''
  } catch (e) {
    console.error(e)
    slots.value = []
    selectedLabels.value = []
    selectionHint.value = ''
  }
}

/** 后端可能返回字符串 / 数组等，统一成可解析的时间串 */
const normalizeTimeRaw = (raw: unknown): string => {
  if (raw == null) return ''
  if (typeof raw === 'string') return raw.trim()
  if (Array.isArray(raw) && raw.length >= 2) {
    const h = Number(raw[0]) || 0
    const m = Number(raw[1]) || 0
    const s = raw.length >= 3 ? Number(raw[2]) || 0 : 0
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(raw).trim()
}

/** 从 0 点起的秒数（含秒，避免 09:59:59 与 10:00:00 被误判为不连续） */
const timeToSeconds = (raw: unknown): number => {
  const t = normalizeTimeRaw(raw)
  if (!t) return 0
  const parts = t.split(':').map((x) => parseInt(x, 10))
  const h = Number.isFinite(parts[0]) ? parts[0]! : 0
  const m = Number.isFinite(parts[1]) ? parts[1]! : 0
  const s = Number.isFinite(parts[2]) ? parts[2]! : 0
  return h * 3600 + m * 60 + s
}

/** 预约时长（分钟，四舍五入，至少 1） */
const durationMinutesBetween = (startRaw: unknown, endRaw: unknown): number => {
  const sec = Math.max(0, timeToSeconds(endRaw) - timeToSeconds(startRaw))
  return Math.max(1, Math.round(sec / 60))
}

type MergedSelection =
  | { ok: false; reason: string }
  | { ok: true; startTime: string; endTime: string; duration: number }

const mergedSelection = computed((): MergedSelection => {
  const labelSet = new Set(selectedLabels.value.filter(Boolean))
  if (labelSet.size === 0) {
    return { ok: false, reason: '' }
  }

  const byLabel = new Map<string, ClassroomSlotStatus>()
  for (const s of slots.value) {
    if (labelSet.has(s.label) && s.status === 'available') {
      byLabel.set(s.label, s)
    }
  }
  if (byLabel.size !== labelSet.size) {
    return { ok: false, reason: '部分已选时段已不可约，请重新选择' }
  }

  const picked = Array.from(byLabel.values())
  const sorted = [...picked].sort(
    (a, b) => timeToSeconds(a.startTime) - timeToSeconds(b.startTime)
  )

  const GAP_TOLERANCE_SEC = 120

  for (let i = 1; i < sorted.length; i++) {
    const prev = sorted[i - 1]!
    const cur = sorted[i]!
    const gap = timeToSeconds(cur.startTime) - timeToSeconds(prev.endTime)
    if (gap < 0 || gap > GAP_TOLERANCE_SEC) {
      return { ok: false, reason: '请选择时间上连续相邻的时段（中间不能空档）' }
    }
  }

  const first = sorted[0]!
  const last = sorted[sorted.length - 1]!
  const startTime = normalizeTimeRaw(first.startTime)
  const endTime = normalizeTimeRaw(last.endTime)
  const duration = durationMinutesBetween(first.startTime, last.endTime)
  if (duration <= 0) {
    return { ok: false, reason: '时段时长无效' }
  }

  const maxMin = reservationLimits.value?.maxDurationMinutes
  const maxNum = Number(maxMin)
  if (Number.isFinite(maxNum) && maxNum > 0 && duration > maxNum) {
    return {
      ok: false,
      reason: `合并后时长 ${formatDurationRule(duration)}，超过单次上限 ${formatDurationRule(maxNum)}`,
    }
  }
  return { ok: true, startTime, endTime, duration }
})

const toggleTime = (timeSlot: ClassroomSlotStatus) => {
  if (timeSlot.status !== 'available') return
  const prev = [...selectedLabels.value]
  const i = prev.indexOf(timeSlot.label)
  if (i >= 0) {
    selectedLabels.value = prev.filter((_, j) => j !== i)
    selectionHint.value = ''
    return
  }
  const next = [...prev, timeSlot.label]
  selectedLabels.value = next
  const merged = mergedSelection.value
  if (merged.ok) {
    selectionHint.value = ''
    return
  }
  // 超过单次时长上限：撤销本次勾选，保留原选择
  if (merged.reason?.includes('超过单次上限')) {
    selectedLabels.value = prev
    selectionHint.value = merged.reason
    return
  }
  // 不连续等情况：以当前点击的时段作为新起点（常见预期是改选该格）
  selectedLabels.value = [timeSlot.label]
  const again = mergedSelection.value
  if (!again.ok) {
    selectedLabels.value = []
    selectionHint.value = again.reason || '当前时段无法预约'
  } else {
    selectionHint.value = ''
  }
}

const buildMergedReservationPayload = (
  startTime: string,
  endTime: string,
  duration: number
): ReservationPayload | null => {
  if (!storedUser.value?.id || !classroom.value?.id) {
    showToast('请先登录后再预约')
    return null
  }
  return {
    userId: storedUser.value.id,
    resourceType: 1,
    classroomId: classroom.value.id,
    reservationDate: getCurrentDate(),
    startTime,
    endTime,
    duration,
    purpose: '教室自习/研讨',
  }
}

const confirmReserve = async () => {
  selectionHint.value = ''
  if (selectedLabels.value.length === 0) {
    showToast('请先选择预约时段')
    return
  }
  const merged = mergedSelection.value
  if (!merged.ok) {
    selectionHint.value = merged.reason || '请选择有效的连续时段'
    return
  }
  const payload = buildMergedReservationPayload(
    merged.startTime,
    merged.endTime,
    merged.duration
  )
  if (!payload) return
  try {
    await createReservation(payload)
    showToast('预约成功')
    selectedLabels.value = []
    selectionHint.value = ''
    await loadSlots()
  } catch (e: unknown) {
    console.error(e)
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '预约失败，请稍后重试')
  }
}

const goBack = () => {
  router.back()
}

const clearSlotSelection = () => {
  selectedLabels.value = []
  selectionHint.value = ''
}

const goAllReviews = () => {
  if (!classroom.value?.id) return
  if (!hasApprovedFeedback.value) {
    showToast('暂无评价')
    return
  }
  router.push({
    name: 'classroom-reviews',
    params: { id: classroom.value.id },
  })
}

onMounted(() => {
  loadUserFromStorage()
  loadReservationLimits()
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
          <p class="score-line">
            <i class="icon iconfont icon-rexian" />
            <span class="score-text-main">
              综合评分：{{ averageScore != null ? averageScore.toFixed(1) : '暂无' }} 分（{{ approvedReviewCount }} 条评价）
            </span>
            <button
              type="button"
              class="all-reviews-link"
              :class="{ disabled: !hasApprovedFeedback }"
              @click.stop="goAllReviews"
            >
              查看全部评价
            </button>
          </p>
        </div>
        <div class="classroom-state">
          {{ slots.some((s) => s.status === 'available') ? '空闲' : '约满' }}
        </div>
      </div>

      <div class="card book-card">
        <div class="book-title">预约时段选择</div>
        <div class="book-sub">可勾选多个<strong>相邻</strong>时段，合并为一次预约，总时长不超过单次上限</div>

        <div class="date-picker">
          <div
            class="date-item"
            :class="{ active: dateTab === 'today' }"
            @click="
              () => {
                dateTab = 'today'
                clearSlotSelection()
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
                clearSlotSelection()
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
                clearSlotSelection()
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
            v-for="timeSlot in slots"
            :key="timeSlot.label"
            class="time-item"
            :class="{
              active: selectedLabels.includes(timeSlot.label),
              disabled: timeSlot.status === 'occupied'
            }"
            @click.stop="toggleTime(timeSlot)"
          >
            {{ timeSlot.label }}
          </div>
        </div>

        <div v-if="selectionHint" class="selection-hint">
          {{ selectionHint }}
        </div>

        <div class="tip-text">
          {{ ruleTipText }}
        </div>

        <button type="button" class="btn btn-yellow" @click.stop="confirmReserve">
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
  padding-bottom: 72px;
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

.score-line {
  flex-wrap: nowrap;
  white-space: nowrap;
}

.score-text-main {
  overflow: hidden;
  text-overflow: ellipsis;
}

.all-reviews-link {
  margin-left: 2px;
  border: none;
  background: transparent;
  padding: 0;
  color: #4a90e2;
  font-size: 14px;
  line-height: 1.2;
  cursor: pointer;
  text-decoration: none;
}

.all-reviews-link:hover,
.all-reviews-link:active {
  color: #3a7ed3;
  text-decoration: underline;
}

.all-reviews-link.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
  text-decoration: none;
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
  margin-bottom: 8px;
}

.book-sub {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 16px;
}

.book-sub strong {
  color: #4a90e2;
  font-weight: 600;
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

.selection-hint {
  font-size: 12px;
  color: #ee0a24;
  line-height: 1.5;
  margin: -8px 0 12px;
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

