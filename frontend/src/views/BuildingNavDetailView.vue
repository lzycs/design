<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBuildingById, type Building } from '@/api/building'
import { getClassroomsByBuilding, type Classroom } from '@/api/classroom'

const route = useRoute()
const router = useRouter()
const buildingId = Number(route.params.id)

const loading = ref(false)
const building = ref<Building | null>(null)
const classrooms = ref<Classroom[]>([])
const activeFloor = ref<number | null>(null)

const floorOptions = computed(() => {
  const set = new Set<number>()
  classrooms.value.forEach((c) => {
    if (typeof c.floor === 'number') set.add(c.floor)
  })
  return Array.from(set.values()).sort((a, b) => a - b)
})

const visibleClassrooms = computed(() => {
  if (activeFloor.value == null) return classrooms.value
  return classrooms.value.filter((c) => c.floor === activeFloor.value)
})

const getStatusText = (s?: number) => {
  if (s === 1) return '正常'
  if (s === 2) return '维修中'
  if (s === 0) return '停用'
  return '未知'
}

const goClassroomReservation = (classroomId?: number) => {
  if (!classroomId) return
  router.push(`/reservation/classroom/${classroomId}`)
}

const goSchedule = () => {
  if (!buildingId) return
  router.push(`/building-schedule/${buildingId}`)
}

const loadData = async () => {
  loading.value = true
  try {
    const [bRes, cRes] = await Promise.all([getBuildingById(buildingId), getClassroomsByBuilding(buildingId)])
    building.value = bRes.data ?? null
    classrooms.value = cRes.data ?? []
    activeFloor.value = floorOptions.value[0] ?? null
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="building-detail-page">
    <van-nav-bar title="教学楼详情" left-arrow @click-left="router.back()" />
    <div class="content" v-if="!loading && building">
      <div class="top-card">
        <div class="name">{{ building.name }}</div>
        <div class="sub">{{ building.buildingNumber || '--' }} · {{ building.address || '暂无地址' }}</div>
        <div class="sub">楼层数：{{ building.floorCount || '--' }}</div>
        <button class="schedule-btn" @click="goSchedule">查看课程表</button>
      </div>

      <div class="floor-tabs">
        <button
          v-for="f in floorOptions"
          :key="f"
          class="tab"
          :class="{ active: activeFloor === f }"
          @click="activeFloor = f"
        >
          {{ f }}F
        </button>
      </div>

      <div class="classroom-list">
        <div v-for="c in visibleClassrooms" :key="c.id" class="classroom-item" @click="goClassroomReservation(c.id)">
          <div class="left">
            <div class="title">{{ c.name }}</div>
            <div class="desc">教室号 {{ c.roomNumber || '--' }} · 容量 {{ c.capacity || '--' }} 人</div>
          </div>
          <div class="status">{{ getStatusText(c.status) }}</div>
        </div>
        <van-empty v-if="visibleClassrooms.length === 0" description="该楼层暂无教室" />
      </div>
    </div>
    <van-loading v-else vertical size="24px">加载中...</van-loading>
  </div>
</template>

<style scoped>
.building-detail-page { min-height: 100vh; background: #f5f7fa; }
.content { padding: 12px; }
.top-card { background: #fff; border-radius: 12px; padding: 14px; }
.name { font-size: 17px; font-weight: 700; color: #1a1a1a; }
.sub { margin-top: 6px; font-size: 12px; color: #8b8b8b; }
.schedule-btn {
  margin-top: 8px;
  border: none;
  border-radius: 10px;
  padding: 6px 10px;
  background: #eef4ff;
  color: #3370e7;
}
.floor-tabs { margin-top: 12px; display: flex; gap: 8px; overflow-x: auto; }
.tab { border: none; background: #eef2f7; color: #666; border-radius: 14px; padding: 6px 12px; }
.tab.active { background: #4a90e2; color: #fff; }
.classroom-list { margin-top: 10px; }
.classroom-item { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 8px; display: flex; justify-content: space-between; gap: 10px; align-items: center; cursor: pointer; }
.title { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.desc { margin-top: 4px; font-size: 12px; color: #8b8b8b; }
.status { font-size: 12px; color: #4a90e2; background: #f0f7ff; padding: 3px 8px; border-radius: 10px; flex-shrink: 0; }
</style>

