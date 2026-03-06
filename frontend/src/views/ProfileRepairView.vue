<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from 'vant'

interface User {
  id?: number
  username: string
}

/** 与后端 RepairVO 对应 */
interface RepairVO {
  id?: number
  userId?: number
  classroomId?: number
  title?: string
  description?: string
  type?: number
  priority?: number
  status?: number
  createTime?: string
  handleTime?: string
  handleRemark?: string
  handlerId?: number
  location?: string
  handlerName?: string
  handlerPhone?: string
}

const router = useRouter()
const storedUser = ref<User | null>(null)
const repairs = ref<RepairVO[]>([])
const loading = ref(false)
const repairDetail = ref<RepairVO | null>(null)
const showRepairDetail = ref(false)

/** 状态 Tab：全部 / 待处理(1) / 处理中(2) / 已完成(3) */
const statusTab = ref<'all' | 'pending' | 'processing' | 'completed'>('all')

const isLoggedIn = computed(() => !!storedUser.value?.id)

const filteredRepairs = computed(() => {
  if (statusTab.value === 'all') return repairs.value
  if (statusTab.value === 'pending') return repairs.value.filter((r) => r.status === 1)
  if (statusTab.value === 'processing') return repairs.value.filter((r) => r.status === 2)
  if (statusTab.value === 'completed') return repairs.value.filter((r) => r.status === 3)
  return repairs.value
})

/** 报修类型：1-照明 2-空调 3-桌椅 4-多媒体设备 5-网络 6-其他 */
const getTypeName = (type?: number) => {
  const map: Record<number, string> = {
    1: '照明',
    2: '空调',
    3: '桌椅',
    4: '设备故障',
    5: '网络',
    6: '其他'
  }
  return type != null ? map[type] ?? '其他' : '其他'
}

/** 状态文案：1-待处理 2-处理中 3-已修复 4-已关闭 */
const getStatusText = (status?: number) => {
  if (status === 1) return '待处理'
  if (status === 2) return '处理中'
  if (status === 3 || status === 4) return '已完成'
  return '未知'
}

const getStatusClass = (status?: number) => {
  if (status === 1) return 'status-pending'
  if (status === 2) return 'status-processing'
  if (status === 3 || status === 4) return 'status-completed'
  return 'status-pending'
}

/** 卡片标题：优先用 location（楼-房间号） */
const getCardTitle = (item: RepairVO) => item.location || item.title || `报修 #${item.id ?? '-'}`

/** 格式化时间 */
const formatTime = (t?: string) => {
  if (!t) return '-'
  const d = new Date(t)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

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
    const res = await request.get<RepairVO[]>(`/repair/user/${storedUser.value.id}`)
    repairs.value = (res as unknown as { data?: RepairVO[] }).data ?? []
  } catch (e) {
    console.error(e)
    repairs.value = []
    showToast('加载报修记录失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/profile')
}

const openDetail = async (item: RepairVO) => {
  if (!item.id) return
  try {
    const res = await request.get<RepairVO>(`/repair/${item.id}`)
    repairDetail.value = (res as unknown as { data?: RepairVO }).data ?? item
  } catch {
    repairDetail.value = item
  }
  showRepairDetail.value = true
}

const onModifyRepair = () => showToast('修改报修功能暂未开放')
const onContactRepair = (item: RepairVO) => {
  if (item.handlerPhone) window.location.href = `tel:${item.handlerPhone}`
  else showToast('暂无维修人员电话')
}
const onEvaluateRepair = () => showToast('评价维修功能暂未开放')

onMounted(async () => {
  loadFromStorage()
  if (storedUser.value?.id) await loadRepairs()
})
</script>

<template>
  <div class="profile-repair">
    <van-nav-bar title="我的报修" left-arrow @click-left="goBack" />

    <template v-if="isLoggedIn">
      <div class="content-area">
        <!-- 状态标签 -->
        <div class="status-tab-bar">
          <div
            v-for="tab in [
              { key: 'all', label: '全部' },
              { key: 'pending', label: '待处理' },
              { key: 'processing', label: '处理中' },
              { key: 'completed', label: '已完成' }
            ]"
            :key="tab.key"
            class="status-tab-item"
            :class="{ active: statusTab === tab.key }"
            @click="statusTab = tab.key as typeof statusTab"
          >
            {{ tab.label }}
          </div>
        </div>

        <van-loading v-if="loading" class="loading-wrap" vertical>加载中...</van-loading>
        <van-empty
          v-else-if="filteredRepairs.length === 0"
          description="暂无报修记录"
          class="empty-wrap"
        />
        <div v-else class="repair-list">
          <div
            v-for="item in filteredRepairs"
            :key="item.id"
            class="repair-card"
          >
            <div class="repair-card-header">
              <div class="repair-card-title">{{ getCardTitle(item) }}</div>
              <div class="repair-status" :class="getStatusClass(item.status)">
                {{ getStatusText(item.status) }}
              </div>
            </div>
            <div class="repair-card-detail">
              <div class="detail-row">
                <span class="detail-label">报修时间：</span>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">报修类型：</span>
                <span>{{ getTypeName(item.type) }}</span>
              </div>
              <div v-if="item.status === 2 || item.status === 3" class="detail-row">
                <span class="detail-label">维修人员：</span>
                <span>{{ item.handlerName ? `${item.handlerName} (${item.handlerPhone || ''})` : '-' }}</span>
              </div>
              <div v-if="item.status === 3 || item.handleTime" class="detail-row">
                <span class="detail-label">完成时间：</span>
                <span>{{ formatTime(item.handleTime) }}</span>
              </div>
            </div>
            <div class="repair-desc">{{ item.description || '-' }}</div>
            <div class="repair-card-actions">
              <template v-if="item.status === 1">
                <button type="button" class="action-btn btn-secondary" @click="onModifyRepair">修改报修</button>
                <button type="button" class="action-btn btn-primary" @click="openDetail(item)">查看进度</button>
              </template>
              <template v-else-if="item.status === 2">
                <button type="button" class="action-btn btn-secondary" @click="onContactRepair(item)">联系维修</button>
                <button type="button" class="action-btn btn-primary" @click="openDetail(item)">查看进度</button>
              </template>
              <template v-else>
                <button type="button" class="action-btn btn-secondary" @click="onEvaluateRepair">评价维修</button>
                <button type="button" class="action-btn btn-primary" @click="openDetail(item)">查看详情</button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看报修记录" class="empty-wrap" />
    </template>

    <van-popup v-model:show="showRepairDetail" round position="bottom" :style="{ height: '60%' }">
      <div v-if="repairDetail" class="detail-wrapper">
        <div class="detail-title">报修详情</div>
        <div class="detail-row">地点：{{ repairDetail.location || repairDetail.title || '-' }}</div>
        <div class="detail-row">描述：{{ repairDetail.description || '-' }}</div>
        <div class="detail-row">类型：{{ getTypeName(repairDetail.type) }}</div>
        <div class="detail-row">状态：{{ getStatusText(repairDetail.status) }}</div>
        <div class="detail-row">报修时间：{{ formatTime(repairDetail.createTime) }}</div>
        <div v-if="repairDetail.handleTime" class="detail-row">完成时间：{{ formatTime(repairDetail.handleTime) }}</div>
        <div class="detail-row">处理备注：{{ repairDetail.handleRemark || '暂无' }}</div>
        <div v-if="repairDetail.handlerName" class="detail-row">维修人员：{{ repairDetail.handlerName }} {{ repairDetail.handlerPhone ? `(${repairDetail.handlerPhone})` : '' }}</div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-repair {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 24px;
}

.content-area {
  padding: 16px 20px;
}

.status-tab-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
  padding-bottom: 8px;
}

.status-tab-bar::-webkit-scrollbar {
  display: none;
}

.status-tab-item {
  padding: 8px 16px;
  border-radius: 16px;
  background-color: #f5f7fa;
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  cursor: pointer;
  border: none;
}

.status-tab-item.active {
  background-color: #4a90e2;
  color: #fff;
}

.repair-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.repair-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
}

.repair-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.repair-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.repair-status {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.status-pending {
  background-color: #fef7e0;
  color: #e6a23c;
}

.status-processing {
  background-color: #ecf5ff;
  color: #4a90e2;
}

.status-completed {
  background-color: #f0f9ff;
  color: #67c23a;
}

.repair-card-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.repair-card-detail .detail-row {
  font-size: 14px;
  color: #666;
}

.detail-label {
  color: #909399;
  margin-right: 4px;
}

.repair-desc {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 12px;
  padding: 8px 12px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.repair-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  border: none;
  cursor: pointer;
}

.btn-primary {
  background-color: #4a90e2;
  color: #fff;
}

.btn-secondary {
  background-color: #f5f7fa;
  color: #666;
}

.loading-wrap,
.empty-wrap {
  padding: 40px 0;
}

.detail-wrapper {
  padding: 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.detail-wrapper .detail-row {
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
}
</style>
