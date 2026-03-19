<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { adminLogin, type AdminLoginRequest } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const loginForm = ref<AdminLoginRequest>({
  username: '',
  password: '',
})

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    showToast('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await adminLogin(loginForm.value)
    if (res.code !== 200) {
      showToast(res.message || '登录失败')
      return
    }
    localStorage.setItem('adminToken', res.data.token)
    showToast('登录成功')
    await router.push('/admin')
  } catch (e) {
    console.error(e)
    showToast('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mobile-admin-container">
    <div class="page-header">
      <div style="width: 24px"></div>
      <div class="page-header-title">管理员后台登录</div>
      <div style="width: 24px"></div>
    </div>

    <div class="content-area">
      <div class="admin-card form-card">
        <label class="form-label">用户名</label>
        <input v-model="loginForm.username" class="form-input" placeholder="请输入管理员账号" />

        <label class="form-label">密码</label>
        <input
          v-model="loginForm.password"
          class="form-input"
          placeholder="请输入管理员密码"
          type="password"
        />

        <button class="submit-btn" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="empty-desc" style="margin-top: 12px">
          测试账号：`admin/123456`、`zhangshifu/123456`、`commentadmin/123456`
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* 主要视觉样式参考 UIDesign/admin.html */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
.mobile-admin-container {
  width: 100%;
  height: 100vh;
  background-color: #ffffff;
  overflow: hidden;
  position: relative;
}
.page-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #4a90e2;
  color: #ffffff;
}
.page-header-title {
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  text-align: center;
}
.content-area {
  padding: 12px 20px;
  overflow-y: auto;
  height: calc(100vh - 60px);
}
.admin-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
}
.form-card {
  padding: 20px;
}
.form-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}
.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 16px;
  outline: none;
}
.form-input:focus {
  border-color: #4a90e2;
}
.submit-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  background-color: #4a90e2;
  color: #ffffff;
  border: none;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
}
.empty-desc {
  font-size: 14px;
  color: #909399;
}
</style>

