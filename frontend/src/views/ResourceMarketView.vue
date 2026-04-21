<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  createTradeIntent,
  getCampusCredit,
  getMarketItems,
  getMyMarketItems,
  getUserTrades,
  publishMarketItem,
  confirmTrade,
  type CampusCredit,
  type ResourceMarketItem,
  type ResourceMarketTrade,
} from '@/api/resourceMarket'

const router = useRouter()
const currentUser = ref<{ id: number } | null>(null)

const tab = ref<'market' | 'free' | 'mine' | 'trades'>('market')
const loading = ref(false)
const items = ref<ResourceMarketItem[]>([])
const myItems = ref<ResourceMarketItem[]>([])
const myTrades = ref<ResourceMarketTrade[]>([])
const credit = ref<CampusCredit | null>(null)
const showPublish = ref(false)

const publishForm = ref<ResourceMarketItem>({
  title: '',
  description: '',
  category: '考研笔记',
  resourceType: 1,
  isFree: 0,
  originType: 1,
  recommendedPlace: '图书馆服务台',
  sourceReference: '',
  price: null,
})

const categoryOptions = [
  '考研笔记',
  '课程资料',
  '闲置教材',
  '实验耗材',
  '二手设备',
  '技能服务',
]

const visibleItems = computed(() => {
  if (tab.value === 'free') return items.value.filter((i) => i.isFree === 1)
  if (tab.value === 'mine') return myItems.value
  return items.value
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

const loadAll = async () => {
  loading.value = true
  try {
    const [marketRes, mineRes, tradeRes, creditRes] = await Promise.all([
      getMarketItems(),
      currentUser.value?.id ? getMyMarketItems(currentUser.value.id) : Promise.resolve({ data: [] }),
      currentUser.value?.id ? getUserTrades(currentUser.value.id) : Promise.resolve({ data: [] }),
      currentUser.value?.id ? getCampusCredit(currentUser.value.id) : Promise.resolve({ data: null }),
    ])
    items.value = marketRes.data ?? []
    myItems.value = mineRes.data ?? []
    myTrades.value = tradeRes.data ?? []
    credit.value = creditRes.data ?? null
  } catch (e) {
    console.error(e)
    showToast('加载资源集市失败')
  } finally {
    loading.value = false
  }
}

const openPublish = () => {
  if (!currentUser.value?.id) {
    showToast('请先登录')
    router.push('/profile')
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
  try {
    const payload: ResourceMarketItem = {
      ...publishForm.value,
      userId: currentUser.value.id,
      price: publishForm.value.isFree === 1 ? null : Number(publishForm.value.price || 0),
    }
    await publishMarketItem(payload)
    showToast('发布成功，等待审核')
    showPublish.value = false
    await loadAll()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '发布失败')
  }
}

const applyItem = async (item: ResourceMarketItem) => {
  if (!currentUser.value?.id || !item.id) return
  try {
    await createTradeIntent(item.id, {
      userId: currentUser.value.id,
      note: item.isFree === 1 ? '申请免费领取' : '申请互助交易',
      meetingPlace: item.recommendedPlace || '图书馆服务台',
    })
    showToast('已发起申请，请等待面交')
    await loadAll()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '申请失败')
  }
}

const confirmMyTrade = async (trade: ResourceMarketTrade) => {
  if (!currentUser.value?.id || !trade.id) return
  try {
    await confirmTrade(trade.id, currentUser.value.id)
    showToast('确认成功')
    await loadAll()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '确认失败')
  }
}

const statusText = (status?: number) => {
  if (status === 0) return '待审核'
  if (status === 1) return '已上架'
  if (status === 2) return '已驳回'
  if (status === 4) return '流转中'
  if (status === 5) return '已完成'
  return '已下架'
}

const tradeText = (status?: number) => {
  if (status === 1) return '待面交确认'
  if (status === 2) return '已完成'
  return '已取消'
}

onMounted(async () => {
  loadUser()
  await loadAll()
})
</script>

<template>
  <div class="resource-market">
    <van-nav-bar title="资源集市" left-arrow @click-left="router.back()">
      <template #right>
        <span class="publish-btn" @click="openPublish">发布</span>
      </template>
    </van-nav-bar>

    <div class="market-content">
      <div class="credit-card" v-if="credit">
        <div>校园学习信用分</div>
        <div class="credit-score">{{ credit.creditScore }} · {{ credit.scoreComment }}</div>
      </div>

      <div class="tab-row">
        <div class="tab-item" :class="{ active: tab === 'market' }" @click="tab = 'market'">资源广场</div>
        <div class="tab-item" :class="{ active: tab === 'free' }" @click="tab = 'free'">免费共享区</div>
        <div class="tab-item" :class="{ active: tab === 'mine' }" @click="tab = 'mine'">我的发布</div>
        <div class="tab-item" :class="{ active: tab === 'trades' }" @click="tab = 'trades'">我的流转</div>
      </div>

      <div v-if="tab !== 'trades'" class="list-wrap">
        <div v-if="loading" class="empty-text">加载中...</div>
        <template v-else>
          <div v-for="item in visibleItems" :key="item.id" class="item-card">
            <div class="item-head">
              <div class="item-title">{{ item.title }}</div>
              <div class="item-status">{{ statusText(item.status) }}</div>
            </div>
            <div class="item-publisher">发布者：{{ item.publisherName || '未知用户' }}</div>
            <div class="item-desc">{{ item.description }}</div>
            <div class="item-meta">
              <span>{{ item.category }}</span>
              <span>{{ item.originType === 1 ? '原创' : '转载' }}</span>
              <span>{{ item.isFree === 1 ? '免费' : `¥${item.price ?? 0}` }}</span>
            </div>
            <div class="item-actions" v-if="tab !== 'mine'">
              <button class="action-btn" :disabled="item.status !== 1" @click="applyItem(item)">
                {{ item.isFree === 1 ? '申请领取' : '申请交易' }}
              </button>
            </div>
          </div>
          <div v-if="visibleItems.length === 0" class="empty-text">暂无资源数据</div>
        </template>
      </div>

      <div v-else class="list-wrap">
        <div v-for="trade in myTrades" :key="trade.id" class="item-card">
          <div class="item-head">
            <div class="item-title">{{ trade.itemTitle || `物品 #${trade.itemId}` }}</div>
            <div class="item-status">{{ tradeText(trade.status) }}</div>
          </div>
          <div class="item-publisher">发布者：{{ trade.publisherName || '未知用户' }}</div>
          <div class="item-desc">
            面交地点：{{ trade.meetingPlace || '图书馆服务台' }} ｜ 备注：{{ trade.note || '无' }}
          </div>
          <div class="item-meta">
            <span>发布方确认：{{ trade.confirmPublisher === 1 ? '已确认' : '未确认' }}</span>
            <span>接收方确认：{{ trade.confirmReceiver === 1 ? '已确认' : '未确认' }}</span>
          </div>
          <div class="item-actions" v-if="trade.status === 1">
            <button class="action-btn" @click="confirmMyTrade(trade)">确认面交</button>
          </div>
        </div>
        <div v-if="myTrades.length === 0" class="empty-text">暂无流转记录</div>
      </div>
    </div>

    <van-popup
      v-model:show="showPublish"
      round
      position="bottom"
      :style="{ height: '72%', width: 'min(760px, 96vw)', left: '50%', transform: 'translateX(-50%)' }"
    >
      <div class="publish-panel">
        <div class="form-title">发布学习资源</div>
        <input v-model="publishForm.title" class="field" type="text" placeholder="标题（如：考研数学笔记）" />
        <textarea v-model="publishForm.description" class="field area" placeholder="描述资源内容、成色或服务说明" />
        <select v-model="publishForm.category" class="field">
          <option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</option>
        </select>
        <select v-model.number="publishForm.resourceType" class="field">
          <option :value="1">实物资料</option>
          <option :value="2">文档资料</option>
          <option :value="3">技能服务</option>
        </select>
        <select v-model.number="publishForm.isFree" class="field">
          <option :value="0">互助成本收费</option>
          <option :value="1">免费共享</option>
        </select>
        <input
          v-if="publishForm.isFree !== 1"
          v-model.number="publishForm.price"
          class="field"
          type="number"
          placeholder="成本金额（非盈利）"
        />
        <select v-model.number="publishForm.originType" class="field">
          <option :value="1">原创</option>
          <option :value="2">转载</option>
        </select>
        <input
          v-if="publishForm.originType === 2"
          v-model="publishForm.sourceReference"
          class="field"
          type="text"
          placeholder="转载来源说明"
        />
        <input v-model="publishForm.recommendedPlace" class="field" type="text" placeholder="推荐面交地点" />
        <button class="submit-btn" @click="submitPublish">提交审核</button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.resource-market { min-height: 100vh; background: #f5f7fa; }
.publish-btn { color: #4a90e2; font-size: 14px; }
.market-content { padding: 12px; }
.credit-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e8edf5;
  margin-bottom: 10px;
}
.credit-score { margin-top: 4px; font-weight: 700; color: #2f6fb8; }
.tab-row { display: flex; gap: 8px; overflow-x: auto; margin-bottom: 10px; }
.tab-item {
  padding: 7px 12px;
  border-radius: 16px;
  background: #eef2f7;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}
.tab-item.active { background: #4a90e2; color: #fff; }
.item-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8edf5;
  padding: 12px;
  margin-bottom: 10px;
}
.item-head { display: flex; justify-content: space-between; gap: 8px; }
.item-title { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.item-status { font-size: 12px; color: #4a90e2; }
.item-publisher { margin-top: 6px; font-size: 12px; color: #64748b; }
.item-desc { margin-top: 6px; font-size: 13px; line-height: 1.5; color: #5b6472; }
.item-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; font-size: 12px; color: #8b8b8b; }
.item-actions { margin-top: 10px; }
.action-btn {
  border: none;
  border-radius: 8px;
  padding: 7px 12px;
  background: #4a90e2;
  color: #fff;
}
.action-btn:disabled { opacity: 0.5; }
.empty-text { padding: 24px 0; text-align: center; color: #94a3b8; }
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

@media (min-width: 1024px) {
  .market-content {
    max-width: 1120px;
    margin: 0 auto;
    padding: 16px 20px 24px;
  }

  .tab-row {
    gap: 10px;
    flex-wrap: wrap;
    overflow-x: visible;
    margin-bottom: 14px;
  }

  .tab-item {
    border: 1px solid transparent;
    transition: all 0.2s ease;
  }

  .list-wrap {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 14px;
    align-content: start;
  }

  .item-card {
    margin-bottom: 0;
    border-radius: 14px;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }
}

@media (min-width: 1024px) and (hover: hover) {
  .tab-item:hover {
    color: #4a90e2;
    border-color: #d5e6fb;
    background: #f0f6ff;
  }

  .item-card:hover {
    transform: translateY(-2px);
    border-color: #cfe1f8;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
  }
}
</style>
