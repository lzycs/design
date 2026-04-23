<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getBuildingList, type Building } from '@/api/building'

type BuildingWithDistance = Building & {
  distanceMeter?: number
}

const router = useRouter()
const loading = ref(false)
const locating = ref(false)
const keyword = ref('')
const filterTab = ref<'all' | 'near' | 'available'>('all')
const myLocation = ref<{ lat: number; lng: number } | null>(null)
const buildings = ref<BuildingWithDistance[]>([])

const toRad = (d: number) => (d * Math.PI) / 180
const calcDistanceMeter = (lat1: number, lng1: number, lat2: number, lng2: number) => {
  const R = 6371000
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(R * c)
}

const walkMinute = (meter?: number) => {
  if (meter == null) return '--'
  const minute = Math.max(1, Math.round(meter / 75))
  return `${minute} 分钟`
}

const formatDistance = (meter?: number) => {
  if (meter == null) return '--'
  if (meter < 1000) return `${meter}m`
  return `${(meter / 1000).toFixed(2)}km`
}

const locateMe = async () => {
  if (!navigator.geolocation) {
    showToast('当前设备不支持定位')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      myLocation.value = {
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
      }
      buildings.value = buildings.value
        .map((b) => {
          if (b.latitude == null || b.longitude == null) return { ...b, distanceMeter: undefined }
          const distance = calcDistanceMeter(
            myLocation.value!.lat,
            myLocation.value!.lng,
            Number(b.latitude),
            Number(b.longitude),
          )
          return { ...b, distanceMeter: distance }
        })
        .sort((a, b) => (a.distanceMeter ?? Number.MAX_SAFE_INTEGER) - (b.distanceMeter ?? Number.MAX_SAFE_INTEGER))
      locating.value = false
      showToast('定位成功，已按距离排序')
    },
    () => {
      locating.value = false
      showToast('定位失败，请检查定位权限')
    },
    {
      enableHighAccuracy: true,
      timeout: 6000,
    },
  )
}

const loadBuildings = async () => {
  loading.value = true
  try {
    const res = await getBuildingList()
    buildings.value = res.data ?? []
    if (myLocation.value) {
      await locateMe()
    }
  } catch (e) {
    console.error(e)
    showToast('加载教学楼失败')
  } finally {
    loading.value = false
  }
}

const visibleBuildings = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = buildings.value.filter((b) => {
    if (!kw) return true
    const nameHit = (b.name || '').toLowerCase().includes(kw)
    const numHit = (b.buildingNumber || '').toLowerCase().includes(kw)
    const addrHit = (b.address || '').toLowerCase().includes(kw)
    return nameHit || numHit || addrHit
  })

  if (filterTab.value === 'near') {
    list = list.filter((b) => b.distanceMeter != null)
  } else if (filterTab.value === 'available') {
    list = list.filter((b) => b.status === 1)
  }
  return list
})

const goBuildingDetail = (id?: number) => {
  if (!id) return
  router.push(`/building-nav/${id}`)
}

const goBuildingSchedule = (id?: number) => {
  if (!id) return
  router.push(`/building-schedule/${id}`)
}

onMounted(() => {
  loadBuildings()
})
</script>

<template>
  <div class="building-nav-page">
    <van-nav-bar title="教学楼导航" left-arrow @click-left="router.back()" />
    <div class="content">
      <div class="toolbar">
        <van-search v-model="keyword" placeholder="搜索教学楼/楼栋号/地址" />
        <div class="tabs">
          <button class="tab-btn" :class="{ active: filterTab === 'all' }" @click="filterTab = 'all'">全部</button>
          <button class="tab-btn" :class="{ active: filterTab === 'near' }" @click="filterTab = 'near'">最近</button>
          <button class="tab-btn" :class="{ active: filterTab === 'available' }" @click="filterTab = 'available'">可预约</button>
        </div>
      </div>

      <div class="list-wrap" v-if="!loading">
        <div v-for="b in visibleBuildings" :key="b.id" class="item" @click="goBuildingDetail(b.id)">
          <div class="item-main">
            <div class="title-row">
              <div class="title">{{ b.name }}</div>
              <div class="status" :class="{ off: b.status !== 1 }">{{ b.status === 1 ? '可预约' : '部分维护' }}</div>
            </div>
            <div class="sub">{{ b.buildingNumber || '--' }} · {{ b.address || '暂无地址' }}</div>
            <div class="meta">距离 {{ formatDistance(b.distanceMeter) }} · 步行约 {{ walkMinute(b.distanceMeter) }}</div>
            <div class="action-row">
              <button class="action-btn primary" @click.stop="goBuildingDetail(b.id)">楼栋详情</button>
              <button class="action-btn" @click.stop="goBuildingSchedule(b.id)">课程表</button>
            </div>
          </div>
        </div>
        <van-empty v-if="visibleBuildings.length === 0" description="没有找到匹配教学楼" />
      </div>

      <van-loading v-else vertical size="24px">加载中...</van-loading>
    </div>

    <button class="locate-btn" :disabled="locating" @click="locateMe">
      {{ locating ? '定位中...' : '我的位置' }}
    </button>
  </div>
</template>

<style scoped>
.building-nav-page { min-height: 100vh; background: #f5f7fa; }
.content { padding: 12px; padding-bottom: 80px; }
.tabs { display: flex; gap: 8px; margin-top: 10px; }
.tab-btn { border: none; padding: 6px 14px; border-radius: 14px; background: #eef2f7; color: #666; }
.tab-btn.active { background: #4a90e2; color: #fff; }
.item { background: #fff; border-radius: 12px; padding: 12px; margin-bottom: 10px; display: flex; gap: 10px; align-items: center; cursor: pointer; }
.item-main { flex: 1; min-width: 0; }
.title-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.title { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.status { font-size: 12px; color: #18a058; background: #edf9f1; padding: 2px 8px; border-radius: 10px; }
.status.off { color: #e6a23c; background: #fff7e8; }
.sub { margin-top: 5px; font-size: 12px; color: #8b8b8b; }
.meta { margin-top: 5px; font-size: 12px; color: #4a90e2; }
.action-row { margin-top: 8px; display: flex; gap: 8px; }
.action-btn {
  border: none;
  border-radius: 10px;
  padding: 4px 10px;
  font-size: 12px;
  background: #eef4ff;
  color: #3370e7;
}
.action-btn.primary {
  background: #f5f7fa;
  color: #667085;
}
.locate-btn {
  position: fixed; right: 14px; bottom: 16px; border: none; background: #1f6feb; color: #fff;
  border-radius: 20px; padding: 10px 14px; box-shadow: 0 4px 12px rgba(31,111,235,.3); font-size: 12px;
}
</style>

