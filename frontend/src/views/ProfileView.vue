<script setup lang="ts">
import { ref } from 'vue'



const userInfo = ref({
  name: '学生1',
  id: '20230001',
  role: '学生'
})
</script>

<template>
  <div class="profile">
    <van-nav-bar title="个人中心" />
    
    <van-cell-group inset>
      <van-cell title="个人信息">
        <template #right-icon>
          <van-icon name="arrow" />
        </template>
      </van-cell>
      <van-cell title="我的预约" />
      <van-cell title="我的报修" />
      <van-cell title="我的评价" />
      <van-cell title="我的团队" />
    </van-cell-group>
  </div>
</template>

<style scoped>
.profile {
  background-color: #f5f5f5;
  min-height: 100vh;
}
</style>
