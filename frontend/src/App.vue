<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { ref, watch } from 'vue'

const route = useRoute()
const active = ref(0)

watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/reservation')) {
      active.value = 1
    } else if (path.startsWith('/profile')) {
      active.value = 2
    } else {
      active.value = 0
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <RouterView />
    <van-tabbar v-model="active" route>
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="calendar-o" to="/reservation">预约</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
