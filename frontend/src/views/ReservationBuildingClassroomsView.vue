<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBuildingById, type Building } from '@/api/building'
import { getClassroomsByBuilding, type Classroom } from '@/api/classroom'
import { getClassroomSlots, type ClassroomSlotStatus } from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const buildingId = Number(route.params.buildingId)
const classroomType = Number(route.params.type)

const loading = ref(false)
const building = ref<Building | null>(null)
const classrooms = ref<Classroom[]>([])
const classroomSlots = ref<Record<number, ClassroomSlotStatus[]>>({})
const activeFloor = ref<number | null>(null)

const pageTitle = computed(() => (classroomType === 2 ? '研讨室列表' : '普通教室列表'))

const sortedClassrooms = computed(() => {
  return [...classrooms.value].sort((a, b) => {
    const fa = a.floor ?? Number.MAX_SAFE_INTEGER
    const fb = b.floor ?? Number.MAX_SAFE_INTEGER
    if (fa !== fb) return fa - fb
    return (a.roomNumber || a.name || '').localeCompare((b.roomNumber || b.name || ''), 'zh-Hans-CN', {
      numeric: true,
      sensitivity: 'base',
    })
  })
})

const floorOptions = computed(() => {
  const set = new Set<number>()
  sortedClassrooms.value.forEach((c) => {
    if (typeof c.floor === 'number') set.add(c.floor)
  })
  return Array.from(set.values()).sort((a, b) => a - b)
})

const visibleClassrooms = computed(() => {
  if (activeFloor.value == null) return sortedClassrooms.value
  return sortedClassrooms.value.filter((c) => c.floor === activeFloor.value)
})

const goClassroomDetail = (id?: number) => {
  if (!id) return
  router.push(`/reservation/classroom/${id}`)
}

const getTodayDate = () => {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const loadSlotStatus = async (list: Classroom[]) => {
  const record: Record<number, ClassroomSlotStatus[]> = {}
  const date = getTodayDate()
  await Promise.all(
    list
      .filter((c) => !!c.id)
      .map(async (c) => {
        try {
          const res = await getClassroomSlots(c.id!, date)
          const maybe = (res as unknown as { data?: unknown }).data
          if (Array.isArray(maybe)) {
            record[c.id!] = maybe as ClassroomSlotStatus[]
          } else if (maybe && Array.isArray((maybe as { data?: unknown }).data)) {
            record[c.id!] = (maybe as { data: ClassroomSlotStatus[] }).data
          }
        } catch (e) {
          console.error('load classroom slots failed', e)
        }
      }),
  )
  classroomSlots.value = record
}

const getRoomStatusText = (room: Classroom) => {
  if (room.realTimeStatus === 2 || room.status === 2) return '维修中'
  const slots = room.id ? (classroomSlots.value[room.id] ?? []) : []
  if (slots.length > 0 && slots.every((s) => s.status === 'occupied')) return '已约满'
  return '可预约'
}

const getRoomStatusClass = (room: Classroom) => {
  const text = getRoomStatusText(room)
  if (text === '维修中') return 'status-maintaining'
  if (text === '已约满') return 'status-full'
  return 'status-available'
}

const loadData = async () => {
  if (!buildingId || ![1, 2].includes(classroomType)) {
    router.back()
    return
  }
  loading.value = true
  try {
    const [bRes, cRes] = await Promise.all([getBuildingById(buildingId), getClassroomsByBuilding(buildingId)])
    building.value = bRes.data ?? null
    const list = (cRes.data ?? []).filter((c) => c.type === classroomType && c.status === 1)
    classrooms.value = list
    await loadSlotStatus(list)
    activeFloor.value = floorOptions.value[0] ?? null
  } catch (e) {
    console.error(e)
    classrooms.value = []
    classroomSlots.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-wrap">
    <van-nav-bar :title="pageTitle" left-arrow @click-left="router.back()" />
    <div v-if="!loading" class="content">
      <div class="building-card">
        <div class="name">{{ building?.name || `教学楼 #${buildingId}` }}</div>
        <div class="sub">{{ building?.buildingNumber || '--' }} · {{ building?.address || '暂无地址' }}</div>
      </div>

      <div class="floor-tabs">
        <button
          v-for="f in floorOptions"
          :key="f"
          class="tab"
          :class="{ active: activeFloor === f }"
          @click="activeFloor = f"
        >
          {{ f }}层
        </button>
      </div>

      <div class="classroom-list">
        <div
          v-for="room in visibleClassrooms"
          :key="room.id"
          class="room-item"
          @click="goClassroomDetail(room.id)"
        >
          <div class="left">
            <div class="title">{{ room.name }}</div>
            <div class="desc">
              教室号 {{ room.roomNumber || '--' }} · 容量 {{ room.capacity || '--' }} 人
            </div>
          </div>
          <div class="right" :class="getRoomStatusClass(room)">{{ getRoomStatusText(room) }}</div>
        </div>
        <van-empty v-if="visibleClassrooms.length === 0" description="该楼层暂无可预约教室" />
      </div>
    </div>
    <van-loading v-else vertical size="24px">加载中...</van-loading>
  </div>
</template>

<style scoped>
.page-wrap { min-height: 100vh; background: #f5f7fa; }
.content { padding: 12px; }
.building-card { background: #fff; border-radius: 12px; padding: 14px; }
.name { font-size: 17px; font-weight: 700; color: #1a1a1a; }
.sub { margin-top: 6px; font-size: 12px; color: #8b8b8b; }
.floor-tabs { margin-top: 12px; display: flex; gap: 8px; overflow-x: auto; }
.tab { border: none; background: #eef2f7; color: #666; border-radius: 14px; padding: 6px 12px; }
.tab.active { background: #4a90e2; color: #fff; }
.classroom-list { margin-top: 10px; }
.room-item {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.title { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.desc { margin-top: 4px; font-size: 12px; color: #8b8b8b; }
.right {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}
.status-available {
  color: #1d8f49;
  background: #eaf8ee;
}
.status-maintaining {
  color: #b45309;
  background: #fff3e0;
}
.status-full {
  color: #8a8f99;
  background: #edf0f3;
}
</style>
