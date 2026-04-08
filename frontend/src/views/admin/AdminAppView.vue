<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAdminMenus, type AdminMenu } from '@/api/admin'
import AdminHomeView from '@/views/admin/AdminHomeView.vue'
import AdminRepairView from '@/views/admin/AdminRepairView.vue'
import AdminReviewView from '@/views/admin/AdminReviewView.vue'
import AdminClassroomView from '@/views/admin/AdminClassroomView.vue'
import AdminCourseView from '@/views/admin/AdminCourseView.vue'
import AdminBuildingView from '@/views/admin/AdminBuildingView.vue'
import AdminReservationLimitView from '@/views/admin/AdminReservationLimitView.vue'
import AdminScanDevicesView from '@/views/admin/AdminScanDevicesView.vue'

const route = useRoute()
const router = useRouter()

const defaultMenus: AdminMenu[] = [
  { id: 1, title: '首页', path: '/admin', permissionKey: null },
  { id: 2, title: '维修管理', path: '/admin/repairs', permissionKey: null },
  { id: 3, title: '评价审核', path: '/admin/reviews', permissionKey: null },
  { id: 4, title: '教室管理', path: '/admin/classrooms', permissionKey: null },
  { id: 5, title: '课程管理', path: '/admin/courses', permissionKey: null },
  { id: 6, title: '楼栋管理', path: '/admin/buildings', permissionKey: null },
]

const tabMenus = [
  { path: '/admin', title: '首页', icon: 'home-o' },
  { path: '/admin/repairs', title: '维修管理', icon: 'warning-o' },
  { path: '/admin/reviews', title: '评价审核', icon: 'comment-o' },
  { path: '/admin/classrooms', title: '教室管理', icon: 'notes-o' },
  { path: '/admin/courses', title: '课程管理', icon: 'calendar-o' },
]

const menus = ref<AdminMenu[]>(defaultMenus)
const activePath = ref<string>(route.path || '/admin')

const loadMenus = async () => {
  const token = localStorage.getItem('adminToken')
  if (!token) {
    await router.replace('/admin/login')
    return
  }
  try {
    const res = await getAdminMenus()
    if (res.code !== 200) {
      console.warn('菜单加载失败，使用默认菜单:', res.message)
      return
    }
    if (res.data && res.data.length > 0) {
      menus.value = res.data
    }
  } catch (e) {
    console.warn('菜单接口不可用，使用默认菜单', e)
  }
}

onMounted(async () => {
  await loadMenus()
})

watch(
  () => route.path,
  p => {
    activePath.value = String(p)
  },
)

const viewMap: Record<string, Component> = {
  '/admin': AdminHomeView,
  '/admin/repairs': AdminRepairView,
  '/admin/reviews': AdminReviewView,
  '/admin/classrooms': AdminClassroomView,
  '/admin/courses': AdminCourseView,
  '/admin/buildings': AdminBuildingView,
  '/admin/reservation-limits': AdminReservationLimitView,
  '/admin/scan-devices': AdminScanDevicesView,
}

const currentView = computed(() => {
  return viewMap[activePath.value] || AdminHomeView
})

// 底部 tab 只展示前5项（首页/维修/评价/教室/课程），楼栋管理通过教室管理内跳转
const visibleTabs = computed(() => {
  const paths = ['/admin', '/admin/repairs', '/admin/reviews', '/admin/classrooms', '/admin/courses']
  return tabMenus.filter(t => {
    // 如果后端菜单有数据，只展示有权限的 tab
    const hasPerm = menus.value.some(m => m.path === t.path)
    return paths.includes(t.path) && hasPerm
  })
})

const logout = async () => {
  localStorage.removeItem('adminToken')
  await router.replace('/profile')
}
</script>

<template>
  <div class="mobile-admin-container">
    <!-- 内容区域 -->
    <div class="admin-main">
      <component :is="currentView" :key="activePath" />
    </div>

    <!-- 底部 Tab 导航 -->
    <div class="admin-tab-bar">
      <div
        v-for="tab in visibleTabs"
        :key="tab.path"
        class="admin-tab-item"
        :class="{ active: activePath === tab.path }"
        @click="router.push(tab.path)"
      >
        <van-icon :name="tab.icon" class="tab-van-icon" />
        <span class="tab-label">{{ tab.title }}</span>
      </div>
    </div>
  </div>
</template>

<style>
.mobile-admin-container {
  width: 100%;
  height: 100vh;
  background-color: #ffffff;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  background-color: #f5f7fa;
  padding-bottom: 0;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.admin-main::-webkit-scrollbar {
  display: none;
}

/* 底部 Tab 导航 */
.admin-tab-bar {
  flex-shrink: 0;
  width: 100%;
  height: 56px;
  background-color: #ffffff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 99;
}

.admin-tab-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
  flex: 1;
  cursor: pointer;
  padding: 4px 0;
  transition: color 0.2s;
}

.admin-tab-item.active {
  color: #4a90e2;
}

.tab-van-icon {
  font-size: 22px;
  margin-bottom: 2px;
  line-height: 1;
}

.tab-label {
  font-size: 10px;
  line-height: 1;
  white-space: nowrap;
}

/* ===== 子页面公共样式（供各 AdminXxxView 使用） ===== */
.page-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #4a90e2;
  color: #ffffff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.page-header-title {
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  text-align: center;
}

.search-bar {
  padding: 12px 20px;
  background-color: #ffffff;
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
}

.search-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #4a90e2;
  display: flex;
  justify-content: center;
  align-items: center;
  border: none;
  color: #ffffff;
  cursor: pointer;
  font-size: 16px;
}

.filter-bar {
  padding: 8px 20px;
  background-color: #ffffff;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid #f5f7fa;
}

.filter-item {
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  background-color: #f5f7fa;
  color: #666;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.filter-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.content-area {
  padding: 12px 20px;
}

.admin-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
}

.stat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #4a90e2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.list-card {
  display: flex;
  padding: 12px;
  border-bottom: 1px solid #f5f7fa;
}

.list-card:last-child {
  border-bottom: none;
}

.list-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #f0f7ff;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 12px;
  flex-shrink: 0;
  font-size: 20px;
}

.list-info {
  flex: 1;
  overflow: hidden;
}

.list-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  gap: 8px;
  align-items: flex-start;
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-tag {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.tag-pending {
  background-color: #fff7e6;
  color: #ff9a2e;
}

.tag-processing {
  background-color: #e8f4ff;
  color: #4a90e2;
}

.tag-completed {
  background-color: #e8f5e9;
  color: #4caf50;
}

.tag-passed {
  background-color: #e8f5e9;
  color: #4caf50;
}

.tag-rejected {
  background-color: #fff0f0;
  color: #f56c6c;
}

.list-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-desc {
  font-size: 12px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.action-group {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.action-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: none;
  font-size: 12px;
  cursor: pointer;
}

.primary-btn {
  background-color: #4a90e2;
  color: #ffffff;
}

.success-btn {
  background-color: #4caf50;
  color: #ffffff;
}

.danger-btn {
  background-color: #f56c6c;
  color: #ffffff;
}

.add-btn {
  position: fixed;
  right: 20px;
  bottom: 72px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background-color: #4a90e2;
  color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.35);
  border: none;
  font-size: 26px;
  z-index: 50;
  cursor: pointer;
  line-height: 1;
}

.form-card {
  padding: 20px;
}

.form-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 16px;
  outline: none;
}

.form-input:focus {
  border-color: #4a90e2;
}

.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 16px;
  outline: none;
  background-color: #ffffff;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  background-color: #4a90e2;
  color: #ffffff;
  border: none;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
  cursor: pointer;
}

.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #e5e6eb;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}
</style>
