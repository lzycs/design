<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBuildingList, type Building } from '@/api/building'
import { Cell, CellGroup, Tag } from 'vant'

const router = useRouter()
const buildings = ref<Building[]>([])

const loadBuildings = async () => {
  try {
    const res = await getBuildingList()
    buildings.value = res.data ?? []
  } catch (error) {
    console.error('Failed to load buildings:', error)
  }
}

const goToClassrooms = (buildingId: number) => {
  router.push(`/classrooms/${buildingId}`)
}

onMounted(() => {
  loadBuildings()
})
</script>

<template>
  <div class="home">
    <van-nav-bar title="校园资源中心" />
    
    <div class="core-functions">
      <van-grid column-num="2" :border="false">
        <van-grid-item icon="map-o" text="实时状态查询" background-color="#93C5FD" />
        <van-grid-item icon="calendar-o" text="在线预约预订" background-color="#1E40AF" />
        <van-grid-item icon="tools-o" text="设施报修反馈" background-color="#F97316" />
        <van-grid-item icon="team-o" text="小组协作匹配" background-color="#86EFAC" />
      </van-grid>
    </div>

    <div class="popular-reservations">
      <h2>热门预约</h2>
      <CellGroup>
        <Cell
          v-for="building in buildings"
          :key="building.id"
          :title="building.name"
          :label="`今日14:00-16:00 可预约`"
          is-link
          @click="goToClassrooms(building.id!)"
        >
          <template #right-icon>
            <Tag type="danger">🔥高热度</Tag>
          </template>
        </Cell>
      </CellGroup>
    </div>
  </div>
</template>

<style scoped>
.home {
  flex: 1;
  background-color: #f5f5f5;
}

.core-functions {
  padding: 16px;
}

.popular-reservations {
  padding: 16px;
}

.popular-reservations h2 {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: bold;
}
</style>
