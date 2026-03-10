import request from '@/utils/request'

export interface StudyPlanVO {
  id?: number
  teamRequestId?: number
  userId?: number
  reservationId?: number
  title?: string
  description?: string
  planDate?: string
  startTime?: string
  endTime?: string
  status?: number
  createTime?: string
  updateTime?: string
  keyTimeNodes?: string
  teamTitle?: string
  classroomName?: string
  reservationDate?: string
  reservationStartTime?: string
  reservationEndTime?: string
}

export interface CreateStudyPlanPayload {
  teamRequestId?: number
  userId: number
  title?: string
  description?: string
  planDate?: string
  startTime?: string
  endTime?: string
  keyTimeNodes?: string
  classroomId?: number
  reservationDate?: string
  resStartTime?: string
  resEndTime?: string
}

export const getSharedPlans = (userId: number) => {
  return request.get<unknown, { data?: StudyPlanVO[] }>(`/study-plan/shared/${userId}`)
}

export const getTeamStudyPlans = (teamRequestId: number) => {
  return request.get<unknown, { data?: StudyPlanVO[] }>(`/study-plan/team/${teamRequestId}`)
}

export const createStudyPlanWithReservation = (payload: CreateStudyPlanPayload) => {
  return request.post<unknown, { data?: unknown }>('/study-plan/create-with-reservation', payload)
}

export const updateStudyPlan = (id: number, payload: CreateStudyPlanPayload) => {
  return request.put<unknown, { data?: unknown }>(`/study-plan/${id}`, payload)
}
