<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getClassroomsByBuilding, type Classroom } from '@/api/classroom'
import { Cell, CellGroup, Tag, showToast } from 'vant'

const route = useRoute()
const buildingId = Number(route.params.buildingId)
const classrooms = ref<Classroom[]>([])

const sortClassroomsByFloor = (list: Classroom[]) => {
  return [...list].sort((a, b) => {
    const floorA = a.floor ?? Number.MAX_SAFE_INTEGER
    const floorB = b.floor ?? Number.MAX_SAFE_INTEGER
    if (floorA !== floorB) return floorA - floorB
    return (a.roomNumber || a.name || '').localeCompare((b.roomNumber || b.name || ''), 'zh-Hans-CN', {
      numeric: true,
      sensitivity: 'base',
    })
  })
}

const loadClassrooms = async () => {
  try {
    const res = await getClassroomsByBuilding(buildingId)
    // Safely handle empty or undefined response data
    classrooms.value = sortClassroomsByFloor(res.data || [])
  } catch (error) {
    console.error('Failed to load classrooms:', error)
    // Show user-friendly error notification using Vant Toast
    showToast({
      message: '加载教室列表失败，请稍后重试',
      position: 'bottom',
      duration: 2000
    })
  }
}

const getStatusColor = (status?: number) => {
  switch (status) {
    case 1:
      return 'success' // 正常
    case 2:
      return 'warning' // 维修中
    case 0:
      return 'danger' // 停用
    default:
      return 'primary'
  }
}

const getStatusText = (status?: number) => {
  switch (status) {
    case 1:
      return '正常'
    case 2:
      return '维修中'
    case 0:
      return '停用'
    default:
      return '未知'
  }
}

onMounted(() => {
  loadClassrooms()
})
</script>

<template>
  <div class="classrooms">
    <van-nav-bar title="教室列表" />
    
    <CellGroup>
      <Cell
        v-for="classroom in classrooms"
        :key="classroom.id"
        :title="classroom.name"
        :label="`楼层: ${classroom.floor} | 容量: ${classroom.capacity}`"
        is-link
      >
        <template #right-icon>
          <Tag :type="getStatusColor(classroom.status)">
            {{ getStatusText(classroom.status) }}
          </Tag>
        </template>
      </Cell>
    </CellGroup>
  </div>
</template>

<style scoped>
.classrooms {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.classrooms > div:first-child {
  margin-bottom: 16px;
}
</style>
