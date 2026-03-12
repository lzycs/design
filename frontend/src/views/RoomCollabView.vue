<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getReservationById, type Reservation } from '@/api/reservation'
import { getClassroomById, type Classroom } from '@/api/classroom'
import { getPlansByReservation, type StudyPlanVO } from '@/api/studyPlan'

const route = useRoute()
const router = useRouter()

const reservation = ref<Reservation | null>(null)
const classroom = ref<Classroom | null>(null)
const studyPlans = ref<StudyPlanVO[]>([])
const loading = ref(true)

const docTitle = ref('研讨室协作文档')
const docContent = ref('')
const saving = ref(false)
const lastSavedTime = ref<string | null>(null)
const onlineCount = ref(3)

const formatTime = (t?: string) => {
  if (!t) return ''
  return t.length >= 8 ? t.substring(0, 5) : t
}

const formatDate = (d?: string) => d ?? ''

const loadRoomDetail = async () => {
  const idParam = route.params.reservationId
  const id = Number(idParam)
  if (!id) {
    showToast('预约不存在')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await getReservationById(id)
    reservation.value = (res as any).data ?? null
    if (reservation.value?.classroomId) {
      const cRes = await getClassroomById(reservation.value.classroomId)
      classroom.value = (cRes as any).data ?? null
    }
    const pRes = await getPlansByReservation(id)
    studyPlans.value = (pRes as any).data ?? []

    // 加载本地协作文档（模拟 Etherpad）
    const key = `room-collab-doc-${id}`
    const stored = localStorage.getItem(key)
    if (stored) {
      try {
        const parsed = JSON.parse(stored) as { title: string; content: string; lastSaved?: string }
        docTitle.value = parsed.title || docTitle.value
        docContent.value = parsed.content || ''
        lastSavedTime.value = parsed.lastSaved ?? null
      } catch {
        // ignore
      }
    }
  } catch (e) {
    console.error(e)
    showToast('加载研讨室协作信息失败')
  } finally {
    loading.value = false
  }
}

let saveTimer: number | null = null

const scheduleAutoSave = () => {
  if (!reservation.value?.id) return
  if (saveTimer != null) {
    window.clearTimeout(saveTimer)
  }
  saving.value = true
  saveTimer = window.setTimeout(() => {
    const key = `room-collab-doc-${reservation.value!.id}`
    const payload = {
      title: docTitle.value,
      content: docContent.value,
      lastSaved: new Date().toISOString()
    }
    localStorage.setItem(key, JSON.stringify(payload))
    lastSavedTime.value = payload.lastSaved
    saving.value = false
  }, 800)
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadRoomDetail()
})
</script>

<template>
  <div class="room-collab">
    <van-nav-bar title="研讨室协作详情" left-arrow @click-left="goBack" />

    <div v-if="loading" class="loading-wrap">
      <van-loading size="24px" vertical>加载中...</van-loading>
    </div>

    <div v-else-if="reservation && classroom" class="page-inner">
      <!-- 1. 研讨室详情基础信息 -->
      <div class="card room-card">
        <div class="room-header">
          <div class="room-title">{{ classroom.name }}</div>
          <div class="room-tag">研讨室</div>
        </div>
        <div class="room-meta">
          <div>{{ classroom.buildingId ? `教学楼 #${classroom.buildingId}` : '教学楼' }}</div>
          <div v-if="classroom.floor">楼层：{{ classroom.floor }}层</div>
          <div>容纳人数：{{ classroom.capacity || '-' }} 人</div>
        </div>
        <div class="room-equip">
          <div class="equip-label">设施配置：</div>
          <div class="equip-tags">
            <span class="equip-tag">投影仪</span>
            <span class="equip-tag">WiFi</span>
            <span class="equip-tag">空调</span>
          </div>
        </div>
      </div>

      <!-- 预约信息 + 关联学习计划 -->
      <div class="card info-card">
        <div class="section-title">预约信息</div>
        <div class="info-row">
          <span class="label">预约日期：</span>
          <span>{{ formatDate(reservation.reservationDate) }}</span>
        </div>
        <div class="info-row">
          <span class="label">预约时段：</span>
          <span>{{ formatTime(reservation.startTime) }} - {{ formatTime(reservation.endTime) }}</span>
        </div>
        <div class="info-row">
          <span class="label">用途：</span>
          <span>{{ reservation.purpose || '自习/讨论/小组协作' }}</span>
        </div>

        <div v-if="studyPlans.length" class="plan-block">
          <div class="plan-title">关联学习计划</div>
          <div v-for="p in studyPlans" :key="p.id" class="plan-item">
            <div class="plan-name">{{ p.title }}</div>
            <div class="plan-sub">
              小组：{{ p.teamTitle || '学习小组' }} · 日期：{{ p.planDate || '-' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 在线协作文档功能卡片 -->
      <div class="card doc-card">
        <div class="doc-header">
          <div class="doc-title">{{ docTitle }}</div>
          <div class="doc-status">
            在线协作中 · 在线人数：{{ onlineCount }}
          </div>
        </div>
        <div class="doc-meta">
          <span>最后编辑：{{ lastSavedTime ? new Date(lastSavedTime).toLocaleString() : '尚未保存' }}</span>
        </div>
      </div>

      <!-- 3. 协作文档编辑区 -->
      <div class="card editor-card">
        <div class="editor-toolbar">
          <button type="button" class="toolbar-btn">保存</button>
          <button type="button" class="toolbar-btn">历史版本</button>
          <button type="button" class="toolbar-btn">分享</button>
          <button type="button" class="toolbar-btn">更多</button>
        </div>
        <textarea
          v-model="docContent"
          class="editor-area"
          placeholder="在这里编写你们的小组协作文档，系统会自动保存修改内容..."
          @input="scheduleAutoSave"
        />
        <div class="editor-footer">
          <div class="save-status">
            {{ saving ? '自动保存中...' : lastSavedTime ? '已自动保存' : '尚未保存' }}
          </div>
          <div class="online-users">
            <div class="user-avatar">张</div>
            <div class="user-avatar">李</div>
            <div class="user-avatar">王</div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-wrap">
      <van-empty description="未找到预约或教室信息" />
    </div>
  </div>
</template>

<style scoped>
.room-collab {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.page-inner {
  padding: 16px;
}

.card {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 16px;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.room-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.room-tag {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  background-color: #e8f4ff;
  color: #4a90e2;
}

.room-meta {
  font-size: 13px;
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.room-equip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.equip-label {
  color: #909399;
}

.equip-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.equip-tag {
  padding: 2px 8px;
  border-radius: 12px;
  background-color: #f5f7fa;
  font-size: 12px;
  color: #4b5563;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.info-row {
  font-size: 14px;
  color: #374151;
  margin-bottom: 6px;
}

.label {
  color: #6b7280;
  margin-right: 4px;
}

.plan-block {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed #e5e7eb;
}

.plan-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
}

.plan-item + .plan-item {
  margin-top: 4px;
}

.plan-name {
  font-size: 14px;
  color: #111827;
}

.plan-sub {
  font-size: 12px;
  color: #6b7280;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.doc-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.doc-status {
  font-size: 12px;
  color: #4b5563;
}

.doc-meta {
  font-size: 12px;
  color: #6b7280;
}

.editor-card {
  padding: 0;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background-color: #f9fafb;
}

.toolbar-btn {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  font-size: 12px;
  color: #374151;
}

.editor-area {
  width: 100%;
  min-height: 220px;
  padding: 12px 16px;
  border: none;
  outline: none;
  resize: vertical;
  font-size: 14px;
  line-height: 1.6;
  color: #111827;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px;
  border-top: 1px solid #e5e7eb;
  background-color: #f9fafb;
}

.save-status {
  font-size: 12px;
  color: #6b7280;
}

.online-users {
  display: flex;
  gap: 6px;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #e5f0ff;
  color: #2563eb;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-wrap {
  margin-top: 32px;
}

.empty-wrap {
  padding: 32px 16px;
}
</style>

