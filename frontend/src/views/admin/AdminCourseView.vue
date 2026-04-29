<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  createAdminCourse,
  deleteAdminCourse,
  getAdminBuildings,
  getAdminClassrooms,
  getAdminCourses,
  updateAdminCourse,
  type AdminBuildingVO,
  type AdminClassroomVO,
  type AdminCourseVO,
} from '@/api/admin'

const keyword = ref('')
const router = useRouter()
const filterBuildingId = ref<number | undefined>(undefined)
const filterFloor = ref<number | undefined>(undefined)
const filterClassroomId = ref<number | undefined>(undefined)
const courses = ref<AdminCourseVO[]>([])
const loading = ref(false)

const buildings = ref<AdminBuildingVO[]>([])
const classrooms = ref<AdminClassroomVO[]>([])

const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = ref<{
  classroomId: number | null
  courseName: string
  teacherName: string
  weekDay: number
  startTime: string
  endTime: string
  startWeek: number
  endWeek: number
}>({
  classroomId: null,
  courseName: '',
  teacherName: '',
  weekDay: 1,
  startTime: '',
  endTime: '',
  startWeek: 1,
  endWeek: 16,
})

const normalizeTime = (v: string) => {
  const s = (v || '').trim()
  if (!s) return undefined
  if (/^\d{2}:\d{2}$/.test(s)) return s + ':00'
  if (/^\d{2}:\d{2}:\d{2}$/.test(s)) return s
  return s
}

const loadCourses = async () => {
  loading.value = true
  try {
    const res = await getAdminCourses({
      keyword: keyword.value || undefined,
      buildingId: filterBuildingId.value,
      floor: filterFloor.value,
      classroomId: filterClassroomId.value,
    })
    if (res.code !== 200) {
      showToast(res.message || '加载失败')
      return
    }
    courses.value = res.data
  } catch (e) {
    console.error(e)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const loadBuildings = async () => {
  const res = await getAdminBuildings({})
  if (res.code === 200) buildings.value = res.data ?? []
}

const loadClassrooms = async () => {
  const res = await getAdminClassrooms({})
  if (res.code !== 200) {
    showToast(res.message || '加载教室失败')
    return
  }
  classrooms.value = res.data
  if (!form.value.classroomId && classrooms.value.length > 0) {
    form.value.classroomId = classrooms.value[0]?.id ?? null
  }
}

const openCreateForm = () => {
  editingId.value = null
  form.value = {
    classroomId: classrooms.value[0]?.id ?? null,
    courseName: '',
    teacherName: '',
    weekDay: 2,
    startTime: '08:00',
    endTime: '09:40',
    startWeek: 1,
    endWeek: 16,
  }
  showForm.value = true
}

const openEditForm = (vo: AdminCourseVO) => {
  editingId.value = vo.id
  form.value = {
    classroomId: vo.classroomId,
    courseName: vo.courseName,
    teacherName: vo.teacherName,
    weekDay: vo.weekDay,
    startTime: vo.startTime ? String(vo.startTime).slice(0, 5) : '',
    endTime: vo.endTime ? String(vo.endTime).slice(0, 5) : '',
    startWeek: vo.startWeek,
    endWeek: vo.endWeek,
  }
  showForm.value = true
}

const save = async () => {
  if (!form.value.classroomId) {
    showToast('请选择教室')
    return
  }
  if (!form.value.courseName || !form.value.teacherName) {
    showToast('请填写课程名称与教师名称')
    return
  }
  const payload: any = {
    id: editingId.value ?? undefined,
    classroomId: form.value.classroomId,
    courseName: form.value.courseName,
    teacherName: form.value.teacherName,
    weekDay: form.value.weekDay,
    startTime: normalizeTime(form.value.startTime),
    endTime: normalizeTime(form.value.endTime),
    startWeek: form.value.startWeek,
    endWeek: form.value.endWeek,
  }

  try {
    const res = editingId.value ? await updateAdminCourse(payload) : await createAdminCourse(payload)
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '保存失败')
      return
    }
    showToast('保存成功')
    showForm.value = false
    await loadCourses()
  } catch (e) {
    console.error(e)
    showToast('保存失败')
  }
}

const del = async (id: number) => {
  const ok = window.confirm('确定删除该课程？（会触发逻辑删除）')
  if (!ok) return
  try {
    const res = await deleteAdminCourse(id)
    if (res.code !== 200 || !res.data) {
      showToast(res.message || '删除失败')
      return
    }
    showToast('删除成功')
    await loadCourses()
  } catch (e) {
    console.error(e)
    showToast('删除失败')
  }
}

onMounted(async () => {
  await loadBuildings()
  await loadClassrooms()
  await loadCourses()
})
</script>

<template>
  <div class="admin-page active">
    <div class="page-header">
      <div style="width: 24px; display: flex; align-items: center">
        <van-icon name="arrow-left" style="font-size: 20px; cursor: pointer" @click="router.push('/admin')" />
      </div>
      <div class="page-header-title">课程表管理</div>
      <div style="width: 24px"></div>
    </div>

    <div class="search-bar" style="justify-content: space-between; align-items: center">
      <input v-model="keyword" type="text" class="search-input" placeholder="搜索课程/教师/教室位置" />
      <button class="action-btn primary-btn" @click="loadCourses">搜索</button>
    </div>
    <div class="filter-bar">
      <select v-model.number="filterBuildingId" class="filter-item">
        <option :value="undefined">全部楼栋</option>
        <option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
      </select>
      <input v-model.number="filterFloor" class="filter-item" type="number" placeholder="楼层" />
      <select v-model.number="filterClassroomId" class="filter-item">
        <option :value="undefined">全部教室</option>
        <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.buildingName }}-{{ c.roomNumber }}</option>
      </select>
      <button class="action-btn primary-btn" @click="loadCourses">应用筛选</button>
    </div>

    <div class="content-area">
      <div v-if="showForm" class="admin-card form-card">
        <label class="form-label">所属教室</label>
        <select v-model="form.classroomId" class="form-select">
          <option v-for="c in classrooms" :key="c.id" :value="c.id">
            {{ c.buildingName }}-{{ c.roomNumber }}
          </option>
        </select>

        <label class="form-label">课程名称</label>
        <input v-model="form.courseName" class="form-input" placeholder="例如：Java编程" />

        <label class="form-label">教师名称</label>
        <input v-model="form.teacherName" class="form-input" placeholder="例如：李老师" />

        <label class="form-label">星期（1-7）</label>
        <input v-model.number="form.weekDay" class="form-input" type="number" min="1" max="7" />

        <label class="form-label">开始时间</label>
        <input v-model="form.startTime" class="form-input" type="time" />

        <label class="form-label">结束时间</label>
        <input v-model="form.endTime" class="form-input" type="time" />

        <label class="form-label">开始周次</label>
        <input v-model.number="form.startWeek" class="form-input" type="number" min="1" />

        <label class="form-label">结束周次</label>
        <input v-model.number="form.endWeek" class="form-input" type="number" min="1" />

        <button class="submit-btn" @click="save">保存课程表</button>
        <button
          class="submit-btn"
          style="background-color: #f5f7fa; color: #4a90e2; margin-top: 8px"
          @click="showForm = false"
        >
          取消
        </button>
      </div>

      <div v-else class="admin-card">
        <div class="action-group" style="justify-content: space-between; margin-bottom: 10px">
          <div style="font-size: 13px; color: #909399">共 {{ courses.length }} 条</div>
          <button class="action-btn primary-btn" @click="openCreateForm">新增课程</button>
        </div>

        <div v-if="courses.length === 0" class="empty-state">
          <div class="empty-icon">-</div>
          <div class="empty-title">暂无数据</div>
          <div class="empty-desc">请稍后重试或调整关键词</div>
        </div>

        <div v-else>
          <div v-for="c in courses" :key="c.id" class="list-card">
            <div class="list-icon">📅</div>
            <div class="list-info">
              <div class="list-header">
                <div class="list-title">{{ c.courseName }} ({{ c.location }})</div>
                <div class="list-tag tag-processing">
                  星期 {{ c.weekDay }}
                </div>
              </div>
              <div class="list-meta">{{ c.teacherName }} | {{ c.startTime?.slice(0, 5) }}-{{ c.endTime?.slice(0, 5) }}</div>
              <div class="list-desc">周次：{{ c.startWeek }}-{{ c.endWeek }}</div>
              <div class="action-group">
                <button class="action-btn primary-btn" @click="openEditForm(c)">编辑</button>
                <button class="action-btn danger-btn" @click="del(c.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" style="padding: 10px 0; color: #909399; font-size: 12px">加载中...</div>
    </div>
  </div>
</template>

