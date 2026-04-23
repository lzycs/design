import request from '@/utils/request'

export interface Result<T> {
  code: number
  message: string
  data: T
}

export type SlotStatus = 'course_occupied' | 'reservation_occupied' | 'available'

export interface CourseScheduleCell {
  slotId: number
  label: string
  startTime: string
  endTime: string
  finalStatus: SlotStatus
  courseName?: string
  teacherName?: string
}

export interface BuildingScheduleRow {
  classroomId: number
  classroomName: string
  roomNumber?: string
  floor?: number
  cells: CourseScheduleCell[]
}

export interface BuildingDaySchedule {
  view: 'day'
  date: string
  buildingId: number
  teachingWeek: number
  rows: BuildingScheduleRow[]
}

export interface ClassroomSchedule {
  classroomId: number
  buildingId: number
  classroomName: string
  roomNumber?: string
  date: string
  view: string
  slots: CourseScheduleCell[]
}

export const getBuildingDaySchedule = (buildingId: number, params: {
  date: string
  floor?: number
  roomKeyword?: string
  courseKeyword?: string
  timeRange?: string
}) => {
  return request.get<any, Result<BuildingDaySchedule>>(`/course-schedule/building/${buildingId}/day`, { params })
}

export const getBuildingWeekSchedule = (buildingId: number, params: { date: string; floor?: number }) => {
  return request.get<any, Result<any>>(`/course-schedule/building/${buildingId}/week`, { params })
}

export const getBuildingMonthSchedule = (buildingId: number, params: { year: number; month: number }) => {
  return request.get<any, Result<any>>(`/course-schedule/building/${buildingId}/month`, { params })
}

export const getClassroomSchedule = (classroomId: number, date: string) => {
  return request.get<any, Result<ClassroomSchedule>>(`/course-schedule/classroom/${classroomId}`, { params: { date } })
}
