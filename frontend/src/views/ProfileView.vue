<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'
import { getTeamBadge } from '@/api/team'
import { useTeamChatStore } from '@/stores/teamChat'

interface User {
  id?: number
  username: string
  password?: string
  realName?: string
  studentId?: string
  email?: string
  phone?: string
  role?: number
}

interface Reservation {
  id?: number
  classroomId?: number
  resourceType?: number
  reservationDate?: string
  startTime?: string
  endTime?: string
  purpose?: string
  status?: number
}

interface Repair {
  id?: number
  classroomId?: number
  title?: string
  description?: string
  status?: number
  createTime?: string
}

interface Review {
  id?: number
  classroomId?: number
  reservationId?: number
  score?: number
  content?: string
  createTime?: string
}

interface TeamRequest {
  id?: number
  title?: string
  description?: string
  tags?: string
  expectedCount?: number
  currentCount?: number
  status?: number
  createTime?: string
}

interface StudyPlan {
  id?: number
  title?: string
  planDate?: string
  startTime?: string
  endTime?: string
  status?: number
}

const activeTab = ref<'login' | 'register'>('login')
const router = useRouter()
const teamChatStore = useTeamChatStore()

const loading = ref(false)
const dataLoading = ref(false)

const storedUser = ref<User | null>(null)

const reservations = ref<Reservation[]>([])
const repairs = ref<Repair[]>([])
const reviews = ref<Review[]>([])
const teams = ref<TeamRequest[]>([])
const studyPlans = ref<StudyPlan[]>([])

const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  realName: '',
  studentId: '',
  email: '',
  phone: ''
})

const isLoggedIn = computed(() => !!storedUser.value)
const teamBadgeTotal = ref(0)

const loadTeamBadge = async () => {
  if (!storedUser.value?.id) return
  try {
    const res = await getTeamBadge(storedUser.value.id)
    const d = (res as unknown as { data?: { total: number } }).data
    teamBadgeTotal.value = d?.total ?? 0
  } catch {
    teamBadgeTotal.value = 0
  }
}

const loadTeamChatUnread = async () => {
  if (!storedUser.value?.id) return
  await teamChatStore.refreshUnreadSummary(storedUser.value.id)
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

const saveToStorage = (user: User) => {
  storedUser.value = user
  localStorage.setItem('currentUser', JSON.stringify(user))
}

const loadUserData = async () => {
  if (!storedUser.value?.id) return
  dataLoading.value = true
  try {
    const userId = storedUser.value.id
    const [reservationRes, repairRes, reviewRes, teamRes, planRes] = await Promise.all([
      request.get(`/reservation/user/${userId}`),
      request.get(`/repair/user/${userId}`),
      request.get(`/review/user/${userId}`),
      request.get(`/team/user/${userId}`),
      request.get(`/study-plan/user/${userId}`)
    ])
    reservations.value = reservationRes.data ?? []
    repairs.value = repairRes.data ?? []
    reviews.value = reviewRes.data ?? []
    teams.value = teamRes.data ?? []
    studyPlans.value = planRes.data ?? []
  } catch (e) {
    console.error(e)
  } finally {
    dataLoading.value = false
  }
}

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    showToast('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post<unknown, { code: number; message?: string; data: User }>('/user/login', loginForm.value)
    if (res.code !== 200) {
      showToast(res.message || '登录失败')
      return
    }
    const user = res.data as User
    // 管理员角色（3=超级管理员, 4=维修人员, 5=评论审核员）自动跳转管理端
    if (user.role === 3 || user.role === 4 || user.role === 5) {
      showToast('检测到管理员账号，正在跳转管理端...')
      await router.push('/admin/login')
      return
    }
    saveToStorage(user)
    showToast('登录成功')
    await loadUserData()
  } catch (e) {
    console.error(e)
    showToast('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    showToast('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post<unknown, { code: number; message?: string; data: User }>('/user/register', registerForm.value)
    if (res.code !== 200) {
      showToast(res.message || '注册失败')
      return
    }
    saveToStorage(res.data)
    showToast('注册并登录成功')
    activeTab.value = 'login'
    await loadUserData()
  } catch (e) {
    console.error(e)
    showToast('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleLogout = async () => {
  const confirm = await showConfirmDialog({
    title: '退出登录',
    message: '确认要退出当前账号吗？'
  }).catch(() => false)
  if (!confirm) return
  storedUser.value = null
  localStorage.removeItem('currentUser')
  showToast('已退出登录')
}

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) {
    await loadUserData()
    await loadTeamBadge()
    await loadTeamChatUnread()
  }
})
</script>

<template>
  <div class="profile">
    <div class="my-page">
      <template v-if="isLoggedIn">
        <div class="page-wrap">
          <!-- 移动端用户信息卡片 -->
          <div class="mobile-user-card">
            <div class="avatar">
              <van-icon name="user-circle-o" />
            </div>
            <div class="user-meta">
              <div class="user-title">{{ storedUser?.realName || storedUser?.username }}</div>
              <div class="user-sub">学号/工号：{{ storedUser?.studentId || '未填写' }}</div>
            </div>
            <button class="setting-btn" type="button" @click="$router.push('/profile/info')">
              <van-icon name="notes-o" />
            </button>
          </div>

          <!-- 功能菜单（置顶） -->
          <div class="card menu-card top-card">
            <div class="card-hd with-divider">功能菜单</div>
            <div class="menu-list">
              <button class="menu-row" type="button" @click="$router.push('/profile/reservations')">
                <div class="menu-left">
                  <div class="mini-icon"><van-icon name="calendar-o" /></div>
                  <span>我的预约</span>
                </div>
                <van-icon name="arrow" class="arrow" />
              </button>
              <button class="menu-row" type="button" @click="$router.push('/profile/my-courses')">
                <div class="menu-left">
                  <div class="mini-icon"><van-icon name="todo-list-o" /></div>
                  <span>我的课程表</span>
                </div>
                <van-icon name="arrow" class="arrow" />
              </button>
              <button
                class="menu-row"
                type="button"
                @click="$router.push({ path: '/feedback', query: { tab: 'evaluation', only: '1' } })"
              >
                <div class="menu-left">
                  <div class="mini-icon"><van-icon name="star-o" /></div>
                  <span>我的评价</span>
                </div>
                <van-icon name="arrow" class="arrow" />
              </button>
              <button class="menu-row" type="button" @click="$router.push('/profile/repairs')">
                <div class="menu-left">
                  <div class="mini-icon"><van-icon name="setting-o" /></div>
                  <span>我的报修</span>
                </div>
                <van-icon name="arrow" class="arrow" />
              </button>
            </div>
          </div>

          <!-- 功能卡片区域 -->
          <div class="cards-grid">
            <!-- 更多服务（与“我的数据”并列） -->
            <div class="card more-card">
              <div class="card-hd with-divider">更多服务</div>
              <div class="menu-list">
                <button class="menu-row" type="button" @click="$router.push('/profile/resources')">
                  <div class="menu-left">
                    <div class="mini-icon"><van-icon name="shop-o" /></div>
                    <span>我的资源</span>
                  </div>
                  <van-icon name="arrow" class="arrow" />
                </button>
                <button class="menu-row" type="button" @click="$router.push('/profile/teams')">
                  <div class="menu-left">
                    <div class="mini-icon"><van-icon name="friends-o" /></div>
                    <span>我的协作</span>
                    <span v-if="teamBadgeTotal > 0" class="pill-badge">{{ teamBadgeTotal }}</span>
                  </div>
                  <van-icon name="arrow" class="arrow" />
                </button>
                <button class="menu-row" type="button" @click="$router.push('/profile/team-messages')">
                  <div class="menu-left">
                    <div class="mini-icon"><van-icon name="chat-o" /></div>
                    <span>小组消息</span>
                    <span v-if="teamChatStore.totalUnread > 0" class="pill-badge">
                      {{ teamChatStore.totalUnread > 99 ? '99+' : teamChatStore.totalUnread }}
                    </span>
                  </div>
                  <van-icon name="arrow" class="arrow" />
                </button>
              </div>
            </div>

            <!-- 我的数据 -->
            <div class="card data-card">
              <div class="card-hd">我的数据</div>
              <div class="data-list">
                <div class="data-row">
                  <div class="data-left">
                    <div class="data-icon primary">
                      <van-icon name="calendar-o" />
                    </div>
                    <div>
                      <div class="data-label">已预约</div>
                      <div class="data-value">{{ reservations.length }}</div>
                    </div>
                  </div>
                </div>
                <div class="data-row">
                  <div class="data-left">
                    <div class="data-icon success">
                      <van-icon name="passed" />
                    </div>
                    <div>
                      <div class="data-label">已完成</div>
                      <div class="data-value">
                        {{ reservations.filter((r) => r.status === 3 || r.status === 2).length }}
                      </div>
                    </div>
                  </div>
                </div>
                <div class="data-row">
                  <div class="data-left">
                    <div class="data-icon secondary">
                      <van-icon name="star-o" />
                    </div>
                    <div>
                      <div class="data-label">已评价</div>
                      <div class="data-value">{{ reviews.length }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <button class="logout-btn" type="button" @click="handleLogout">退出登录</button>
        </div>
      </template>

      <template v-else>
        <van-tabs v-model:active="activeTab">
          <van-tab title="登录" name="login">
            <div class="form">
              <van-field v-model="loginForm.username" label="用户名" placeholder="请输入用户名" />
              <van-field
                v-model="loginForm.password"
                label="密码"
                type="password"
                placeholder="请输入密码"
              />
              <van-button
                type="primary"
                block
                class="form-button"
                :loading="loading"
                @click="handleLogin"
              >
                登录
              </van-button>
              <div class="admin-entry" @click="$router.push('/admin/login')">
                管理员入口 →
              </div>
            </div>
          </van-tab>
          <van-tab title="注册" name="register">
            <div class="form">
              <van-field v-model="registerForm.username" label="用户名" placeholder="请输入用户名" />
              <van-field
                v-model="registerForm.password"
                label="密码"
                type="password"
                placeholder="请输入密码"
              />
              <van-field v-model="registerForm.realName" label="姓名" placeholder="请输入姓名" />
              <van-field v-model="registerForm.studentId" label="学号" placeholder="请输入学号" />
              <van-field v-model="registerForm.email" label="邮箱" placeholder="请输入邮箱" />
              <van-field v-model="registerForm.phone" label="手机号" placeholder="请输入手机号" />
              <van-button
                type="primary"
                block
                class="form-button"
                :loading="loading"
                @click="handleRegister"
              >
                注册并登录
              </van-button>
            </div>
          </van-tab>
        </van-tabs>
      </template>
    </div>
  </div>
</template>

<style scoped>
.profile {
  background-color: var(--ls-bg);
  min-height: 100vh;
}

.my-page {
  width: 100%;
  min-height: 100vh;
  background-color: var(--ls-bg);
  padding-bottom: 72px;
}

.page-wrap {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--ls-space-5);
}

.mobile-user-card {
  background: var(--ls-surface);
  border-radius: var(--ls-radius-card);
  box-shadow: var(--ls-shadow-card);
  padding: var(--ls-space-5);
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: var(--ls-space-5);
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: var(--ls-radius-pill);
  background: color-mix(in srgb, var(--ls-primary) 10%, transparent);
  color: var(--ls-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.avatar :deep(.van-icon) {
  font-size: 30px;
}

.user-meta {
  min-width: 0;
}
.user-title {
  font-size: var(--ls-font-size-title);
  font-weight: var(--ls-font-weight-bold);
  color: var(--ls-text-strong);
  line-height: 1.2;
}
.user-sub {
  margin-top: var(--ls-space-1);
  font-size: var(--ls-font-size-sm);
  color: var(--ls-text-muted);
}

.setting-btn {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border-radius: var(--ls-radius-pill);
  border: none;
  background: var(--ls-divider);
  color: var(--ls-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
}
.setting-btn :deep(.van-icon) {
  font-size: 16px;
}

.cards-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--ls-space-4);
  margin-bottom: var(--ls-space-5);
}

.top-card {
  margin-bottom: var(--ls-space-5);
}

.card {
  background: var(--ls-surface);
  border-radius: var(--ls-radius-card);
  box-shadow: var(--ls-shadow-card);
  overflow: hidden;
}
.card-hd {
  padding: var(--ls-space-4) var(--ls-space-5);
  font-size: var(--ls-font-size-section);
  font-weight: var(--ls-font-weight-semibold);
  color: var(--ls-text-muted);
}
.card-hd.with-divider {
  border-bottom: 1px solid var(--ls-divider);
}

.data-card .data-list {
  padding: 0 var(--ls-space-5) var(--ls-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--ls-space-3);
}
.data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.data-left {
  display: flex;
  align-items: center;
  gap: var(--ls-space-2);
}
.data-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ls-radius-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.data-icon.primary {
  background: color-mix(in srgb, var(--ls-primary) 12%, transparent);
  color: var(--ls-primary);
}
.data-icon.success {
  background: color-mix(in srgb, var(--ls-success) 12%, transparent);
  color: var(--ls-success);
}
.data-icon.secondary {
  background: color-mix(in srgb, var(--ls-primary) 18%, transparent);
  color: var(--ls-primary);
}
.data-icon :deep(.van-icon) {
  font-size: 18px;
}
.data-label {
  font-size: var(--ls-font-size-sm);
  color: var(--ls-text-muted);
}
.data-value {
  margin-top: 2px;
  font-size: var(--ls-font-size-title);
  font-weight: var(--ls-font-weight-bold);
  color: var(--ls-text);
}

.menu-list {
  display: flex;
  flex-direction: column;
}
.menu-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ls-space-4) var(--ls-space-5);
  border: none;
  background: transparent;
  border-top: 1px solid var(--ls-divider);
  color: var(--ls-text-strong);
  font-size: var(--ls-font-size-base);
  font-weight: var(--ls-font-weight-semibold);
}
.menu-row:first-of-type {
  border-top: none;
}
.menu-row:active {
  background: var(--ls-surface-muted);
}
.menu-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.mini-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ls-radius-soft);
  background: color-mix(in srgb, var(--ls-primary) 12%, transparent);
  color: var(--ls-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.mini-icon :deep(.van-icon) {
  font-size: 16px;
}
.arrow {
  color: var(--ls-text-faint);
}
.pill-badge {
  margin-left: 6px;
  width: 20px;
  height: 20px;
  border-radius: var(--ls-radius-pill);
  background: var(--ls-danger);
  color: #fff;
  font-size: var(--ls-font-size-xs);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--ls-font-weight-bold);
}

.logout-btn {
  width: 100%;
  margin-top: var(--ls-space-4);
  padding: var(--ls-space-3) var(--ls-space-4);
  border-radius: var(--ls-radius-control);
  border: 1px solid var(--ls-border);
  background: var(--ls-surface);
  color: var(--ls-danger);
  font-weight: var(--ls-font-weight-bold);
  font-size: var(--ls-font-size-base);
}
.logout-btn:active {
  background: color-mix(in srgb, var(--ls-danger) 6%, transparent);
}

.form {
  padding: var(--ls-space-5);
}

.form-button {
  margin-top: var(--ls-space-5);
}

.admin-entry {
  margin-top: var(--ls-space-5);
  text-align: center;
  font-size: var(--ls-font-size-section);
  color: var(--ls-primary);
  cursor: pointer;
  padding: 8px 0;
}

@media (min-width: 768px) {
  .page-wrap {
    padding: 18px 12px;
  }
  .cards-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (min-width: 1024px) {
  .page-wrap {
    padding: 22px 16px;
  }
}
</style>
