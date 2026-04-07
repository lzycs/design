<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { auditAdminReview, getAdminReviews, type AdminReviewVO } from '@/api/admin'

const keyword = ref('')
const activeStatus = ref<number | null>(0)

const reviews = ref<AdminReviewVO[]>([])
const loading = ref(false)

const statusFilters: { label: string; value: number | null }[] = [
  { label: '全部', value: null },
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已驳回', value: 2 },
]

const scoreText = (score: number | null | undefined) => {
  if (score == null) return '暂无评分'
  return `${score}星综合评分`
}

const loadReviews = async () => {
  loading.value = true
  try {
    const res = await getAdminReviews({
      status: activeStatus.value === null ? undefined : activeStatus.value,
      keyword: keyword.value || undefined,
    })
    if (res.code !== 200) {
      showToast(res.message || '加载失败')
      return
    }
    reviews.value = res.data
  } catch (e) {
    console.error(e)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const tagClass = (status: number | null | undefined) => {
  switch (status) {
    case 0:
      return 'tag-pending'
    case 1:
      return 'tag-passed'
    case 2:
      return 'tag-rejected'
    default:
      return ''
  }
}

const statusLabel = (status: number | null | undefined) => {
  switch (status) {
    case 0:
      return '待审核'
    case 1:
      return '已通过'
    case 2:
      return '已驳回'
    default:
      return '未知'
  }
}

const handleAudit = async (review: AdminReviewVO, approve: boolean) => {
  const remark = window.prompt('审核备注（可选）', '')
  try {
    const res = await auditAdminReview(review.id, { approve, remark: remark || '' })
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '审核失败')
      return
    }
    showToast('审核成功')
    await loadReviews()
  } catch (e) {
    console.error(e)
    showToast('审核失败')
  }
}

onMounted(loadReviews)
</script>

<template>
  <div class="admin-page active">
    <div class="page-header">
      <div style="width: 24px"></div>
      <div class="page-header-title">评价审核管理</div>
      <div style="width: 24px"></div>
    </div>

    <div class="search-bar">
      <input v-model="keyword" type="text" class="search-input" placeholder="搜索用户名/教室/评价内容" />
      <button class="search-btn" @click="loadReviews">🔍</button>
    </div>

    <div class="filter-bar">
      <button
        v-for="f in statusFilters"
        :key="String(f.value ?? 'all')"
        class="filter-item"
        :class="{ active: activeStatus === f.value }"
        @click="activeStatus = f.value; loadReviews()"
      >
        {{ f.label }}
      </button>
    </div>

    <div class="content-area">
      <div v-if="reviews.length === 0" class="empty-state">
        <div class="empty-icon">-</div>
        <div class="empty-title">暂无数据</div>
        <div class="empty-desc">请调整筛选条件后重试</div>
      </div>

      <div v-else class="admin-card">
        <div v-for="item in reviews" :key="item.id" class="list-card">
          <div class="list-icon">💬</div>
          <div class="list-info">
            <div class="list-header">
              <div class="list-title">{{ item.reviewerName }} - {{ item.location }}</div>
              <div class="list-tag" :class="tagClass(item.status)">
                {{ statusLabel(item.status) }}
              </div>
            </div>
            <div class="list-meta">{{ scoreText(item.score) }} | {{ item.createTime?.slice(0, 10) }}</div>
            <div class="list-desc">{{ item.content || '用户未填写文字评价' }}</div>

            <div v-if="item.status === 0" class="action-group">
              <button class="action-btn success-btn" @click="handleAudit(item, true)">
                通过
              </button>
              <button class="action-btn danger-btn" @click="handleAudit(item, false)">
                驳回
              </button>
            </div>

            <div v-else class="action-group">
              <button class="action-btn" style="background-color: #f5f7fa; color: #909399" disabled>
                {{ statusLabel(item.status) }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" style="padding: 10px 20px; color: #909399; font-size: 12px">加载中...</div>
  </div>
</template>

