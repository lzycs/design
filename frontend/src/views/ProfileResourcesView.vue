<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  confirmTrade,
  getCampusCredit,
  getMyMarketItems,
  getUserTrades,
  publishMarketItem,
  type CampusCredit,
  type ResourceMarketItem,
  type ResourceMarketTrade,
} from '@/api/resourceMarket'

type ResourceTab = 'mine' | 'trades' | 'done'

const router = useRouter()
const currentUser = ref<{ id: number } | null>(null)
const loading = ref(false)
const submitting = ref(false)
const activeTab = ref<ResourceTab>('mine')
const showPublish = ref(false)

const myItems = ref<ResourceMarketItem[]>([])
const myTrades = ref<ResourceMarketTrade[]>([])
const credit = ref<CampusCredit | null>(null)

const publishForm = ref<ResourceMarketItem>({
  title: '',
  description: '',
  category: '课程资料',
  resourceType: 1,
  isFree: 0,
  originType: 1,
  recommendedPlace: '图书馆服务台',
  sourceReference: '',
  price: null,
})

const categoryOptions = ['考研笔记', '课程资料', '闲置教材', '实验耗材', '二手设备', '技能服务']

const listedCount = computed(() => myItems.value.filter((item) => item.status === 1).length)
const ongoingTrades = computed(() => myTrades.value.filter((trade) => trade.status === 1))
const doneTrades = computed(() => myTrades.value.filter((trade) => trade.status === 2))

const tradeList = computed(() => {
  if (activeTab.value === 'trades') return ongoingTrades.value
  if (activeTab.value === 'done') return doneTrades.value
  return []
})

const loadUser = () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) return
  try {
    currentUser.value = JSON.parse(raw) as { id: number }
  } catch {
    currentUser.value = null
  }
}

const loadData = async () => {
  if (!currentUser.value?.id) return
  loading.value = true
  try {
    const [itemRes, tradeRes, creditRes] = await Promise.all([
      getMyMarketItems(currentUser.value.id),
      getUserTrades(currentUser.value.id),
      getCampusCredit(currentUser.value.id),
    ])
    myItems.value = itemRes.data ?? []
    myTrades.value = tradeRes.data ?? []
    credit.value = creditRes.data ?? null
  } catch (error) {
    console.error(error)
    showToast('加载我的资源失败')
  } finally {
    loading.value = false
  }
}

const itemStatusText = (status?: number) => {
  if (status === 0) return '待审核'
  if (status === 1) return '上架中'
  if (status === 2) return '审核拒绝'
  if (status === 5) return '已完成'
  return '已下架'
}

const tradeStatusText = (status?: number) => {
  if (status === 1) return '待确认'
  if (status === 2) return '已完成'
  return '已取消'
}

const canConfirmTrade = (trade: ResourceMarketTrade) => {
  if (!currentUser.value?.id || trade.status !== 1) return false
  if (trade.publisherId === currentUser.value.id) return trade.confirmPublisher !== 1
  if (trade.receiverId === currentUser.value.id) return trade.confirmReceiver !== 1
  return false
}

const handleConfirmTrade = async (trade: ResourceMarketTrade) => {
  if (!currentUser.value?.id || !trade.id) return
  try {
    await confirmTrade(trade.id, currentUser.value.id)
    showToast('确认成功')
    await loadData()
  } catch (error: any) {
    showToast(error?.response?.data?.message || '确认失败')
  }
}

const openPublish = () => {
  if (!currentUser.value?.id) {
    showToast('请先登录')
    return
  }
  showPublish.value = true
}

const submitPublish = async () => {
  if (!currentUser.value?.id) return
  if (!publishForm.value.title?.trim()) {
    showToast('请填写资源标题')
    return
  }
  if (!publishForm.value.description?.trim()) {
    showToast('请填写资源描述')
    return
  }
  submitting.value = true
  try {
    await publishMarketItem({
      ...publishForm.value,
      userId: currentUser.value.id,
      price: publishForm.value.isFree === 1 ? null : Number(publishForm.value.price || 0),
    })
    showToast('发布成功，等待审核')
    showPublish.value = false
    await loadData()
  } catch (error: any) {
    showToast(error?.response?.data?.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loadUser()
  await loadData()
})
</script>

<template>
  <div class="profile-resources">
    <van-nav-bar title="我的资源" left-arrow @click-left="router.push('/profile')">
      <template #right>
        <span class="header-btn" @click="openPublish">发布</span>
      </template>
    </van-nav-bar>

    <template v-if="currentUser?.id">
      <div class="content">
        <div class="summary-card">
          <div class="credit-row">
            <div class="label">校园学分</div>
            <div class="value">{{ credit?.creditScore ?? '-' }}</div>
          </div>
          <div class="credit-desc">{{ credit?.scoreComment || '保持良好交易记录可提升学分' }}</div>
          <div class="stats">
            <div class="stat-item">
              <div class="stat-value">{{ myItems.length }}</div>
              <div class="stat-label">发布总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ listedCount }}</div>
              <div class="stat-label">上架中</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ ongoingTrades.length }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </div>

        <div class="tab-row">
          <div class="tab-item" :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">我发布的</div>
          <div class="tab-item" :class="{ active: activeTab === 'trades' }" @click="activeTab = 'trades'">我参与的</div>
          <div class="tab-item" :class="{ active: activeTab === 'done' }" @click="activeTab = 'done'">已完成</div>
        </div>

        <div class="list-wrap" v-if="loading">
          <div class="empty-text">加载中...</div>
        </div>

        <div class="list-wrap" v-else-if="activeTab === 'mine'">
          <div v-for="item in myItems" :key="item.id" class="item-card">
            <div class="head">
              <div class="title">{{ item.title }}</div>
              <div class="status">{{ itemStatusText(item.status) }}</div>
            </div>
            <div class="desc">{{ item.description || '暂无描述' }}</div>
            <div class="meta">
              <span>{{ item.category || '未分类' }}</span>
              <span>{{ item.isFree === 1 ? '免费' : `¥${item.price ?? 0}` }}</span>
              <span>{{ item.createTime?.slice(0, 10) || '-' }}</span>
            </div>
          </div>
          <div v-if="myItems.length === 0" class="empty-block">
            <div class="empty-title">你还没有发布资源</div>
            <div class="empty-desc">去资源集市发布第一条内容吧</div>
            <button class="plain-btn" @click="router.push('/resource-market')">去资源集市</button>
          </div>
        </div>

        <div class="list-wrap" v-else>
          <div v-for="trade in tradeList" :key="trade.id" class="item-card">
            <div class="head">
              <div class="title">{{ trade.itemTitle || `资源 #${trade.itemId}` }}</div>
              <div class="status">{{ tradeStatusText(trade.status) }}</div>
            </div>
            <div class="desc">地点：{{ trade.meetingPlace || '图书馆服务台' }}</div>
            <div class="desc">备注：{{ trade.note || '无' }}</div>
            <div class="meta">
              <span>发布方：{{ trade.confirmPublisher === 1 ? '已确认' : '未确认' }}</span>
              <span>接收方：{{ trade.confirmReceiver === 1 ? '已确认' : '未确认' }}</span>
            </div>
            <div class="actions" v-if="canConfirmTrade(trade)">
              <button class="primary-btn" @click="handleConfirmTrade(trade)">确认交易</button>
            </div>
          </div>
          <div v-if="tradeList.length === 0" class="empty-block">
            <div class="empty-title">{{ activeTab === 'done' ? '暂无已完成交易' : '暂无进行中交易' }}</div>
            <div class="empty-desc">可在资源集市浏览资源并发起交易意向</div>
            <button class="plain-btn" @click="router.push('/resource-market')">去资源集市</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <van-empty description="请先登录后查看我的资源" />
    </template>

    <van-popup
      v-model:show="showPublish"
      round
      position="bottom"
      :style="{ height: '72%', width: 'min(760px, 96vw)', left: '50%', transform: 'translateX(-50%)' }"
    >
      <div class="publish-panel">
        <div class="form-title">发布资源</div>
        <input v-model="publishForm.title" class="field" type="text" placeholder="资源标题" />
        <textarea v-model="publishForm.description" class="field area" placeholder="资源描述" />
        <select v-model="publishForm.category" class="field">
          <option v-for="item in categoryOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model.number="publishForm.resourceType" class="field">
          <option :value="1">实物资料</option>
          <option :value="2">文档资料</option>
          <option :value="3">技能服务</option>
        </select>
        <select v-model.number="publishForm.isFree" class="field">
          <option :value="0">有偿互助</option>
          <option :value="1">免费共享</option>
        </select>
        <input
          v-if="publishForm.isFree !== 1"
          v-model.number="publishForm.price"
          class="field"
          type="number"
          placeholder="金额"
        />
        <input v-model="publishForm.recommendedPlace" class="field" type="text" placeholder="推荐交易地点" />
        <button class="submit-btn" :disabled="submitting" @click="submitPublish">提交审核</button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-resources { min-height: 100vh; background: #f5f7fa; }
.header-btn { color: #4a90e2; font-size: 14px; }
.content { padding: 12px; }
.summary-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e7edf6;
  padding: 14px;
  margin-bottom: 12px;
}
.credit-row { display: flex; justify-content: space-between; align-items: center; }
.label { color: #64748b; font-size: 13px; }
.value { color: #2f6fb8; font-size: 24px; font-weight: 700; }
.credit-desc { margin-top: 4px; color: #8a94a6; font-size: 12px; }
.stats { margin-top: 12px; display: flex; gap: 10px; }
.stat-item { flex: 1; background: #f7faff; border-radius: 10px; padding: 10px; text-align: center; }
.stat-value { font-size: 18px; font-weight: 700; color: #1f3f68; }
.stat-label { margin-top: 3px; font-size: 12px; color: #64748b; }
.tab-row { display: flex; gap: 8px; margin-bottom: 10px; }
.tab-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  background: #edf2f8;
  color: #64748b;
  border-radius: 16px;
  font-size: 13px;
}
.tab-item.active { background: #4a90e2; color: #fff; }
.item-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8edf5;
  padding: 12px;
  margin-bottom: 10px;
}
.head { display: flex; justify-content: space-between; gap: 8px; }
.title { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.status { font-size: 12px; color: #4a90e2; }
.desc { margin-top: 6px; color: #5b6472; font-size: 13px; line-height: 1.5; }
.meta { margin-top: 8px; color: #8b8b8b; font-size: 12px; display: flex; gap: 8px; flex-wrap: wrap; }
.actions { margin-top: 10px; }
.primary-btn {
  border: none;
  background: #4a90e2;
  color: #fff;
  border-radius: 8px;
  padding: 7px 12px;
}
.empty-text { text-align: center; color: #94a3b8; padding: 24px 0; }
.empty-block {
  background: #fff;
  border-radius: 12px;
  border: 1px dashed #dbe5f2;
  padding: 18px 14px;
  text-align: center;
}
.empty-title { font-size: 14px; color: #334155; font-weight: 600; }
.empty-desc { margin-top: 6px; color: #94a3b8; font-size: 12px; }
.plain-btn {
  margin-top: 10px;
  border: 1px solid #bcd5f8;
  background: #eff6ff;
  color: #2f6fb8;
  border-radius: 8px;
  padding: 7px 12px;
}
.publish-panel { padding: 14px; height: 100%; overflow-y: auto; box-sizing: border-box; }
.form-title { font-size: 16px; font-weight: 700; margin-bottom: 10px; }
.field {
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
}
.area { min-height: 90px; resize: none; }
.submit-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 10px;
  background: #4a90e2;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}
.submit-btn:disabled { opacity: 0.6; }
</style>
