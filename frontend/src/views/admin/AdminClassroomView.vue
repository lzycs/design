<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  createAdminClassroom,
  deleteAdminClassroom,
  getAdminClassrooms,
  updateAdminClassroom,
  type AdminClassroomVO,
} from '@/api/admin'
import { getBuildingList, type Result as ApiResult, type Building } from '@/api/building'

const classrooms = ref<AdminClassroomVO[]>([])
const buildings = ref<Building[]>([])
const router = useRouter()

const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = ref<{
  buildingId: number | null
  name: string
  roomNumber: string
  floor: number | null
  type: number | null
  capacity: number | null
  equipment: string
  status: number
  latitude: number | null
  longitude: number | null
}>({
  buildingId: null,
  name: '',
  roomNumber: '',
  floor: null,
  type: 1,
  capacity: null,
  equipment: '',
  status: 1,
  latitude: null,
  longitude: null,
})

const loadClassrooms = async () => {
  const res = await getAdminClassrooms({})
  if (res.code !== 200) {
    showToast(res.message || '加载教室失败')
    return
  }
  classrooms.value = res.data
}

const loadBuildings = async () => {
  const res = await getBuildingList()
  if (res.code !== 200) {
    showToast(res.message || '加载教学楼失败')
    return
  }
  buildings.value = (res.data || []) as Building[]
}

const openCreateForm = () => {
  editingId.value = null
  form.value = {
    buildingId: buildings.value[0]?.id ?? null,
    name: '',
    roomNumber: '',
    floor: 1,
    type: 1,
    capacity: null,
    equipment: '',
    status: 1,
    latitude: null,
    longitude: null,
  }
  showForm.value = true
}

const openEditForm = (vo: AdminClassroomVO) => {
  editingId.value = vo.id
  form.value = {
    buildingId: vo.buildingId ?? null,
    name: vo.name,
    roomNumber: vo.roomNumber,
    floor: vo.floor ?? null,
    type: vo.type ?? null,
    capacity: vo.capacity ?? null,
    equipment: vo.equipment ?? '',
    status: vo.status ?? 1,
    latitude: vo.latitude ?? null,
    longitude: vo.longitude ?? null,
  }
  showForm.value = true
}

const save = async () => {
  if (!form.value.buildingId) {
    showToast('请选择教学楼')
    return
  }
  if (!form.value.roomNumber || !form.value.name) {
    showToast('请填写教室名称和教室编号')
    return
  }
  if (form.value.capacity == null) {
    showToast('请填写容纳人数')
    return
  }

  const payload: any = {
    id: editingId.value ?? undefined,
    buildingId: form.value.buildingId,
    name: form.value.name,
    roomNumber: form.value.roomNumber,
    floor: form.value.floor,
    type: form.value.type,
    capacity: form.value.capacity,
    equipment: form.value.equipment,
    status: form.value.status,
    latitude: form.value.latitude,
    longitude: form.value.longitude,
  }

  try {
    const res = editingId.value ? await updateAdminClassroom(payload) : await createAdminClassroom(payload)
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '保存失败')
      return
    }
    showToast('保存成功')
    showForm.value = false
    await loadClassrooms()
  } catch (e) {
    console.error(e)
    showToast('保存失败')
  }
}

const deviceStatusLabel = (status: number | null | undefined) => {
  switch (status) {
    case 0:
      return '停用'
    case 1:
      return '正常'
    case 2:
      return '维修中'
    default:
      return '未知'
  }
}

const availableLabel = (isAvailable: number | null | undefined) => {
  return isAvailable === 1 ? '可用' : '不可用'
}

const del = async (id: number) => {
  const ok = window.confirm('确定删除该教室？（会触发逻辑删除）')
  if (!ok) return
  try {
    const res = await deleteAdminClassroom(id)
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '删除失败')
      return
    }
    showToast('删除成功')
    await loadClassrooms()
  } catch (e) {
    console.error(e)
    showToast('删除失败')
  }
}

onMounted(async () => {
  await loadBuildings()
  await loadClassrooms()
})
</script>

<template>
  <div class="admin-page active">
    <div class="page-header">
      <div style="width: 24px; display: flex; align-items: center">
        <van-icon name="arrow-left" style="font-size: 20px; cursor: pointer" @click="router.push('/admin')" />
      </div>
      <div class="page-header-title">教室信息管理</div>
      <div style="width: 24px"></div>
    </div>

    <div class="search-bar" style="justify-content: space-between; align-items: center">
      <div style="font-size: 13px; color: #666">支持新增/编辑/删除教室</div>
      <button class="action-btn primary-btn" @click="openCreateForm">新增教室</button>
    </div>

    <div class="content-area">
      <div v-if="showForm" class="admin-card form-card">
        <label class="form-label">所属教学楼</label>
        <select v-model="form.buildingId" class="form-select">
          <option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
        </select>

        <label class="form-label">教室名称</label>
        <input v-model="form.name" class="form-input" placeholder="例如：101多媒体教室" />

        <label class="form-label">教室编号</label>
        <input v-model="form.roomNumber" class="form-input" placeholder="例如：101" />

        <label class="form-label">楼层</label>
        <input v-model.number="form.floor" class="form-input" type="number" placeholder="请输入楼层" />

        <label class="form-label">教室类型</label>
        <select v-model="form.type" class="form-select">
          <option :value="1">普通教室</option>
          <option :value="2">研讨室/会议室</option>
        </select>

        <label class="form-label">容纳人数</label>
        <input v-model.number="form.capacity" class="form-input" type="number" placeholder="请输入容纳人数" />

        <label class="form-label">设备状态</label>
        <select v-model="form.status" class="form-select">
          <option :value="0">停用（不可用）</option>
          <option :value="1">正常（可用）</option>
          <option :value="2">维修中（不可用）</option>
        </select>

        <div style="font-size: 12px; color: #909399; margin-top: -8px; margin-bottom: 16px">
          是否可用：{{ form.status === 1 ? '可用' : '不可用' }}
        </div>

        <label class="form-label">设施配置</label>
        <input v-model="form.equipment" class="form-input" placeholder="例如：投影仪、空调、音响" />

        <button class="submit-btn" @click="save">保存教室信息</button>
        <button class="submit-btn" style="background-color: #f5f7fa; color: #4a90e2; margin-top: 8px" @click="showForm=false">
          取消
        </button>
      </div>

      <div v-else>
        <div class="admin-card">
          <div v-for="vo in classrooms" :key="vo.id" class="list-card">
            <div class="list-icon">🏫</div>
            <div class="list-info">
              <div class="list-header">
                <div class="list-title">{{ vo.buildingName }}-{{ vo.roomNumber }}</div>
              </div>
              <div class="list-meta">
                {{ vo.name }} | 楼层 {{ vo.floor }} | 类型 {{ vo.type }} | 设备状态：{{ deviceStatusLabel(vo.status) }}
                | 是否可用：{{ availableLabel(vo.isAvailable) }}
              </div>
              <div class="list-desc">容量：{{ vo.capacity }} | 设施：{{ vo.equipment || '-' }}</div>
              <div class="action-group">
                <button class="action-btn primary-btn" @click="openEditForm(vo)">编辑</button>
                <button class="action-btn danger-btn" @click="del(vo.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
        <div style="height: 16px" />
      </div>
    </div>
  </div>
</template>

