<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getAdminRepairs, updateAdminRepairStatus, type AdminRepairVO } from '@/api/admin'

const keyword = ref('')
const activeStatus = ref<number | null>(null)
const loading = ref(false)

const repairs = ref<AdminRepairVO[]>([])

const statusFilters: { label: string; value: number | null }[] = [
  { label: '全部', value: null },
  { label: '待处理', value: 1 },
  { label: '维修中', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已驳回', value: 4 },
]

const loadRepairs = async () => {
  loading.value = true
  try {
    const res = await getAdminRepairs({
      status: activeStatus.value === null ? undefined : activeStatus.value,
      keyword: keyword.value || undefined,
    })
    if (res.code !== 200) {
      showToast(res.message || '加载失败')
      return
    }
    repairs.value = res.data
  } catch (e) {
    console.error(e)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const tagClass = (status: number | null | undefined) => {
  switch (status) {
    case 1:
      return 'tag-pending'
    case 2:
      return 'tag-processing'
    case 3:
      return 'tag-completed'
    case 4:
      return 'tag-rejected'
    default:
      return ''
  }
}

const statusLabel = (status: number | null | undefined) => {
  switch (status) {
    case 1:
      return '待处理'
    case 2:
      return '维修中'
    case 3:
      return '已完成'
    case 4:
      return '已驳回'
    default:
      return '未知'
  }
}

const handleAction = async (repair: AdminRepairVO, nextStatus: number) => {
  const remark =
    nextStatus === 4
      ? window.prompt('驳回原因/备注（可选）', '不符合/无法处理等')
      : window.prompt('处理备注（可选）', '')

  try {
    const res = await updateAdminRepairStatus(repair.id, {
      status: nextStatus,
      handleRemark: remark || '',
    })
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '更新失败')
      return
    }
    showToast('更新成功')
    await loadRepairs()
  } catch (e) {
    console.error(e)
    showToast('更新失败')
  }
}

onMounted(loadRepairs)
</script>

<template>
  <div class="admin-page active">
    <div class="page-header">
      <div style="width: 24px"></div>
      <div class="page-header-title">维修工单管理</div>
      <div style="width: 24px"></div>
    </div>

    <div class="search-bar">
      <input v-model="keyword" type="text" class="search-input" placeholder="搜索工单编号/位置/报修人" />
      <button class="search-btn" @click="loadRepairs">🔍</button>
    </div>

    <div class="filter-bar">
      <button
        v-for="f in statusFilters"
        :key="String(f.value ?? 'all')"
        class="filter-item"
        :class="{ active: activeStatus === f.value }"
        @click="activeStatus = f.value; loadRepairs()"
      >
        {{ f.label }}
      </button>
    </div>

    <div class="content-area">
      <div v-if="repairs.length === 0" class="empty-state">
        <div class="empty-icon">-</div>
        <div class="empty-title">暂无数据</div>
        <div class="empty-desc">请调整筛选条件后重试</div>
      </div>

      <div v-else class="admin-card">
        <div v-for="item in repairs" :key="item.id" class="list-card">
          <div class="list-icon">🔧</div>
          <div class="list-info">
            <div class="list-header">
              <div class="list-title">{{ item.title }} ({{ item.id }})</div>
              <div class="list-tag" :class="tagClass(item.status)">
                {{ statusLabel(item.status) }}
              </div>
            </div>

            <div class="list-meta">{{ item.location }} | {{ item.requesterName }}</div>
            <div class="list-desc">{{ item.description }}</div>

            <div v-if="item.status === 1 || item.status === 2" class="action-group">
              <button
                v-if="item.status === 1"
                class="action-btn primary-btn"
                @click="handleAction(item, 2)"
              >
                开始维修
              </button>
              <button
                v-if="item.status === 2"
                class="action-btn success-btn"
                @click="handleAction(item, 3)"
              >
                标记完成
              </button>
              <button
                v-if="item.status === 2"
                class="action-btn danger-btn"
                @click="handleAction(item, 4)"
              >
                驳回
              </button>
            </div>

            <div v-else class="action-group">
              <button class="action-btn" style="background-color: #f5f7fa; color: #909399" disabled>
                查看详情
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" style="padding: 10px 20px; color: #909399; font-size: 12px">加载中...</div>
  </div>
</template>

