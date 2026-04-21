<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { ref, computed, watch } from 'vue'

const route = useRoute()
const active = ref(0)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

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
</script>

<template>
  <div class="app-container">
    <RouterView />
    <van-tabbar v-if="!isAdminRoute" v-model="active">
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
  .app-container :deep(.van-tabbar) {
    border-top: 1px solid #e7edf7;
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(8px);
  }
}
</style>
