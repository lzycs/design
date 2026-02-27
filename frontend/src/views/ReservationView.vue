<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NavBar, Cell, CellGroup, Tabs, Tab } from 'vant'
import { getClassroomList, type Classroom } from '@/api/classroom'

const activeTab = ref(0)
const classrooms = ref<Classroom[]>([])

const loadClassrooms = async () => {
  try {
    const res = await getClassroomList()
    console.log('API Response:', res)
    classrooms.value = res.data
  } catch (error) {
    console.error('Failed to load classrooms:', error)
  }
}

onMounted(() => {
  loadClassrooms()
})
</script>

<template>
  <div class="reservation">
    <van-nav-bar title="在线预约预订" />
    
    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="普通教室">
        <CellGroup>
          <Cell
            v-for="classroom in classrooms"
            :key="classroom.id"
            :title="`${classroom.name}`"
            :label="`楼层: ${classroom.floor} | 容量: ${classroom.capacity}`"
            is-link
          />
        </CellGroup>
      </van-tab>
      <van-tab title="研讨室">
        <CellGroup>
          <Cell
            v-for="classroom in classrooms"
            :key="classroom.id"
            :title="`${classroom.name}`"
            :label="`楼层: ${classroom.floor} | 容量: ${classroom.capacity}`"
            is-link
          />
        </CellGroup>
      </van-tab>
      <van-tab title="图书馆座位">
        <CellGroup>
          <Cell
            v-for="classroom in classrooms"
            :key="classroom.id"
            :title="`${classroom.name}`"
            :label="`楼层: ${classroom.floor} | 容量: ${classroom.capacity}`"
            is-link
          />
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
