<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

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

const router = useRouter()
const storedUser = ref<User | null>(null)

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

const goBack = () => {
  router.push('/profile')
}

onMounted(() => {
  loadFromStorage()
})
</script>

<template>
  <div class="profile-info">
    <van-nav-bar title="我的信息" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <van-cell-group inset class="info-card">
        <van-cell title="用户名" :value="storedUser?.username" />
        <van-cell title="姓名" :value="storedUser?.realName || '未填写'" />
        <van-cell title="学号" :value="storedUser?.studentId || '未填写'" />
        <van-cell title="邮箱" :value="storedUser?.email || '未填写'" />
        <van-cell title="手机号" :value="storedUser?.phone || '未填写'" />
      </van-cell-group>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看个人信息" />
    </template>
  </div>
</template>

<style scoped>
.profile-info {
  min-height: 100vh;
  background-color: var(--ls-bg);
}

.info-card {
  margin-top: 16px;
  border-radius: var(--ls-radius-card);
  overflow: hidden;
  box-shadow: var(--ls-shadow-card);
}
</style>

