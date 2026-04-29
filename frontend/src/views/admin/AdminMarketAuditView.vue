<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { auditAdminMarketItem, getAdminMarketItems, type AdminMarketItemVO } from '@/api/admin'

const keyword = ref('')
const router = useRouter()
const list = ref<AdminMarketItemVO[]>([])
const loading = ref(false)

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAdminMarketItems()
    if (res.code !== 200) {
      showToast(res.message || '加载失败')
      return
    }
    const all = res.data || []
    const kw = keyword.value.trim().toLowerCase()
    if (!kw) {
      list.value = all
    } else {
      list.value = all.filter(item => {
        const title = (item.title || '').toLowerCase()
        const user = (item.publisherName || '').toLowerCase()
        const category = (item.category || '').toLowerCase()
        const desc = (item.description || '').toLowerCase()
        return title.includes(kw) || user.includes(kw) || category.includes(kw) || desc.includes(kw)
      })
    }
  } catch (e) {
    console.error(e)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const handleAudit = async (item: AdminMarketItemVO, approve: boolean) => {
  const remark = window.prompt('审核备注（可选）', '')
  try {
    const res = await auditAdminMarketItem(item.id, { approve, remark: remark || '' })
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '审核失败')
      return
    }
    showToast('审核成功')
    await loadList()
  } catch (e) {
    console.error(e)
    showToast('审核失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="admin-page active">
    <div class="page-header">
      <div style="width: 24px; display: flex; align-items: center">
        <van-icon name="arrow-left" style="font-size: 20px; cursor: pointer" @click="router.push('/admin')" />
      </div>
      <div class="page-header-title">资源集市审核</div>
      <div style="width: 24px"></div>
    </div>

    <div class="search-bar">
      <input v-model="keyword" type="text" class="search-input" placeholder="搜索标题/发布者/分类" />
      <button class="search-btn" @click="loadList">🔍</button>
    </div>

    <div class="content-area">
      <div v-if="list.length === 0" class="empty-state">
        <div class="empty-icon">-</div>
        <div class="empty-title">暂无待审核资源</div>
        <div class="empty-desc">所有发布内容均已处理</div>
      </div>

      <div v-else class="admin-card">
        <div v-for="item in list" :key="item.id" class="list-card">
          <div class="list-icon">📦</div>
          <div class="list-info">
            <div class="list-header">
              <div class="list-title">{{ item.title }}</div>
              <div class="list-tag tag-pending">待审核</div>
            </div>
            <div class="list-meta">
              发布者：{{ item.publisherName || '未知用户' }} | 分类：{{ item.category || '-' }}
            </div>
            <div class="list-meta">
              价格：{{ item.isFree === 1 ? '免费共享' : `¥${item.price ?? 0}` }} | 来源：{{ item.originType === 1 ? '原创' : '转载' }}
            </div>
            <div class="list-desc">{{ item.description || '未填写描述' }}</div>

            <div class="action-group">
              <button class="action-btn success-btn" @click="handleAudit(item, true)">通过</button>
              <button class="action-btn danger-btn" @click="handleAudit(item, false)">驳回</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" style="padding: 10px 20px; color: #909399; font-size: 12px">加载中...</div>
  </div>
</template>
