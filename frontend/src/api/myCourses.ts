import request from '@/utils/request'

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface MyCourseItem {
  courseId: number
  courseName: string
  teacherName: string
  weekDay: number
  teachingWeek: number
  date: string
  startTime: string
  endTime: string
  classroomId?: number
  classroomName?: string
  roomNumber?: string
  buildingName?: string
  isStarred?: number
  note?: string
  remindBeforeMinutes?: number
}

export const getMyCoursesByDay = (params: { userId: number; date: string; keyword?: string }) => {
  return request.get<any, Result<MyCourseItem[]>>('/my-courses/day', { params })
}

export const getMyCoursesByWeek = (params: { userId: number; date: string; keyword?: string }) => {
  return request.get<any, Result<{ startDate: string; days: Array<{ date: string; weekDay: number; courses: MyCourseItem[] }> }>>(
    '/my-courses/week',
    { params },
  )
}

export const updateMyCourseNote = (
  courseId: number,
  payload: { userId: number; isStarred?: number; note?: string; remindBeforeMinutes?: number },
) => {
  return request.put<any, Result<boolean>>(`/my-courses/${courseId}/note`, payload)
}
