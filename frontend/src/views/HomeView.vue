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

const goToBuildingSchedule = (buildingId?: number) => {
  if (!buildingId) return
  router.push(`/building-schedule/${buildingId}`)
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

const goCampusNav = () => {
  router.push('/campus-nav')
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
      <!-- 顶部轻量头部 -->
      <div class="top-header">
        <div class="header-main">
          <div class="header-text">
            <h1>校园学习空间预约系统</h1>
            <p>便捷预约 · 高效学习</p>
          </div>
          <div class="header-avatar" @click="goMine">
            <van-icon name="user-o" class="avatar-icon" />
          </div>
        </div>
        <div class="header-tags">
          <span class="header-tag active">今日可预约</span>
          <span class="header-tag">多端协作</span>
        </div>
      </div>

      <!-- 功能入口 -->
      <div class="quick-section">
        <div class="quick-title">
          <div class="quick-title-left">
            <div class="quick-logo">
              <van-icon name="apps-o" />
            </div>
            <span>快捷功能</span>
          </div>
        </div>
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
          <div class="function-item" @click="goCampusNav">
            <van-icon name="location-o" class="func-icon" />
            <p>校园导航</p>
          </div>
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
            <div class="building-status" :class="{ maintenance: building.status !== 1 }">
              {{ building.status === 1 ? '可预约' : '部分维护' }}
            </div>
            <button class="course-btn" @click.stop="goToBuildingSchedule(building.id)">课程表</button>
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
  --color-primary: #3370e7;
  --color-primary-soft: #6691f7;
  --color-primary-bg: #eef4ff;
  --color-success: #2daf60;
  --color-danger: #f56c6c;
  width: 100%;
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 72px;
}

.top-header {
  background-color: #ffffff;
  border-radius: 0 0 16px 16px;
  box-shadow: 0 2px 14px rgba(15, 23, 42, 0.06);
  padding: 18px 20px 16px;
  margin-bottom: 16px;
}

.header-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-text h1 {
  font-size: 20px;
  line-height: 1.25;
  color: var(--color-primary);
  font-weight: 700;
}

.header-text p {
  margin-top: 7px;
  font-size: 12px;
  color: #98a2b3;
}

.header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--color-primary-bg);
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.header-tags {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.header-tag {
  font-size: 11px;
  color: var(--color-primary);
  background-color: var(--color-primary-bg);
  border: 1px solid #e0ecfb;
  border-radius: 16px;
  padding: 3px 10px;
}

.header-tag.active {
  color: #ffffff;
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.quick-section {
  margin: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  border-radius: 12px;
  border: 1px solid #e8eef7;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  padding: 16px 16px 18px;
}

.quick-title {
  padding: 0 0 12px;
}

.quick-title-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.quick-logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e8f2ff 0%, #f2f7ff 100%);
  color: var(--color-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-logo :deep(.van-icon) {
  font-size: 20px;
}

.quick-title-left span {
  font-size: 18px;
  color: #1e3657;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.avatar-icon {
  font-size: 18px;
  color: var(--color-primary);
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 0;
}

.function-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #ffffff;
  border: 1px solid #e2eaf5;
  padding: 16px 8px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.func-icon {
  font-size: 24px;
  color: var(--color-primary-soft);
  margin-bottom: 12px;
}

.function-item p {
  font-size: 16px;
  color: #333333;
  font-weight: 500;
}

.building-nav {
  margin: 0 20px 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
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
  color: var(--color-primary);
  font-size: 12px;
}

.module-icon {
  font-size: 18px;
  color: var(--color-primary);
  margin-right: 8px;
}

.building-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.building-item {
  padding: 16px 8px;
  border-radius: 12px;
  background-color: #ffffff;
  border: 1px solid #e2eaf5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.building-number {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.building-name {
  font-size: 16px;
  color: #333333;
  font-weight: 500;
}

.building-status {
  font-size: 12px;
  color: var(--color-success);
  margin-top: 4px;
}

.building-status.maintenance {
  color: var(--color-danger);
}
.course-btn {
  margin-top: 6px;
  border: none;
  border-radius: 10px;
  padding: 4px 8px;
  background: #eef4ff;
  color: #3370e7;
  font-size: 12px;
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
  color: var(--color-primary);
  border-bottom: 2px solid var(--color-primary);
}

@media (min-width: 1024px) {
  .home-page {
    padding-bottom: 84px;
    max-width: 1200px;
    margin: 0 auto;
  }

  .top-header {
    border-radius: 0 0 16px 16px;
    padding: 20px 28px 18px;
    margin-bottom: 24px;
  }

  .quick-title {
    padding: 0 0 14px;
  }

  .quick-section {
    margin: 24px 28px;
    padding: 20px 24px 24px;
    border: 1px solid #dfe8f4;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  }

  .quick-logo {
    width: 34px;
    height: 34px;
  }

  .quick-title-left span {
    font-size: 18px;
  }

  .header-text h1 {
    font-size: 24px;
  }

  .header-text p {
    font-size: 14px;
  }

  .header-avatar {
    width: 48px;
    height: 48px;
  }

  .avatar-icon {
    font-size: 24px;
  }

  .header-tags {
    margin-top: 14px;
  }

  .header-tag {
    font-size: 12px;
    padding: 4px 12px;
  }

  .function-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 18px;
    padding: 0;
  }

  .function-item {
    min-height: 126px;
    border: 1px solid #dce7f4;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }

  .building-nav {
    margin: 0 28px 24px;
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
    border: 1px solid #dce7f4;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
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
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }
}

@media (max-width: 375px) {
  .function-grid,
  .building-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
