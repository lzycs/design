<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { aiChat, checkAiDeviation, generateAiPlan, generateAiPlanAndSave } from '@/api/ai'
import { getTimeSlots, getUserAvailability, saveUserAvailability, type TimeSlot } from '@/api/availability'

const router = useRouter()

const userId = ref<number | null>(null)
const activeTab = ref<'plan' | 'qa'>('plan')

const goalType = ref<'exam' | 'cert' | 'review'>('review')
const goalText = ref('')
const dailyMinutes = ref(120)
const startDate = ref(new Date().toISOString().slice(0, 10))
const planDays = ref(7)

const timeSlots = ref<TimeSlot[]>([])
const editingAvailability = ref(false)
const availabilityMap = ref<Record<number, Record<number, boolean>>>({}) // weekDay -> slotId -> checked

const generated = ref<
  | null
  | {
      startDate: string
      days: Array<{ date: string; tasks: Array<{ title: string; minutes: number; suggestedSlotLabel?: string }> }>
    }
>(null)

const qaInput = ref('')
const qaHistory = ref<
  Array<{
    role: 'user' | 'assistant'
    content: string
    resources?: Array<{ id: number; title: string; category?: string; isFree?: number; price?: number | null; publisherName?: string }>
    teams?: Array<{ id: number; title: string; status?: number; currentCount?: number; expectedCount?: number }>
  }>
>([])
const deviationTip = ref<string | null>(null)

const canGenerate = computed(() => !!goalText.value.trim() && Number(dailyMinutes.value) > 0 && !!startDate.value)

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

const initAvailabilityMap = () => {
  const m: Record<number, Record<number, boolean>> = {}
  for (let wd = 1; wd <= 7; wd++) {
    m[wd] = {}
    for (const s of timeSlots.value) m[wd][s.id] = false
  }
  availabilityMap.value = m
}

const loadAvailability = async () => {
  if (!userId.value) return
  try {
    const [slotsRes, availRes] = await Promise.all([getTimeSlots(), getUserAvailability(userId.value)])
    if (slotsRes.code === 200) timeSlots.value = slotsRes.data ?? []
    initAvailabilityMap()
    if (availRes.code === 200) {
      for (const it of availRes.data ?? []) {
        if (!availabilityMap.value[it.weekDay]) availabilityMap.value[it.weekDay] = {}
        availabilityMap.value[it.weekDay][it.slotId] = true
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const saveAvailability = async () => {
  if (!userId.value) return
  const items: Array<{ weekDay: number; slotId: number }> = []
  for (let wd = 1; wd <= 7; wd++) {
    const row = availabilityMap.value[wd] ?? {}
    for (const s of timeSlots.value) {
      if (row[s.id]) items.push({ weekDay: wd, slotId: s.id })
    }
  }
  try {
    const res = await saveUserAvailability({ userId: userId.value, items })
    if (res.code !== 200) {
      showToast(res.message || '保存失败')
      return
    }
    showToast('已保存可用时段')
    editingAvailability.value = false
  } catch (e) {
    console.error(e)
    showToast('保存失败，请稍后重试')
  }
}

const generatePlan = async () => {
  if (!userId.value) {
    showToast('请先登录')
    await router.replace('/profile')
    return
  }
  if (!canGenerate.value) {
    showToast('请填写学习目标与每日可用时长')
    return
  }
  try {
    const res = await generateAiPlan({
      userId: userId.value,
      goalType: goalType.value,
      goalText: goalText.value.trim(),
      dailyMinutes: Number(dailyMinutes.value) || 120,
      startDate: startDate.value,
      days: Math.max(1, Math.min(14, Number(planDays.value) || 7)),
    })
    if (res.code !== 200) {
      showToast(res.message || '生成失败')
      return
    }
    generated.value = res.data ?? null
    showToast('已生成')
  } catch (e) {
    console.error(e)
    showToast('生成失败，请稍后重试')
  }
}

const generateAndSavePlan = async () => {
  if (!userId.value) {
    showToast('请先登录')
    await router.replace('/profile')
    return
  }
  if (!canGenerate.value) {
    showToast('请填写学习目标与每日可用时长')
    return
  }
  try {
    const res = await generateAiPlanAndSave({
      userId: userId.value,
      goalType: goalType.value,
      goalText: goalText.value.trim(),
      dailyMinutes: Number(dailyMinutes.value) || 120,
      startDate: startDate.value,
      days: Math.max(1, Math.min(14, Number(planDays.value) || 7)),
    })
    if (res.code !== 200) {
      showToast(res.message || '生成失败')
      return
    }
    generated.value = res.data ?? null
    showToast(`已保存 ${res.data?.savedStudyPlanIds?.length ?? 0} 条到学习计划`)
  } catch (e) {
    console.error(e)
    showToast('生成失败，请稍后重试')
  }
}

const ask = async () => {
  const q = qaInput.value.trim()
  if (!q) return
  qaHistory.value.push({ role: 'user', content: q })
  qaInput.value = ''
  try {
    if (!userId.value) {
      showToast('请先登录')
      await router.replace('/profile')
      return
    }
    const res = await aiChat({
      userId: userId.value,
      scene: 'qa',
      messages: [{ role: 'user', content: q }],
    })
    if (res.code !== 200) {
      qaHistory.value.push({ role: 'assistant', content: res.message || '请求失败' })
      return
    }
    qaHistory.value.push({
      role: 'assistant',
      content: res.data?.text || '（无返回）',
      resources: res.data?.resources,
      teams: res.data?.teams,
    })
  } catch (e) {
    console.error(e)
    qaHistory.value.push({ role: 'assistant', content: '请求失败，请稍后重试' })
  }
}

onMounted(() => {
  loadUser()
  loadAvailability()
  ;(async () => {
    if (!userId.value) return
    try {
      const res = await checkAiDeviation({ userId: userId.value })
      if (res.code !== 200) return
      const overdue = res.data?.overdueStudyPlanIds?.length ?? 0
      const missed = res.data?.missedCheckinReservationIds?.length ?? 0
      if (overdue > 0 || missed > 0) {
        deviationTip.value = `提醒：你有 ${overdue} 条学习计划待完成，${missed} 条预约可能未按时签到。`
      } else {
        deviationTip.value = null
      }
    } catch (e) {
      console.error(e)
    }
  })()
})
</script>

<template>
  <div class="ai-page">
    <van-nav-bar title="智能助手" left-arrow @click-left="router.back()" />
    <div class="content">
      <div class="card tab-head">
        <button class="tab-btn" :class="{ active: activeTab === 'plan' }" @click="activeTab = 'plan'">生成计划</button>
        <button class="tab-btn" :class="{ active: activeTab === 'qa' }" @click="activeTab = 'qa'">智能答疑</button>
      </div>

      <div v-if="activeTab === 'plan'" class="card">
        <div class="title">AI 学习计划（MVP）</div>
        <div v-if="deviationTip" class="warn">{{ deviationTip }}</div>
        <div class="sub-actions">
          <button class="sub-btn" @click="editingAvailability = !editingAvailability">
            {{ editingAvailability ? '收起可用时段' : '编辑可用时段' }}
          </button>
        </div>

        <div v-if="editingAvailability" class="avail-wrap">
          <div class="avail-tip">勾选你每周可学习的时间段，生成计划会优先从这里挑选并自动避开课程冲突。</div>
          <div v-if="timeSlots.length === 0" class="avail-empty">暂无时间段数据</div>
          <div v-else class="avail-table">
            <div class="avail-head">
              <div class="c0">星期</div>
              <div v-for="s in timeSlots" :key="s.id" class="c">{{ s.label }}</div>
            </div>
            <div v-for="wd in [1, 2, 3, 4, 5, 6, 7]" :key="wd" class="avail-row">
              <div class="c0">{{ ['一','二','三','四','五','六','日'][wd - 1] }}</div>
              <div v-for="s in timeSlots" :key="s.id" class="c">
                <input v-model="availabilityMap[wd][s.id]" type="checkbox" />
              </div>
            </div>
          </div>
          <button class="primary-btn" @click="saveAvailability">保存可用时段</button>
        </div>

        <div class="row">
          <select v-model="goalType" class="input">
            <option value="review">课程复习</option>
            <option value="exam">考研</option>
            <option value="cert">考证</option>
          </select>
          <input v-model.number="dailyMinutes" type="number" class="input" placeholder="每日分钟" />
        </div>
        <div class="row">
          <input v-model.trim="goalText" class="input" placeholder="学习目标（如：数据结构、408、六级）" />
        </div>
        <div class="row">
          <input v-model="startDate" type="date" class="input" />
          <input v-model.number="planDays" type="number" class="input" placeholder="天数" min="1" max="14" />
        </div>
        <div class="btn-row">
          <button class="primary-btn" :disabled="!canGenerate" @click="generatePlan">一键生成</button>
          <button class="ghost-btn" :disabled="!canGenerate" @click="generateAndSavePlan">生成并保存</button>
        </div>

        <div v-if="generated" class="plan-wrap">
          <div class="plan-title">计划预览</div>
          <div v-for="d in generated.days" :key="d.date" class="plan-day">
            <div class="day-head">{{ d.date }}</div>
            <div v-for="(t, idx) in d.tasks" :key="idx" class="task">
              <div class="task-title">{{ t.title }}</div>
              <div class="task-meta">预计 {{ t.minutes }} 分钟 · {{ t.suggestedSlotLabel }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="card">
        <div class="title">智能答疑（MVP）</div>
        <div class="qa-list">
          <div v-for="(m, idx) in qaHistory" :key="idx" class="qa-item" :class="m.role">
            <div>{{ m.content }}</div>
            <div v-if="m.role === 'assistant' && (m.resources?.length || m.teams?.length)" class="rec-wrap">
              <div v-if="m.resources?.length" class="rec-block">
                <div class="rec-title">资源市场推荐</div>
                <div v-for="r in m.resources" :key="r.id" class="rec-card">
                  <div class="rec-card-title">{{ r.title }}</div>
                  <div class="rec-card-meta">
                    <span v-if="r.category">{{ r.category }}</span>
                    <span v-if="r.isFree === 1">免费</span>
                    <span v-else-if="r.price != null">￥{{ r.price }}</span>
                  </div>
                </div>
              </div>
              <div v-if="m.teams?.length" class="rec-block">
                <div class="rec-title">协作小组推荐</div>
                <div v-for="t in m.teams" :key="t.id" class="rec-card">
                  <div class="rec-card-title">{{ t.title }}</div>
                  <div class="rec-card-meta">
                    <span v-if="t.currentCount != null && t.expectedCount != null">{{ t.currentCount }}/{{ t.expectedCount }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="qaHistory.length === 0" class="empty">输入问题开始对话，例如：什么是快速排序？</div>
        </div>
        <div class="qa-input-row">
          <input v-model.trim="qaInput" class="input" placeholder="请输入你的问题" @keyup.enter="ask" />
          <button class="send-btn" @click="ask">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-page { min-height: 100vh; background: #f5f7fa; }
.content { padding: 12px; }
.card { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; }
.tab-head { display: flex; gap: 8px; }
.tab-btn { flex: 1; border: none; border-radius: 14px; padding: 8px 12px; background: #eef2f7; color: #667085; }
.tab-btn.active { background: #4a90e2; color: #fff; }
.title { font-size: 15px; font-weight: 700; color: #111827; margin-bottom: 10px; }
.warn { background: #fff7ed; border: 1px solid #fed7aa; color: #9a3412; border-radius: 10px; padding: 10px; font-size: 12px; margin-bottom: 10px; line-height: 1.4; }
.sub-actions { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.sub-btn { border: 1px solid #e5e7eb; background: #fff; border-radius: 10px; padding: 8px 10px; color: #111827; font-size: 12px; }
.avail-wrap { border: 1px solid #eef0f3; border-radius: 10px; padding: 10px; margin-bottom: 10px; background: #fbfdff; }
.avail-tip { font-size: 12px; color: #667085; margin-bottom: 8px; line-height: 1.4; }
.avail-empty { font-size: 12px; color: #98a2b3; padding: 8px 0; }
.avail-table { overflow-x: auto; border: 1px solid #eef0f3; border-radius: 10px; }
.avail-head, .avail-row { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(90px, 1fr); }
.avail-head { background: #f8fafc; font-size: 12px; font-weight: 700; color: #111827; }
.avail-row { background: #fff; border-top: 1px solid #eef0f3; }
.c0 { min-width: 54px; padding: 8px; display: flex; align-items: center; justify-content: center; color: #111827; }
.c { padding: 8px; display: flex; align-items: center; justify-content: center; }
.row { display: flex; gap: 8px; margin-bottom: 8px; }
.input { flex: 1; border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px; }
.primary-btn { width: 100%; border: none; border-radius: 10px; padding: 10px 12px; background: #4a90e2; color: #fff; font-weight: 600; }
.primary-btn:disabled { opacity: 0.6; }
.btn-row { display: flex; gap: 8px; }
.ghost-btn { flex: 1; border: 1px solid #cfd8e3; border-radius: 10px; padding: 10px 12px; background: #fff; color: #111827; font-weight: 600; }
.ghost-btn:disabled { opacity: 0.6; }
.plan-wrap { margin-top: 12px; }
.plan-title { font-size: 13px; font-weight: 700; margin-bottom: 8px; color: #374151; }
.plan-day { border: 1px solid #eef0f3; border-radius: 10px; padding: 10px; margin-bottom: 8px; }
.day-head { font-size: 13px; font-weight: 700; margin-bottom: 6px; color: #111827; }
.task { padding: 8px; border-radius: 8px; background: #f8fafc; margin-bottom: 6px; }
.task-title { font-size: 13px; color: #111827; }
.task-meta { margin-top: 4px; font-size: 12px; color: #667085; }
.qa-list { max-height: 52vh; overflow: auto; padding: 4px; }
.qa-item { margin-bottom: 8px; padding: 8px 10px; border-radius: 10px; font-size: 13px; line-height: 1.5; }
.qa-item.user { background: #eef4ff; color: #1d4ed8; }
.qa-item.assistant { background: #f5f6f8; color: #111827; }
.rec-wrap { margin-top: 8px; display: grid; gap: 10px; }
.rec-block { background: #ffffff; border: 1px solid #eef0f3; border-radius: 10px; padding: 10px; }
.rec-title { font-size: 12px; font-weight: 700; color: #111827; margin-bottom: 6px; }
.rec-card { padding: 8px; border-radius: 8px; background: #f8fafc; margin-bottom: 6px; }
.rec-card-title { font-size: 12px; font-weight: 700; color: #111827; }
.rec-card-meta { margin-top: 4px; font-size: 12px; color: #667085; display: flex; gap: 8px; flex-wrap: wrap; }
.empty { color: #98a2b3; text-align: center; padding: 16px 0; font-size: 13px; }
.qa-input-row { display: flex; gap: 8px; margin-top: 10px; }
.send-btn { border: none; border-radius: 8px; background: #4a90e2; color: #fff; padding: 8px 14px; }
</style>

