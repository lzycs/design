<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'

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
    await Promise.all([
      request.get(`/reservation/user/${userId}`),
      request.get(`/repair/user/${userId}`),
      request.get(`/review/user/${userId}`),
      request.get(`/team/user/${userId}`),
      request.get(`/study-plan/user/${userId}`)
    ])
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
  }
})
</script>

<template>
  <div class="profile">
    <van-nav-bar title="个人中心" />

    <template v-if="isLoggedIn">
      <div class="profile-main">
        <div class="profile-card">
          <div
            class="menu-item menu-item-info"
            @click="$router.push('/profile/info')"
          >
            <div class="menu-icon-dot" />
            <div class="menu-text">我的信息</div>
          </div>

          <div
            class="menu-item"
            @click="$router.push('/profile/reservations')"
          >
            <van-icon name="calendar-o" class="menu-icon" />
            <div class="menu-text">我的预约</div>
          </div>

          <div
            class="menu-item"
            @click="$router.push('/profile/repairs')"
          >
            <van-icon name="todo-list-o" class="menu-icon" />
            <div class="menu-text">我的报修</div>
          </div>

          <div
            class="menu-item"
            @click="$router.push('/profile/reviews')"
          >
            <van-icon name="star-o" class="menu-icon" />
            <div class="menu-text">我的评价</div>
          </div>

          <div
            class="menu-item"
            @click="$router.push('/profile/teams')"
          >
            <van-icon name="friends-o" class="menu-icon" />
            <div class="menu-text">我的团队</div>
          </div>

          <div
            class="menu-item"
            @click="$router.push('/profile/plans')"
          >
            <van-icon name="cluster-o" class="menu-icon" />
            <div class="menu-text">学习计划</div>
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
</template>

<style scoped>
.profile {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.profile-main {
  padding: 16px;
}

.profile-card {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 35, 95, 0.06);
  overflow: hidden;
  margin-bottom: 16px;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  font-size: 14px;
  color: #1f2933;
  border-bottom: 1px solid #f0f2f5;
  background: #ffffff;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item.active {
  background: linear-gradient(90deg, #f5f9ff, #ffffff);
}

.menu-icon-dot {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #e5efff;
  margin-right: 10px;
}

.menu-icon {
  font-size: 18px;
  color: #4b7cff;
  margin-right: 10px;
}

.menu-text {
  flex: 1;
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
