<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getActiveTeamRequests,
  joinTeam,
  createTeamRequest,
  type TeamRequestVO,
  getTeamMembers,
  type TeamMemberVO,
} from '@/api/team'
import { showToast } from 'vant'

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
const teams = ref<TeamRequestVO[]>([])
const loading = ref(false)

const showDetail = ref(false)
const currentTeam = ref<TeamRequestVO | null>(null)
const joining = ref(false)
const members = ref<TeamMemberVO[]>([])

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
  tagsText: '',
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

const goChat = () => {
  if (!currentTeam.value?.id) return
  router.push({
    path: `/team-chat/${currentTeam.value.id}`,
    query: { title: currentTeam.value.title },
  })
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
    tagsText: '',
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
    const tags = createForm.value.tagsText.trim()
      ? JSON.stringify(
          createForm.value.tagsText
            .split(/[，,]/)
            .map((t) => t.trim())
            .filter(Boolean),
        )
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

const handleJoin = async () => {
  if (!isLoggedIn.value || !storedUser.value?.id) {
    window.alert('请先登录后再加入小组')
    router.push('/profile')
    return
  }
  if (!currentTeam.value?.id) return
  if (
    currentTeam.value.expectedCount &&
    currentTeam.value.currentCount &&
    currentTeam.value.currentCount >= currentTeam.value.expectedCount
  ) {
    window.alert('该小组已满员')
    return
  }
  joining.value = true
  try {
    await joinTeam(currentTeam.value.id, {
      userId: storedUser.value.id,
    })
    showToast('加入小组成功！')
    if (currentTeam.value) {
      currentTeam.value.currentCount =
        (currentTeam.value.currentCount ?? 0) + 1
    }
  } catch (e) {
    console.error('加入小组失败', e)
    const msg =
      (e as any)?.response?.data?.message ||
      (e as any)?.response?.data?.msg ||
      '加入失败，请稍后重试'
    showToast(msg)
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  loadUserFromStorage()
  loadTeams()
})
</script>

<template>
  <div class="collaboration">
    <div class="phone-container">
      <!-- 标题栏 -->
      <div class="page-header">
        <div class="placeholder"></div>
        <div class="page-title">协作广场</div>
        <div class="header-action" @click="openCreate">发起小组</div>
      </div>

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
          <div class="btn-row">
            <button
              class="join-btn"
              :disabled="
                joining ||
                (currentTeam.expectedCount &&
                  currentTeam.currentCount &&
                  currentTeam.currentCount >= currentTeam.expectedCount) ||
                currentTeam.status === 2
              "
              @click.stop="handleJoin"
            >
              {{
                joining
                  ? '加入中...'
                  : currentTeam.status === 2 ||
                      (currentTeam.expectedCount &&
                        currentTeam.currentCount &&
                        currentTeam.currentCount >= currentTeam.expectedCount)
                    ? '已满员'
                    : '加入小组'
              }}
            </button>
            <button class="chat-btn" @click.stop="goChat">
              进入小组聊天
            </button>
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
            <div class="form-label">标签（可选，逗号分隔）</div>
            <input v-model="createForm.tagsText" class="form-input" type="text" placeholder="英语,考研,刷题" />
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
.collaboration {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.phone-container {
  width: 100%;
  max-width: 375px;
  margin: 0 auto;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 72px;
}

.page-header {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #f5f7fa;
  position: sticky;
  top: 0;
  z-index: 10;
}

.placeholder {
  width: 24px;
}

.header-action {
  font-size: 14px;
  color: #4a90e2;
  font-weight: 500;
  cursor: pointer;
  width: 70px;
  text-align: right;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  text-align: center;
}

.category-bar {
  display: flex;
  padding: 12px 20px;
  background-color: #ffffff;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid #f5f7fa;
}

.category-bar::-webkit-scrollbar {
  display: none;
}

.category-item {
  padding: 6px 16px;
  font-size: 14px;
  color: #909399;
  background-color: #f5f7fa;
  border-radius: 20px;
  margin-right: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.category-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.team-list {
  padding: 16px 20px;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
}

.team-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
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
  gap: 12px;
}

.join-btn {
  flex: 1;
}

.chat-btn {
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
  width: 100%;
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
</style>

