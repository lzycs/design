<script setup lang="ts">
import { RouterView } from 'vue-router'
import { ref } from 'vue'

const active = ref(0)
</script>

<template>
  <div class="app-container">
    <RouterView />
    <van-tabbar v-model="active" route>
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="calendar-o" to="/reservation">预约</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
      <van-tabbar-item icon="setting-o" to="/settings">设置</van-tabbar-item>
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
