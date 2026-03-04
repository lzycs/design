<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface User {
  id?: number
  username: string
}

interface Review {
  id?: number
  classroomId?: number
  reservationId?: number
  score?: number
  content?: string
  tags?: string
  status?: number
  createTime?: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const reviews = ref<Review[]>([])
const loading = ref(false)

const reviewDetail = ref<Review | null>(null)
const showReviewDetail = ref(false)

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

const loadReviews = async () => {
  if (!storedUser.value?.id) return
  loading.value = true
  try {
    const res = await request.get(`/review/user/${storedUser.value.id}`)
    reviews.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openReviewDetail = async (item: Review) => {
  if (!item.id) return
  try {
    const res = await request.get(`/review/${item.id}`)
    reviewDetail.value = res.data || item
  } catch {
    reviewDetail.value = item
  } finally {
    showReviewDetail.value = true
  }
}

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) {
    await loadReviews()
  }
})
</script>

<template>
  <div class="profile-review">
    <van-nav-bar title="我的评价" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <van-empty
        v-if="!loading && reviews.length === 0"
        description="暂无评价记录"
      />
      <van-list v-else :finished="true">
        <div
          v-for="item in reviews"
          :key="item.id"
          class="info-item"
          @click="openReviewDetail(item)"
        >
          <div class="info-item-title">
            评分：{{ item.score ?? '-' }}
          </div>
          <div class="info-item-sub">
            内容：{{ item.content || '暂无内容' }}
          </div>
        </div>
      </van-list>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看评价记录" />
    </template>

    <van-popup v-model:show="showReviewDetail" round position="bottom" :style="{ height: '60%' }">
      <div class="detail-wrapper" v-if="reviewDetail">
        <div class="detail-title">评价详情</div>
        <div class="detail-row">评分：{{ reviewDetail.score }}</div>
        <div class="detail-row">标签：{{ reviewDetail.tags || '无' }}</div>
        <div class="detail-row">内容：{{ reviewDetail.content || '暂无内容' }}</div>
        <div class="detail-row">状态：{{ reviewDetail.status }}</div>
        <div class="detail-row">创建时间：{{ reviewDetail.createTime }}</div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-review {
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

