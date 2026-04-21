import request from '@/utils/request'

export interface TeamChatMessage {
  id: number
  teamRequestId: number
  senderId: number
  senderName?: string
  type: number
  content: string
  fileName?: string
  fileSize?: number
  clientMsgId?: string
  status?: number
  recalled?: number
  canRecall?: number
  createTime: string
}

export interface TeamChatPage {
  list: TeamChatMessage[]
  nextCursor: number | null
  hasMore: boolean
}

export interface TeamUnreadItem {
  teamRequestId: number
  teamTitle?: string
  unreadCount: number
  lastMessagePreview?: string
  lastMessageTime?: string
}

export interface TeamUnreadSummary {
  totalUnread: number
  teamUnreadList: TeamUnreadItem[]
}

interface Result<T> {
  code: number
  message: string
  data: T
}

export const getTeamChatMessages = (teamId: number, userId: number, beforeId?: number, size = 100) => {
  return request.get<unknown, Result<TeamChatPage>>(`/team-chat/${teamId}/messages`, {
    params: { userId, beforeId, size },
  })
}

export const sendTeamChatMessage = (
  teamId: number,
  payload: { userId: number; type?: number; content: string; fileName?: string; fileSize?: number; clientMsgId?: string },
) => {
  return request.post<unknown, Result<TeamChatMessage>>(`/team-chat/${teamId}/send`, payload)
}

export const markTeamChatRead = (teamId: number, payload: { userId: number; lastReadMessageId: number }) => {
  return request.post<unknown, Result<boolean>>(`/team-chat/${teamId}/read`, payload)
}

export const getTeamUnreadSummary = (userId: number) => {
  return request.get<unknown, Result<TeamUnreadSummary>>('/team-chat/unread-summary', { params: { userId } })
}

export const recallTeamChatMessage = (teamId: number, messageId: number, userId: number) => {
  return request.post<unknown, Result<TeamChatMessage>>(`/team-chat/${teamId}/messages/${messageId}/recall`, { userId })
}

export const uploadTeamChatFile = (teamId: number, userId: number, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<unknown, Result<{ url: string; fileName: string; fileSize: number; isImage: boolean }>>(
    `/team-chat/${teamId}/upload?userId=${encodeURIComponent(String(userId))}`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}
