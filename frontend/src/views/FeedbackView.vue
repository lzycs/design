<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getAvailableClassrooms, type Classroom as ApiClassroom } from '../api/classroom'
import {
  createRepair,
  getPendingFeedback,
  getFeedbackList,
  submitFeedback,
  deleteFeedback,
  type PendingFeedbackItem,
  type FeedbackItem,
  type CreateRepairPayload
} from '../api/feedback'

const FEEDBACK_STATUS_LABEL: Record<number, string> = {
  2: '待审核',
  3: '审核通过',
  4: '审核不通过'
}

const router = useRouter()
const route = useRoute()

// 顶部标签：一键报修 / 评价列表（支持从「我的」页「我的评价」跳转时仅显示评价列表，不显示报修和标签栏）
const activeTopTab = ref<'repair' | 'evaluation'>('repair')
const isEvaluationOnly = computed(() => route.query.tab === 'evaluation' && route.query.only === '1')

// 评价筛选：全部 / 待评价 / 已评价
const evalFilter = ref<'all' | 'pending' | 'evaluated'>('all')

// 教室下拉：从后端 /api/classroom/available 获取
const classrooms = ref<ApiClassroom[]>([])

// 一键报修表单
const repairClassroomId = ref<number | null>(null)
const repairFaultType = ref('air')
const repairDesc = ref('')
const repairPhotos = ref<string[]>([])
const repairPhotoUploading = ref(false)
const maxRepairPhotos = 6
const maxPhotoSize = 3 * 1024 * 1024 // 3MB

function onRepairPhotoRead(file: File) {
  if (repairPhotos.value.length >= maxRepairPhotos) {
    window.alert(`最多上传 ${maxRepairPhotos} 张照片`)
    return
  }
  if (file.size > maxPhotoSize) {
    window.alert('单张照片不超过 3MB')
    return
  }
  repairPhotoUploading.value = true
  const reader = new FileReader()
  reader.onload = () => {
    const dataUrl = reader.result as string
    if (dataUrl) repairPhotos.value.push(dataUrl)
    repairPhotoUploading.value = false
  }
  reader.onerror = () => {
    repairPhotoUploading.value = false
    window.alert('读取图片失败')
  }
  reader.readAsDataURL(file)
}

function removeRepairPhoto(index: number) {
  repairPhotos.value.splice(index, 1)
}

// 评价数据
const pendingList = ref<PendingFeedbackItem[]>([])
const evaluatedList = ref<FeedbackItem[]>([])

// 评价表单弹层
const showEvalForm = ref(false)
const evalClassroomName = ref('')
const evalFeedbackId = ref<number | null>(null)
const evalEnvScore = ref(0)
const evalEquipScore = ref(0)
const evalContent = ref('')

// 从 localStorage 获取当前登录用户
const storedUser = ref<{ id: number } | null>(null)
const currentUserId = computed(() => storedUser.value?.id ?? null)

const filteredPendingList = computed(() => {
  if (evalFilter.value === 'all' || evalFilter.value === 'pending') {
    return pendingList.value
  }
  return []
})

const filteredEvaluatedList = computed(() => {
  if (evalFilter.value === 'all' || evalFilter.value === 'evaluated') {
    return evaluatedList.value
  }
  return []
})

const classroomNameById = (id: number) => {
  return classrooms.value.find(c => c.id === id)?.name ?? ''
}

const evaluationStatusLabel = (status: number) => FEEDBACK_STATUS_LABEL[status] ?? '未知状态'

const evaluationStatusClass = (status: number) => {
  if (status === 2) return 'status-pending'
  if (status === 3) return 'status-approved'
  if (status === 4) return 'status-rejected'
  return 'status-unknown'
}

const evaluationStatusHint = (status: number) => {
  if (status === 2) return '评价提交后将由 commentadmin 账号审核，通过后才会展示在教室详情页。'
  if (status === 3) return '该评价已通过审核，当前已在教室详情页展示。'
  if (status === 4) return '该评价未通过审核，记录保留但不会在教室详情页展示。'
  return ''
}

const loadUserFromStorage = () => {
  const raw = localStorage.getItem('currentUser')
  if (raw) {
    try {
      storedUser.value = JSON.parse(raw) as { id: number }
    } catch {
      storedUser.value = null
    }
  } else {
    storedUser.value = null
  }
}

const loadClassrooms = async () => {
  try {
    const res = await getAvailableClassrooms()
    const list = res.data ?? []
    classrooms.value = Array.isArray(list) ? list : []
    if (repairClassroomId.value == null && classrooms.value.length > 0) {
      repairClassroomId.value = classrooms.value[0]?.id ?? null
    }
  } catch (e) {
    console.error('加载教室列表失败', e)
    classrooms.value = []
  }
}

// 加载评价数据
const loadFeedbackData = async () => {
  if (!currentUserId.value) {
    pendingList.value = []
    evaluatedList.value = []
    return
  }
  try {
    const [pendingRes, evaluatedRes] = await Promise.all([
      getPendingFeedback(currentUserId.value),
      getFeedbackList(currentUserId.value)
    ])
    pendingList.value = pendingRes.data ?? []
    evaluatedList.value = evaluatedRes.data ?? []
  } catch (e) {
    console.error('加载评价数据失败', e)
  }
}

// 提交报修
const onSubmitRepair = async () => {
  if (!repairDesc.value.trim()) {
    window.alert('请填写问题描述后提交！')
    return
  }
  if (!currentUserId.value) {
    window.alert('请先登录后再提交报修')
    return
  }
  if (!repairClassroomId.value) {
    window.alert('请先选择教室')
    return
  }
  try {
    // 映射故障类型到后端 type 字段
    const faultTypeMap: Record<string, number> = {
      air: 2,
      projector: 4,
      furniture: 3,
      power: 5,
      network: 5,
      other: 6
    }
    const type = faultTypeMap[repairFaultType.value] ?? 6

    const classroomName = classroomNameById(repairClassroomId.value)
    const payload: CreateRepairPayload = {
      userId: currentUserId.value,
      resourceType: 1,
      classroomId: repairClassroomId.value,
      title: classroomName ? `${classroomName} 报修` : '教室报修',
      description: repairDesc.value.trim(),
      type,
      priority: 2,
      images: repairPhotos.value.length ? JSON.stringify(repairPhotos.value) : undefined
    }

    const res = await createRepair(payload) as { code?: number; message?: string; data?: boolean }
    if (res?.code !== 200 && res?.code !== undefined) {
      showToast(res?.message || '提交失败，请稍后重试')
      return
    }
    repairDesc.value = ''
    repairPhotos.value = []
    showToast('报修工单提交成功')
    router.push('/profile/repairs')
  } catch (e) {
    console.error('提交报修失败', e)
    window.alert('提交失败，请稍后重试')
  }
}

// 打开评价表单
const openEvalForm = (item: PendingFeedbackItem | FeedbackItem) => {
  evalFeedbackId.value = item.id
  if ('envScore' in item) {
    evalEnvScore.value = item.envScore ?? 0
    evalEquipScore.value = item.equipScore ?? 0
    evalContent.value = item.content ?? ''
  } else {
    evalEnvScore.value = 0
    evalEquipScore.value = 0
    evalContent.value = ''
  }
  evalClassroomName.value =
    'classroomName' in item && item.classroomName
      ? item.classroomName
      : classroomNameById(item.classroomId)
  showEvalForm.value = true
}

// 星级点击
const setEnvScore = (score: number) => {
  evalEnvScore.value = score
}

const setEquipScore = (score: number) => {
  evalEquipScore.value = score
}

// 提交评价
const onSubmitEvaluation = async () => {
  if (!evalFeedbackId.value) return
  if (!evalEnvScore.value || !evalEquipScore.value) {
    window.alert('请完成整体环境和设备设施的评分！')
    return
  }
  try {
    await submitFeedback(evalFeedbackId.value, {
      envScore: evalEnvScore.value,
      equipScore: evalEquipScore.value,
      content: evalContent.value.trim() || undefined
    })
    window.alert('评价已提交，当前状态为待审核，审核通过后才会展示在教室详情页。')
    showEvalForm.value = false
    await loadFeedbackData()
  } catch (e) {
    console.error('提交评价失败', e)
    window.alert('提交失败，请稍后重试')
  }
}

// 删除评价
const onDeleteEvaluation = async (item: FeedbackItem) => {
  if (!window.confirm(`确定要删除【${item.classroomName}】的评价吗？`)) {
    return
  }
  try {
    await deleteFeedback(item.id)
    window.alert('删除成功')
    await loadFeedbackData()
  } catch (e) {
    console.error('删除评价失败', e)
    window.alert('删除失败，请稍后重试')
  }
}

// 底部 Tab 跳转
const goHome = () => router.push('/')

const goBackToProfile = () => router.push('/profile')

onMounted(() => {
  loadUserFromStorage()
  loadClassrooms()
  loadFeedbackData()
  if (route.query.tab === 'evaluation' || route.query.only === '1') {
    activeTopTab.value = 'evaluation'
  }
})
</script>

<template>
  <div class="feedback">
    <!-- 顶部标题栏：从「我的评价」进入时只显示「评价列表」，返回个人中心 -->
    <van-nav-bar
      :title="isEvaluationOnly ? '评价列表' : '反馈报修'"
      left-arrow
      @click-left="isEvaluationOnly ? goBackToProfile() : goHome()"
    />

    <!-- 顶部标签导航（仅反馈页完整模式显示） -->
    <div v-if="!isEvaluationOnly" class="top-tab-bar">
      <div
        class="top-tab-item"
        :class="{ active: activeTopTab === 'repair' }"
        @click="activeTopTab = 'repair'"
      >
        一键报修
      </div>
      <div
        class="top-tab-item"
        :class="{ active: activeTopTab === 'evaluation' }"
        @click="activeTopTab = 'evaluation'"
      >
        评价列表
      </div>
    </div>

    <!-- 一键报修表单区域（仅反馈页且选了一键报修时显示） -->
    <div v-if="!isEvaluationOnly && activeTopTab === 'repair'" class="form-area">
      <div class="form-card">
        <!-- 选择教室 -->
        <div class="form-item">
          <label class="form-label">选择教室</label>
          <select v-model="repairClassroomId" class="form-select">
            <option v-for="c in classrooms" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
        </div>

        <!-- 故障类型 -->
        <div class="form-item">
          <label class="form-label">故障类型</label>
          <select v-model="repairFaultType" class="form-select">
            <option value="air">空调故障</option>
            <option value="projector">投影仪故障</option>
            <option value="furniture">桌椅损坏</option>
            <option value="power">水电问题</option>
            <option value="network">网络故障</option>
            <option value="other">其他问题</option>
          </select>
        </div>

        <!-- 问题描述 -->
        <div class="form-item">
          <label class="form-label">问题描述</label>
          <textarea
            v-model="repairDesc"
            class="form-textarea"
            placeholder="请详细描述故障情况，便于后勤人员处理"
          />
        </div>

        <!-- 上传照片（可选，base64 存入工单） -->
        <div class="form-item">
          <label class="form-label">上传照片（可选，最多 {{ maxRepairPhotos }} 张）</label>
          <div class="upload-area">
            <div
              v-for="(photo, idx) in repairPhotos"
              :key="idx"
              class="upload-item upload-preview"
            >
              <img :src="photo" alt="报修图" class="upload-preview-img" />
              <button type="button" class="upload-remove" @click="removeRepairPhoto(idx)">×</button>
            </div>
            <label v-if="repairPhotos.length < maxRepairPhotos" class="upload-item upload-trigger">
              <input
                type="file"
                accept="image/*"
                class="upload-input"
                :disabled="repairPhotoUploading"
                @change="(e: Event) => { const t = (e.target as HTMLInputElement); if (t.files?.[0]) { onRepairPhotoRead(t.files[0]); t.value = ''; } }"
              />
              <span class="upload-plus">{{ repairPhotoUploading ? '…' : '+' }}</span>
            </label>
          </div>
        </div>

        <!-- 提交按钮 -->
        <button class="submit-btn" @click="onSubmitRepair">提交报修工单</button>
      </div>
    </div>

    <!-- 评价列表区域（从「我的评价」进入或顶部选「评价列表」时显示） -->
    <div v-if="isEvaluationOnly || activeTopTab === 'evaluation'" class="form-area">
      <!-- 筛选标签 -->
      <div class="eval-filter-bar">
        <div
          class="filter-item"
          :class="{ active: evalFilter === 'all' }"
          @click="evalFilter = 'all'"
        >
          全部
        </div>
        <div
          class="filter-item"
          :class="{ active: evalFilter === 'pending' }"
          @click="evalFilter = 'pending'"
        >
          待评价
        </div>
        <div
          class="filter-item"
          :class="{ active: evalFilter === 'evaluated' }"
          @click="evalFilter = 'evaluated'"
        >
          已评价
        </div>
      </div>

      <!-- 待评价教室列表 -->
      <div class="pending-list">
        <div
          v-for="item in filteredPendingList"
          :key="item.id"
          class="classroom-card"
          data-status="pending"
        >
          <div class="classroom-header">
            <div class="classroom-name">
              {{ item.classroomName || classroomNameById(item.classroomId) }}
            </div>
            <div class="classroom-status">已签到</div>
          </div>
          <div class="classroom-time">
            使用时间：
            {{ item.usedStartTime }} - {{ item.usedEndTime }}
          </div>
          <button class="go-eval-btn" @click="openEvalForm(item)">去评价</button>
        </div>
      </div>

      <!-- 已评价列表 -->
      <div class="evaluated-list">
        <div
          v-for="item in filteredEvaluatedList"
          :key="item.id"
          class="evaluation-card"
          data-status="evaluated"
        >
          <div class="evaluation-header">
            <div>
              <div class="evaluation-classroom">{{ item.classroomName }}</div>
              <div class="evaluation-time">评价时间：{{ item.createdAt }}</div>
            </div>
            <div class="evaluation-status" :class="evaluationStatusClass(item.status)">
              {{ evaluationStatusLabel(item.status) }}
            </div>
          </div>

          <div class="evaluation-category">
            <div class="category-item">
              <span class="category-name">整体环境</span>
              <div class="star-rating">
                <span
                  v-for="n in 5"
                  :key="n"
                  class="star-item"
                  :class="{ active: n <= item.envScore }"
                >
                  ★
                </span>
                <span class="star-score">{{ item.envScore.toFixed(1) }}分</span>
              </div>
            </div>
            <div class="category-item">
              <span class="category-name">设备设施</span>
              <div class="star-rating">
                <span
                  v-for="n in 5"
                  :key="n"
                  class="star-item"
                  :class="{ active: n <= item.equipScore }"
                >
                  ★
                </span>
                <span class="star-score">{{ item.equipScore.toFixed(1) }}分</span>
              </div>
            </div>
          </div>

          <div v-if="item.content" class="evaluation-content">
            {{ item.content }}
          </div>

          <div class="evaluation-audit-hint">
            {{ evaluationStatusHint(item.status) }}
          </div>

          <div class="evaluation-actions">
            <button class="eval-btn btn-edit" @click="openEvalForm(item)">
              编辑评价
            </button>
            <button class="eval-btn btn-delete" @click="onDeleteEvaluation(item)">
              删除评价
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 评价表单页面（简化为弹层区域） -->
    <div v-if="showEvalForm" class="evaluation-form-mask">
      <div class="evaluation-form-panel">
        <div class="page-header">
          <a
            href="javascript:void(0)"
            class="back-btn"
            @click="showEvalForm = false"
            style="text-decoration: none;"
          >
            <span class="back-icon">&lt;</span>
          </a>
          <div class="page-header-title">评价教室</div>
          <div style="width: 24px;"></div>
        </div>

        <div class="form-area">
          <div class="form-card">
            <div class="form-item">
              <label class="form-label">教室名称</label>
              <div class="form-select readonly">{{ evalClassroomName }}</div>
            </div>

            <div class="form-item">
              <label class="form-label">整体环境评分</label>
              <div class="star-rating">
                <span
                  v-for="n in 5"
                  :key="'env-' + n"
                  class="star-item"
                  :class="{ active: n <= evalEnvScore }"
                  @click="setEnvScore(n)"
                >
                  ★
                </span>
                <span class="star-score">{{ evalEnvScore.toFixed(1) }}分</span>
              </div>
            </div>

            <div class="form-item">
              <label class="form-label">设备设施评分</label>
              <div class="star-rating">
                <span
                  v-for="n in 5"
                  :key="'equip-' + n"
                  class="star-item"
                  :class="{ active: n <= evalEquipScore }"
                  @click="setEquipScore(n)"
                >
                  ★
                </span>
                <span class="star-score">{{ evalEquipScore.toFixed(1) }}分</span>
              </div>
            </div>

            <div class="form-item">
              <label class="form-label">评价内容</label>
              <textarea
                v-model="evalContent"
                class="form-textarea"
                placeholder="请输入你的评价内容（选填）"
              />
            </div>

            <button class="submit-btn" @click="onSubmitEvaluation">提交评价</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feedback {
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #f5f7fa;
}

.back-btn {
  margin-right: 16px;
}

.back-icon {
  font-size: 20px;
  color: #1a1a1a;
}

.page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  text-align: center;
}

.top-tab-bar {
  display: flex;
  background-color: #ffffff;
  border-bottom: 1px solid #f5f7fa;
}

.top-tab-item {
  flex: 1;
  text-align: center;
  padding: 16px 0;
  font-size: 16px;
  color: #909399;
  font-weight: 500;
  position: relative;
  cursor: pointer;
}

.top-tab-item.active {
  color: #4a90e2;
}

.top-tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 3px;
  background-color: #4a90e2;
  border-radius: 2px;
}

.form-area {
  padding: 20px;
  padding-bottom: 80px;
}

.form-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px;
}

.form-item {
  margin-bottom: 20px;
}

.form-label {
  font-size: 14px;
  color: #333333;
  font-weight: 500;
  margin-bottom: 8px;
  display: block;
}

.form-select {
  width: 100%;
  height: 44px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 14px;
  color: #1a1a1a;
}

.form-select.readonly {
  display: flex;
  align-items: center;
  border: none;
  padding-left: 0;
}

.form-textarea {
  width: 100%;
  min-height: 120px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: #1a1a1a;
  resize: none;
}

.form-textarea::placeholder {
  color: #909399;
}

.upload-area {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.upload-item {
  width: 80px;
  height: 80px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f9f9f9;
}

.upload-plus {
  font-size: 24px;
  color: #c0c4cc;
}

.upload-trigger {
  cursor: pointer;
}

.upload-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
}

.upload-preview {
  position: relative;
  overflow: hidden;
}

.upload-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn {
  width: 100%;
  height: 48px;
  background-color: #4a90e2;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 12px;
}

.submit-btn:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.eval-filter-bar {
  display: flex;
  background-color: #ffffff;
  padding: 12px 20px;
  border-bottom: 1px solid #f5f7fa;
  margin: -20px -20px 20px;
}

.filter-item {
  padding: 6px 16px;
  font-size: 14px;
  color: #909399;
  border-radius: 20px;
  margin-right: 12px;
  cursor: pointer;
}

.filter-item.active {
  background-color: #4a90e2;
  color: #ffffff;
}

.classroom-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
  position: relative;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.classroom-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.classroom-status {
  font-size: 12px;
  color: #67c23a;
  background-color: #f0f9ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.classroom-time {
  font-size: 12px;
  color: #909399;
}

.go-eval-btn {
  position: absolute;
  right: 16px;
  bottom: 16px;
  padding: 6px 12px;
  background-color: #4a90e2;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.evaluation-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 12px;
}

.evaluation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.evaluation-classroom {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.evaluation-time {
  font-size: 12px;
  color: #909399;
}

.evaluation-status {
  min-width: 72px;
  text-align: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fff7e8;
  color: #d48806;
}

.status-approved {
  background: #edf9f1;
  color: #2b8a3e;
}

.status-rejected {
  background: #fff1f0;
  color: #cf1322;
}

.status-unknown {
  background: #f2f3f5;
  color: #86909c;
}

.evaluation-category {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-name {
  font-size: 14px;
  color: #333333;
}

.evaluation-content {
  font-size: 14px;
  color: #666666;
  line-height: 1.5;
  padding: 8px 12px;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 12px;
}

.evaluation-audit-hint {
  font-size: 12px;
  line-height: 1.6;
  color: #7b8190;
  margin-bottom: 12px;
}

.evaluation-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.eval-btn {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  border: none;
  cursor: pointer;
}

.btn-edit {
  background-color: #f5f7fa;
  color: #666666;
}

.btn-delete {
  background-color: #fef0f0;
  color: #f56c6c;
}

.star-rating {
  display: flex;
  gap: 4px;
  align-items: center;
}

.star-item {
  font-size: 20px;
  color: #e5e6eb;
  cursor: pointer;
}

.star-item.active {
  color: #e6a23c;
}

.star-score {
  font-size: 14px;
  color: #666666;
  margin-left: 8px;
}

.evaluation-form-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: center;
  align-items: flex-end;
  z-index: 100;
}

.evaluation-form-panel {
  width: 100%;
  background: #f5f7fa;
  max-height: 90%;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}
</style>

