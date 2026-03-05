<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getTeamMessages,
  sendTeamMessage,
  type TeamMessage,
} from '@/api/team'

interface User {
  id?: number
  username?: string
}

const route = useRoute()
const router = useRouter()

const teamId = Number(route.params.id)
const teamTitle = (route.query.title as string) || '小组聊天'

const storedUser = ref<User | null>(null)
const messages = ref<TeamMessage[]>([])
const loading = ref(false)

const inputText = ref('')
const sending = ref(false)

const loadUserFromStorage = () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) {
    storedUser.value = null
    return
  }
  try {
    storedUser.value = JSON.parse(raw) as User
  } catch {
    storedUser.value = null
  }
}

const loadMessages = async () => {
  if (!teamId) return
  loading.value = true
  try {
    const res = await getTeamMessages(teamId, 100)
    messages.value = res.data ?? []
  } catch (e) {
    console.error('加载小组消息失败', e)
    messages.value = []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const isMyMessage = (msg: TeamMessage) => {
  if (!storedUser.value?.id) return false
  return msg.senderId === storedUser.value.id
}

const formatTime = (value?: string) => {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return value
  }
  const h = d.getHours().toString().padStart(2, '0')
  const m = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

const handleSend = async () => {
  const content = inputText.value.trim()
  if (!content) return
  if (!storedUser.value?.id) {
    window.alert('请先登录后再发送消息')
    router.push('/profile')
    return
  }
  if (!teamId) {
    window.alert('无效的小组编号')
    return
  }
  sending.value = true
  try {
    const res = await sendTeamMessage(teamId, {
      senderId: storedUser.value.id,
      senderName: storedUser.value.username,
      type: 1,
      content,
      fileName: undefined,
      fileSize: undefined,
    })
    const msg = res.data
    if (msg) {
      messages.value.push(msg)
    } else {
      messages.value.push({
        senderId: storedUser.value.id,
        senderName: storedUser.value.username,
        type: 1,
        content,
        createTime: new Date().toISOString(),
      })
    }
    inputText.value = ''
  } catch (e) {
    console.error('发送消息失败', e)
    window.alert('发送失败，请稍后再试')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadUserFromStorage()
  loadMessages()
})
</script>

<template>
  <div class="chat-page">
    <div class="phone-container">
      <!-- 聊天标题栏 -->
      <div class="chat-header">
        <a href="javascript:void(0)" class="back-btn" @click="goBack">
          <span class="back-icon">&lt;</span>
        </a>
        <div class="chat-avatar"></div>
        <div class="chat-title">{{ teamTitle }}</div>
        <div class="chat-tools"></div>
      </div>

      <!-- 聊天内容 -->
      <div class="chat-content">
        <van-loading
          v-if="loading"
          size="24px"
          type="spinner"
          style="margin-top: 16px"
        >
          加载中...
        </van-loading>
        <template v-else>
          <div
            v-for="msg in messages"
            :key="msg.id ?? msg.createTime"
            class="chat-message"
            :class="{ right: isMyMessage(msg) }"
          >
            <div v-if="!isMyMessage(msg)" class="message-avatar"></div>
            <div class="message-content">
              <div class="message-text">
                <div v-if="msg.senderName" class="sender-name">
                  {{ msg.senderName }}
                </div>
                <div>
                  {{ msg.content }}
                </div>
              </div>
              <div class="message-time">
                {{ formatTime(msg.createTime) }}
              </div>
            </div>
            <div v-if="isMyMessage(msg)" class="message-avatar"></div>
          </div>
        </template>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="chat-input">
          <input
            v-model="inputText"
            type="text"
            class="input-box"
            placeholder="输入消息..."
            @keyup.enter="handleSend"
          />
          <button
            class="send-btn"
            :disabled="!inputText.trim() || sending"
            @click="handleSend"
          >
            {{ sending ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
}

.phone-container {
  width: 100%;
  max-width: 375px;
  height: 640px;
  background-color: #ffffff;
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #f5f7fa;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  margin-right: 12px;
  text-decoration: none;
}

.back-icon {
  font-size: 20px;
  color: #1a1a1a;
}

.chat-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e5e6eb;
  margin-right: 12px;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
}

.chat-content {
  padding: 16px 20px;
  flex: 1;
  overflow-y: auto;
}

.chat-message {
  margin-bottom: 16px;
  display: flex;
}

.chat-message.right {
  justify-content: flex-end;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #e5e6eb;
  margin-right: 8px;
  flex-shrink: 0;
}

.chat-message.right .message-avatar {
  margin-right: 0;
  margin-left: 8px;
}

.message-content {
  max-width: 70%;
}

.message-text {
  background-color: #ffffff;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.5;
}

.chat-message.right .message-text {
  background-color: #4a90e2;
  color: #ffffff;
}

.sender-name {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  text-align: right;
}

.chat-input-area {
  padding: 12px 16px 20px;
  background-color: #ffffff;
  border-top: 1px solid #f5f7fa;
}

.chat-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-box {
  flex: 1;
  height: 40px;
  border: 1px solid #e5e6eb;
  border-radius: 20px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
}

.send-btn {
  width: 80px;
  height: 40px;
  background-color: #4a90e2;
  color: #ffffff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.send-btn:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}
</style>

