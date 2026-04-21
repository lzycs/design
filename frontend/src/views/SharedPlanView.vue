<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getUserTeams, type TeamRequestVO } from '@/api/team'
import { getAvailableClassrooms, type Classroom } from '@/api/classroom'
import { getBuildingList, type Building } from '@/api/building'
import {
  getSharedPlans,
  createStudyPlanWithReservation,
  updateStudyPlan,
  type StudyPlanVO,
  type CreateStudyPlanPayload
} from '@/api/studyPlan'

const router = useRouter()
const storedUser = ref<{ id: number } | null>(null)
const plans = ref<StudyPlanVO[]>([])
const teams = ref<TeamRequestVO[]>([])
const seminarRooms = ref<Classroom[]>([])
const buildings = ref<Building[]>([])
const loading = ref(false)
const filterTab = ref<'all' | 'ongoing' | 'completed' | 'expired' | 'mine'>('all')
const searchKeyword = ref('')
const showCreateModal = ref(false)
const showBindRoomModal = ref(false)
const editingPlan = ref<StudyPlanVO | null>(null)

const planDate = ref('')
const planStartTime = ref('')
const planEndTime = ref('')
const teamRequestId = ref<number | null>(null)
const title = ref('')
const description = ref('')
const classroomId = ref<number | null>(null)
const keyTimeNodes = ref('')
const resDateTab = ref<'today' | 'tomorrow' | 'dayAfter'>('today')
const resStartTime = ref('')
const resEndTime = ref('')
const submitting = ref(false)

// 关联研讨室：关键时间节点选择（已有 + 新增）
type KeyNode = { title: string; datetime: string }
const existingNodes = ref<KeyNode[]>([])
const selectedExistingIdx = ref<number[]>([])
const newNodes = ref<Array<KeyNode>>([])

const today = new Date()
const tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000)
const dayAfter = new Date(today.getTime() + 2 * 24 * 60 * 60 * 1000)
const formatDateShort = (d: Date) =>
  `${`${d.getMonth() + 1}`.padStart(2, '0')}/${`${d.getDate()}`.padStart(2, '0')}`
const todayText = computed(() => formatDateShort(today))
const tomorrowText = computed(() => formatDateShort(tomorrow))
const dayAfterText = computed(() => formatDateShort(dayAfter))

const TIME_SLOTS = [
  { label: '8:00-10:00', start: '08:00', end: '10:00' },
  { label: '10:00-12:00', start: '10:00', end: '12:00' },
  { label: '12:00-14:00', start: '12:00', end: '14:00' },
  { label: '14:00-16:00', start: '14:00', end: '16:00' },
  { label: '16:00-18:00', start: '16:00', end: '18:00' }
]

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

const loadPlans = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await getSharedPlans(storedUser.value.id)
    plans.value = res.data ?? []
  } catch (e) {
    console.error(e)
    plans.value = []
  } finally {
    loading.value = false
  }
}

const loadTeams = async () => {
  if (!storedUser.value?.id) return
  try {
    const res = await getUserTeams(storedUser.value.id)
    teams.value = res.data ?? []
  } catch (e) {
    console.error(e)
    teams.value = []
  }
}

const loadSeminarRooms = async () => {
  try {
    const [roomRes, buildingRes] = await Promise.all([
      getAvailableClassrooms(2),
      getBuildingList(),
    ])
    const roomData = (roomRes as { data?: Classroom[] }).data
    seminarRooms.value = Array.isArray(roomData) ? roomData : []
    const buildingData = (buildingRes as { data?: Building[] }).data
    buildings.value = Array.isArray(buildingData) ? buildingData : []
  } catch (e) {
    console.error(e)
    seminarRooms.value = []
    buildings.value = []
  }
}

const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${`${d.getMonth() + 1}`.padStart(2, '0')}-${`${d.getDate()}`.padStart(2, '0')}`
}

const isLoggedIn = computed(() => !!storedUser.value?.id)

/** 当前用户是哪些小组的组长（组长=team_request.user_id） */
const leaderTeamIdSet = computed(() => {
  const set = new Set<number>()
  const uid = storedUser.value?.id
  if (!uid) return set
  teams.value.forEach((t) => {
    if (t.id != null && t.userId === uid) set.add(t.id)
  })
  return set
})

/** 仅组长可创建学习计划：所属小组下拉只显示组长小组 */
const leaderTeams = computed(() => {
  const uid = storedUser.value?.id
  if (!uid) return []
  return teams.value.filter((t) => t.userId === uid && t.id != null)
})

const canCreatePlan = computed(() => leaderTeams.value.length > 0)

const canBindRoom = (p: StudyPlanVO) => {
  const id = p.teamRequestId
  if (id == null) return false
  return leaderTeamIdSet.value.has(id)
}

const filteredPlans = computed(() => {
  let list = plans.value
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(
      (p) =>
        (p.title ?? '').toLowerCase().includes(kw) ||
        (p.teamTitle ?? '').toLowerCase().includes(kw) ||
        (p.classroomName ?? '').toLowerCase().includes(kw)
    )
  }
  const today = todayStr()
  if (filterTab.value === 'ongoing') {
    list = list.filter((p) => p.status === 1 && (p.planDate ?? '') >= today)
  } else if (filterTab.value === 'completed') {
    list = list.filter((p) => p.status === 2)
  } else if (filterTab.value === 'expired') {
    list = list.filter((p) => p.status === 1 && (p.planDate ?? '') < today)
  } else if (filterTab.value === 'mine') {
    list = list.filter((p) => p.userId === storedUser.value?.id)
  }
  return list
})

const statusTag = (p: StudyPlanVO) => {
  const today = todayStr()
  if (p.status === 2) return { text: '已完成', cls: 'tag-completed' }
  if ((p.planDate ?? '') < today) return { text: '已过期', cls: 'tag-expired' }
  return { text: '进行中', cls: 'tag-active' }
}

/** 时间：去掉秒，10:00:00 -> 10:00 */
const formatTime = (t?: string) => {
  if (!t) return ''
  if (t.length >= 8) return t.substring(0, 5)
  return t
}

/** 日期：去掉年份，2026-03-15 -> 03-15 */
const formatDateNoYear = (d?: string) => {
  if (!d) return ''
  const m = d.match(/^\d{4}-(\d{2}-\d{2})/)
  return m ? m[1] : d
}

/** 日期时间：去掉年份和秒，2026-03-15 18:00:00 -> 03-15 18:00 */
const formatDateTimeNoYear = (s?: string) => {
  if (!s) return ''
  const parts = s.split(/\s+/)
  const datePart = formatDateNoYear(parts[0])
  const timePart = parts[1] ? formatTime(parts[1]) : ''
  return timePart ? `${datePart} ${timePart}` : datePart
}

/** 解析关键时间节点：格式 "节点名|日期时间,节点名|日期时间" */
const parseKeyTimeNodes = (raw?: string): { title: string; datetime: string }[] => {
  if (!raw || !raw.trim()) return []
  return raw.split(',').map((s) => {
    const [title = '', datetime = ''] = s.split('|').map((x) => x.trim())
    return { title, datetime: formatDateTimeNoYear(datetime) }
  }).filter((n) => n.title || n.datetime)
}

/** 研讨室标签文案：研讨室名 (日期 时段)，日期去年份、时间去秒 */
const meetingRoomLabel = (p: StudyPlanVO) => {
  const roomText = [p.buildingName, p.classroomName].filter(Boolean).join(' - ')
  if (!roomText) return ''
  if (p.reservationDate && (p.reservationStartTime || p.reservationEndTime)) {
    return `${roomText} (${formatDateNoYear(p.reservationDate)} ${formatTime(p.reservationStartTime)}-${formatTime(p.reservationEndTime)})`
  }
  return roomText
}

/** 计划日期范围展示：日期去年份、时间去秒 */
const planDateRange = (p: StudyPlanVO) => {
  const d = formatDateNoYear(p.planDate ?? '')
  const st = formatTime(p.startTime)
  const et = formatTime(p.endTime)
  if (st && et) return `${d} ${st}-${et}`
  return d || (p.planDate ?? '')
}

const getReservationDateStr = () => {
  const d = resDateTab.value === 'today' ? today : resDateTab.value === 'tomorrow' ? tomorrow : dayAfter
  return `${d.getFullYear()}-${`${d.getMonth() + 1}`.padStart(2, '0')}-${`${d.getDate()}`.padStart(2, '0')}`
}

const openCreate = () => {
  planDate.value = todayStr()
  teamRequestId.value = leaderTeams.value[0]?.id ?? null
  title.value = ''
  description.value = ''
  keyTimeNodes.value = ''
  classroomId.value = null
  resDateTab.value = 'today'
  resStartTime.value = ''
  resEndTime.value = ''
  showCreateModal.value = true
}

const openBindRoom = (p: StudyPlanVO) => {
  editingPlan.value = p
  planDate.value = p.planDate ?? todayStr()
  description.value = p.description ?? ''
  // 关联研讨室页：可选择已有节点 + 新增节点
  existingNodes.value = parseKeyTimeNodes(p.keyTimeNodes).map((n) => ({
    title: n.title,
    datetime: n.datetime,
  }))
  selectedExistingIdx.value = existingNodes.value.map((_, i) => i) // 默认全选已有
  newNodes.value = [{ title: '', datetime: '' }]
  keyTimeNodes.value = ''
  classroomId.value = null
  resDateTab.value = 'today'
  resStartTime.value = ''
  resEndTime.value = ''
  showBindRoomModal.value = true
}

const addNewNodeRow = () => {
  newNodes.value.push({ title: '', datetime: '' })
}

const toggleExistingNode = (idx: number, checked: boolean) => {
  const arr = selectedExistingIdx.value
  if (checked) {
    if (!arr.includes(idx)) arr.push(idx)
  } else {
    selectedExistingIdx.value = arr.filter((i) => i !== idx)
  }
}

const removeNewNodeRow = (idx: number) => {
  if (newNodes.value.length <= 1) {
    newNodes.value = [{ title: '', datetime: '' }]
    return
  }
  newNodes.value.splice(idx, 1)
}

const buildMergedKeyTimeNodes = () => {
  const selected = selectedExistingIdx.value
    .map((i) => existingNodes.value[i])
    .filter((n) => n && (n.title || n.datetime))
  const added = newNodes.value
    .map((n) => ({ title: n.title.trim(), datetime: n.datetime.trim() }))
    .filter((n) => n.title) // 新增：至少要填标题
  const merged = [...selected, ...added]
  // 格式：节点名|日期时间,节点名|日期时间（日期时间可为空）
  return merged
    .map((n) => `${n.title}|${n.datetime ?? ''}`)
    .join(',')
    .trim()
}

const seminarRoomOptionLabel = (room: Classroom) => {
  const buildingName = buildings.value.find((item) => item.id === room.buildingId)?.name
  return `${buildingName ? `${buildingName} - ` : ''}${room.name} (容纳${room.capacity}人)`
}

const selectResTime = (slot: { start: string; end: string }) => {
  resStartTime.value = slot.start
  resEndTime.value = slot.end
}

const submitCreate = async () => {
  if (!teamRequestId.value || !title.value.trim()) {
    showToast('请选择所属小组并填写计划名称')
    return
  }
  if (!storedUser.value?.id) {
    showToast('请先登录')
    return
  }
  const resDate = getReservationDateStr()
  const hasRes = classroomId.value && resStartTime.value && resEndTime.value
  if (classroomId.value && !hasRes) {
    showToast('选择研讨室后请选择预约时段')
    return
  }
  submitting.value = true
  try {
    const payload: CreateStudyPlanPayload = {
      teamRequestId: teamRequestId.value,
      userId: storedUser.value.id,
      title: title.value.trim(),
      description: description.value.trim() || undefined,
      planDate: planDate.value || undefined,
      keyTimeNodes: keyTimeNodes.value.trim() || undefined
    }
    if (hasRes) {
      payload.classroomId = classroomId.value!
      payload.reservationDate = resDate
      payload.resStartTime = resStartTime.value.length >= 8 ? resStartTime.value : resStartTime.value + ':00'
      payload.resEndTime = resEndTime.value.length >= 8 ? resEndTime.value : resEndTime.value + ':00'
    }
    await createStudyPlanWithReservation(payload)
    showToast('创建成功')
    showCreateModal.value = false
    await loadPlans()
  } catch (e: unknown) {
    console.error(e)
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const submitBindRoom = async () => {
  if (!editingPlan.value?.id || !storedUser.value?.id) return
  const mergedKeyNodes = buildMergedKeyTimeNodes()
  if (!mergedKeyNodes) {
    showToast('请填写关键时间节点')
    return
  }
  const resDate = getReservationDateStr()
  const hasRes = classroomId.value && resStartTime.value && resEndTime.value
  if (classroomId.value && !hasRes) {
    showToast('选择研讨室后请选择预约时段')
    return
  }
  submitting.value = true
  try {
    const payload: CreateStudyPlanPayload = {
      userId: storedUser.value.id,
      description: description.value.trim() || undefined,
      planDate: planDate.value || undefined,
      keyTimeNodes: mergedKeyNodes || undefined
    }
    if (hasRes) {
      payload.classroomId = classroomId.value!
      payload.reservationDate = resDate
      payload.resStartTime = resStartTime.value.length >= 8 ? resStartTime.value : resStartTime.value + ':00'
      payload.resEndTime = resEndTime.value.length >= 8 ? resEndTime.value : resEndTime.value + ':00'
    }
    await updateStudyPlan(editingPlan.value.id, payload)
    showToast('保存成功')
    showBindRoomModal.value = false
    editingPlan.value = null
    await loadPlans()
  } catch (e: unknown) {
    console.error(e)
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loadUserFromStorage()
  if (storedUser.value?.id) {
    await Promise.all([loadPlans(), loadTeams(), loadSeminarRooms()])
  }
})
</script>

<template>
  <div class="shared-plan">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-back" @click="router.back()">
        <van-icon name="arrow-left" />
      </div>
      <div class="page-header-title">学习计划</div>
      <div class="header-placeholder"></div>
    </div>

    <template v-if="isLoggedIn">
      <!-- 搜索栏：input + 圆形搜索按钮 -->
      <div class="search-bar">
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索小组/学习计划/研讨室"
        />
        <button class="search-btn" type="button">
          <van-icon name="search" />
        </button>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <button
          v-for="item in [
            { key: 'all', text: '全部计划' },
            { key: 'ongoing', text: '进行中' },
            { key: 'completed', text: '已完成' },
            { key: 'expired', text: '已过期' },
            { key: 'mine', text: '我的创建' }
          ]"
          :key="item.key"
          type="button"
          class="filter-item"
          :class="{ active: filterTab === item.key }"
          @click="filterTab = item.key as typeof filterTab"
        >
          {{ item.text }}
        </button>
      </div>

      <!-- 内容区：学习计划列表（按图片样式，每卡片独立，显示关键时间节点） -->
      <div class="content-area">
        <div class="section-header">
          <span class="section-title">学习计划</span>
          <span v-if="canCreatePlan" class="create-link" @click="openCreate">创建计划</span>
        </div>

        <van-loading v-if="loading" class="loading-wrap" type="spinner" />
        <van-empty v-else-if="filteredPlans.length === 0" description="暂无学习计划" />
        <div v-else class="plan-cards">
          <div
            v-for="p in filteredPlans"
            :key="p.id"
            class="plan-card"
          >
            <div class="plan-card-inner">
              <div class="plan-content">
                <div class="plan-header">
                  <span class="plan-title">{{ p.title }}</span>
                  <span class="list-tag" :class="statusTag(p).cls">{{ statusTag(p).text }}</span>
                </div>
                <div class="plan-meta">
                  {{ p.teamTitle || '小组' }} | {{ planDateRange(p) }}
                </div>
                <div v-if="p.description" class="plan-desc">{{ p.description }}</div>
                <!-- 关键时间节点：每个节点显示，研讨室时间放在节点后面 -->
                <div v-if="parseKeyTimeNodes(p.keyTimeNodes).length > 0" class="time-nodes">
                  <div class="time-nodes-title">关键时间节点：</div>
                  <div
                    v-for="(node, idx) in parseKeyTimeNodes(p.keyTimeNodes)"
                    :key="idx"
                    class="time-node-item"
                  >
                    <div class="time-node-dot"></div>
                    <div class="time-node-content">
                      <div class="time-node-title">{{ node.title }}</div>
                      <div v-if="meetingRoomLabel(p)" class="time-node-time">
                        <span class="meeting-room-tag">{{ meetingRoomLabel(p) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="action-group">
                  <button
                    v-if="(statusTag(p).text === '进行中' || statusTag(p).text === '已过期') && canBindRoom(p)"
                    type="button"
                    class="action-btn success-btn"
                    @click="openBindRoom(p)"
                  >
                    关联研讨室
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <button v-if="canCreatePlan" type="button" class="add-btn" @click="openCreate">+</button>
    </template>

    <van-empty v-else description="请先登录后查看共享学习计划" />

    <!-- 创建计划弹层：样式匹配 UI 设计，预约时间用日期+时段选项 -->
    <van-popup
      v-model:show="showCreateModal"
      position="bottom"
      round
      :style="{ height: '90%' }"
      get-container="body"
    >
      <div class="create-form">
        <div class="create-page-header">
          <div class="header-back" @click="showCreateModal = false">
            <van-icon name="arrow-left" />
          </div>
          <div class="page-header-title">创建学习计划</div>
          <div class="header-placeholder"></div>
        </div>
        <div class="form-scroll">
          <div class="admin-card form-card">
            <label class="form-label">所属小组</label>
            <select v-model="teamRequestId" class="form-select">
              <option :value="null">请选择所属小组</option>
              <option v-for="t in leaderTeams" :key="t.id" :value="t.id">{{ t.title }}</option>
            </select>

            <label class="form-label">计划名称</label>
            <input v-model="title" class="form-input" placeholder="例如：Java项目实战计划、高数真题刷题计划" />

            <label class="form-label">计划描述</label>
            <input v-model="description" class="form-input" placeholder="详细描述学习计划的目标、内容和要求" />

            <label class="form-label">计划日期</label>
            <input v-model="planDate" type="date" class="form-date" />

            <label class="form-label">关键时间节点（选填）</label>
            <input
              v-model="keyTimeNodes"
              class="form-input"
              placeholder="例如：需求分析完成|2026-03-15 18:00,核心功能开发|2026-03-25 18:00"
            />

            <label class="form-label">关联研讨室（选填）</label>
            <select v-model="classroomId" class="form-select">
              <option :value="null">暂不关联</option>
              <option v-for="r in seminarRooms" :key="r.id" :value="r.id">
                {{ seminarRoomOptionLabel(r) }}
              </option>
            </select>

            <!-- 研讨室预约：日期选项（今天/明天/后天）+ 时段选项，类似预约页，样式匹配创建计划 -->
            <template v-if="classroomId">
              <label class="form-label">研讨室预约日期</label>
              <div class="date-tag-bar">
                <div
                  class="date-tag-item"
                  :class="{ active: resDateTab === 'today' }"
                  @click="resDateTab = 'today'"
                >
                  <span>今天</span>
                  <span>{{ todayText }}</span>
                </div>
                <div
                  class="date-tag-item"
                  :class="{ active: resDateTab === 'tomorrow' }"
                  @click="resDateTab = 'tomorrow'"
                >
                  <span>明天</span>
                  <span>{{ tomorrowText }}</span>
                </div>
                <div
                  class="date-tag-item"
                  :class="{ active: resDateTab === 'dayAfter' }"
                  @click="resDateTab = 'dayAfter'"
                >
                  <span>后天</span>
                  <span>{{ dayAfterText }}</span>
                </div>
              </div>

              <label class="form-label">研讨室预约时段</label>
              <div class="time-slot-wrap">
                <div
                  v-for="slot in TIME_SLOTS"
                  :key="'res-' + slot.label"
                  class="time-pill"
                  :class="{ active: resStartTime === slot.start }"
                  @click="selectResTime(slot)"
                >
                  {{ slot.label }}
                </div>
              </div>
            </template>

            <button type="button" class="submit-btn" :disabled="submitting" @click="submitCreate">
              {{ submitting ? '提交中...' : '创建学习计划' }}
            </button>
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 关联研讨室弹层：所属小组、计划名称固定，可填关键时间节点与其余内容 -->
    <van-popup
      v-model:show="showBindRoomModal"
      position="bottom"
      round
      :style="{ height: '90%' }"
      get-container="body"
    >
      <div class="create-form">
        <div class="create-page-header">
          <div class="header-back" @click="showBindRoomModal = false">
            <van-icon name="arrow-left" />
          </div>
          <div class="page-header-title">关联研讨室</div>
          <div class="header-placeholder"></div>
        </div>
        <div class="form-scroll">
          <div class="admin-card form-card">
            <label class="form-label">所属小组</label>
            <input
              :value="editingPlan?.teamTitle"
              class="form-input form-readonly"
              readonly
              disabled
            />

            <label class="form-label">计划名称</label>
            <input
              :value="editingPlan?.title"
              class="form-input form-readonly"
              readonly
              disabled
            />

            <label class="form-label">计划描述</label>
            <input v-model="description" class="form-input" placeholder="详细描述学习计划的目标、内容和要求" />

            <label class="form-label">计划日期</label>
            <input v-model="planDate" type="date" class="form-date" />

            <label class="form-label">关键时间节点</label>

            <div v-if="existingNodes.length > 0" class="node-picker">
              <div class="node-picker-title">已有节点（可多选）</div>
              <label
                v-for="(n, idx) in existingNodes"
                :key="'ex-' + idx"
                class="node-check"
              >
                <input
                  type="checkbox"
                  :checked="selectedExistingIdx.includes(idx)"
                  @change="(e) => toggleExistingNode(idx, (e.target as HTMLInputElement).checked)"
                />
                <span class="node-check-text">{{ n.title }}</span>
              </label>
            </div>

            <div class="node-picker">
              <div class="node-picker-title">新增节点</div>
              <div v-for="(n, idx) in newNodes" :key="'nw-' + idx" class="node-new-row">
                <input
                  v-model="n.title"
                  class="form-input node-new-input"
                  placeholder="节点名称（必填）"
                />
                <input
                  v-model="n.datetime"
                  class="form-input node-new-input"
                  placeholder="节点时间（可选，如 2026-03-15 18:00）"
                />
                <button type="button" class="node-del-btn" @click="removeNewNodeRow(idx)">删除</button>
              </div>
              <button type="button" class="node-add-btn" @click="addNewNodeRow">+ 新增节点</button>
            </div>

            <label class="form-label">关联研讨室（选填）</label>
            <select v-model="classroomId" class="form-select">
              <option :value="null">暂不关联</option>
              <option v-for="r in seminarRooms" :key="r.id" :value="r.id">
                {{ seminarRoomOptionLabel(r) }}
              </option>
            </select>

            <template v-if="classroomId">
              <label class="form-label">研讨室预约日期</label>
              <div class="date-tag-bar">
                <div
                  class="date-tag-item"
                  :class="{ active: resDateTab === 'today' }"
                  @click="resDateTab = 'today'"
                >
                  <span>今天</span>
                  <span>{{ todayText }}</span>
                </div>
                <div
                  class="date-tag-item"
                  :class="{ active: resDateTab === 'tomorrow' }"
                  @click="resDateTab = 'tomorrow'"
                >
                  <span>明天</span>
                  <span>{{ tomorrowText }}</span>
                </div>
                <div
                  class="date-tag-item"
                  :class="{ active: resDateTab === 'dayAfter' }"
                  @click="resDateTab = 'dayAfter'"
                >
                  <span>后天</span>
                  <span>{{ dayAfterText }}</span>
                </div>
              </div>

              <label class="form-label">研讨室预约时段</label>
              <div class="time-slot-wrap">
                <div
                  v-for="slot in TIME_SLOTS"
                  :key="'bind-res-' + slot.label"
                  class="time-pill"
                  :class="{ active: resStartTime === slot.start }"
                  @click="selectResTime(slot)"
                >
                  {{ slot.label }}
                </div>
              </div>
            </template>

            <button type="button" class="submit-btn" :disabled="submitting" @click="submitBindRoom">
              {{ submitting ? '保存中...' : '确认关联' }}
            </button>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.shared-plan {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 80px;
}

/* 页面头部：与 UI 设计一致 */
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

.header-back {
  width: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-placeholder {
  width: 24px;
}

/* 搜索栏 */
.search-bar {
  padding: 12px 20px;
  background-color: #fff;
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
}

.search-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #4a90e2;
  display: flex;
  justify-content: center;
  align-items: center;
  border: none;
  color: #fff;
  cursor: pointer;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 8px;
  padding: 8px 20px;
  background-color: #fff;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid #f5f7fa;
}

.filter-item {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  background-color: #f5f7fa;
  color: #666;
  border: none;
  cursor: pointer;
}

.filter-item.active {
  background-color: #4a90e2;
  color: #fff;
}

.content-area {
  padding: 12px 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.create-link {
  font-size: 12px;
  color: #4a90e2;
  cursor: pointer;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 40px;
}

/* 学习计划卡片：按图片样式，每卡片独立，显示关键时间节点 */
.plan-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.plan-card-inner {
  display: flex;
  padding: 16px;
}

.plan-content {
  flex: 1;
  min-width: 0;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.plan-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-tag {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  flex-shrink: 0;
}

.tag-active {
  background-color: #e8f4ff;
  color: #4a90e2;
}

.tag-completed {
  background-color: #e8f5e9;
  color: #4caf50;
}

.tag-expired {
  background-color: #fff0f0;
  color: #f56c6c;
}

.plan-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.plan-meeting {
  margin-bottom: 4px;
}

.meeting-room-tag {
  display: inline-block;
  padding: 2px 8px;
  background-color: #e8f4ff;
  color: #4a90e2;
  border-radius: 4px;
  font-size: 11px;
  margin-left: 6px;
  margin-right: 4px;
  margin-top: 4px;
}

.plan-desc {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
}

/* 关键时间节点：时间轴样式 */
.time-nodes {
  margin-top: 8px;
  margin-bottom: 8px;
}

.time-nodes-title {
  font-size: 12px;
  color: #333;
  margin-bottom: 4px;
}

.time-node-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;
  border-bottom: 1px dashed #e5e6eb;
}

.time-node-item:last-child {
  border-bottom: none;
}

.time-node-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #4a90e2;
  margin-right: 8px;
  margin-top: 5px;
  flex-shrink: 0;
}

.time-node-content {
  flex: 1;
  min-width: 0;
}

.time-node-title {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 500;
}

.time-node-time {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* 操作按钮 */
.action-group {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.action-btn {
  padding: 6px 14px;
  border-radius: 6px;
  border: none;
  font-size: 12px;
  cursor: pointer;
}

.primary-btn {
  background-color: #4a90e2;
  color: #fff;
}

.success-btn {
  background-color: #4caf50;
  color: #fff;
}

/* 创建计划弹层内复用 */
.admin-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.add-btn {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: #4a90e2;
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
  border: none;
  font-size: 24px;
  cursor: pointer;
  z-index: 9;
}

/* 创建计划弹层 */
.create-form {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.create-page-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #4a90e2;
  color: #fff;
}

.create-page-header .header-back {
  color: #fff;
}

.form-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
}

.form-card {
  padding: 20px;
}

.form-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.form-input,
.form-select,
.form-date {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 16px;
  outline: none;
  background-color: #fff;
}

.form-input:focus,
.form-select:focus,
.form-date:focus {
  border-color: #4a90e2;
}

.form-select {
  cursor: pointer;
}

.form-readonly {
  background-color: #f5f7fa;
  color: #909399;
  cursor: not-allowed;
}

/* 日期选项：今天/明天/后天，类似预约页 */
.date-tag-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.date-tag-item {
  flex: 1;
  padding: 10px 8px;
  border-radius: 8px;
  background-color: #f5f7fa;
  color: #666;
  border: 1px solid #e5e6eb;
  cursor: pointer;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.date-tag-item span:first-child {
  font-size: 12px;
}

.date-tag-item span:last-child {
  font-size: 11px;
  color: #909399;
}

.date-tag-item.active {
  background-color: #e8f4ff;
  border-color: #4a90e2;
  color: #4a90e2;
}

.date-tag-item.active span:last-child {
  color: #4a90e2;
}

.time-slot-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.time-pill {
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  background-color: #f5f7fa;
  color: #666;
  border: 1px solid #e5e6eb;
  cursor: pointer;
}

.time-pill.active {
  background-color: #4a90e2;
  color: #fff;
  border-color: #4a90e2;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  background-color: #4a90e2;
  color: #fff;
  border: none;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 关联研讨室：关键时间节点选择 UI */
.node-picker {
  margin-bottom: 16px;
}

.node-picker-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.node-check {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 8px;
}

.node-check input[type='checkbox'] {
  width: 16px;
  height: 16px;
}

.node-check-text {
  font-size: 13px;
  color: #1a1a1a;
}

.node-new-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
  align-items: start;
  margin-bottom: 8px;
}

.node-new-input {
  margin-bottom: 0;
}

.node-del-btn {
  height: 44px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #e5e6eb;
  background: #fff;
  color: #f56c6c;
  cursor: pointer;
}

.node-add-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  border: 1px dashed #cbd5e1;
  background: #f5f7fa;
  color: #4a90e2;
  cursor: pointer;
}
</style>
