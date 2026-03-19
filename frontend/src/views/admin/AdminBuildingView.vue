<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import {
  createAdminBuilding,
  deleteAdminBuilding,
  getAdminBuildings,
  updateAdminBuilding,
  type AdminBuildingVO,
} from '@/api/admin'

const keyword = ref('')

const buildings = ref<AdminBuildingVO[]>([])
const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = ref<{
  name: string
  buildingNumber: string
  location: string
}>({
  name: '',
  buildingNumber: '',
  location: '',
})

const loadBuildings = async () => {
  try {
    const res = await getAdminBuildings({ keyword: keyword.value || undefined })
    if (res.code !== 200) {
      showToast(res.message || '加载教学楼失败')
      return
    }
    buildings.value = res.data || []
  } catch (e) {
    console.error(e)
    showToast('加载教学楼失败')
  }
}

const openCreateForm = () => {
  editingId.value = null
  form.value = {
    name: '',
    buildingNumber: '',
    location: '',
  }
  showForm.value = true
}

const openEditForm = (vo: AdminBuildingVO) => {
  editingId.value = vo.id
  form.value = {
    name: vo.name || '',
    buildingNumber: vo.buildingNumber || '',
    location: vo.location || '',
  }
  showForm.value = true
}

const save = async () => {
  if (!form.value.name.trim()) {
    showToast('请输入教学楼名称')
    return
  }
  if (!form.value.buildingNumber.trim()) {
    showToast('请输入楼栋号')
    return
  }
  if (!form.value.location.trim()) {
    showToast('请输入位置')
    return
  }

  const payload: any = {
    id: editingId.value ?? undefined,
    name: form.value.name.trim(),
    buildingNumber: form.value.buildingNumber.trim(),
    address: form.value.location.trim(),
  }

  try {
    const res = editingId.value ? await updateAdminBuilding(payload) : await createAdminBuilding(payload)
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '保存失败')
      return
    }
    showToast('保存成功')
    showForm.value = false
    await loadBuildings()
  } catch (e) {
    console.error(e)
    showToast('保存失败')
  }
}

const del = async (id: number) => {
  const ok = window.confirm('确定删除该教学楼？（逻辑删除）')
  if (!ok) return
  try {
    const res = await deleteAdminBuilding(id)
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '删除失败')
      return
    }
    showToast('删除成功')
    await loadBuildings()
  } catch (e) {
    console.error(e)
    showToast('删除失败')
  }
}

onMounted(loadBuildings)
</script>

<template>
  <div class="admin-page active">
    <div class="page-header">
      <div style="width: 24px"></div>
      <div class="page-header-title">教学楼信息管理</div>
      <div style="width: 24px"></div>
    </div>

    <div class="search-bar" style="justify-content: space-between; align-items: center">
      <input v-model="keyword" type="text" class="search-input" placeholder="搜索名称/楼栋号/位置" />
      <button class="action-btn primary-btn" @click="loadBuildings">搜索</button>
    </div>

    <div class="content-area">
      <div v-if="showForm" class="admin-card form-card">
        <label class="form-label">教学楼名称</label>
        <input v-model="form.name" class="form-input" placeholder="例如：第一教学楼" />

        <label class="form-label">楼栋号</label>
        <input v-model="form.buildingNumber" class="form-input" placeholder="例如：J1 / 1号楼" />

        <label class="form-label">位置</label>
        <input v-model="form.location" class="form-input" placeholder="例如：学校东区主路1号" />

        <button class="submit-btn" @click="save">保存教学楼信息</button>
        <button
          class="submit-btn"
          style="background-color: #f5f7fa; color: #4a90e2; margin-top: 8px"
          @click="showForm = false"
        >
          取消
        </button>
      </div>

      <div v-else class="admin-card">
        <div class="action-group" style="justify-content: space-between; margin: 0 0 10px 0">
          <div style="font-size: 13px; color: #909399">共 {{ buildings.length }} 条</div>
          <button class="action-btn primary-btn" @click="openCreateForm">新增教学楼</button>
        </div>

        <div v-if="buildings.length === 0" class="empty-state">
          <div class="empty-icon">-</div>
          <div class="empty-title">暂无数据</div>
          <div class="empty-desc">请尝试调整关键词</div>
        </div>

        <div v-else>
          <div v-for="b in buildings" :key="b.id" class="list-card">
            <div class="list-icon">🏢</div>
            <div class="list-info">
              <div class="list-header">
                <div class="list-title">{{ b.name }} ({{ b.buildingNumber }})</div>
              </div>
              <div class="list-meta">
                位置：{{ b.location }} | 创建时间：{{ b.createTime ? b.createTime.slice(0, 19).replace('T', ' ') : '-' }}
              </div>
              <div class="action-group">
                <button class="action-btn primary-btn" @click="openEditForm(b)">编辑</button>
                <button class="action-btn danger-btn" @click="del(b.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

