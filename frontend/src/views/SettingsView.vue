<script setup lang="ts">
import { ref } from 'vue'
import { NavBar, Cell, CellGroup, Switch } from 'vant'

const notificationsEnabled = ref(true)
const darkModeEnabled = ref(false)
</script>

<template>
  <div class="settings">
    <van-nav-bar title="设置" />
    
    <van-cell-group inset>
      <van-cell title="通知提醒">
        <template #right-icon>
          <van-switch v-model="notificationsEnabled" />
        </template>
      </van-cell>
      <van-cell title="深色模式">
        <template #right-icon>
          <van-switch v-model="darkModeEnabled" />
        </template>
      </van-cell>
      <van-cell title="关于我们" />
      <van-cell title="帮助中心" />
    </van-cell-group>
  </div>
</template>

<style scoped>
.settings {
  background-color: #f5f5f5;
  min-height: 100vh;
}
</style>
