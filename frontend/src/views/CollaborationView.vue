<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getActiveTeamRequests,
  applyToJoinTeam,
  createTeamRequest,
  type TeamRequestVO,
  getTeamMembers,
  type TeamMemberVO,
  getPendingByApplicant,
  quitTeam,
  deleteTeamRequest,
  type TeamJoinApplicationVO,
} from '@/api/team'
import { showConfirmDialog, showToast } from 'vant'

type CategoryKey = 'all' | 'exam' | 'project' | 'postgrad' | 'language'

const router = useRouter()

const categories: { key: CategoryKey; label: string }[] = [
  { key: 'all', label: '全部组队' },
  { key: 'exam', label: '考试复习' },
  { key: 'project', label: '项目开发' },
  { key: 'postgrad', label: '考研自习' },
  { key: 'language', label: '语言学习' },
]

const activeCategory = ref<CategoryKey>('all')
const selectableCategories = categories.filter((item) => item.key !== 'all')
const teams = ref<TeamRequestVO[]>([])
const loading = ref(false)

const showDetail = ref(false)
const currentTeam = ref<TeamRequestVO | null>(null)
const joining = ref(false)
const quitting = ref(false)
const deleting = ref(false)
const members = ref<TeamMemberVO[]>([])
const myPendingApplications = ref<TeamJoinApplicationVO[]>([])

const storedUser = ref<{ id: number; username?: string } | null>(null)

const isLoggedIn = computed(() => !!storedUser.value?.id)

// 发起小组
const showCreate = ref(false)
const createForm = ref({
  title: '',
  description: '',
  expectedCount: 5,
  startTime: '',
  endTime: '',
  category: '' as '' | Exclude<CategoryKey, 'all'>,
})
const creating = ref(false)

const loadUserFromStorage = () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) {
    storedUser.value = null
    return
  }
  try {
    storedUser.value = JSON.parse(raw) as { id: number; username?: string }
  } catch {
    storedUser.value = null
  }
}

const parseTags = (item: TeamRequestVO): string[] => {
  if (!item.tags) return []
  try {
    const parsed = JSON.parse(item.tags)
    if (Array.isArray(parsed)) {
      return parsed.filter((t) => typeof t === 'string')
    }
  } catch {
    // fall through
  }
  return item.tags
    .split(/[，,]/)
    .map((t) => t.trim())
    .filter(Boolean)
}

const getCategoryForTeam = (item: TeamRequestVO): CategoryKey => {
  const text = `${item.title ?? ''} ${item.description ?? ''} ${item.tags ?? ''}`
  if (/考研|研究生/.test(text)) return 'postgrad'
  if (/项目|开发|程序|代码|编程/.test(text)) return 'project'
  if (/英语|日语|法语|德语|口语|语言/.test(text)) return 'language'
  if (/考试|期末|复习|刷题/.test(text)) return 'exam'
  return 'all'
}

const filteredTeams = computed(() => {
  if (activeCategory.value === 'all') return teams.value
  return teams.value.filter(
    (t) => getCategoryForTeam(t) === activeCategory.value,
  )
})

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

const loadTeams = async () => {
  loading.value = true
  try {
    const res = await getActiveTeamRequests()
    const list = res.data ?? []
    const map = new Map<string, TeamRequestVO>()
    list.forEach((item) => {
      const key = `${item.title ?? ''}|${item.description ?? ''}|${item.expectedCount ?? ''}`
      if (!map.has(key)) {
        map.set(key, item)
      }
    })
    teams.value = Array.from(map.values())
  } catch (e) {
    console.error('加载协作组队失败', e)
    teams.value = []
  } finally {
    loading.value = false
  }
}

const loadApplyState = async () => {
  if (!storedUser.value?.id) {
    myPendingApplications.value = []
    return
  }
  try {
    const pendingRes = await getPendingByApplicant(storedUser.value.id)
    myPendingApplications.value = pendingRes.data ?? []
  } catch (e) {
    console.error('加载申请状态失败', e)
    myPendingApplications.value = []
  }
}

const openDetail = (item: TeamRequestVO) => {
  currentTeam.value = item
  members.value = []
  showDetail.value = true
  if (item.id) {
    getTeamMembers(item.id)
      .then((res) => {
        members.value = res.data ?? []
      })
      .catch((e) => {
        console.error('加载小组成员失败', e)
        members.value = []
      })
  }
}

const openCreate = () => {
  if (!isLoggedIn.value || !storedUser.value?.id) {
    showToast('请先登录后再发起小组')
    router.push('/profile')
    return
  }
  createForm.value = {
    title: '',
    description: '',
    expectedCount: 5,
    startTime: '',
    endTime: '',
    category: '',
  }
  showCreate.value = true
}

const normalizeDateTimeLocal = (v: string) => {
  const s = (v || '').trim()
  if (!s) return undefined
  // datetime-local: 2026-03-06T09:00 -> 2026-03-06T09:00:00
  return s.length === 16 ? `${s}:00` : s
}

const submitCreate = async () => {
  if (!storedUser.value?.id) return
  if (!createForm.value.title.trim()) {
    showToast('请输入小组标题')
    return
  }
  creating.value = true
  try {
    const tags = createForm.value.category
      ? JSON.stringify([
          categories.find((item) => item.key === createForm.value.category)?.label ?? createForm.value.category,
        ])
      : undefined
    await createTeamRequest({
      userId: storedUser.value.id,
      title: createForm.value.title.trim(),
      description: createForm.value.description.trim() || undefined,
      expectedCount: Math.max(2, Number(createForm.value.expectedCount) || 5),
      currentCount: 1,
      status: 1,
      startTime: normalizeDateTimeLocal(createForm.value.startTime),
      endTime: normalizeDateTimeLocal(createForm.value.endTime),
      tags,
    })
    showToast('发起成功')
    showCreate.value = false
    await loadTeams()
  } catch (e) {
    console.error('发起失败', e)
    showToast('发起失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

const hasPendingForCurrentTeam = computed(() => {
  if (!currentTeam.value?.id) return false
  return myPendingApplications.value.some((a) => a.teamRequestId === currentTeam.value?.id && a.status === 0)
})

const isMemberOfCurrentTeam = computed(() => {
  if (!storedUser.value?.id) return false
  return members.value.some((m) => m.userId === storedUser.value?.id)
})

const isLeaderOfCurrentTeam = computed(() => {
  if (!storedUser.value?.id || !currentTeam.value?.id) return false
  if (currentTeam.value.userId === storedUser.value.id) return true
  return members.value.some((m) => m.userId === storedUser.value?.id && m.role === 1)
})

const handleQuitTeam = async () => {
  if (!storedUser.value?.id || !currentTeam.value?.id) return
  if (isLeaderOfCurrentTeam.value) {
    showToast('组长不能退出小组')
    return
  }
  if (!isMemberOfCurrentTeam.value) {
    showToast('你不是该小组成员')
    return
  }
  const confirm = await showConfirmDialog({
    title: '退出小组',
    message: `确认退出“${currentTeam.value.title ?? '该小组'}”吗？`,
  }).catch(() => false)
  if (!confirm) return

  quitting.value = true
  try {
    const res = await quitTeam(currentTeam.value.id, storedUser.value.id)
    if (res.code !== 200) {
      showToast(res.message || '退出失败')
      return
    }
    showToast('已退出小组')
    // 刷新列表与详情中的成员/人数
    await Promise.all([loadTeams(), loadApplyState()])
    const cur = currentTeam.value
    if (cur?.id) {
      const hit = teams.value.find((t) => t.id === cur.id)
      if (hit) currentTeam.value = hit
      const memRes = await getTeamMembers(cur.id)
      members.value = memRes.data ?? []
    }
  } catch (e) {
    console.error('退出小组失败', e)
    const msg = (e as any)?.response?.data?.message || '退出失败，请稍后重试'
    showToast(msg)
  } finally {
    quitting.value = false
  }
}

const handleDeleteTeam = async () => {
  if (!storedUser.value?.id || !currentTeam.value?.id) return
  if (!isLeaderOfCurrentTeam.value) {
    showToast('仅组长可删除小组')
    return
  }
  const confirm = await showConfirmDialog({
    title: '删除小组',
    message: `确认删除“${currentTeam.value.title ?? '该小组'}”吗？删除后成员、申请及关联计划将一并隐藏。`,
  }).catch(() => false)
  if (!confirm) return

  deleting.value = true
  try {
    const res = await deleteTeamRequest(currentTeam.value.id, storedUser.value.id)
    if (res.code !== 200) {
      showToast(res.message || '删除失败')
      return
    }
    showToast('小组已删除')
    showDetail.value = false
    currentTeam.value = null
    members.value = []
    await Promise.all([loadTeams(), loadApplyState()])
  } catch (e) {
    console.error('删除小组失败', e)
    const msg = (e as any)?.response?.data?.message || '删除失败，请稍后重试'
    showToast(msg)
  } finally {
    deleting.value = false
  }
}

const handleApplyJoin = async () => {
  if (!isLoggedIn.value || !storedUser.value?.id) {
    showToast('请先登录后再申请加入小组')
    router.push('/profile')
    return
  }
  if (!currentTeam.value?.id) return
  if (hasPendingForCurrentTeam.value) {
    showToast('你已提交过该小组申请，请等待组长审批')
    return
  }
  if (isMemberOfCurrentTeam.value) {
    showToast('你已经是该小组成员')
    return
  }
  if (
    currentTeam.value.expectedCount &&
    currentTeam.value.currentCount &&
    currentTeam.value.currentCount >= currentTeam.value.expectedCount
  ) {
    showToast('该小组已满员')
    return
  }
  const confirm = await showConfirmDialog({
    title: '申请加入',
    message: `确认申请加入“${currentTeam.value.title ?? '该小组'}”吗？`,
  }).catch(() => false)
  if (!confirm) return

  joining.value = true
  try {
    await applyToJoinTeam(currentTeam.value.id, storedUser.value.id, '')
    showToast('申请已提交，等待组长审批')
    await loadApplyState()
  } catch (e) {
    console.error('申请加入失败', e)
    const msg =
      (e as any)?.response?.data?.message ||
      (e as any)?.response?.data?.msg ||
      '申请失败，请稍后重试'
    showToast(msg)
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  loadUserFromStorage()
  loadTeams()
  loadApplyState()
})
</script>

<template>
  <div class="collaboration">
    <div class="phone-container">
      <!-- 顶栏：与下方内容共用同一水平内边距，标题真正居中 -->
      <header class="page-header">
        <button type="button" class="page-header__back" @click="router.back()" aria-label="返回上一页">
          <span>‹</span>
        </button>
        <h1 class="page-header__title">协作广场</h1>
        <button type="button" class="page-header__action" @click="openCreate">
          发起小组
        </button>
      </header>

      <!-- 分类标签 -->
      <div class="category-bar">
        <div
          v-for="c in categories"
          :key="c.key"
          class="category-item"
          :class="{ active: activeCategory === c.key }"
          @click="activeCategory = c.key"
        >
          {{ c.label }}
        </div>
      </div>

      <!-- 组队列表 -->
      <div class="team-list">
        <van-loading v-if="loading" size="24px" type="spinner">
          加载中...
        </van-loading>

        <van-empty
          v-else-if="!filteredTeams.length"
          description="暂时没有符合条件的组队"
        />

        <div
          v-else
          v-for="item in filteredTeams"
          :key="item.id"
          class="team-card"
          @click="openDetail(item)"
        >
          <div class="team-header">
            <div class="team-title">
              {{ item.title }}
            </div>
          </div>
          <div class="team-tags">
            <div class="tag">
              {{ getCategoryForTeam(item) === 'project'
                ? '项目开发'
                : getCategoryForTeam(item) === 'exam'
                  ? '考试复习'
                  : getCategoryForTeam(item) === 'postgrad'
                    ? '考研自习'
                    : getCategoryForTeam(item) === 'language'
                      ? '语言学习'
                      : '学习协作' }}
            </div>
            <div class="tag" v-if="item.creatorName">
              发起人：{{ item.creatorName }}
            </div>
            <div class="tag">
              人数：{{ item.currentCount ?? 1 }}/{{ item.expectedCount ?? '-' }}
            </div>
            <div class="tag" v-if="formatRange(item.startTime, item.endTime)">
              时间：{{ formatRange(item.startTime, item.endTime) }}
            </div>
            <div
              v-for="tag in parseTags(item)"
              :key="tag"
              class="tag"
            >
              {{ tag }}
            </div>
          </div>
          <div class="team-desc">
            {{ item.description || '暂无描述' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹层 -->
    <van-popup
      v-model:show="showDetail"
      round
      position="bottom"
      :style="{ height: '70%' }"
    >
      <div v-if="currentTeam" class="detail-wrapper">
        <div class="team-detail-header">
          <div class="team-detail-title">
            {{ currentTeam.title }}
          </div>
          <div class="team-detail-tags">
            <div
              v-for="tag in parseTags(currentTeam)"
              :key="tag"
              class="team-detail-tag"
            >
              {{ tag }}
            </div>
          </div>
          <div class="team-detail-desc">
            {{ currentTeam.description || '发起人暂未填写详细说明。' }}
          </div>
          <div class="team-detail-extra">
            当前人数：
            {{ currentTeam.currentCount ?? 1 }}/{{ currentTeam.expectedCount ?? '-' }}
          </div>
          <div class="team-detail-extra" v-if="formatRange(currentTeam.startTime, currentTeam.endTime)">
            时间范围：{{ formatRange(currentTeam.startTime, currentTeam.endTime) }}
          </div>
          <div class="btn-row" :class="{ 'leader-mode': isLeaderOfCurrentTeam }">
            <button
              class="join-btn"
              :disabled="
                joining ||
                hasPendingForCurrentTeam ||
                isMemberOfCurrentTeam ||
                (currentTeam.expectedCount &&
                  currentTeam.currentCount &&
                  currentTeam.currentCount >= currentTeam.expectedCount) ||
                currentTeam.status === 2
              "
              @click.stop="handleApplyJoin"
            >
              {{
                joining
                  ? '提交中...'
                  : hasPendingForCurrentTeam
                    ? '等待组长审核'
                    : isMemberOfCurrentTeam
                      ? '你已经是该小组成员'
                      : currentTeam.status === 2 ||
                          (currentTeam.expectedCount &&
                            currentTeam.currentCount &&
                            currentTeam.currentCount >= currentTeam.expectedCount)
                        ? '已满员'
                        : '申请加入'
              }}
            </button>
            <button
              v-if="isMemberOfCurrentTeam && !isLeaderOfCurrentTeam"
              class="quit-btn"
              :disabled="quitting"
              @click.stop="handleQuitTeam"
            >
              {{
                quitting
                  ? '退出中...'
                  : '退出小组'
              }}
            </button>
            <button
              v-if="isLeaderOfCurrentTeam"
              class="delete-btn"
              :disabled="deleting"
              @click.stop="handleDeleteTeam"
            >
              {{ deleting ? '删除中...' : '删除小组' }}
            </button>
          </div>
          <div v-if="isLeaderOfCurrentTeam" class="leader-tip">
            你是组长，可删除小组；删除后成员、申请与关联计划会一并隐藏。
          </div>
        </div>
        <div class="member-section">
          <div class="section-title">组长</div>
          <div class="member-item" v-if="members.length">
            <div class="member-avatar"></div>
            <div class="member-name">
              {{ members[0]?.memberName ?? (members[0]?.userId ? `用户 ${members[0]?.userId}` : '组长') }}
            </div>
            <div class="member-role">组长</div>
          </div>
          <div class="member-empty" v-else>暂无成员数据</div>
        </div>
        <div class="member-section">
          <div class="section-title">
            成员（{{ currentTeam.currentCount ?? 1 }}/{{ currentTeam.expectedCount ?? '-' }}）
          </div>
          <div
            v-if="members.length > 1"
            class="member-list"
          >
            <div
              v-for="m in members.slice(1)"
              :key="m.id"
              class="member-item"
            >
              <div class="member-avatar"></div>
              <div class="member-name">
                {{ m.memberName ?? (m.userId ? `用户 ${m.userId}` : '成员') }}
              </div>
              <div class="member-role member-role-normal">成员</div>
            </div>
          </div>
          <div v-else class="member-empty">
            目前只有组长，快来加入一起学习吧～
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 发起小组弹层 -->
    <van-popup v-model:show="showCreate" round position="bottom" :style="{ height: '60%' }">
      <div class="create-wrapper">
        <div class="create-title">发起小组</div>
        <div class="create-form">
          <div class="form-item">
            <div class="form-label">小组标题</div>
            <input v-model="createForm.title" class="form-input" type="text" placeholder="如：考研英语复习" />
          </div>
          <div class="form-item">
            <div class="form-label">小组描述</div>
            <textarea
              v-model="createForm.description"
              class="form-input form-textarea"
              placeholder="简单描述目标、计划、时间等"
            />
          </div>
          <div class="form-item">
            <div class="form-label">期望人数</div>
            <input v-model.number="createForm.expectedCount" class="form-input" type="number" min="2" max="50" />
          </div>
          <div class="form-item">
            <div class="form-label">开始时间</div>
            <input v-model="createForm.startTime" class="form-input" type="datetime-local" />
          </div>
          <div class="form-item">
            <div class="form-label">结束时间</div>
            <input v-model="createForm.endTime" class="form-input" type="datetime-local" />
          </div>
          <div class="form-item">
            <div class="form-label">标签</div>
            <select v-model="createForm.category" class="form-input form-select">
              <option value="">请选择分类</option>
              <option
                v-for="item in selectableCategories"
                :key="item.key"
                :value="item.key"
              >
                {{ item.label }}
              </option>
            </select>
          </div>
          <div class="create-btn-row">
            <button class="create-btn" :disabled="creating" @click="submitCreate">
              {{ creating ? '提交中...' : '提交' }}
            </button>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
/* ------------------------------------------------------------------ *
 * 协作页：单一水平基准线
 * --collab-inset-left / right 统管顶栏、分类条、列表及卡片的左右对齐
 * ------------------------------------------------------------------ */
.collaboration {
  --collab-page-gutter: 16px;
  --collab-inset-left: max(
    var(--collab-page-gutter),
    env(safe-area-inset-left, 0px)
  );
  --collab-inset-right: max(
    var(--collab-page-gutter),
    env(safe-area-inset-right, 0px)
  );

  min-height: 100vh;
  height: 100vh;
  box-sizing: border-box;
  padding-bottom: 72px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.phone-container {
  flex: 1;
  min-height: 0;
  width: 100%;
  max-width: 375px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 顶栏：三列网格，左列留空、标题居中、操作贴右，与下方共用 inset */
.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  column-gap: 8px;
  padding: 12px var(--collab-inset-right) 12px var(--collab-inset-left);
  background-color: #ffffff;
  border-bottom: 1px solid #f0f2f5;
  position: sticky;
  top: 0;
  z-index: 10;
  box-sizing: border-box;
}

.page-header__title {
  grid-column: 2;
  justify-self: center;
  margin: 0;
  max-width: 100%;
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
  text-align: center;
}

.page-header__back {
  grid-column: 1;
  justify-self: start;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 14px;
  background: #f5f7fa;
  color: #4a5568;
  font-size: 22px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.page-header__action {
  grid-column: 3;
  justify-self: end;
  margin: 0;
  padding: 4px 0;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: #4a90e2;
  line-height: 1.3;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  white-space: nowrap;
}

.category-bar {
  display: flex;
  align-items: center;
  padding: 12px var(--collab-inset-right) 12px var(--collab-inset-left);
  background-color: #ffffff;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid #f0f2f5;
  box-sizing: border-box;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.category-bar::-webkit-scrollbar {
  display: none;
}

.category-item {
  padding: 6px 14px;
  font-size: 14px;
  color: #909399;
  background-color: #f5f7fa;
  border-radius: 20px;
  margin-right: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.category-item:last-child {
  margin-right: 0;
}

.category-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.team-list {
  flex: 1;
  min-height: 0;
  padding: 12px var(--collab-inset-right) 16px var(--collab-inset-left);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;
  box-sizing: border-box;
}

.team-list :deep(.van-empty) {
  padding-left: 0;
  padding-right: 0;
}

.team-list::-webkit-scrollbar {
  display: none;
}

.team-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 14px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  box-sizing: border-box;
}

.team-card:last-child {
  margin-bottom: 0;
}

.team-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.team-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.team-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0;
}

.tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #f5f7fa;
  color: #666666;
}

.team-desc {
  font-size: 14px;
  color: #666666;
  line-height: 1.5;
}

.detail-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.team-detail-header {
  padding: 20px;
  background-color: #4a90e2;
  color: #ffffff;
}

.team-detail-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.team-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.team-detail-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.team-detail-desc {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
}

.team-detail-extra {
  font-size: 13px;
  margin-bottom: 16px;
}

.btn-row {
  display: flex;
  gap: 10px;
}

.btn-row.leader-mode {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
}

.join-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  border: none;
  background-color: #ffffff;
  color: #4a90e2;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.join-btn {
  height: 44px;
  background-color: #ffffff;
  color: #4a90e2;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}

.join-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.quit-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #f6c4c4;
  background-color: #fff5f5;
  color: #d03050;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.quit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.delete-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #ef9a9a;
  background-color: #fdecec;
  color: #c62828;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.delete-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.leader-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.9);
}

.member-tip {
  padding: 16px 20px;
  font-size: 13px;
  color: #666666;
}

.member-section {
  padding: 16px 20px;
  background-color: #ffffff;
  margin-top: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  align-items: center;
}

.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e5e6eb;
  margin-right: 12px;
}

.member-name {
  flex: 1;
  font-size: 14px;
}

.member-role {
  font-size: 12px;
  color: #ffffff;
  background-color: #4a90e2;
  padding: 2px 8px;
  border-radius: 4px;
}

.member-role-normal {
  background-color: #909399;
}

.member-empty {
  font-size: 13px;
  color: #909399;
}

.create-wrapper {
  height: 100%;
  padding: 16px 20px;
  background: #ffffff;
}

.create-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  text-align: center;
}

.create-form .form-item {
  margin-bottom: 12px;
}

.create-form .form-label {
  font-size: 13px;
  color: #333333;
  margin-bottom: 6px;
}

.create-form .form-input {
  width: 100%;
  height: 40px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  box-sizing: border-box;
  background-color: #ffffff;
}

.create-form .form-select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 36px;
  background-image: linear-gradient(45deg, transparent 50%, #9ca3af 50%), linear-gradient(135deg, #9ca3af 50%, transparent 50%);
  background-position: calc(100% - 18px) calc(50% - 2px), calc(100% - 12px) calc(50% - 2px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
}

.create-form .form-textarea {
  height: 90px;
  padding: 10px 12px;
  resize: none;
}

.create-btn-row {
  margin-top: 8px;
}

.create-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 10px;
  background: #4a90e2;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.create-btn:disabled {
  opacity: 0.7;
}

/* Web 端增强：移动端样式保持默认，仅在大屏渐进增强 */
@media (min-width: 1024px) {
  .collaboration {
    --collab-page-gutter: 24px;
    height: 100vh;
    padding-bottom: 0;
    background:
      radial-gradient(circle at 10% -10%, rgba(74, 144, 226, 0.1), transparent 40%),
      radial-gradient(circle at 90% 110%, rgba(96, 165, 250, 0.08), transparent 45%),
      #f3f6fb;
  }

  .phone-container {
    width: min(1200px, 100%);
    max-width: none;
    min-height: 0;
    margin: 0 auto;
    border-left: 1px solid #e9eef5;
    border-right: 1px solid #e9eef5;
    box-shadow: 0 8px 28px rgba(15, 23, 42, 0.06);
  }

  .page-header {
    padding-top: 16px;
    padding-bottom: 16px;
    border-bottom-color: #e7ebf3;
  }

  .page-header__title {
    font-size: 20px;
    letter-spacing: 0.02em;
  }

  .page-header__back {
    width: 34px;
    height: 34px;
    border-radius: 17px;
    font-size: 26px;
    transition: background-color 0.2s ease, color 0.2s ease;
  }

  .page-header__action {
    padding: 7px 14px;
    border: 1px solid #d6e5fa;
    border-radius: 999px;
    background: linear-gradient(180deg, #ffffff 0%, #f3f8ff 100%);
    transition: all 0.2s ease;
  }

  .category-bar {
    gap: 12px;
    padding-top: 14px;
    padding-bottom: 14px;
    overflow-x: visible;
    white-space: normal;
    flex-wrap: wrap;
  }

  .category-item {
    margin-right: 0;
    padding: 8px 16px;
    border: 1px solid transparent;
    transition: all 0.2s ease;
  }

  .team-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
    align-content: start;
    padding-top: 16px;
    padding-bottom: 24px;
  }

  .team-card {
    margin-bottom: 0;
    min-height: 184px;
    border: 1px solid #e8edf5;
    border-radius: 14px;
    box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .team-title {
    font-size: 17px;
  }

  .team-desc {
    line-height: 1.65;
  }

  .tag {
    border-radius: 999px;
    border: 1px solid #edf1f7;
    background-color: #f8fafd;
  }

  .detail-wrapper {
    background-color: #f7f9fd;
  }

  .team-detail-header {
    padding: 24px 28px;
    background: linear-gradient(135deg, #4a90e2 0%, #5aa0f2 100%);
    border-radius: 16px 16px 0 0;
  }

  .team-detail-title {
    font-size: 24px;
  }

  .team-detail-desc {
    font-size: 15px;
  }

  .btn-row {
    gap: 12px;
  }

  .btn-row.leader-mode {
    grid-template-columns: 2fr 1fr;
  }

  .join-btn,
  .quit-btn,
  .delete-btn,
  .create-btn {
    height: 46px;
  }

  .member-section {
    margin-top: 14px;
    border: 1px solid #edf2f8;
    border-radius: 12px;
  }

  .create-wrapper {
    padding: 24px 28px;
    background: #f9fbff;
  }

  .create-form {
    max-width: 760px;
    margin: 0 auto;
  }

  .create-form .form-item {
    margin-bottom: 14px;
  }

  .create-btn-row {
    margin-top: 12px;
  }

  :deep(.van-popup--bottom) {
    width: min(900px, calc(100vw - 48px));
    left: 50%;
    transform: translateX(-50%);
    border-radius: 16px 16px 0 0;
    max-height: 88vh;
    box-shadow: 0 14px 40px rgba(15, 23, 42, 0.24);
  }
}

@media (min-width: 1024px) and (hover: hover) {
  .page-header__back:hover {
    background: #e9f2ff;
    color: #2f6fb8;
  }

  .page-header__action:hover {
    color: #2f6fb8;
    border-color: #bfd7f8;
    box-shadow: 0 2px 10px rgba(59, 130, 246, 0.14);
  }

  .category-item:hover {
    color: #4a90e2;
    border-color: #d5e6fb;
    background: #f0f6ff;
  }

  .team-card:hover {
    transform: translateY(-2px);
    border-color: #cfe1f8;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
  }

  .join-btn:not(:disabled):hover,
  .create-btn:not(:disabled):hover {
    filter: brightness(0.98);
  }

  .quit-btn:not(:disabled):hover,
  .delete-btn:not(:disabled):hover {
    filter: brightness(0.96);
  }
}
</style>

