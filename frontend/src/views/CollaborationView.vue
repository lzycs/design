<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getActiveTeamRequests,
  joinTeam,
  type TeamRequest,
  getTeamMembers,
  type TeamMember,
} from '@/api/team'

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
const teams = ref<TeamRequest[]>([])
const loading = ref(false)

const showDetail = ref(false)
const currentTeam = ref<TeamRequest | null>(null)
const joining = ref(false)
const members = ref<TeamMember[]>([])

const storedUser = ref<{ id: number; username?: string } | null>(null)

const isLoggedIn = computed(() => !!storedUser.value?.id)

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

const parseTags = (item: TeamRequest): string[] => {
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

const getCategoryForTeam = (item: TeamRequest): CategoryKey => {
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

const loadTeams = async () => {
  loading.value = true
  try {
    const res = await getActiveTeamRequests()
    const list = res.data ?? []
    const map = new Map<string, TeamRequest>()
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

const openDetail = (item: TeamRequest) => {
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
    window.alert('加入小组成功！')
    if (currentTeam.value) {
      currentTeam.value.currentCount =
        (currentTeam.value.currentCount ?? 0) + 1
    }
  } catch (e) {
    console.error('加入小组失败', e)
    window.alert('加入失败，请稍后重试')
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
        <div class="placeholder"></div>
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
            <div class="avatar"></div>
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
            <div class="tag">
              人数：{{ item.currentCount ?? 1 }}/{{ item.expectedCount ?? '-' }}
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
          <div class="btn-row">
            <button
              class="join-btn"
              :disabled="joining"
              @click.stop="handleJoin"
            >
              {{ joining ? '加入中...' : '加入小组' }}
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
              {{ members[0]?.userId ? `用户 ${members[0]?.userId}` : '组长' }}
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
                {{ m.userId ? `用户 ${m.userId}` : '成员' }}
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

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #e5e6eb;
  margin-right: 12px;
  flex-shrink: 0;
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
</style>

