<script setup lang="ts">
import { ref } from 'vue'
import {  Cell, CellGroup } from 'vant'

const activeTab = ref(0)
</script>

<template>
  <div class="reservation">
    <van-nav-bar title="在线预约预订" />
    
    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="普通教室">
        <CellGroup>
          <Cell title="教学楼A-101" label="今日14:00-16:00 可预约" is-link />
          <Cell title="教学楼A-102" label="今日16:00-18:00 可预约" is-link />
        </CellGroup>
      </van-tab>
      <van-tab title="研讨室">
        <CellGroup>
          <Cell title="研讨室B-201" label="今日13:00-15:00 可预约" is-link />
        </CellGroup>
      </van-tab>
      <van-tab title="图书馆座位">
        <CellGroup>
          <Cell title="图书馆3楼研修间" label="今日15:00-17:00 可预约" is-link />
        </CellGroup>
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.reservation {
  background-color: #f5f5f5;
  min-height: 100vh;
}
</style>
