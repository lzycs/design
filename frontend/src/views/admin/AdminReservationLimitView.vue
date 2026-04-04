<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getAdminReservationLimits,
  updateAdminReservationLimits,
} from '@/api/admin'

const router = useRouter()

const maxPerWeek = ref<string>('4')
const maxDurationMinutes = ref<string>('240')
const loading = ref(false)
const saving = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminReservationLimits()
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '加载失败')
      return
    }
    maxPerWeek.value = String(res.data.maxPerWeek)
    maxDurationMinutes.value = String(res.data.maxDurationMinutes)
  } catch (e: unknown) {
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '加载失败，请确认已登录且有权限')
    if (ax.response?.data?.message?.includes('登录')) {
      await router.replace('/admin/login')
    }
  } finally {
    loading.value = false
  }
}

const save = async () => {
  const mw = Number(maxPerWeek.value)
  const md = Number(maxDurationMinutes.value)
  if (!Number.isFinite(mw) || mw < 1) {
    showToast('每周预约次数须为不小于 1 的整数')
    return
  }
  if (!Number.isFinite(md) || md < 15) {
    showToast('单次最长时长至少 15 分钟')
    return
  }
  saving.value = true
  try {
    const res = await updateAdminReservationLimits({
      maxPerWeek: Math.floor(mw),
      maxDurationMinutes: Math.floor(md),
    })
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '保存失败')
      return
    }
    showToast('已保存')
    maxPerWeek.value = String(res.data.maxPerWeek)
    maxDurationMinutes.value = String(res.data.maxDurationMinutes)
  } catch (e: unknown) {
    const ax = e as { response?: { data?: { message?: string } } }
    showToast(ax.response?.data?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div class="header-side">
        <van-icon name="arrow-left" class="back-icon" @click="router.back()" />
      </div>
      <div class="page-header-title">预约上限</div>
      <div style="width: 24px"></div>
    </div>

    <div class="content-area">
      <div class="admin-card" v-if="!loading">
        <p class="hint">
          对用户新建预约生效：统计「待签到 / 已签到 / 已完成」；每周按自然周（周一至周日）按预约日期计数；取消、违约不计入本周次数。
        </p>
        <van-field
          v-model="maxPerWeek"
          type="digit"
          label="每人每周最多"
          placeholder="如 4"
        >
          <template #extra>次</template>
        </van-field>
        <van-field
          v-model="maxDurationMinutes"
          type="digit"
          label="单次最长"
          placeholder="如 240（4小时）"
        >
          <template #extra>分钟</template>
        </van-field>
        <van-button
          type="primary"
          block
          round
          :loading="saving"
          style="margin-top: 16px"
          @click="save"
        >
          保存
        </van-button>
      </div>
      <div v-else class="admin-card" style="text-align: center; padding: 32px; color: #909399">
        加载中…
      </div>
    </div>
  </div>
</template>

<style scoped>
.header-side {
  width: 24px;
  display: flex;
  align-items: center;
}

.back-icon {
  font-size: 20px;
  cursor: pointer;
}

.hint {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin: 0 0 12px;
}
</style>
