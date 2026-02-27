import request from '@/utils/request'

export interface Classroom {
  id?: number
  buildingId?: number
  name: string
  floor?: number
  capacity?: number
  type?: string
  status?: string
  description?: string
}

export const getClassroomList = () => {
  return request.get<Classroom[]>('/classroom/list')
}

export const getClassroomsByBuilding = (buildingId: number) => {
  return request.get<Classroom[]>(`/classroom/building/${buildingId}`)
}

export const getAvailableClassrooms = () => {
  return request.get<Classroom[]>('/classroom/available')
}

export const getClassroomById = (id: number) => {
  return request.get<Classroom>(`/classroom/${id}`)
}
