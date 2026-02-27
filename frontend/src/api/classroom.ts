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

export const getClassroomList = () => {
  return request.get<Result<Classroom[]>>('/classroom/list')
}

export const getClassroomsByBuilding = (buildingId: number) => {
  return request.get<Result<Classroom[]>>(`/classroom/building/${buildingId}`)
}

export const getAvailableClassrooms = (type?: number) => {
  return request.get<Result<Classroom[]>>('/classroom/available', { params: { type } })
}

export const getClassroomById = (id: number) => {
  return request.get<Result<Classroom>>(`/classroom/${id}`)
}

export const saveClassroom = (classroom: Classroom) => {
  return request.post<Result<boolean>>('/classroom', classroom)
}

export const updateClassroom = (classroom: Classroom) => {
  return request.put<Result<boolean>>('/classroom', classroom)
}

export const deleteClassroom = (id: number) => {
  return request.delete<Result<boolean>>(`/classroom/${id}`)
}
