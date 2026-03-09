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

export const createRepair = (payload: CreateRepairPayload) => {
  // 实际路径为 http://localhost:8080/api/repair
  return request.post<any, Result<boolean>>('/repair', payload)
}

// 待评价教室项
export interface PendingFeedbackItem {
  id: number
  userId: number
  classroomId: number
  classroomName: string
  usedStartTime?: string
  usedEndTime?: string
  status: number
}

// 已评价项
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
}

// 获取待评价列表
export const getPendingFeedback = (userId: number) => {
  return request.get<any, Result<PendingFeedbackItem[]>>('/feedback/pending', {
    params: { userId }
  })
}

// 获取已评价列表
export const getFeedbackList = (userId: number) => {
  return request.get<any, Result<FeedbackItem[]>>('/feedback', {
    params: { userId }
  })
}

// 提交/更新评价
export const submitFeedback = (
  id: number,
  data: { envScore: number; equipScore: number; content?: string }
) => {
  return request.put<any, Result<boolean>>(`/feedback/${id}`, data)
}

// 删除评价
export const deleteFeedback = (id: number) => {
  return request.delete<any, Result<boolean>>(`/feedback/${id}`)
}

