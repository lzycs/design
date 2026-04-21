<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getAdminOverview, type AdminOverviewVO } from '@/api/admin'

const router = useRouter()

const overview = ref<AdminOverviewVO>({
  pendingRepairCount: 0,
  pendingReviewCount: 0,
  classroomTotal: 0,
})

const loadOverview = async () => {
  try {
    const res = await getAdminOverview()
    if (res.code === 200) {
      overview.value = res.data
    }
  } catch (e) {
    console.warn('概览加载失败', e)
  }
}

const logout = async () => {
  localStorage.removeItem('adminToken')
  showToast('已退出登录')
  await router.replace('/profile')
}

onMounted(loadOverview)
</script>

<template>
  <div>
    <div class="page-header">
      <div style="width: 24px"></div>
      <div class="page-header-title">管理后台</div>
      <button class="logout-top-btn" @click="logout">退出</button>
    </div>

    <div class="content-area">
      <!-- 数据统计卡片 -->
      <div class="admin-card stat-card">
        <div class="stat-item">
          <div class="stat-value">{{ overview.pendingRepairCount }}</div>
          <div class="stat-label">待处理工单</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ overview.pendingReviewCount }}</div>
          <div class="stat-label">待审核评价</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ overview.classroomTotal }}</div>
          <div class="stat-label">教室总数</div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="admin-card">
        <div style="font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #1a1a1a">快捷操作</div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/buildings')">新增教学楼</button>
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/classrooms')">新增教室</button>
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/courses')">导入课程表</button>
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/reviews')">审核评价</button>
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/market-audit')">审核集市资源</button>
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/reservation-limits')">预约上限</button>
          <button class="action-btn primary-btn" style="padding: 12px 0" @click="router.push('/admin/scan-devices')">设备授权</button>
        </div>
      </div>

      <!-- 最近工单入口 -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin: 4px 0 8px">
        <div style="font-size: 16px; font-weight: 600; color: #1a1a1a">待处理工单</div>
        <div style="font-size: 12px; color: #4a90e2; cursor: pointer" @click="router.push('/admin/repairs')">查看全部</div>
      </div>
      <div class="admin-card">
        <div v-if="overview.pendingRepairCount > 0" class="list-card" style="border-bottom: none">
          <div class="list-icon">🔧</div>
          <div class="list-info">
            <div class="list-header">
              <div class="list-title">共 {{ overview.pendingRepairCount }} 条待处理工单</div>
              <div class="list-tag tag-pending">待处理</div>
            </div>
            <div class="list-meta">点击右上角"查看全部"进入维修管理</div>
          </div>
        </div>
        <div v-else class="empty-state" style="padding: 24px 20px">
          <div style="font-size: 32px; margin-bottom: 8px">✅</div>
          <div style="font-size: 14px; color: #909399">暂无待处理工单</div>
        </div>
      </div>

      <!-- 待审核评价入口 -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin: 4px 0 8px">
        <div style="font-size: 16px; font-weight: 600; color: #1a1a1a">待审核评价</div>
        <div style="font-size: 12px; color: #4a90e2; cursor: pointer" @click="router.push('/admin/reviews')">查看全部</div>
      </div>
      <div class="admin-card">
        <div v-if="overview.pendingReviewCount > 0" class="list-card" style="border-bottom: none">
          <div class="list-icon">💬</div>
          <div class="list-info">
            <div class="list-header">
              <div class="list-title">共 {{ overview.pendingReviewCount }} 条待审核评价</div>
              <div class="list-tag tag-pending">待审核</div>
            </div>
            <div class="list-meta">点击右上角"查看全部"进入评价审核</div>
          </div>
        </div>
        <div v-else class="empty-state" style="padding: 24px 20px">
          <div style="font-size: 32px; margin-bottom: 8px">✅</div>
          <div style="font-size: 14px; color: #909399">暂无待审核评价</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.logout-top-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
  white-space: nowrap;
}
</style>
