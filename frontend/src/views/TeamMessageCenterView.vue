<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTeamChatStore } from '@/stores/teamChat'

const router = useRouter()
const store = useTeamChatStore()
const currentUser = ref<{ id?: number } | null>(null)

onMounted(async () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) return
  try {
    currentUser.value = JSON.parse(raw)
  } catch {
    currentUser.value = null
  }
  if (currentUser.value?.id) {
    await store.refreshUnreadSummary(currentUser.value.id)
  }
})
</script>

<template>
  <div class="message-center">
    <van-nav-bar title="小组消息" left-arrow @click-left="router.back()" />
    <div class="content">
      <div v-if="store.unreadList.length === 0" class="empty">暂无未读消息</div>
      <div
        v-for="item in store.unreadList"
        :key="item.teamRequestId"
        class="msg-card"
        @click="router.push(`/profile/teams/${item.teamRequestId}/chat`)"
      >
        <div class="title">{{ item.teamTitle || `小组 ${item.teamRequestId}` }}</div>
        <div class="preview">{{ item.lastMessagePreview || '有新消息' }}</div>
        <div class="meta">
          <span>{{ item.lastMessageTime?.replace('T', ' ') || '' }}</span>
          <span class="dot">{{ item.unreadCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-center {
  min-height: 100vh;
  background: #f5f7fa;
}

.content {
  padding: 12px;
}

.msg-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 10px;
}

.title {
  font-weight: 600;
}

.preview {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
}

.meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.dot {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  text-align: center;
  line-height: 18px;
  padding: 0 4px;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 40px;
}
</style>
