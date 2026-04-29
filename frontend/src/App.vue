<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { ref, computed, watch, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const active = ref(0)
const isDesktop = ref(false)

const menuItems = [
  { index: 0, title: '首页', icon: 'home-o', to: '/' },
  { index: 1, title: '预约', icon: 'calendar-o', to: '/reservation' },
  { index: 2, title: '协作', icon: 'friends-o', to: '/collaboration' },
  { index: 3, title: '反馈', icon: 'comment-o', to: '/feedback' },
  { index: 4, title: '我的', icon: 'user-o', to: '/profile' },
]

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const hideTabbar = computed(() => route.path.startsWith('/profile/teams/') && route.path.endsWith('/chat'))
const showDesktopSidebar = computed(() => !isAdminRoute.value && isDesktop.value && !hideTabbar.value)
const showMobileTabbar = computed(() => !isAdminRoute.value && !hideTabbar.value && !isDesktop.value)

const updateViewport = () => {
  isDesktop.value = window.innerWidth >= 1024
}

watch(
  () => ({ path: route.path, only: route.query.only }),
  ({ path, only }) => {
    // 从「我的评价」进入的反馈页（仅评价列表）：不点亮「反馈」tab，保持「我的」高亮
    if (path.startsWith('/feedback') && only === '1') {
      active.value = 4
      return
    }
    if (path.startsWith('/reservation')) {
      active.value = 1
    } else if (path.startsWith('/collaboration')) {
      active.value = 2
    } else if (path.startsWith('/feedback')) {
      active.value = 3
    } else if (path.startsWith('/profile')) {
      active.value = 4
    } else {
      active.value = 0
    }
  },
  { immediate: true, deep: true }
)

watch(
  () => route.path,
  path => {
    document.documentElement.classList.toggle('is-admin-route', path.startsWith('/admin'))
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  document.documentElement.classList.remove('is-admin-route')
  window.removeEventListener('resize', updateViewport)
})

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})
</script>

<template>
  <div class="app-container" :class="{ 'is-desktop-layout': showDesktopSidebar }">
    <aside v-if="showDesktopSidebar" class="user-sidebar">
      <div class="user-sidebar-brand">
        <div class="brand-dot"></div>
        <div>
          <div class="brand-title">LearningSpace</div>
          <div class="brand-sub">用户端</div>
        </div>
      </div>
      <div class="user-menu-list">
        <button
          v-for="item in menuItems"
          :key="item.to"
          type="button"
          class="user-menu-item"
          :class="{ active: active === item.index }"
          @click="router.push(item.to)"
        >
          <van-icon :name="item.icon" />
          <span>{{ item.title }}</span>
        </button>
      </div>
    </aside>

    <div class="app-main">
      <RouterView />
    </div>

    <van-tabbar v-if="showMobileTabbar" v-model="active">
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="calendar-o" to="/reservation">预约</van-tabbar-item>
      <van-tabbar-item icon="friends-o" to="/collaboration">协作</van-tabbar-item>
      <van-tabbar-item icon="comment-o" to="/feedback">反馈</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.app-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

@media (min-width: 1024px) {
  .app-container.is-desktop-layout {
    flex-direction: row;
    gap: 16px;
    min-height: 100dvh;
    padding: 16px;
    box-sizing: border-box;
  }

  .user-sidebar {
    width: 232px;
    flex: 0 0 232px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    border-radius: var(--ls-radius-card);
    border: 1px solid var(--ls-divider);
    background: var(--ls-surface);
    box-shadow: var(--ls-shadow-card);
    padding: 16px 12px;
  }

  .user-sidebar-brand {
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

  .user-menu-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .user-menu-item {
    border: none;
    background: transparent;
    color: var(--ls-text-muted);
    border-radius: 10px;
    padding: 10px 12px;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    text-align: left;
  }

  .user-menu-item.active {
    background: color-mix(in srgb, var(--ls-primary) 12%, transparent);
    color: var(--ls-primary);
    font-weight: 600;
  }

  .app-main {
    flex: 1;
    min-width: 0;
    border-radius: var(--ls-radius-card);
    overflow: hidden;
  }

  .app-container :deep(.van-tabbar) {
    display: none;
  }
}
</style>
