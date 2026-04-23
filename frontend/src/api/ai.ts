import request from '@/utils/request'

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface AiPlanGenerateRequest {
  userId: number
  goalType: 'exam' | 'cert' | 'review'
  goalText: string
  dailyMinutes: number
  startDate: string
  days: number
}

export interface AiPlanGenerateResponse {
  startDate: string
  goalType: string
  goalText: string
  dailyMinutes: number
  days: Array<{ date: string; tasks: Array<{ title: string; minutes: number; suggestedSlotLabel?: string }> }>
  savedStudyPlanIds?: number[]
}

export const generateAiPlan = (payload: AiPlanGenerateRequest) => {
  return request.post<any, Result<AiPlanGenerateResponse>>('/ai/plan/generate', payload)
}

export const generateAiPlanAndSave = (payload: AiPlanGenerateRequest) => {
  return request.post<any, Result<AiPlanGenerateResponse>>('/ai/plan/generate-save', payload)
}

export interface AiChatMessage {
  role: 'system' | 'user' | 'assistant'
  content: string
}

export const aiChat = (payload: { userId: number; scene?: string; messages: AiChatMessage[] }) => {
  return request.post<any, Result<{
    text: string
    provider: string
    resources?: Array<{ id: number; title: string; category?: string; isFree?: number; price?: number | null; publisherName?: string }>
    teams?: Array<{ id: number; title: string; status?: number; currentCount?: number; expectedCount?: number }>
  }>>('/ai/chat', payload)
}

export const checkAiDeviation = (params: { userId: number; date?: string }) => {
  return request.get<any, Result<{ overdueStudyPlanIds: number[]; missedCheckinReservationIds: number[] }>>(
    '/ai/deviation/check',
    { params },
  )
}

