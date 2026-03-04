<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface User {
  id?: number
  username: string
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

const router = useRouter()
const storedUser = ref<User | null>(null)
const teams = ref<TeamRequest[]>([])
const loading = ref(false)

const teamDetail = ref<TeamRequest | null>(null)
const showTeamDetail = ref(false)

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

const loadTeams = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await request.get(`/team/user/${storedUser.value.id}`)
    teams.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openTeamDetail = async (item: TeamRequest) => {
  if (!item.id) return
  try {
    const res = await request.get(`/team/request/${item.id}`)
    teamDetail.value = res.data || item
  } catch {
    teamDetail.value = item
  } finally {
    showTeamDetail.value = true
  }
}

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) {
    await loadTeams()
  }
})
</script>

<template>
  <div class="profile-team">
    <van-nav-bar title="我的团队" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <van-empty
        v-if="!loading && teams.length === 0"
        description="暂无团队"
      />
      <van-list v-else :finished="true">
        <div
          v-for="item in teams"
          :key="item.id"
          class="info-item"
          @click="openTeamDetail(item)"
        >
          <div class="info-item-title">
            {{ item.title }}
          </div>
          <div class="info-item-sub">
            人数：{{ item.currentCount || 0 }}/{{ item.expectedCount || 0 }}｜标签：{{ item.tags || '无' }}
          </div>
        </div>
      </van-list>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看团队信息" />
    </template>

    <van-popup v-model:show="showTeamDetail" round position="bottom" :style="{ height: '60%' }">
      <div class="detail-wrapper" v-if="teamDetail">
        <div class="detail-title">团队详情</div>
        <div class="detail-row">标题：{{ teamDetail.title }}</div>
        <div class="detail-row">描述：{{ teamDetail.description }}</div>
        <div class="detail-row">
          人数：{{ teamDetail.currentCount || 0 }}/{{ teamDetail.expectedCount || 0 }}
        </div>
        <div class="detail-row">标签：{{ teamDetail.tags || '无' }}</div>
        <div class="detail-row">状态：{{ teamDetail.status }}</div>
        <div class="detail-row">创建时间：{{ teamDetail.createTime }}</div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-team {
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

