<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { getMyCoursesByDay, getMyCoursesByWeek, updateMyCourseNote, type MyCourseItem } from '@/api/myCourses'

const router = useRouter()
const userId = ref<number | null>(null)
const loading = ref(false)
const mode = ref<'day' | 'week'>('day')
const date = ref(new Date().toISOString().slice(0, 10))
const keyword = ref('')

const dayCourses = ref<MyCourseItem[]>([])
const weekDays = ref<Array<{ date: string; weekDay: number; courses: MyCourseItem[] }>>([])

const weekDayText = (n?: number) => ['-', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][n || 0] || '-'
const timeText = (t?: string) => (t ? String(t).slice(0, 5) : '--:--')

const hasData = computed(() => {
  if (mode.value === 'day') return dayCourses.value.length > 0
  return weekDays.value.some(d => d.courses.length > 0)
})

const loadUser = () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) return
  try {
    const u = JSON.parse(raw) as { id?: number }
    userId.value = u.id ?? null
  } catch {
    userId.value = null
  }
}

const loadData = async () => {
  if (!userId.value) {
    showToast('请先登录')
    await router.replace('/profile')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'day') {
      const res = await getMyCoursesByDay({ userId: userId.value, date: date.value, keyword: keyword.value || undefined })
      dayCourses.value = res.data ?? []
    } else {
      const res = await getMyCoursesByWeek({ userId: userId.value, date: date.value, keyword: keyword.value || undefined })
      weekDays.value = res.data?.days ?? []
    }
  } finally {
    loading.value = false
  }
}

const goClassroom = (classroomId?: number) => {
  if (!classroomId) return
  router.push(`/reservation/classroom/${classroomId}`)
}

const toggleStar = async (course: MyCourseItem) => {
  if (!userId.value) return
  const next = course.isStarred === 1 ? 0 : 1
  const res = await updateMyCourseNote(course.courseId, { userId: userId.value, isStarred: next, note: course.note })
  if (res.code !== 200 || !res.data) {
    showToast(res.message || '更新失败')
    return
  }
  course.isStarred = next
  showToast(next === 1 ? '已星标' : '已取消星标')
}

const editNote = async (course: MyCourseItem) => {
  if (!userId.value) return
  const input = window.prompt('输入课程备注（最多500字）', course.note || '')
  if (input === null) return
  const res = await updateMyCourseNote(course.courseId, {
    userId: userId.value,
    isStarred: course.isStarred ?? 0,
    note: input.slice(0, 500),
    remindBeforeMinutes: course.remindBeforeMinutes,
  })
  if (res.code !== 200 || !res.data) {
    showDialog({ message: res.message || '保存失败' })
    return
  }
  course.note = input.slice(0, 500)
  showToast('备注已保存')
}

onMounted(async () => {
  loadUser()
  await loadData()
})
</script>

<template>
  <div class="my-courses-page">
    <van-nav-bar title="我的课程表" left-arrow @click-left="router.back()" />
    <div class="content">
      <div class="card">
        <div class="toolbar">
          <button class="mode-btn" :class="{ active: mode === 'day' }" @click="mode = 'day'; loadData()">当日</button>
          <button class="mode-btn" :class="{ active: mode === 'week' }" @click="mode = 'week'; loadData()">本周</button>
          <input v-model="date" type="date" class="date-input" @change="loadData" />
        </div>
        <div class="toolbar">
          <input v-model.trim="keyword" class="keyword-input" placeholder="搜索课程/教师/地点" />
          <button class="search-btn" @click="loadData">搜索</button>
        </div>
      </div>

      <div v-if="loading" class="card">加载中...</div>

      <div v-else-if="!hasData" class="card empty">暂无课程数据</div>

      <template v-else>
        <div v-if="mode === 'day'" class="card">
          <div v-for="course in dayCourses" :key="course.courseId" class="course-item" @click="goClassroom(course.classroomId)">
            <div class="title-row">
              <div class="title">{{ course.courseName }}</div>
              <button class="star-btn" @click.stop="toggleStar(course)">{{ course.isStarred === 1 ? '★' : '☆' }}</button>
            </div>
            <div class="meta">{{ weekDayText(course.weekDay) }} {{ timeText(course.startTime) }}-{{ timeText(course.endTime) }}</div>
            <div class="meta">{{ course.buildingName || '-' }} {{ course.roomNumber || '' }} · {{ course.teacherName || '-' }}</div>
            <div v-if="course.note" class="note">备注：{{ course.note }}</div>
            <div class="actions">
              <button class="action-btn" @click.stop="editNote(course)">编辑备注</button>
              <button class="action-btn primary" @click.stop="goClassroom(course.classroomId)">查看教室</button>
            </div>
          </div>
        </div>

        <div v-else class="week-wrap">
          <div v-for="d in weekDays" :key="d.date" class="card">
            <div class="day-title">{{ d.date }} {{ weekDayText(d.weekDay) }}</div>
            <div v-if="d.courses.length === 0" class="empty">无课程</div>
            <div v-for="course in d.courses" :key="`${d.date}-${course.courseId}`" class="week-course" @click="goClassroom(course.classroomId)">
              <span>{{ timeText(course.startTime) }}-{{ timeText(course.endTime) }}</span>
              <span>{{ course.courseName }}</span>
              <span>{{ course.roomNumber || '-' }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.my-courses-page { min-height: 100vh; background: #f5f7fa; }
.content { padding: 12px; }
.card { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 8px; }
.mode-btn { border: none; background: #eef2f7; border-radius: 14px; padding: 6px 12px; }
.mode-btn.active { background: #4a90e2; color: #fff; }
.date-input, .keyword-input { flex: 1; border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px; }
.search-btn { border: none; background: #4a90e2; color: #fff; border-radius: 8px; padding: 8px 14px; }
.empty { color: #98a2b3; text-align: center; }
.course-item { border: 1px solid #eef0f3; border-radius: 10px; padding: 10px; margin-bottom: 8px; }
.title-row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.star-btn { border: none; background: transparent; font-size: 18px; color: #f59e0b; }
.meta { margin-top: 4px; font-size: 12px; color: #667085; }
.note { margin-top: 6px; font-size: 12px; color: #1f2937; background: #f8fafc; padding: 6px 8px; border-radius: 6px; }
.actions { margin-top: 8px; display: flex; gap: 8px; }
.action-btn { border: none; background: #eef2f7; border-radius: 8px; padding: 6px 10px; font-size: 12px; }
.action-btn.primary { background: #ecf5ff; color: #3370e7; }
.day-title { font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 8px; }
.week-course {
  display: grid;
  grid-template-columns: 100px 1fr 60px;
  gap: 8px;
  font-size: 12px;
  padding: 8px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  margin-bottom: 6px;
}
</style>
