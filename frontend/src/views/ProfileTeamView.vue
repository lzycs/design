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

/** 筛选：全部 / 我发起的 / 我参与的 */
const filterTab = ref<'all' | 'initiated' | 'joined'>('all')

const isLoggedIn = computed(() => !!storedUser.value?.id)

const filteredTeams = computed(() => {
  if (!storedUser.value?.id) return []
  if (filterTab.value === 'all') return teams.value
  if (filterTab.value === 'initiated') return teams.value.filter((t) => t.userId === storedUser.value!.id)
  return teams.value.filter((t) => t.userId !== storedUser.value!.id)
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

/** 协作进度百分比 */
const progressPercent = computed(() => {
  const t = currentTeam.value
  if (!t || !t.expectedCount || t.expectedCount === 0) return 0
  const cur = t.currentCount ?? 0
  return Math.min(100, Math.round((cur / t.expectedCount) * 100))
})

const formatDate = (s?: string) => {
  if (!s) return '-'
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
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
  if (!item.id) return
  membersLoading.value = true
  try {
    const detailRes = await getTeamRequestDetail(item.id)
    const vo = (detailRes as unknown as { data?: TeamRequestVO }).data
    if (vo) currentTeam.value = vo
    const memRes = await getTeamMembers(item.id)
    members.value = (memRes as unknown as { data?: TeamMemberVO[] }).data ?? []
  } catch (e) {
    console.error(e)
  } finally {
    membersLoading.value = false
  }
}

const goCreate = () => {
  screen.value = 'create'
  createForm.title = ''
  createForm.description = ''
  createForm.initStatus = 1
}

const markDone = async () => {
  if (!currentTeam.value?.id) return
  try {
    await updateTeamStatus(currentTeam.value.id, 0)
    currentTeam.value.status = 0
    const idx = teams.value.findIndex((t) => t.id === currentTeam.value!.id)
    if (idx >= 0) teams.value[idx].status = 0
    showToast('已标记为完成')
  } catch (e) {
    console.error(e)
    showToast('操作失败')
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
    await createTeamRequest({
      userId: storedUser.value.id,
      title: createForm.value.title.trim(),
      description: createForm.value.description.trim() || undefined,
      status: createForm.value.initStatus,
      expectedCount: 5,
      currentCount: 1,
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
          <span class="header-right-btn" @click="goCreate">发起协作</span>
        </template>
      </van-nav-bar>

      <template v-if="isLoggedIn">
        <div class="filter-tabs">
          <div
            v-for="tab in [
              { key: 'all', label: '全部' },
              { key: 'initiated', label: '我发起的' },
              { key: 'joined', label: '我参与的' },
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
            <button type="button" class="empty-btn" @click="goCreate">发起协作</button>
          </div>
          <div v-else class="collab-list">
            <div
              v-for="item in filteredTeams"
              :key="item.id"
              class="collab-card"
              @click="openDetail(item)"
            >
              <div class="collab-cover">
                <van-icon name="friends-o" />
              </div>
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

        <div class="progress-section">
          <div class="section-title">协作进度</div>
          <div class="progress-bar-container">
            <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div class="progress-text">当前进度：{{ progressPercent }}%</div>
        </div>
      </div>

      <div class="action-bar">
        <button
          v-if="currentTeam.status !== 0"
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

.collab-cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #f0f7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
  font-size: 32px;
  color: #4a90e2;
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
.progress-section {
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

.progress-bar-container {
  width: 100%;
  height: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
  margin: 8px 0;
}

.progress-bar {
  height: 100%;
  background: #4a90e2;
  border-radius: 4px;
  transition: width 0.3s;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  text-align: right;
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
