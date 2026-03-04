<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface User {
  id?: number
  username: string
}

interface Repair {
  id?: number
  classroomId?: number
  title?: string
  description?: string
  status?: number
  type?: number
  priority?: number
  handleRemark?: string
  createTime?: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const repairs = ref<Repair[]>([])
const loading = ref(false)

const repairDetail = ref<Repair | null>(null)
const showRepairDetail = ref(false)

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

const loadRepairs = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await request.get(`/repair/user/${storedUser.value.id}`)
    repairs.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openRepairDetail = async (item: Repair) => {
  if (!item.id) return
  try {
    const res = await request.get(`/repair/${item.id}`)
    repairDetail.value = res.data || item
  } catch {
    repairDetail.value = item
  } finally {
    showRepairDetail.value = true
  }
}

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) {
    await loadRepairs()
  }
})
</script>

<template>
  <div class="profile-repair">
    <van-nav-bar title="我的报修" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <van-empty
        v-if="!loading && repairs.length === 0"
        description="暂无报修记录"
      />
      <van-list v-else :finished="true">
        <div
          v-for="item in repairs"
          :key="item.id"
          class="info-item"
          @click="openRepairDetail(item)"
        >
          <div class="info-item-title">
            {{ item.title }}
          </div>
          <div class="info-item-sub">
            描述：{{ item.description }}
          </div>
        </div>
      </van-list>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看报修记录" />
    </template>

    <van-popup v-model:show="showRepairDetail" round position="bottom" :style="{ height: '60%' }">
      <div class="detail-wrapper" v-if="repairDetail">
        <div class="detail-title">报修详情</div>
        <div class="detail-row">标题：{{ repairDetail.title }}</div>
        <div class="detail-row">描述：{{ repairDetail.description }}</div>
        <div class="detail-row">类型：{{ repairDetail.type }}</div>
        <div class="detail-row">优先级：{{ repairDetail.priority }}</div>
        <div class="detail-row">状态：{{ repairDetail.status }}</div>
        <div class="detail-row">处理备注：{{ repairDetail.handleRemark || '暂无' }}</div>
        <div class="detail-row">创建时间：{{ repairDetail.createTime }}</div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-repair {
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

