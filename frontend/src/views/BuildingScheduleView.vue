<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBuildingById, type Building } from '@/api/building'
import {
  getBuildingDaySchedule,
  type BuildingDaySchedule,
  type BuildingScheduleRow,
} from '@/api/courseSchedule'

const route = useRoute()
const router = useRouter()
const buildingId = computed(() => Number(route.params.buildingId || route.query.buildingId))

const building = ref<Building | null>(null)
const loading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const floor = ref<number | undefined>(undefined)
const roomKeyword = ref('')
const courseKeyword = ref('')
const timeRange = ref('')

const daySchedule = ref<BuildingDaySchedule | null>(null)

const rows = computed<BuildingScheduleRow[]>(() => daySchedule.value?.rows ?? [])
const slotLabels = computed(() => {
  const first = rows.value[0]
  return first ? first.cells.map((c) => c.label) : []
})

const statusText = (status: string) => {
  if (status === 'course_occupied') return '课程中'
  if (status === 'reservation_occupied') return '已预约'
  return '空闲'
}

const fetchBuilding = async () => {
  if (!buildingId.value) return
  const res = await getBuildingById(buildingId.value)
  building.value = res.data ?? null
}

const loadData = async () => {
  if (!buildingId.value) return
  loading.value = true
  try {
    const res = await getBuildingDaySchedule(buildingId.value, {
      date: selectedDate.value,
      floor: floor.value,
      roomKeyword: roomKeyword.value || undefined,
      courseKeyword: courseKeyword.value || undefined,
      timeRange: timeRange.value || undefined,
    })
    daySchedule.value = res.data
  } finally {
    loading.value = false
  }
}

const goClassroom = (classroomId?: number) => {
  if (!classroomId) return
  router.push(`/reservation/classroom/${classroomId}`)
}

onMounted(async () => {
  await fetchBuilding()
  await loadData()
})
</script>

<template>
  <div class="building-schedule-page">
    <van-nav-bar :title="`${building?.name || '教学楼'}课程表`" left-arrow @click-left="router.back()" />
    <div class="content">
      <div class="filters card">
        <div class="row">
          <input v-model="selectedDate" type="date" class="input" @change="loadData" />
          <input v-model.number="floor" type="number" class="input" placeholder="楼层" @change="loadData" />
        </div>
        <div class="row">
          <input v-model.trim="roomKeyword" class="input" placeholder="教室名/编号" />
          <input v-model.trim="courseKeyword" class="input" placeholder="课程名" />
        </div>
        <div class="row">
          <input v-model.trim="timeRange" class="input" placeholder="时间段(如 14:00-16:00)" />
          <button class="search-btn" @click="loadData">筛选</button>
        </div>
      </div>

      <div v-if="loading" class="card">加载中...</div>

      <div v-else class="card">
        <div v-if="rows.length === 0" class="empty">暂无课程表数据</div>
        <div v-else class="table-wrap">
          <table class="schedule-table">
            <thead>
              <tr>
                <th>教室</th>
                <th v-for="label in slotLabels" :key="label">{{ label }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.classroomId" @click="goClassroom(row.classroomId)">
                <td>{{ row.roomNumber || row.classroomName }}</td>
                <td v-for="cell in row.cells" :key="cell.slotId" :class="cell.finalStatus">
                  {{ cell.courseName || statusText(cell.finalStatus) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.building-schedule-page { min-height: 100vh; background: #f5f7fa; }
.content { padding: 12px; }
.card { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; }
.row { display: flex; gap: 8px; margin-bottom: 8px; }
.input { flex: 1; border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px; }
.search-btn { border: none; border-radius: 8px; background: #4a90e2; color: #fff; padding: 8px 14px; }
.table-wrap { overflow-x: auto; }
.schedule-table { border-collapse: collapse; min-width: 760px; width: 100%; }
.schedule-table th, .schedule-table td { border: 1px solid #eef0f3; padding: 6px; font-size: 12px; text-align: center; }
.course_occupied { background: #fff1f0; color: #b42318; }
.reservation_occupied { background: #f5f6f8; color: #667085; }
.available { background: #ecfdf3; color: #027a48; }
.empty { color: #98a2b3; text-align: center; padding: 12px 0; }
</style>
