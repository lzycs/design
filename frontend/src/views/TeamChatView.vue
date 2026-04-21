<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getTeamRequestDetail } from '@/api/team'
import {
  getTeamChatMessages,
  markTeamChatRead,
  recallTeamChatMessage,
  sendTeamChatMessage,
  uploadTeamChatFile,
  type TeamChatMessage,
} from '@/api/teamChat'
import { useTeamChatStore } from '@/stores/teamChat'
import { getTeamStudyPlans, type StudyPlanVO } from '@/api/studyPlan'

const route = useRoute()
const router = useRouter()
const teamChatStore = useTeamChatStore()
const currentUser = ref<{ id?: number; username?: string } | null>(null)

const teamId = computed(() => Number(route.params.teamId))
const teamTitle = ref('小组聊天')
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const nextCursor = ref<number | null>(null)
type UiMsg = TeamChatMessage & { localStatus?: 'sending' | 'sent' | 'failed' }
const messages = ref<UiMsg[]>([])
const sending = ref(false)
const inputText = ref('')
const wsConnected = ref(false)
const scrollRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const readCursor = ref(0)
const showPlanPicker = ref(false)
const showMsgActions = ref(false)
const pressedMessage = ref<UiMsg | null>(null)
const planOptions = ref<StudyPlanVO[]>([])
let ws: WebSocket | null = null
let longPressTimer: number | null = null

const readUser = () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) return null
  try {
    return JSON.parse(raw) as { id?: number; username?: string }
  } catch {
    return null
  }
}

const wsUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/team-chat`
}

const appendMessages = (list: TeamChatMessage[]) => {
  const map = new Map<number, UiMsg>()
  messages.value.forEach((item) => map.set(item.id, item))
  list.forEach((item) => map.set(item.id, { ...item, localStatus: 'sent' }))
  messages.value = Array.from(map.values()).sort((a, b) => a.id - b.id)
}

const scrollToBottom = async () => {
  await nextTick()
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
}

const displayTime = (iso?: string) => {
  if (!iso) return ''
  const d = new Date(iso)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const fullTime = (iso?: string) => {
  if (!iso) return ''
  const d = new Date(iso)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

const renderedMessages = computed(() => {
  const out: Array<{ msg: UiMsg; showDivider: boolean; dividerText: string }> = []
  let prev: Date | null = null
  for (const msg of messages.value) {
    const cur = msg.createTime ? new Date(msg.createTime) : null
    const showDivider = !!cur && (!prev || cur.getTime() - prev.getTime() > 5 * 60 * 1000)
    out.push({ msg, showDivider, dividerText: showDivider ? fullTime(msg.createTime) : '' })
    if (cur) prev = cur
  }
  return out
})

const loadTitle = async () => {
  if (!teamId.value) return
  try {
    const res = await getTeamRequestDetail(teamId.value)
    teamTitle.value = res.data?.title || '小组聊天'
  } catch {
    teamTitle.value = '小组聊天'
  }
}

const loadHistory = async (isLoadMore = false) => {
  if (!teamId.value || !currentUser.value?.id) return
  if (isLoadMore) {
    if (!hasMore.value || loadingMore.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await getTeamChatMessages(teamId.value, currentUser.value.id, isLoadMore ? nextCursor.value ?? undefined : undefined, 100)
    const page = res.data
    const list = page?.list ?? []
    appendMessages(list)
    hasMore.value = !!page?.hasMore
    nextCursor.value = page?.nextCursor ?? null
    if (!isLoadMore) {
      await scrollToBottom()
    }
    const last = messages.value[messages.value.length - 1]
    if (last) {
      await markTeamChatRead(teamId.value, { userId: currentUser.value.id, lastReadMessageId: last.id })
      teamChatStore.clearTeamUnread(teamId.value)
    }
  } catch (e: any) {
    showToast(e?.response?.data?.message || '加载聊天记录失败')
  } finally {
    if (isLoadMore) loadingMore.value = false
    else loading.value = false
  }
}

const connectWs = () => {
  if (!currentUser.value?.id || !teamId.value) return
  ws = new WebSocket(wsUrl())
  ws.onopen = () => {
    wsConnected.value = true
    ws?.send(JSON.stringify({ action: 'subscribe', userId: currentUser.value?.id, teamId: teamId.value }))
  }
  ws.onmessage = async (evt) => {
    const data = JSON.parse(evt.data || '{}')
    if (data.event === 'message' && Number(data.teamId) === teamId.value && data.data) {
      appendMessages([data.data as TeamChatMessage])
      const incoming = data.data as TeamChatMessage
      if (incoming.senderId === currentUser.value?.id && incoming.clientMsgId) {
        messages.value = messages.value.filter((m) => !(m.id < 0 && m.clientMsgId === incoming.clientMsgId))
      }
      await scrollToBottom()
      if (currentUser.value?.id) {
        const last = messages.value[messages.value.length - 1]
        if (last) {
          await markTeamChatRead(teamId.value, { userId: currentUser.value.id, lastReadMessageId: last.id })
          teamChatStore.clearTeamUnread(teamId.value)
        }
      }
    }
    if (data.event === 'read' && Number(data.teamId) === teamId.value) {
      const reader = Number(data.userId)
      if (reader !== Number(currentUser.value?.id)) {
        readCursor.value = Math.max(readCursor.value, Number(data.lastReadMessageId || 0))
      }
    }
    if (data.event === 'recall' && Number(data.teamId) === teamId.value && data.data) {
      appendMessages([data.data as TeamChatMessage])
    }
  }
  ws.onclose = () => {
    wsConnected.value = false
  }
}

const closeWs = () => {
  if (ws) {
    ws.close()
    ws = null
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || !teamId.value || !currentUser.value?.id || sending.value) return
  const clientMsgId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const tempId = -Date.now()
  const temp: UiMsg = {
    id: tempId,
    teamRequestId: teamId.value,
    senderId: currentUser.value.id,
    senderName: currentUser.value.username || `用户 ${currentUser.value.id}`,
    type: 1,
    content: text,
    clientMsgId,
    createTime: new Date().toISOString(),
    localStatus: 'sending',
    status: 1,
    recalled: 0,
  }
  messages.value.push(temp)
  await scrollToBottom()
  inputText.value = ''
  sending.value = true
  try {
    if (wsConnected.value && ws) {
      ws.send(JSON.stringify({
        action: 'send',
        userId: currentUser.value.id,
        teamId: teamId.value,
        type: 1,
        content: text,
        clientMsgId,
      }))
      temp.localStatus = 'sent'
    } else {
      const res = await sendTeamChatMessage(teamId.value, {
        userId: currentUser.value.id,
        type: 1,
        content: text,
        clientMsgId,
      })
      appendMessages([res.data])
      messages.value = messages.value.filter((m) => m.id !== tempId)
      await scrollToBottom()
    }
  } catch {
    temp.localStatus = 'failed'
    showToast('发送失败')
  } finally {
    sending.value = false
  }
}

const resendMessage = async (msg: UiMsg) => {
  if (!teamId.value || !currentUser.value?.id) return
  msg.localStatus = 'sending'
  try {
    const res = await sendTeamChatMessage(teamId.value, {
      userId: currentUser.value.id,
      type: msg.type,
      content: msg.content,
      fileName: msg.fileName,
      fileSize: msg.fileSize,
      clientMsgId: msg.clientMsgId || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    })
    appendMessages([res.data])
    messages.value = messages.value.filter((m) => m.id !== msg.id)
  } catch {
    msg.localStatus = 'failed'
    showToast('重发失败')
  }
}

const recallMessage = async (msg: UiMsg) => {
  if (!teamId.value || !currentUser.value?.id || !msg.id || msg.id < 0) return
  try {
    if (wsConnected.value && ws) {
      ws.send(JSON.stringify({ action: 'recall', userId: currentUser.value.id, teamId: teamId.value, messageId: msg.id }))
    } else {
      const res = await recallTeamChatMessage(teamId.value, msg.id, currentUser.value.id)
      appendMessages([res.data])
    }
  } catch (e: any) {
    showToast(e?.response?.data?.message || '撤回失败')
  }
}

const canRecall = (msg: UiMsg) => isMine(msg) && msg.recalled !== 1 && msg.canRecall === 1
const getMsgState = (msg: UiMsg) => {
  if (msg.localStatus === 'sending') return '发送中'
  if (msg.localStatus === 'failed') return '发送失败'
  if (!isMine(msg)) return ''
  return msg.id <= readCursor.value ? '已读' : '已发送'
}
const isMine = (msg: UiMsg) => Number(msg.senderId) === Number(currentUser.value?.id)

const fileSizeText = (size?: number) => {
  if (!size || size <= 0) return '-'
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

const fileIcon = (name?: string) => {
  const s = (name || '').toLowerCase()
  if (s.endsWith('.pdf')) return 'PDF'
  if (s.endsWith('.doc') || s.endsWith('.docx')) return 'DOC'
  if (s.endsWith('.xls') || s.endsWith('.xlsx')) return 'XLS'
  if (s.endsWith('.ppt') || s.endsWith('.pptx')) return 'PPT'
  if (s.endsWith('.txt')) return 'TXT'
  return 'FILE'
}

const previewFile = (msg: UiMsg) => {
  if (!msg.content) return
  window.open(msg.content, '_blank')
}

const downloadFile = (msg: UiMsg) => {
  if (!msg.content) return
  const a = document.createElement('a')
  a.href = msg.content
  a.download = msg.fileName || 'file'
  a.target = '_blank'
  a.click()
}

const handlePickImage = async (evt: Event) => {
  const input = evt.target as HTMLInputElement
  const files = Array.from(input.files || []).slice(0, 9)
  if (!teamId.value || !currentUser.value?.id || files.length === 0) return
  for (const file of files) {
    try {
      const upload = await uploadTeamChatFile(teamId.value, currentUser.value.id, file)
      const payload = {
        userId: currentUser.value.id,
        type: 2,
        content: upload.data.url,
        fileName: upload.data.fileName,
        fileSize: upload.data.fileSize,
        clientMsgId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      }
      if (wsConnected.value && ws) ws.send(JSON.stringify({ action: 'send', teamId: teamId.value, ...payload }))
      else appendMessages([(await sendTeamChatMessage(teamId.value, payload)).data])
    } catch (e: any) {
      showToast(e?.response?.data?.message || '图片发送失败')
    }
  }
  input.value = ''
}

const handlePickFile = async (evt: Event) => {
  const input = evt.target as HTMLInputElement
  const file = input.files?.[0]
  if (!teamId.value || !currentUser.value?.id || !file) return
  try {
    const upload = await uploadTeamChatFile(teamId.value, currentUser.value.id, file)
    const payload = {
      userId: currentUser.value.id,
      type: 3,
      content: upload.data.url,
      fileName: upload.data.fileName,
      fileSize: upload.data.fileSize,
      clientMsgId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    }
    if (wsConnected.value && ws) ws.send(JSON.stringify({ action: 'send', teamId: teamId.value, ...payload }))
    else appendMessages([(await sendTeamChatMessage(teamId.value, payload)).data])
  } catch (e: any) {
    showToast(e?.response?.data?.message || '文件发送失败')
  } finally {
    input.value = ''
  }
}

const chooseImage = () => {
  imageInputRef.value?.click()
}

const chooseFile = () => {
  fileInputRef.value?.click()
}

const openPlanPicker = async () => {
  if (!teamId.value) return
  const res = await getTeamStudyPlans(teamId.value)
  planOptions.value = res.data ?? []
  showPlanPicker.value = true
}
const sendPlanCard = async (plan: StudyPlanVO) => {
  if (!teamId.value || !currentUser.value?.id) return
  const card = {
    planId: plan.id,
    title: plan.title,
    reservationDate: plan.reservationDate,
    timeRange: `${plan.reservationStartTime || ''}-${plan.reservationEndTime || ''}`,
    keyTimeNodes: plan.keyTimeNodes,
  }
  const payload = { userId: currentUser.value.id, type: 4, content: JSON.stringify(card), clientMsgId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}` }
  if (wsConnected.value && ws) ws.send(JSON.stringify({ action: 'send', teamId: teamId.value, ...payload }))
  else appendMessages([(await sendTeamChatMessage(teamId.value, payload)).data])
  showPlanPicker.value = false
}

const parseCard = (content: string) => {
  try { return JSON.parse(content) } catch { return null }
}

const focusTextInput = () => inputRef.value?.focus()

const startPress = (msg: UiMsg) => {
  stopPress()
  longPressTimer = window.setTimeout(() => {
    pressedMessage.value = msg
    showMsgActions.value = true
  }, 450)
}
const stopPress = () => {
  if (longPressTimer != null) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
}

const onMsgAction = async (action: string) => {
  const msg = pressedMessage.value
  showMsgActions.value = false
  if (!msg) return
  if (action === 'copy' && msg.type === 1) {
    await navigator.clipboard.writeText(msg.content || '')
    showToast('已复制')
    return
  }
  if (action === 'resend') {
    await resendMessage(msg)
    return
  }
  if (action === 'recall') {
    await recallMessage(msg)
  }
}

const msgActions = computed(() => {
  const msg = pressedMessage.value
  if (!msg) return []
  const list: Array<{ name: string; key: string }> = []
  if (msg.type === 1 && msg.content) list.push({ name: '复制', key: 'copy' })
  if (msg.localStatus === 'failed') list.push({ name: '重发', key: 'resend' })
  if (canRecall(msg)) list.push({ name: '撤回', key: 'recall' })
  return list
})

onMounted(async () => {
  currentUser.value = readUser()
  if (!currentUser.value?.id) {
    showToast('请先登录')
    router.push('/profile')
    return
  }
  await Promise.all([loadTitle(), loadHistory(false)])
  await teamChatStore.refreshUnreadSummary(currentUser.value.id)
  connectWs()
})

onBeforeUnmount(() => {
  closeWs()
})
</script>

<template>
  <div class="team-chat-page">
    <van-nav-bar :title="teamTitle" left-arrow @click-left="router.back()">
      <template #right>
        <span class="nav-action" @click="router.push('/profile/teams')">小组详情</span>
      </template>
    </van-nav-bar>
    <div ref="scrollRef" class="message-list">
      <div v-if="hasMore" class="load-more" @click="loadHistory(true)">
        {{ loadingMore ? '加载中...' : '加载更早消息' }}
      </div>
      <van-loading v-if="loading" vertical size="24">加载中...</van-loading>
      <template v-for="entry in renderedMessages" :key="entry.msg.id">
        <div v-if="entry.showDivider" class="time-divider">{{ entry.dividerText }}</div>
        <div
          class="message-row"
          :class="{ mine: isMine(entry.msg) }"
          @touchstart="startPress(entry.msg)"
          @touchend="stopPress"
          @touchmove="stopPress"
          @mousedown="startPress(entry.msg)"
          @mouseup="stopPress"
          @mouseleave="stopPress"
          @contextmenu.prevent="startPress(entry.msg)"
          :title="fullTime(entry.msg.createTime)"
        >
          <div class="avatar">{{ (entry.msg.senderName || 'U').slice(0, 1) }}</div>
          <div class="bubble" :class="{ recalled: entry.msg.recalled === 1 }">
            <div v-if="entry.msg.recalled === 1" class="sender">
              {{ entry.msg.senderName || `用户 ${entry.msg.senderId}` }}
            </div>
            <template v-if="entry.msg.recalled === 1">
              <div class="recalled-text">{{ entry.msg.senderName || `用户 ${entry.msg.senderId}` }} 撤回了一条消息</div>
            </template>
            <template v-else-if="entry.msg.type === 2">
              <img class="img-msg" :src="entry.msg.content" alt="img" @click="window.open(entry.msg.content, '_blank')" />
            </template>
            <template v-else-if="entry.msg.type === 3">
              <div class="file-card">
                <div class="file-icon">{{ fileIcon(entry.msg.fileName) }}</div>
                <div class="file-meta">
                  <div class="file-name">{{ entry.msg.fileName || '文件' }}</div>
                  <div class="file-size">{{ fileSizeText(entry.msg.fileSize) }}</div>
                </div>
                <button class="mini-btn" @click.stop="previewFile(entry.msg)">预览</button>
                <button class="mini-btn" @click.stop="downloadFile(entry.msg)">下载</button>
              </div>
            </template>
            <template v-else-if="entry.msg.type === 4">
              <div class="card-msg plan" @click="router.push('/shared-plan')">
                <div class="card-title">学习计划</div>
                <div>{{ parseCard(entry.msg.content)?.title || '计划' }}</div>
              </div>
            </template>
            <template v-else>
              <div class="text">{{ entry.msg.content }}</div>
            </template>
            <div v-if="entry.msg.recalled === 1" class="time">
              {{ displayTime(entry.msg.createTime) }}
              <span class="state">{{ getMsgState(entry.msg) }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-if="!loading && messages.length === 0" class="empty">暂无消息，开始聊天吧</div>
    </div>
    <div class="input-bar">
      <div class="tool-buttons">
        <button class="tool-btn" @click="chooseImage"><van-icon name="photo-o" class="tool-ico" />图片</button>
        <button class="tool-btn" @click="chooseFile"><van-icon name="description" class="tool-ico" />文件</button>
        <button class="tool-btn" @click="openPlanPicker"><van-icon name="todo-list-o" class="tool-ico" />计划</button>
      </div>
      <div class="input-row">
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="input"
          rows="1"
          maxlength="2000"
          placeholder="输入消息..."
        />
        <button class="send-btn" :disabled="sending || !inputText.trim()" @click="sendMessage">
          发送
        </button>
      </div>
    </div>
    <input ref="imageInputRef" type="file" accept="image/*" multiple class="hidden-input" @change="handlePickImage" />
    <input ref="fileInputRef" type="file" accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.txt" class="hidden-input" @change="handlePickFile" />
    <van-action-sheet
      v-model:show="showMsgActions"
      :actions="msgActions.map((a) => ({ name: a.name, key: a.key }))"
      @select="(a:any) => onMsgAction(a.key)"
    />
    <van-popup v-model:show="showPlanPicker" round position="bottom" :style="{ height: '50%' }">
      <div class="picker-wrap">
        <div class="picker-title">选择学习计划</div>
        <div v-for="p in planOptions" :key="p.id" class="picker-item" @click="sendPlanCard(p)">
          {{ p.title }}
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.team-chat-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.nav-action {
  color: #4a90e2;
  font-size: 14px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.load-more,
.empty {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.message-row {
  display: flex;
  width: 100%;
  margin-bottom: 10px;
  align-items: flex-end;
}

.message-row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 78%;
  border-radius: 12px;
  padding: 10px 12px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-row.mine .bubble {
  background: #4a90e2;
}

.message-row.mine .sender,
.message-row.mine .text,
.message-row.mine .time,
.message-row.mine .state {
  color: #ffffff;
}

.message-row.mine .state {
  opacity: 0.9;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #dbe8fb;
  color: #2f6fb8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  margin: 0 8px;
  flex-shrink: 0;
}

.message-row.mine .bubble {
  order: 1;
}

.message-row.mine .avatar {
  order: 2;
  margin-left: 8px;
  margin-right: 0;
}

.sender {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.text {
  font-size: 14px;
  color: #1a1a1a;
  white-space: pre-wrap;
  word-break: break-word;
}

.recalled-text {
  font-size: 13px;
  color: #6b7280;
  text-decoration: line-through;
}

.bubble.recalled {
  background: #eef1f5 !important;
  border: 1px dashed #d5dbe5;
  box-shadow: none;
}

.time {
  margin-top: 4px;
  text-align: right;
  font-size: 11px;
  color: #909399;
}
.state { margin-left: 6px; color: #4a90e2; }
.img-msg { max-width: 220px; border-radius: 8px; cursor: pointer; display:block; }
.file-card {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  background: #f7fbff;
  border: 1px solid #d7e7fb;
  border-radius: 10px;
  padding: 8px;
}
.file-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #eaf3ff;
  color: #2f6fb8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.file-meta { min-width: 0; }
.file-name {
  font-size: 13px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-size { font-size: 11px; color: #909399; }
.mini-btn {
  border: 1px solid #d7e7fb;
  background: #fff;
  color: #2f6fb8;
  border-radius: 6px;
  font-size: 12px;
  padding: 4px 8px;
}
.card-msg { border: 1px solid #d9e6f8; border-radius: 8px; padding: 8px; background: #f7fbff; cursor: pointer; }
.card-title { font-size: 12px; color: #909399; margin-bottom: 4px; }
.time-divider {
  margin: 8px auto 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: #edf2f8;
  color: #909399;
  font-size: 12px;
  width: fit-content;
}

.input-bar {
  border-top: 1px solid #edf2f8;
  background: #fff;
  padding: 8px 12px calc(8px + env(safe-area-inset-bottom, 0px));
  display: flex;
  gap: 8px;
  flex-direction: column;
}
.tool-buttons { display:flex; gap:6px; }
.tool-btn {
  border: 1px solid #dbe3ef;
  background: #fff;
  border-radius: 6px;
  height: 28px;
  min-width: 40px;
  font-size: 12px;
  color: #4a5568;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tool-ico {
  font-size: 14px;
  color: #4a90e2;
}
.hidden-input { display:none; }
.picker-wrap { padding: 12px; }
.picker-title { font-weight: 600; margin-bottom: 8px; }
.picker-item {
  padding: 10px 8px;
  border-bottom: 1px solid #eef2f8;
}

.input {
  flex: 1;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  resize: none;
  padding: 8px;
  font-size: 14px;
  min-height: 38px;
  max-height: 120px;
}

.send-btn {
  width: 78px;
  height: 38px;
  border: none;
  border-radius: 8px;
  background: #4a90e2;
  color: #fff;
}
.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.send-btn:disabled {
  opacity: 0.6;
}

@media (min-width: 1024px) {
  .message-list {
    max-width: 980px;
    width: 100%;
    margin: 0 auto;
    padding: 16px;
  }
}
</style>
