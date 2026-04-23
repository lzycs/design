<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getBuildingList, type Building } from '@/api/building'

type BuildingWithCoord = Building & { id: number; latitude: number; longitude: number }
type CampusSpot = { id: string; name: string; address: string; latitude: number; longitude: number; category: string }
type CampusMapPoint = {
  id: string
  name: string
  buildingNumber: string
  address: string
  latitude: number
  longitude: number
  category: string
  sourceType: 'building' | 'spot'
  rawBuildingId: number | null
}

const router = useRouter()
const loading = ref(false)
const locating = ref(false)
const keyword = ref('')
const buildings = ref<BuildingWithCoord[]>([])
const selectedBuildingId = ref<string | null>(null)
const myLocation = ref<{ lat: number; lng: number } | null>(null)
// 公开地图 POI 坐标（浙工商下沙校区），用于补充非教学楼导航点位
const extraSpots = ref<CampusSpot[]>([
  {
    id: 'stadium-1',
    name: '浙江工商大学第一田径场',
    address: '浙江工商大学下沙校区（学正街18号）',
    latitude: 30.307981,
    longitude: 120.382695,
    category: '体育场地',
  },
  {
    id: 'stadium-2',
    name: '风雨操场',
    address: '浙江工商大学下沙校区体育区',
    latitude: 30.309825,
    longitude: 120.37803,
    category: '体育场地',
  },
  {
    id: 'gym-center',
    name: '浙江工商大学文体中心场馆',
    address: '浙江工商大学下沙校区文体中心',
    latitude: 30.312487,
    longitude: 120.378441,
    category: '体育场馆',
  },
  {
    id: 'swimming',
    name: '游泳馆',
    address: '浙江工商大学下沙校区体育区',
    latitude: 30.30961,
    longitude: 120.378691,
    category: '体育场馆',
  },
  {
    id: 'library-main',
    name: '浙江工商大学图书馆',
    address: '浙江工商大学下沙校区图书馆',
    latitude: 30.312244,
    longitude: 120.385042,
    category: '学习服务',
  },
  {
    id: 'student-center',
    name: '学生活动中心',
    address: '浙江工商大学下沙校区学生活动中心',
    latitude: 30.311917,
    longitude: 120.382572,
    category: '校园服务',
  },
])

const mapEl = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let poiLayer: L.LayerGroup | null = null
let myLocationLayer: L.CircleMarker | null = null
let routeLayer: L.Polyline | null = null
let selectedLabelMarker: L.Marker | null = null

// 高德底图使用 GCJ-02，这里将 WGS-84 坐标转换后再渲染，避免点位偏移
const transformLat = (lng: number, lat: number) => {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin((lat / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((lat / 12.0) * Math.PI) + 320 * Math.sin((lat * Math.PI) / 30.0)) * 2.0) / 3.0
  return ret
}
const transformLng = (lng: number, lat: number) => {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin((lng / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((lng / 12.0) * Math.PI) + 300.0 * Math.sin((lng / 30.0) * Math.PI)) * 2.0) / 3.0
  return ret
}
const outOfChina = (lng: number, lat: number) => {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}
const wgs84ToGcj02 = (lng: number, lat: number) => {
  if (outOfChina(lng, lat)) return { lng, lat }
  const a = 6378245.0
  const ee = 0.00669342162296594323
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
  dLng = (dLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI)
  return { lng: lng + dLng, lat: lat + dLat }
}

const filteredBuildings = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const buildingItems: CampusMapPoint[] = buildings.value.map((item) => ({
    id: `building-${item.id}`,
    name: item.name || '',
    buildingNumber: item.buildingNumber || '',
    address: item.address || '',
    latitude: wgs84ToGcj02(item.longitude, item.latitude).lat,
    longitude: wgs84ToGcj02(item.longitude, item.latitude).lng,
    category: '教学楼',
    sourceType: 'building' as const,
    rawBuildingId: item.id,
  }))
  const spotItems: CampusMapPoint[] = extraSpots.value.map((item) => ({
    ...item,
    id: item.id,
    name: item.name,
    buildingNumber: '',
    address: item.address,
    latitude: wgs84ToGcj02(item.longitude, item.latitude).lat,
    longitude: wgs84ToGcj02(item.longitude, item.latitude).lng,
    category: item.category,
    sourceType: 'spot' as const,
    rawBuildingId: null,
  }))

  return [...buildingItems, ...spotItems].filter((item) => {
    if (!kw) return true
    return item.name.toLowerCase().includes(kw) || item.buildingNumber.toLowerCase().includes(kw) || item.address.toLowerCase().includes(kw)
  })
})

const selectedBuilding = computed(() => {
  if (!selectedBuildingId.value) return null
  return filteredBuildings.value.find((item) => item.id === selectedBuildingId.value) ?? null
})

const formatDistance = (meter: number) => {
  if (meter < 1000) return `${Math.round(meter)}m`
  return `${(meter / 1000).toFixed(2)}km`
}

const calcDistanceMeter = (lat1: number, lng1: number, lat2: number, lng2: number) => {
  const toRad = (d: number) => (d * Math.PI) / 180
  const R = 6371000
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

const drawRouteLine = (target: { latitude: number; longitude: number }) => {
  if (!map || !myLocation.value) return
  if (routeLayer) {
    routeLayer.remove()
  }
  routeLayer = L.polyline(
    [
      [myLocation.value.lat, myLocation.value.lng],
      [target.latitude, target.longitude],
    ],
    { color: '#1f6feb', weight: 4, opacity: 0.75, dashArray: '6 8' },
  ).addTo(map)
}

const renderSelectedLabel = (point: CampusMapPoint) => {
  if (!map) return
  if (selectedLabelMarker) {
    selectedLabelMarker.remove()
  }
  selectedLabelMarker = L.marker([point.latitude, point.longitude], {
    icon: L.divIcon({
      className: 'building-label-icon',
      html: `<div class="building-label-badge">${point.name}</div>`,
      iconSize: [140, 28],
      iconAnchor: [70, 38],
    }),
    interactive: false,
    keyboard: false,
  }).addTo(map)
}

const selectBuilding = (building: CampusMapPoint) => {
  selectedBuildingId.value = building.id
  if (!map) return
  map.flyTo([building.latitude, building.longitude], Math.max(map.getZoom(), 16), { duration: 0.8 })
  drawRouteLine(building)
  renderSelectedLabel(building)
}

const renderBuildingMarkers = () => {
  if (!map) return
  if (poiLayer) {
    poiLayer.remove()
  }

  poiLayer = L.layerGroup()

  filteredBuildings.value.forEach((item) => {
    const marker = L.circleMarker([item.latitude, item.longitude], {
      radius: selectedBuildingId.value === item.id ? 11 : 8,
      color: '#2b7fff',
      fillColor: item.sourceType === 'spot' ? '#ef4444' : selectedBuildingId.value === item.id ? '#1f6feb' : '#5ea1ff',
      fillOpacity: 0.9,
      weight: 2,
    })
      .bindPopup(`<b>${item.name}</b><br/>${item.address || '暂无地址信息'}`)
      .on('click', () => {
        selectBuilding(item)
      })
    poiLayer?.addLayer(marker)
  })
  poiLayer.addTo(map)
}

const renderMyLocation = () => {
  if (!map || !myLocation.value) return
  if (myLocationLayer) {
    myLocationLayer.remove()
  }
  myLocationLayer = L.circleMarker([myLocation.value.lat, myLocation.value.lng], {
    radius: 9,
    color: '#0ea5e9',
    fillColor: '#38bdf8',
    fillOpacity: 0.95,
    weight: 3,
  })
    .bindPopup('我的位置')
    .addTo(map)
}

const locateMe = () => {
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
      locating.value = false
      renderMyLocation()
      map?.flyTo([myLocation.value.lat, myLocation.value.lng], 16, { duration: 0.8 })
      if (selectedBuilding.value) {
        drawRouteLine(selectedBuilding.value)
      }
      showToast('定位成功')
    },
    () => {
      locating.value = false
      showToast('定位失败，请检查权限后重试')
    },
    { enableHighAccuracy: true, timeout: 8000 },
  )
}

const openExternalNavigation = () => {
  if (!selectedBuilding.value) {
    showToast('请先选择一个目的地')
    return
  }
  const target = selectedBuilding.value
  const targetName = encodeURIComponent(target.name)
  const to = `${target.longitude},${target.latitude},${targetName}`
  const gaodeUrl = `https://uri.amap.com/navigation?to=${to}&mode=walk&src=campus-learning-space`
  window.open(gaodeUrl, '_blank')
}

const initMap = async () => {
  await nextTick()
  if (!mapEl.value) return
  map = L.map(mapEl.value, { zoomControl: true }).setView([30.311041, 120.384348], 16)
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    attribution: '&copy; AutoNavi',
  }).addTo(map)
}

const loadBuildings = async () => {
  loading.value = true
  try {
    const res = await getBuildingList()
    const list = (res.data ?? [])
      .filter((item): item is BuildingWithCoord => {
        return !!item.id && item.latitude != null && item.longitude != null
      })
      .map((item) => ({
        ...item,
        latitude: Number(item.latitude),
        longitude: Number(item.longitude),
      }))

    buildings.value = list
    selectedBuildingId.value = list[0] ? `building-${list[0].id}` : extraSpots.value[0]?.id ?? null
    renderBuildingMarkers()

    if (map && (list.length > 0 || extraSpots.value.length > 0)) {
      const allPoints = [
        ...list.map((item) => [item.latitude, item.longitude] as [number, number]),
        ...extraSpots.value.map((item) => [item.latitude, item.longitude] as [number, number]),
      ]
      const bounds = L.latLngBounds(allPoints)
      map.fitBounds(bounds, { padding: [28, 28] })
    }
  } catch (error) {
    console.error(error)
    showToast('加载地图点位失败')
  } finally {
    loading.value = false
  }
}

const getDistanceText = (item: { latitude: number; longitude: number }) => {
  if (!myLocation.value) return '未定位'
  const meter = calcDistanceMeter(myLocation.value.lat, myLocation.value.lng, item.latitude, item.longitude)
  return `距离约 ${formatDistance(meter)}`
}

onMounted(async () => {
  await initMap()
  await loadBuildings()
  setTimeout(() => map?.invalidateSize(), 120)
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
  selectedLabelMarker = null
})
</script>

<template>
  <div class="campus-nav-page">
    <van-nav-bar title="校园导航" left-arrow @click-left="router.back()" />
    <div class="page-content">
      <div class="toolbar-card">
        <van-search v-model="keyword" placeholder="搜索教学楼/场馆/地址" @update:model-value="renderBuildingMarkers" />
        <div class="toolbar-actions">
          <van-button size="small" type="primary" plain :loading="locating" @click="locateMe">
            {{ locating ? '定位中...' : '我的位置' }}
          </van-button>
          <van-button size="small" type="primary" @click="openExternalNavigation">一键导航</van-button>
        </div>
      </div>

      <div ref="mapEl" class="map-panel" />

      <div class="poi-card">
        <div class="poi-title">
          <span>校园点位（教学楼+公共场馆）</span>
          <span class="poi-count">{{ filteredBuildings.length }} 个</span>
        </div>
        <van-loading v-if="loading" size="20px">加载中...</van-loading>
        <div v-else class="poi-list">
          <div
            v-for="item in filteredBuildings"
            :key="item.id"
            class="poi-item"
            :class="{ active: selectedBuildingId === item.id }"
            @click="selectBuilding(item)"
          >
            <div class="poi-head">
              <div class="poi-name">{{ item.name }}</div>
              <div class="poi-distance">{{ getDistanceText(item) }}</div>
            </div>
            <div class="poi-sub">{{ item.category }} · {{ item.buildingNumber || '--' }} · {{ item.address || '暂无地址信息' }}</div>
          </div>
          <van-empty v-if="filteredBuildings.length === 0" description="暂无匹配教学楼点位" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.campus-nav-page {
  min-height: 100vh;
  background: #f4f7fb;
}

.page-content {
  padding: 12px;
}

.toolbar-card {
  background: #fff;
  border-radius: 12px;
  padding: 10px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.map-panel {
  height: 42vh;
  min-height: 260px;
  border-radius: 12px;
  margin-top: 12px;
  overflow: hidden;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
}

.poi-card {
  margin-top: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.poi-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: #1f2937;
  font-weight: 600;
}

.poi-count {
  color: #1f6feb;
  font-size: 12px;
}

.poi-list {
  margin-top: 8px;
  max-height: 34vh;
  overflow-y: auto;
}

.poi-item {
  border: 1px solid #e5ebf3;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 8px;
  background: #fff;
}

.poi-item.active {
  border-color: #1f6feb;
  background: #f1f6ff;
}

.poi-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.poi-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.poi-distance {
  font-size: 12px;
  color: #1f6feb;
}

.poi-sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

:global(.building-label-icon) {
  background: transparent;
  border: none;
}

:global(.building-label-badge) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: rgba(31, 111, 235, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 14px;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.22);
}
</style>
