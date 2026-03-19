<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'
import { getTeamBadge } from '@/api/team'

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
    const res = await request.post('/user/login', loginForm.value)
    if (res.code !== 200) {
      showToast(res.message || '登录失败')
      return
    }
    saveToStorage(res.data)
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
    const res = await request.post('/user/register', registerForm.value)
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
  }
})
</script>

<template>
  <div class="profile">
    <div class="my-page">
      <template v-if="isLoggedIn">
        <div class="user-header">
          <div class="user-info">
            <div class="user-name">{{ storedUser?.realName || storedUser?.username }}</div>
            <div class="user-id">
              学号/工号：{{ storedUser?.studentId || '未填写' }}
            </div>
          </div>
        </div>

        <div class="function-menu">
          <div class="menu-group">
            <div
              class="menu-item"
              @click="$router.push('/profile/reservations')"
            >
              <div class="menu-left">
                <div class="menu-icon">
                  <van-icon name="calendar-o" />
                </div>
                <div class="menu-text">我的预约</div>
              </div>
              <div class="menu-right">
                <van-icon name="arrow" class="menu-arrow" />
              </div>
            </div>

            <div
              class="menu-item"
              @click="$router.push({ path: '/feedback', query: { tab: 'evaluation', only: '1' } })"
            >
              <div class="menu-left">
                <div class="menu-icon">
                  <van-icon name="star-o" />
                </div>
                <div class="menu-text">我的评价</div>
              </div>
              <div class="menu-right">
                <van-icon name="arrow" class="menu-arrow" />
              </div>
            </div>

            <div
              class="menu-item"
              @click="$router.push('/profile/repairs')"
            >
              <div class="menu-left">
                <div class="menu-icon">
                  <van-icon name="setting-o" />
                </div>
                <div class="menu-text">我的报修</div>
              </div>
              <div class="menu-right">
                <van-icon name="arrow" class="menu-arrow" />
              </div>
            </div>

            <div
              class="menu-item"
              @click="$router.push('/profile/teams')"
            >
              <div class="menu-left">
                <div class="menu-icon">
                  <van-icon name="friends-o" />
                </div>
                <div class="menu-text">我的协作</div>
              </div>
              <div class="menu-right">
                <div v-if="teamBadgeTotal > 0" class="menu-badge">{{ teamBadgeTotal }}</div>
                <van-icon name="arrow" class="menu-arrow" />
              </div>
            </div>
          </div>
        </div>

        <div class="profile-actions">
          <van-button type="primary" block @click="handleLogout">退出登录</van-button>
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
  background-color: #f5f5f5;
  min-height: 100vh;
}

.my-page {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 72px;
}

.user-header {
  width: 100%;
  height: 160px;
  background: linear-gradient(135deg, #4a90e2 0%, #5c6bc0 100%);
  border-radius: 0 0 24px 24px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  color: #ffffff;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.user-id {
  font-size: 12px;
  opacity: 0.8;
}

.function-menu {
  padding: 20px;
}

.menu-group {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f7fa;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
}

.menu-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background-color: #ecf5ff;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 12px;
}

.menu-icon :deep(.van-icon) {
  font-size: 16px;
  color: #4a90e2;
}

.menu-text {
  font-size: 16px;
  color: #1a1a1a;
  font-weight: 500;
}

.menu-right {
  display: flex;
  align-items: center;
}

.menu-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #f56c6c;
  color: #ffffff;
  font-size: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 8px;
}

.menu-arrow :deep(.van-icon) {
  font-size: 16px;
  color: #909399;
}

.form {
  padding: 16px;
}

.form-button {
  margin-top: 16px;
}

.profile-actions {
  padding: 0 16px 24px;
}
</style>
