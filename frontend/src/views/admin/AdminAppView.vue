<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
import AdminMarketAuditView from '@/views/admin/AdminMarketAuditView.vue'

const route = useRoute()
const router = useRouter()

const defaultMenus: AdminMenu[] = [
  { id: 1, title: '首页', path: '/admin', permissionKey: null },
  { id: 2, title: '维修管理', path: '/admin/repairs', permissionKey: null },
  { id: 3, title: '评价审核', path: '/admin/reviews', permissionKey: null },
  { id: 4, title: '教室管理', path: '/admin/classrooms', permissionKey: null },
  { id: 5, title: '课程管理', path: '/admin/courses', permissionKey: null },
  { id: 6, title: '楼栋管理', path: '/admin/buildings', permissionKey: null },
  { id: 7, title: '资源集市审核', path: '/admin/market-audit', permissionKey: null },
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
const isDesktop = ref(false)

const updateLayoutMode = () => {
  isDesktop.value = window.innerWidth >= 1024
}

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
  updateLayoutMode()
  window.addEventListener('resize', updateLayoutMode)
  await loadMenus()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateLayoutMode)
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
  '/admin/market-audit': AdminMarketAuditView,
}

const currentView = computed(() => {
  return viewMap[activePath.value] || AdminHomeView
})

const currentTitle = computed(() => {
  return menus.value.find(item => item.path === activePath.value)?.title || '管理后台'
})

// 底部 tab 只展示前5项（首页/维修/评价/教室/课程），楼栋管理通过教室管理内跳转
const visibleTabs = computed(() => {
  const paths = ['/admin', '/admin/repairs', '/admin/reviews', '/admin/classrooms', '/admin/courses']
  const filtered = tabMenus.filter(t => {
    // 如果后端菜单有数据，只展示有权限的 tab
    const hasPerm = menus.value.some(m => m.path === t.path)
    return paths.includes(t.path) && hasPerm
  })
  // 兜底：避免接口返回路径与 tab 不一致时底部导航完全不显示
  return filtered.length > 0 ? filtered : tabMenus.filter(t => paths.includes(t.path))
})

const logout = async () => {
  localStorage.removeItem('adminToken')
  await router.replace('/profile')
}
</script>

<template>
  <div class="admin-shell">
    <aside v-if="isDesktop" class="admin-sidebar">
      <div class="admin-brand">
        <div class="brand-dot"></div>
        <div>
          <div class="brand-title">LearningSpace</div>
          <div class="brand-sub">管理端</div>
        </div>
      </div>
      <div class="admin-menu-list">
        <button
          v-for="menu in menus"
          :key="menu.path"
          type="button"
          class="admin-menu-item"
          :class="{ active: activePath === menu.path }"
          @click="router.push(menu.path)"
        >
          {{ menu.title }}
        </button>
      </div>
      <button type="button" class="admin-logout-btn" @click="logout">退出登录</button>
    </aside>

    <div class="admin-content-shell">
      <div v-if="isDesktop" class="admin-topbar">
        <div class="topbar-title">{{ currentTitle }}</div>
      </div>
      <div class="admin-main">
        <component :is="currentView" :key="activePath" />
      </div>
    </div>

    <div v-if="!isDesktop" class="admin-tab-bar">
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
.admin-shell {
  width: 100%;
  height: 100dvh;
  min-height: 100vh;
  background: var(--ls-bg);
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.admin-sidebar {
  display: none;
}

.admin-content-shell {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  display: none;
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  background: var(--ls-bg);
  padding-bottom: 56px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.admin-main::-webkit-scrollbar {
  display: none;
}

.admin-tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 56px;
  background: var(--ls-surface);
  border-top: 1px solid var(--ls-divider);
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
  color: var(--ls-text-faint);
  flex: 1;
  cursor: pointer;
  padding: 4px 0;
  transition: color 0.2s;
}

.admin-tab-item.active {
  color: var(--ls-primary);
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
  background: linear-gradient(135deg, #4a90e2 0%, #5b9cff 100%);
  color: #ffffff;
  position: sticky;
  top: 0;
  z-index: 30;
}

.page-header-title {
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  text-align: center;
}

.search-bar {
  padding: 12px 20px;
  background: var(--ls-surface);
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid var(--ls-border);
  border-radius: 24px;
  font-size: 14px;
  outline: none;
}

.search-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--ls-primary);
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
  background: var(--ls-surface);
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid var(--ls-divider);
}

.filter-item {
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  background: var(--ls-surface-muted);
  color: var(--ls-text-muted);
  border: none;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.filter-item.active {
  background: var(--ls-primary);
  color: #ffffff;
}

.content-area {
  padding: 12px 20px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

.admin-card {
  background: var(--ls-surface);
  border-radius: 12px;
  box-shadow: var(--ls-shadow-card);
  border: 1px solid var(--ls-divider);
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
  color: var(--ls-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--ls-text-faint);
}

.list-card {
  display: flex;
  padding: 12px;
  border-bottom: 1px solid var(--ls-divider);
}

.list-card:last-child {
  border-bottom: none;
}

.list-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--ls-primary) 10%, transparent);
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
  color: var(--ls-text-strong);
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
  color: var(--ls-text-faint);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-desc {
  font-size: 12px;
  color: var(--ls-text-muted);
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
  background-color: var(--ls-primary);
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
  background-color: var(--ls-primary);
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
  color: var(--ls-text-strong);
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--ls-border);
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 16px;
  outline: none;
}

.form-input:focus {
  border-color: var(--ls-primary);
}

.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--ls-border);
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
  background-color: var(--ls-primary);
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
  color: var(--ls-divider);
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  color: var(--ls-text-strong);
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--ls-text-faint);
  margin-bottom: 24px;
}

@media (min-width: 1024px) {
  .admin-shell {
    flex-direction: row;
    gap: 16px;
    padding: 16px;
  }

  .admin-sidebar {
    display: flex;
    flex-direction: column;
    width: 232px;
    flex: 0 0 232px;
    background: var(--ls-surface);
    border: 1px solid var(--ls-divider);
    border-radius: var(--ls-radius-card);
    box-shadow: var(--ls-shadow-card);
    padding: 16px 12px;
    gap: 12px;
  }

  .admin-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 8px 14px;
    border-bottom: 1px solid var(--ls-divider);
  }

  .brand-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--ls-primary);
    box-shadow: 0 0 0 6px color-mix(in srgb, var(--ls-primary) 14%, transparent);
  }

  .brand-title {
    font-size: 15px;
    font-weight: 700;
    color: var(--ls-text-strong);
  }

  .brand-sub {
    font-size: 12px;
    color: var(--ls-text-faint);
  }

  .admin-menu-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
    overflow-y: auto;
  }

  .admin-menu-item {
    border: none;
    background: transparent;
    text-align: left;
    padding: 10px 12px;
    border-radius: 10px;
    color: var(--ls-text-muted);
    font-size: 14px;
    cursor: pointer;
  }

  .admin-menu-item:hover {
    background: var(--ls-surface-muted);
    color: var(--ls-text-strong);
  }

  .admin-menu-item.active {
    background: color-mix(in srgb, var(--ls-primary) 12%, transparent);
    color: var(--ls-primary);
    font-weight: 600;
  }

  .admin-logout-btn {
    margin-top: auto;
    border: 1px solid var(--ls-divider);
    background: var(--ls-surface);
    color: var(--ls-danger);
    border-radius: 10px;
    padding: 8px 12px;
    cursor: pointer;
  }

  .admin-topbar {
    display: flex;
    align-items: center;
    height: 64px;
    margin-bottom: 12px;
    padding: 0 18px;
    border-radius: var(--ls-radius-card);
    background: var(--ls-surface);
    border: 1px solid var(--ls-divider);
    box-shadow: var(--ls-shadow-card);
  }

  .topbar-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--ls-text-strong);
  }

  .admin-tab-bar {
    display: none;
  }

  .admin-main .page-header {
    display: none;
  }

  .admin-main {
    padding-bottom: 0;
  }
}
</style>
