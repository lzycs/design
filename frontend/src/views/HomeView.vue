<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBuildingList, type Building } from '@/api/building'

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
  router.push(`/building-nav/${buildingId}`)
}

const goBuildingNav = () => {
  router.push('/building-nav')
}

const goReservation = () => {
  router.push('/reservation')
}

const goFeedback = () => {
  router.push('/feedback')
}

const goCollaboration = () => {
  router.push('/collaboration')
}

const goSharedPlan = () => {
  router.push('/shared-plan')
}

const goResourceMarket = () => {
  router.push('/resource-market')
}

const goMine = () => {
  router.push('/profile')
}

onMounted(() => {
  loadBuildings()
})
</script>

<template>
  <div class="home">
    <div class="home-page">
      <!-- 顶部 Banner -->
      <div class="banner">
        <h1>校园学习空间预约系统</h1>
        <p>便捷预约，高效学习</p>
        <div class="banner-avatar">
          <van-icon name="user-circle-o" class="avatar-icon" />
        </div>
      </div>

      <!-- 功能入口 -->
      <div class="function-grid">
        <div class="function-item" @click="goReservation">
          <van-icon name="calendar-o" class="func-icon" />
          <p>在线预约</p>
        </div>
        <div class="function-item" @click="goFeedback">
          <van-icon name="setting-o" class="func-icon" />
          <p>反馈评价</p>
        </div>
        <div class="function-item" @click="goCollaboration">
          <van-icon name="friends-o" class="func-icon" />
          <p>组队匹配</p>
        </div>
        <div class="function-item" @click="goSharedPlan">
          <van-icon name="notes-o" class="func-icon" />
          <p>共享计划</p>
        </div>
        <div class="function-item" @click="goResourceMarket">
          <van-icon name="shop-o" class="func-icon" />
          <p>资源集市</p>
        </div>
      </div>

      <!-- 教学楼导航 -->
      <div class="building-nav">
        <div class="module-title">
          <van-icon name="home-o" class="module-icon" />
          <span>教学楼导航</span>
          <button class="more-btn" @click="goBuildingNav">查看全部</button>
        </div>
        <div class="building-list">
          <div
            v-for="(building, index) in buildings"
            :key="building.id"
            class="building-item"
            @click="goToClassrooms(building.id!)"
          >
            <div class="building-number">{{ index + 1 }}</div>
            <div class="building-name">{{ building.name }}</div>
            <div class="building-status">
              {{ building.status === 1 ? '可预约' : '部分维护' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 底部 TabBar（仅样式，路由逻辑由 App.vue 控制） -->
      <div class="tab-bar">
        <div class="tab-item active">
          <van-icon name="home-o" />
          <span>首页</span>
        </div>
        <div class="tab-item" @click="goReservation">
          <van-icon name="calendar-o" />
          <span>预约</span>
        </div>
        <div class="tab-item" @click="goCollaboration">
          <van-icon name="friends-o" />
          <span>协作</span>
        </div>
        <div class="tab-item" @click="goMine">
          <van-icon name="user-o" />
          <span>我的</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.home-page {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 72px;
}

.banner {
  width: 100%;
  height: 180px;
  background: linear-gradient(135deg, #4a90e2 0%, #5c6bc0 100%);
  border-radius: 0 0 24px 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #ffffff;
  padding: 20px;
  position: relative;
}

.banner h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.banner p {
  font-size: 14px;
  opacity: 0.9;
}

.banner-avatar {
  position: absolute;
  bottom: -30px;
  right: 20px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.avatar-icon {
  font-size: 32px;
  color: #4a90e2;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 40px 20px 20px;
}

.function-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #ffffff;
  padding: 16px 8px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.func-icon {
  font-size: 28px;
  color: #4a90e2;
  margin-bottom: 8px;
}

.function-item p {
  font-size: 14px;
  color: #333333;
  font-weight: 500;
}

.building-nav {
  margin: 20px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px;
}

.module-title {
  font-size: 18px;
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.more-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: #4a90e2;
  font-size: 12px;
}

.module-icon {
  font-size: 20px;
  color: #4a90e2;
  margin-right: 8px;
}

.building-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.building-item {
  padding: 16px;
  border-radius: 12px;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.building-number {
  font-size: 24px;
  font-weight: 600;
  color: #4a90e2;
  margin-bottom: 4px;
}

.building-name {
  font-size: 14px;
  color: #333333;
}

.building-status {
  font-size: 12px;
  color: #67c23a;
  margin-top: 4px;
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  max-width: 375px;
  height: 56px;
  background-color: #ffffff;
  border-top: 1px solid #f5f5f5;
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #909399;
  font-size: 12px;
}

.tab-item :deep(.van-icon) {
  font-size: 24px;
  margin-bottom: 4px;
}

.tab-item.active {
  color: #4a90e2;
}

@media (min-width: 1024px) {
  .home-page {
    padding-bottom: 84px;
  }

  .banner {
    height: 220px;
    border-radius: 0 0 28px 28px;
  }

  .banner h1 {
    font-size: 30px;
  }

  .banner p {
    font-size: 16px;
  }

  .banner-avatar {
    width: 72px;
    height: 72px;
    right: 32px;
    bottom: -36px;
  }

  .avatar-icon {
    font-size: 40px;
  }

  .function-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 18px;
    padding: 52px 28px 24px;
  }

  .function-item {
    min-height: 126px;
    border: 1px solid #e9eef6;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }

  .building-nav {
    margin: 24px 28px;
    padding: 24px;
    border: 1px solid #e9eef6;
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  }

  .building-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
  }

  .building-item {
    min-height: 116px;
    justify-content: center;
    border: 1px solid #e8edf5;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }

  .tab-bar {
    display: none;
  }
}

@media (min-width: 1024px) and (hover: hover) {
  .function-item:hover,
  .building-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 22px rgba(15, 23, 42, 0.1);
  }
}
</style>
