<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface User {
  id?: number
  username: string
}

interface StudyPlan {
  id?: number
  title?: string
  description?: string
  planDate?: string
  startTime?: string
  endTime?: string
  status?: number
  createTime?: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const plans = ref<StudyPlan[]>([])
const loading = ref(false)

const planDetail = ref<StudyPlan | null>(null)
const showPlanDetail = ref(false)

const isLoggedIn = computed(() => !!storedUser.value?.id)

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

const loadPlans = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await request.get(`/study-plan/user/${storedUser.value.id}`)
    plans.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openPlanDetail = async (item: StudyPlan) => {
  if (!item.id) return
  try {
    const res = await request.get(`/study-plan/${item.id}`)
    planDetail.value = res.data || item
  } catch {
    planDetail.value = item
  } finally {
    showPlanDetail.value = true
  }
}

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) {
    await loadPlans()
  }
})
</script>

<template>
  <div class="profile-plan">
    <van-nav-bar title="学习计划" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <van-empty
        v-if="!loading && plans.length === 0"
        description="暂无学习计划"
      />
      <van-list v-else :finished="true">
        <div
          v-for="item in plans"
          :key="item.id"
          class="info-item"
          @click="openPlanDetail(item)"
        >
          <div class="info-item-title">
            {{ item.title }}
          </div>
          <div class="info-item-sub">
            时间：{{ item.planDate || '' }} {{ item.startTime || '' }}-{{ item.endTime || '' }}
          </div>
        </div>
      </van-list>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看学习计划" />
    </template>

    <van-popup v-model:show="showPlanDetail" round position="bottom" :style="{ height: '60%' }">
      <div class="detail-wrapper" v-if="planDetail">
        <div class="detail-title">学习计划详情</div>
        <div class="detail-row">标题：{{ planDetail.title }}</div>
        <div class="detail-row">描述：{{ planDetail.description || '暂无' }}</div>
        <div class="detail-row">
          时间：{{ planDetail.planDate }} {{ planDetail.startTime }} - {{ planDetail.endTime }}
        </div>
        <div class="detail-row">状态：{{ planDetail.status }}</div>
        <div class="detail-row">创建时间：{{ planDetail.createTime }}</div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-plan {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.info-item {
  background: #ffffff;
  margin: 12px 16px;
  padding: 12px 14px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(15, 35, 95, 0.04);
}

.info-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
}

.info-item-sub {
  font-size: 12px;
  color: #6b7280;
}

.detail-wrapper {
  padding: 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.detail-row {
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
}
</style>

