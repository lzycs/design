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
const infoTab = ref(0)

const storedUser = ref<User | null>(null)

const reservations = ref<Reservation[]>([])
const repairs = ref<Repair[]>([])
const reviews = ref<Review[]>([])
const teams = ref<TeamRequest[]>([])
const plans = ref<StudyPlan[]>([])

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
    const [resvRes, repairRes, reviewRes, teamRes, planRes] = await Promise.all([
      request.get(`/reservation/user/${userId}`),
      request.get(`/repair/user/${userId}`),
      request.get(`/review/user/${userId}`),
      request.get(`/team/user/${userId}`),
      request.get(`/study-plan/user/${userId}`)
    ])
    reservations.value = resvRes.data ?? []
    repairs.value = repairRes.data ?? []
    reviews.value = reviewRes.data ?? []
    teams.value = teamRes.data ?? []
    plans.value = planRes.data ?? []
  } catch (e) {
    console.error(e)
    showToast('加载个人数据失败，请稍后重试')
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
  reservations.value = []
  repairs.value = []
  reviews.value = []
  teams.value = []
  plans.value = []
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
      <van-cell-group inset>
        <van-cell title="用户名" :value="storedUser?.username" />
        <van-cell title="姓名" :value="storedUser?.realName || '未填写'" />
        <van-cell title="学号" :value="storedUser?.studentId || '未填写'" />
        <van-cell title="邮箱" :value="storedUser?.email || '未填写'" />
        <van-cell title="手机号" :value="storedUser?.phone || '未填写'" />
      </van-cell-group>

      <van-tabs v-model:active="infoTab" class="info-tabs">
        <van-tab title="我的预约">
          <van-empty v-if="!dataLoading && reservations.length === 0" description="暂无预约记录" />
          <van-list v-else :finished="true">
            <van-cell
              v-for="item in reservations"
              :key="item.id"
              :title="`${item.reservationDate || ''} ${item.startTime || ''}-${item.endTime || ''}`"
              :label="item.purpose || '自习/讨论'"
            />
          </van-list>
        </van-tab>
        <van-tab title="我的报修">
          <van-empty v-if="!dataLoading && repairs.length === 0" description="暂无报修记录" />
          <van-list v-else :finished="true">
            <van-cell
              v-for="item in repairs"
              :key="item.id"
              :title="item.title"
              :label="item.description"
            />
          </van-list>
        </van-tab>
        <van-tab title="我的评价">
          <van-empty v-if="!dataLoading && reviews.length === 0" description="暂无评价记录" />
          <van-list v-else :finished="true">
            <van-cell
              v-for="item in reviews"
              :key="item.id"
              :title="`评分: ${item.score ?? '-' }`"
              :label="item.content"
            />
          </van-list>
        </van-tab>
        <van-tab title="我的团队">
          <van-empty v-if="!dataLoading && teams.length === 0" description="暂无团队" />
          <van-list v-else :finished="true">
            <van-cell
              v-for="item in teams"
              :key="item.id"
              :title="item.title"
              :label="`${item.currentCount || 0}/${item.expectedCount || 0} 人 | 标签: ${item.tags || '无'}`"
            />
          </van-list>
        </van-tab>
        <van-tab title="学习计划">
          <van-empty v-if="!dataLoading && plans.length === 0" description="暂无学习计划" />
          <van-list v-else :finished="true">
            <van-cell
              v-for="item in plans"
              :key="item.id"
              :title="item.title"
              :label="`${item.planDate || ''} ${item.startTime || ''}-${item.endTime || ''}`"
            />
          </van-list>
        </van-tab>
      </van-tabs>

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

.info-tabs {
  margin-top: 16px;
}

.form {
  padding: 16px;
}

.form-button {
  margin-top: 16px;
}

.profile-actions {
  padding: 16px;
}
</style>
