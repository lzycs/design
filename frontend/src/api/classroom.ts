import request from '@/utils/request'

export interface Classroom {
  id?: number
  buildingId?: number
  name: string
  roomNumber?: string
  floor?: number
  type?: number
  capacity?: number
  equipment?: string
  latitude?: number
  longitude?: number
  checkinRadius?: number
  status?: number
  realTimeStatus?: number
  environmentScore?: number
  totalReviews?: number
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 教室详情（含热度星级、综合评分均值、签到次数） */
export interface ClassroomDetailVO {
  classroom: Classroom
  /** 热度星级 1-5 */
  popularityStars: number
  /** 综合评分（历史评分平均值） */
  averageScore: number | null
  /** 历史评价条数 */
  totalReviews: number
  /** 签到次数 */
  checkinCount: number
}

export const getClassroomList = () => {
  return request.get<any, Result<Classroom[]>>('/classroom/list')
}

export const getClassroomsByBuilding = (buildingId: number) => {
  return request.get<any, Result<Classroom[]>>(`/classroom/building/${buildingId}`)
}

export const getAvailableClassrooms = (type?: number) => {
  return request.get<any, Result<Classroom[]>>('/classroom/available', { params: { type } })
}

export const getClassroomById = (id: number) => {
  return request.get<any, Result<Classroom>>(`/classroom/${id}`)
}

export const getClassroomDetail = (id: number) => {
  return request.get<any, Result<ClassroomDetailVO>>(`/classroom/${id}/detail`)
}

export const saveClassroom = (classroom: Classroom) => {
  return request.post<any, Result<boolean>>('/classroom', classroom)
}

export const updateClassroom = (classroom: Classroom) => {
  return request.put<any, Result<boolean>>('/classroom', classroom)
}

export const deleteClassroom = (id: number) => {
  return request.delete<any, Result<boolean>>(`/classroom/${id}`)
}
