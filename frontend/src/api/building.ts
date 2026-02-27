import request from '@/utils/request'

export interface Building {
  id?: number
  name: string
  location?: string
  floors?: number
  description?: string
  active?: boolean
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const getBuildingList = () => {
  return request.get<Result<Building[]>>('/building/list')
}

export const getBuildingById = (id: number) => {
  return request.get<Building>(`/building/${id}`)
}

export const saveBuilding = (building: Building) => {
  return request.post<boolean>('/building', building)
}

export const updateBuilding = (building: Building) => {
  return request.put<boolean>('/building', building)
}

export const deleteBuilding = (id: number) => {
  return request.delete<boolean>(`/building/${id}`)
}
