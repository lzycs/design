import request from '@/utils/request'

export interface Result<T> {
  code: number
  message: string
  data: T
}

// 后端 Repair 实体创建所需字段（resourceType: 1-教室 2-图书馆座位）
export interface CreateRepairPayload {
  userId: number
  resourceType?: number
  classroomId: number
  title: string
  description: string
  type: number
  priority?: number
  images?: string
}

export interface PendingFeedbackItem {
  id: number
  userId: number
  classroomId: number
  classroomName: string
  usedStartTime?: string
  usedEndTime?: string
  status: number
}

export interface FeedbackItem {
  id: number
  userId: number
  classroomId: number
  classroomName: string
  envScore: number
  equipScore: number
  content?: string
  status: number
  createdAt: string
  updatedAt?: string
  averageScore?: number | null
}

export const createRepair = (payload: CreateRepairPayload) => {
  return request.post<any, Result<boolean>>('/repair', payload)
}

export const getPendingFeedback = (userId: number) => {
  return request.get<any, Result<PendingFeedbackItem[]>>('/feedback/pending', {
    params: { userId }
  })
}

export const getFeedbackList = (userId: number) => {
  return request.get<any, Result<FeedbackItem[]>>('/feedback', {
    params: { userId }
  })
}

export const getApprovedClassroomFeedback = (classroomId: number) => {
  return request.get<any, Result<FeedbackItem[]>>(`/feedback/classroom/${classroomId}/approved`)
}

export const submitFeedback = (
  id: number,
  data: { envScore: number; equipScore: number; content?: string }
) => {
  return request.put<any, Result<boolean>>(`/feedback/${id}`, data)
}

export const deleteFeedback = (id: number) => {
  return request.delete<any, Result<boolean>>(`/feedback/${id}`)
}

