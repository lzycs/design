<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getClassroomDetail, type ClassroomDetailVO } from '@/api/classroom'
import { getApprovedClassroomFeedback, type FeedbackItem } from '@/api/feedback'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<ClassroomDetailVO | null>(null)
const approvedFeedbackList = ref<FeedbackItem[]>([])

const classroomId = computed(() => Number(route.params.id))
const classroomName = computed(() => detail.value?.classroom?.name || '教室')
const reviewCount = computed(() => approvedFeedbackList.value.length)

const averageScore = computed(() => {
  if (approvedFeedbackList.value.length === 0) return null
  const sum = approvedFeedbackList.value.reduce((acc, item) => acc + Number(item.averageScore ?? 0), 0)
  return sum / approvedFeedbackList.value.length
})

const envAvgScore = computed(() => {
  if (approvedFeedbackList.value.length === 0) return null
  const sum = approvedFeedbackList.value.reduce((acc, item) => acc + Number(item.envScore ?? 0), 0)
  return sum / approvedFeedbackList.value.length
})

const equipAvgScore = computed(() => {
  if (approvedFeedbackList.value.length === 0) return null
  const sum = approvedFeedbackList.value.reduce((acc, item) => acc + Number(item.equipScore ?? 0), 0)
  return sum / approvedFeedbackList.value.length
})

const progressWidth = (score: number | null) => {
  if (score == null) return '0%'
  return `${Math.max(0, Math.min(100, (score / 5) * 100))}%`
}

const goBack = () => {
  router.back()
}

const loadPageData = async () => {
  if (!classroomId.value) {
    showToast('教室信息不存在')
    router.back()
    return
  }

  loading.value = true
  try {
    const [detailRes, feedbackRes] = await Promise.all([
      getClassroomDetail(classroomId.value),
      getApprovedClassroomFeedback(classroomId.value),
    ])
    detail.value = (detailRes as unknown as { data?: ClassroomDetailVO }).data ?? null
    approvedFeedbackList.value = Array.isArray(feedbackRes.data) ? feedbackRes.data : []
  } catch (e) {
    console.error(e)
    showToast('评价加载失败，请稍后重试')
    approvedFeedbackList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPageData()
})
</script>

<template>
  <div class="reviews-page">
    <van-nav-bar title="全部评价" left-arrow @click-left="goBack" />

    <div class="page-inner">
      <div class="score-card">
        <h2 class="score-title">{{ classroomName }} 评价统计</h2>
        <div class="score-stats">
          <div class="main-score">{{ averageScore != null ? averageScore.toFixed(1) : '暂无' }}</div>
          <div class="score-detail">
            <div class="score-item">
              <span class="score-label">环境</span>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: progressWidth(envAvgScore) }" />
              </div>
              <span class="score-text">{{ envAvgScore != null ? envAvgScore.toFixed(1) : '暂无' }}</span>
            </div>
            <div class="score-item">
              <span class="score-label">设备</span>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: progressWidth(equipAvgScore) }" />
              </div>
              <span class="score-text">{{ equipAvgScore != null ? equipAvgScore.toFixed(1) : '暂无' }}</span>
            </div>
          </div>
        </div>
        <div class="total-reviews">共 {{ reviewCount }} 条评价（仅展示审核通过）</div>
      </div>

      <div v-if="loading" class="empty-wrap">
        <div class="empty-text">正在加载评价...</div>
      </div>

      <div v-else-if="reviewCount === 0" class="empty-wrap">
        <div class="empty-text">暂无评价</div>
      </div>

      <div v-else class="review-list">
        <div v-for="item in approvedFeedbackList" :key="item.id" class="review-card">
          <div class="review-header">
            <div class="review-score">
              <span>综合：{{ item.averageScore?.toFixed(1) ?? '0.0' }}</span>
              <span>环境：{{ item.envScore.toFixed(1) }}</span>
              <span>设备：{{ item.equipScore.toFixed(1) }}</span>
            </div>
            <div class="review-time">{{ item.updatedAt || item.createdAt }}</div>
          </div>
          <div class="review-content">{{ item.content || '该用户未填写文字评价。' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reviews-page {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-inner {
  padding: 16px;
}

.score-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.score-title {
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 14px;
}

.score-stats {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.main-score {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-right: 16px;
  min-width: 64px;
}

.score-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #666;
}

.score-label {
  width: 36px;
}

.score-bar {
  flex: 1;
  height: 6px;
  background-color: #f0f0f0;
  border-radius: 3px;
  margin: 0 8px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  background-color: #4a90e2;
}

.score-text {
  color: #333;
  min-width: 28px;
  text-align: right;
}

.total-reviews {
  font-size: 13px;
  color: #8a94a6;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.review-score {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #4f5d75;
}

.review-time {
  font-size: 12px;
  color: #8a94a6;
  white-space: nowrap;
}

.review-content {
  font-size: 14px;
  color: #2d3648;
  line-height: 1.6;
}

.empty-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 40px 16px;
}

.empty-text {
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
