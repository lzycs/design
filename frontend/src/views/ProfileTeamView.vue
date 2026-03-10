<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  getUserTeams,
  getTeamRequestDetail,
  getTeamMembers,
  createTeamRequest,
  updateTeamStatus,
  type TeamRequestVO,
  type TeamMemberVO,
} from '@/api/team'
import { getTeamStudyPlans, type StudyPlanVO } from '@/api/studyPlan'
import { showToast } from 'vant'

interface User {
  id?: number
  username: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const teams = ref<TeamRequestVO[]>([])
const loading = ref(false)
const screen = ref<'list' | 'detail' | 'create'>('list')
const currentTeam = ref<TeamRequestVO | null>(null)
const members = ref<TeamMemberVO[]>([])
const membersLoading = ref(false)
const teamPlans = ref<StudyPlanVO[]>([])
const teamPlansLoading = ref(false)

/** 筛选：全部 / 我发起的 / 我参与的 / 已完成 */
const filterTab = ref<'all' | 'initiated' | 'joined' | 'done'>('all')

const isLoggedIn = computed(() => !!storedUser.value?.id)

/** 是否至少为一个小组组长（这里组长=发起人） */
const canCreateCollab = computed(() => {
  if (!storedUser.value?.id) return false
  return teams.value.some((t) => t.userId === storedUser.value!.id)
})

const filteredTeams = computed(() => {
  if (!storedUser.value?.id) return []
  if (filterTab.value === 'all') return teams.value
  if (filterTab.value === 'initiated') return teams.value.filter((t) => t.userId === storedUser.value!.id)
  if (filterTab.value === 'joined') return teams.value.filter((t) => t.userId !== storedUser.value!.id)
  return teams.value.filter((t) => t.status === 0)
})

/** 状态：0-已完成 1-招募中 2-已满员 -> 进行中/已完成 */
const getStatusText = (status?: number) => {
  if (status === 0) return '已完成'
  if (status === 1 || status === 2) return '进行中'
  return '进行中'
}

const getStatusClass = (status?: number) => {
  if (status === 0) return 'status-done'
  return 'status-doing'
}

const loadTeamPlans = async (teamRequestId: number) => {
  teamPlansLoading.value = true
  try {
    const res = await getTeamStudyPlans(teamRequestId)
    teamPlans.value = (res as unknown as { data?: StudyPlanVO[] }).data ?? []
  } catch (e) {
    console.error(e)
    teamPlans.value = []
  } finally {
    teamPlansLoading.value = false
  }
}

const formatDate = (s?: string) => {
  if (!s) return '-'
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const formatDateTime = (s?: string) => {
  if (!s) return ''
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return s
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

const formatRange = (start?: string, end?: string) => {
  const a = formatDateTime(start)
  const b = formatDateTime(end)
  if (a && b) return `${a} - ${b}`
  if (a) return `开始：${a}`
  if (b) return `结束：${b}`
  return ''
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

const loadTeams = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await getUserTeams(storedUser.value.id)
    teams.value = (res as unknown as { data?: TeamRequestVO[] }).data ?? []
  } catch (e) {
    console.error(e)
    teams.value = []
    showToast('加载协作列表失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (screen.value === 'detail' || screen.value === 'create') {
    screen.value = 'list'
    currentTeam.value = null
    members.value = []
  } else {
    router.push('/profile')
  }
}

const openDetail = async (item: TeamRequestVO) => {
  currentTeam.value = item
  screen.value = 'detail'
  members.value = []
  teamPlans.value = []
  if (!item.id) return
  membersLoading.value = true
  try {
    const detailRes = await getTeamRequestDetail(item.id)
    const vo = (detailRes as unknown as { data?: TeamRequestVO }).data
    if (vo) currentTeam.value = vo
    const memRes = await getTeamMembers(item.id)
    members.value = (memRes as unknown as { data?: TeamMemberVO[] }).data ?? []
    await loadTeamPlans(item.id)
  } catch (e) {
    console.error(e)
  } finally {
    membersLoading.value = false
  }
}

const parseKeyTimeNodes = (raw?: string) => {
  if (!raw || !raw.trim()) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => {
      const [title = '', datetime = ''] = s.split('|').map((x) => x.trim())
      return { title, datetime }
    })
}

const formatTime = (t?: string) => {
  if (!t) return ''
  return t.length >= 8 ? t.substring(0, 5) : t
}

const formatDateNoYear = (d?: string) => {
  if (!d) return ''
  const m = d.match(/^\d{4}-(\d{2}-\d{2})/)
  return m ? m[1] : d
}

/** 研讨室标签：地点 + 日期 + 时段 */
const meetingRoomLabel = (p: StudyPlanVO) => {
  if (!p.classroomName) return ''
  if (p.reservationDate && (p.reservationStartTime || p.reservationEndTime)) {
    return `${p.classroomName} (${formatDateNoYear(p.reservationDate)} ${formatTime(p.reservationStartTime)}-${formatTime(p.reservationEndTime)})`
  }
  return p.classroomName
}

const goCreate = () => {
  if (!canCreateCollab.value) {
    showToast('仅组长可创建协作')
    return
  }
  screen.value = 'create'
  createForm.title = ''
  createForm.description = ''
  createForm.initStatus = 1
  createForm.startTime = ''
  createForm.endTime = ''
}

const markDone = async () => {
  if (!currentTeam.value?.id) return
  if (!storedUser.value?.id) return
  try {
    await updateTeamStatus(currentTeam.value.id, 0, storedUser.value.id)
    currentTeam.value.status = 0
    const idx = teams.value.findIndex((t) => t.id === currentTeam.value!.id)
    if (idx >= 0) teams.value[idx].status = 0
    showToast('已标记为完成')
  } catch (e) {
    console.error(e)
    const msg =
      (e as any)?.response?.data?.message ||
      (e as any)?.response?.data?.msg ||
      '操作失败'
    showToast(msg)
  }
}

const goChat = () => {
  if (!currentTeam.value?.id) return
  router.push({
    path: `/team-chat/${currentTeam.value.id}`,
    query: { title: currentTeam.value.title ?? '协作' },
  })
}

const createForm = ref({
  title: '',
  description: '',
  initStatus: 1 as number, // 1-进行中 0-已完成
  startTime: '',
  endTime: '',
})

const createSubmitting = ref(false)
const submitCreate = async () => {
  if (!createForm.value.title.trim()) {
    showToast('请输入协作标题')
    return
  }
  if (!storedUser.value?.id) {
    showToast('请先登录')
    return
  }
  createSubmitting.value = true
  try {
    const normalizeDateTimeLocal = (v: string) => {
      const s = (v || '').trim()
      if (!s) return undefined
      return s.length === 16 ? `${s}:00` : s
    }
    await createTeamRequest({
      userId: storedUser.value.id,
      title: createForm.value.title.trim(),
      description: createForm.value.description.trim() || undefined,
      status: createForm.value.initStatus,
      expectedCount: 5,
      currentCount: 1,
      startTime: normalizeDateTimeLocal(createForm.value.startTime),
      endTime: normalizeDateTimeLocal(createForm.value.endTime),
    })
    showToast('发起成功')
    screen.value = 'list'
    await loadTeams()
  } catch (e) {
    console.error(e)
    showToast('发起失败')
  } finally {
    createSubmitting.value = false
  }
}

const isLeader = computed(() => {
  if (!storedUser.value?.id || !currentTeam.value?.id) return false
  // leader 通常就是发起人
  if (currentTeam.value.userId === storedUser.value.id) return true
  return members.value.some((m) => m.userId === storedUser.value!.id && m.role === 1)
})

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) await loadTeams()
})
</script>

<template>
  <div class="profile-team">
    <!-- 列表页 -->
    <template v-if="screen === 'list'">
      <van-nav-bar title="我的协作" left-arrow @click-left="goBack">
        <template #right>
          <span v-if="canCreateCollab" class="header-right-btn" @click="goCreate">发起协作</span>
        </template>
      </van-nav-bar>

      <template v-if="isLoggedIn">
        <div class="filter-tabs">
          <div
            v-for="tab in [
              { key: 'all', label: '全部' },
              { key: 'initiated', label: '我发起的' },
              { key: 'joined', label: '我参与的' },
              { key: 'done', label: '已完成' },
            ]"
            :key="tab.key"
            class="filter-tab"
            :class="{ active: filterTab === tab.key }"
            @click="filterTab = tab.key as typeof filterTab"
          >
            {{ tab.label }}
          </div>
        </div>

        <div class="content-area">
          <van-loading v-if="loading" class="loading-wrap" vertical>加载中...</van-loading>
          <div v-else-if="filteredTeams.length === 0" class="empty-collab">
            <van-icon name="friends-o" class="empty-icon" />
            <div class="empty-title">暂无协作</div>
            <div class="empty-desc">你还没有发起或参与任何协作，快来创建第一个协作吧</div>
            <button v-if="canCreateCollab" type="button" class="empty-btn" @click="goCreate">发起协作</button>
          </div>
          <div v-else class="collab-list">
            <div
              v-for="item in filteredTeams"
              :key="item.id"
              class="collab-card"
              @click="openDetail(item)"
            >
              <div class="collab-info">
                <div class="collab-header">
                  <div class="collab-title">{{ item.title }}</div>
                  <div class="collab-status" :class="getStatusClass(item.status)">
                    {{ getStatusText(item.status) }}
                  </div>
                </div>
                <div class="collab-meta">
                  发起人：{{ item.creatorName ?? '-' }} | 更新时间：{{ formatDate(item.updateTime) }}
                </div>
                <div class="collab-meta" v-if="formatRange(item.startTime, item.endTime)">
                  时间：{{ formatRange(item.startTime, item.endTime) }}
                </div>
                <div class="collab-desc">{{ item.description || '无描述' }}</div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <van-empty description="请先登录后查看协作" class="empty-wrap" />
      </template>
    </template>

    <!-- 详情页 -->
    <template v-else-if="screen === 'detail' && currentTeam">
      <van-nav-bar title="协作详情" left-arrow @click-left="goBack" />

      <div class="content-area detail-content">
        <div class="collab-detail-header">
          <div class="detail-title">{{ currentTeam.title }}</div>
          <div class="detail-meta">
            <span>发起人：{{ currentTeam.creatorName ?? '-' }}</span>
            <span>更新时间：{{ formatDate(currentTeam.updateTime) }}</span>
          </div>
          <div class="detail-meta" v-if="formatRange(currentTeam.startTime, currentTeam.endTime)">
            <span>时间：{{ formatRange(currentTeam.startTime, currentTeam.endTime) }}</span>
          </div>
          <div class="detail-status" :class="getStatusClass(currentTeam.status)">
            {{ getStatusText(currentTeam.status) }}
          </div>
          <div class="detail-desc">{{ currentTeam.description || '无描述' }}</div>
        </div>

        <div class="member-section">
          <div class="section-title">
            <span>参与成员</span>
          </div>
          <van-loading v-if="membersLoading" size="24" vertical>加载成员...</van-loading>
          <div v-else class="member-list">
            <div v-for="m in members" :key="m.id ?? m.userId" class="member-item">
              <div class="member-avatar">
                <van-icon name="user-o" />
              </div>
              <div class="member-name">{{ m.memberName ?? '未知' }}</div>
            </div>
          </div>
        </div>

        <div class="plan-section">
          <div class="section-title">学习计划</div>
          <van-loading v-if="teamPlansLoading" size="24" vertical>加载计划...</van-loading>
          <div v-else-if="teamPlans.length === 0" class="plan-empty">该小组暂无学习计划</div>
          <div v-else class="plan-list">
            <div v-for="p in teamPlans" :key="p.id" class="plan-card">
              <div class="plan-title-row">
                <div class="plan-title">{{ p.title }}</div>
                <div class="plan-tag" :class="{ done: p.status === 2 }">{{ p.status === 2 ? '已完成' : '进行中' }}</div>
              </div>
              <div class="plan-sub">{{ p.planDate || '-' }}</div>
              <div v-if="p.description" class="plan-desc">{{ p.description }}</div>
              <div v-if="parseKeyTimeNodes(p.keyTimeNodes).length > 0" class="plan-nodes">
                <div class="nodes-title">关键时间节点：</div>
                <div v-for="(n, idx) in parseKeyTimeNodes(p.keyTimeNodes)" :key="idx" class="node-item">
                  <div class="node-dot"></div>
                    <div class="node-text">
                      {{ n.title }}
                      <span v-if="meetingRoomLabel(p)" class="meeting-room-tag">{{ meetingRoomLabel(p) }}</span>
                    </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="action-bar">
        <button
          v-if="currentTeam.status !== 0 && isLeader"
          type="button"
          class="action-btn primary-btn"
          @click="markDone"
        >
          标记完成
        </button>
        <button type="button" class="action-btn secondary-btn" @click="goChat">进入聊天</button>
      </div>
    </template>

    <!-- 发起协作页 -->
    <template v-else-if="screen === 'create'">
      <van-nav-bar title="发起协作" left-arrow @click-left="goBack" />

      <div class="content-area create-content">
        <div class="form-section">
          <label class="form-label">协作标题 <span class="required">*</span></label>
          <input
            v-model="createForm.title"
            type="text"
            class="form-input"
            placeholder="请输入协作标题"
          />
        </div>
        <div class="form-section">
          <label class="form-label">协作描述</label>
          <textarea
            v-model="createForm.description"
            class="form-input form-textarea"
            placeholder="请输入协作详细描述"
          ></textarea>
        </div>
        <div class="form-section">
          <label class="form-label">初始状态</label>
          <select v-model.number="createForm.initStatus" class="form-select">
            <option :value="1">进行中</option>
            <option :value="0">已完成</option>
          </select>
        </div>
        <div class="form-section">
          <label class="form-label">开始时间</label>
          <input v-model="createForm.startTime" type="datetime-local" class="form-input" />
        </div>
        <div class="form-section">
          <label class="form-label">结束时间</label>
          <input v-model="createForm.endTime" type="datetime-local" class="form-input" />
        </div>
        <div class="action-bar single">
          <button
            type="button"
            class="action-btn primary-btn"
            :disabled="!createForm.title.trim() || createSubmitting"
            @click="submitCreate"
          >
            提交
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-team {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 24px;
}

.header-right-btn {
  font-size: 14px;
  color: #4a90e2;
  font-weight: 500;
}

.filter-tabs {
  display: flex;
  background: #fff;
  padding: 8px 20px;
  border-bottom: 1px solid #f5f7fa;
}

.filter-tab {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 14px;
  color: #909399;
  border-bottom: 2px solid transparent;
}

.filter-tab.active {
  color: #4a90e2;
  border-bottom-color: #4a90e2;
  font-weight: 600;
}

.content-area {
  padding: 16px 20px;
}

.collab-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collab-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  align-items: flex-start;
}

.collab-info {
  flex: 1;
  min-width: 0;
}

.collab-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.collab-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collab-status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.status-doing {
  background: #fff7e6;
  color: #ff9a2e;
}

.status-done {
  background: #e8f5e9;
  color: #4caf50;
}

.collab-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collab-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-collab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #e5e6eb;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}

.empty-btn {
  padding: 10px 24px;
  border-radius: 8px;
  background: #4a90e2;
  color: #fff;
  border: none;
  font-size: 14px;
  font-weight: 500;
}

.loading-wrap,
.empty-wrap {
  padding: 40px 0;
}

/* 详情：底部留白，避免内容被固定操作栏遮挡；操作栏需在 tabbar 上方 */
.detail-content {
  padding-bottom: 130px;
}

.collab-detail-header {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.detail-status {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 16px;
}

.detail-desc {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  padding: 16px 0;
  border-top: 1px solid #f5f7fa;
  border-bottom: 1px solid #f5f7fa;
}

.member-section,
.plan-section {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.member-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 60px;
}

.member-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f0f7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  font-size: 24px;
  color: #4a90e2;
}

.member-name {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  text-align: center;
}

.plan-empty {
  font-size: 12px;
  color: #909399;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 14px;
}

.plan-title-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 6px;
}

.plan-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  background: #e8f4ff;
  color: #4a90e2;
  flex-shrink: 0;
}

.plan-tag.done {
  background: #e8f5e9;
  color: #4caf50;
}

.plan-sub {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.plan-desc {
  font-size: 12px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 8px;
}

.plan-nodes .nodes-title {
  font-size: 12px;
  color: #333;
  margin-bottom: 6px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #e5e6eb;
}

.node-item:last-child {
  border-bottom: none;
}

.node-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4a90e2;
  flex-shrink: 0;
}

.node-text {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 500;
}

.meeting-room-tag {
  display: inline-block;
  padding: 2px 8px;
  background-color: #e8f4ff;
  color: #4a90e2;
  border-radius: 4px;
  font-size: 11px;
  margin-left: 8px;
  margin-top: 4px;
}

.action-bar {
  position: fixed;
  bottom: 50px; /* 避开底部 van-tabbar 高度 */
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #f5f7fa;
  z-index: 10;
}

.action-bar.single {
  justify-content: stretch;
}

.action-btn {
  flex: 1;
  padding: 12px 0;
  border-radius: 8px;
  border: none;
  font-size: 16px;
  font-weight: 500;
}

.primary-btn {
  background: #4a90e2;
  color: #fff;
}

.primary-btn:disabled {
  background: #c9d8eb;
  cursor: not-allowed;
}

.secondary-btn {
  background: #f5f7fa;
  color: #4a90e2;
}

/* 发起协作表单 */
.form-section {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.form-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.form-label .required {
  color: #f56c6c;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 0;
  box-sizing: border-box;
}

.form-textarea {
  min-height: 100px;
  resize: none;
}

.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  background: #fff;
  box-sizing: border-box;
}

/* 发起协作页：底部留白，避免提交按钮被 tabbar 遮挡 */
.create-content {
  padding-bottom: 130px;
}
</style>
