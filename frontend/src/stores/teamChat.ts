import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getTeamUnreadSummary, type TeamUnreadItem } from '@/api/teamChat'

export const useTeamChatStore = defineStore('teamChat', () => {
  const unreadMap = ref<Record<number, number>>({})
  const unreadList = ref<TeamUnreadItem[]>([])
  const totalUnread = ref(0)
  const loading = ref(false)

  const hasUnread = computed(() => totalUnread.value > 0)

  const setUnread = (teamId: number, count: number) => {
    unreadMap.value[teamId] = Math.max(0, count)
    totalUnread.value = Object.values(unreadMap.value).reduce((sum, cur) => sum + cur, 0)
  }

  const clearTeamUnread = (teamId: number) => {
    setUnread(teamId, 0)
  }

  const increaseTeamUnread = (teamId: number, delta = 1) => {
    const current = unreadMap.value[teamId] ?? 0
    setUnread(teamId, current + delta)
  }

  const refreshUnreadSummary = async (userId?: number) => {
    if (!userId) return
    loading.value = true
    try {
      const res = await getTeamUnreadSummary(userId)
      const summary = res.data
      const nextMap: Record<number, number> = {}
      ;(summary?.teamUnreadList ?? []).forEach((item) => {
        nextMap[item.teamRequestId] = item.unreadCount ?? 0
      })
      unreadMap.value = nextMap
      unreadList.value = summary?.teamUnreadList ?? []
      totalUnread.value = summary?.totalUnread ?? 0
    } finally {
      loading.value = false
    }
  }

  return {
    unreadMap,
    unreadList,
    totalUnread,
    hasUnread,
    loading,
    setUnread,
    clearTeamUnread,
    increaseTeamUnread,
    refreshUnreadSummary,
  }
})
