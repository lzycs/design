<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Cell, CellGroup } from 'vant'
import { getAvailableClassrooms, type Classroom } from '@/api/classroom'
import { getLibraryList, type Library } from '../api/library'

const activeTab = ref(0)
const normalClassrooms = ref<Classroom[]>([])
const seminarRooms = ref<Classroom[]>([])
const libraries = ref<Library[]>([])

const loading = ref(false)

const loadClassroomsByType = async (type: number): Promise<Classroom[]> => {
  const res = await getAvailableClassrooms(type)
  // 兼容两种返回类型：
  // - axios 默认类型: AxiosResponse<Result<Classroom[]>>
  // - 我们的拦截器实际返回: Result<Classroom[]>
  const maybeData = (res as unknown as { data?: unknown }).data
  if (Array.isArray(maybeData)) return maybeData as Classroom[]
  if (maybeData && Array.isArray((maybeData as { data?: unknown }).data)) {
    return (maybeData as { data: Classroom[] }).data
  }
  return []
}

const loadAll = async () => {
  try {
    loading.value = true
    const [normal, seminar, libraryList] = await Promise.all([
      loadClassroomsByType(1), // 普通教室
      loadClassroomsByType(2), // 研讨室/会议室
      getLibraryList()
    ])
    normalClassrooms.value = normal
    seminarRooms.value = seminar
    const maybeLibData = (libraryList as unknown as { data?: unknown }).data
    if (Array.isArray(maybeLibData)) {
      libraries.value = maybeLibData as Library[]
    } else if (maybeLibData && Array.isArray((maybeLibData as { data?: unknown }).data)) {
      libraries.value = (maybeLibData as { data: Library[] }).data
    } else {
      libraries.value = []
    }
  } catch (error) {
    console.error('Failed to load classrooms:', error)
    normalClassrooms.value = []
    seminarRooms.value = []
    libraries.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="reservation">
    <van-nav-bar title="在线预约预订" />
    
    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="普通教室">
        <CellGroup>
          <Cell
            v-for="classroom in normalClassrooms"
            :key="classroom.id"
            :title="`${classroom.name}`"
            :label="`楼层: ${classroom.floor} | 容量: ${classroom.capacity}`"
            is-link
          />
          <Cell v-if="!loading && normalClassrooms.length === 0" title="暂无普通教室数据" />
        </CellGroup>
      </van-tab>
      <van-tab title="研讨室">
        <CellGroup>
          <Cell
            v-for="classroom in seminarRooms"
            :key="classroom.id"
            :title="`${classroom.name}`"
            :label="`楼层: ${classroom.floor} | 容量: ${classroom.capacity}`"
            is-link
          />
          <Cell v-if="!loading && seminarRooms.length === 0" title="暂无研讨室数据" />
        </CellGroup>
      </van-tab>
      <van-tab title="图书馆座位">
        <CellGroup>
          <Cell
            v-for="library in libraries"
            :key="library.id"
            :title="library.name"
            :label="`${library.address || ''} | 楼层数: ${library.floorCount ?? ''}`"
            is-link
          />
          <Cell v-if="!loading && libraries.length === 0" title="暂无图书馆数据" />
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
