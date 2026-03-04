<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface User {
  id?: number
  username: string
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
  createTime?: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const reservations = ref<Reservation[]>([])
const loading = ref(false)

const reservationDetail = ref<Reservation | null>(null)
const showReservationDetail = ref(false)

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

const loadReservations = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await request.get(`/reservation/user/${storedUser.value.id}`)
    reservations.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openReservationDetail = async (item: Reservation) => {
  if (!item.id) return
  try {
    const res = await request.get(`/reservation/${item.id}`)
    reservationDetail.value = res.data || item
  } catch {
    reservationDetail.value = item
  } finally {
    showReservationDetail.value = true
  }
}

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) {
    await loadReservations()
  }
})
</script>

<template>
  <div class="profile-reservation">
    <van-nav-bar title="我的预约" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <van-empty
        v-if="!loading && reservations.length === 0"
        description="暂无预约记录"
      />
      <van-list v-else :finished="true">
        <div
          v-for="item in reservations"
          :key="item.id"
          class="info-item"
          @click="openReservationDetail(item)"
        >
          <div class="info-item-title">
            {{ item.reservationDate || '' }} {{ item.startTime || '' }}-{{ item.endTime || '' }}
          </div>
          <div class="info-item-sub">
            用途：{{ item.purpose || '自习/讨论' }}
          </div>
        </div>
      </van-list>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看预约记录" />
    </template>

    <van-popup v-model:show="showReservationDetail" round position="bottom" :style="{ height: '60%' }">
      <div class="detail-wrapper" v-if="reservationDetail">
        <div class="detail-title">预约详情</div>
        <div class="detail-row">日期：{{ reservationDetail.reservationDate }}</div>
        <div class="detail-row">
          时间：{{ reservationDetail.startTime }} - {{ reservationDetail.endTime }}
        </div>
        <div class="detail-row">用途：{{ reservationDetail.purpose || '自习/讨论' }}</div>
        <div class="detail-row">状态：{{ reservationDetail.status }}</div>
        <div class="detail-row">创建时间：{{ reservationDetail.createTime }}</div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-reservation {
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

