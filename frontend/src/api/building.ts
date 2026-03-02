import request from '@/utils/request'

export interface Building {
  id?: number
  name: string
  address?: string
  floorCount?: number
  description?: string
  latitude?: number
  longitude?: number
  status?: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const getBuildingList = () => {
  return request.get<unknown, Result<Building[]>>('/building/list')
}

export const getBuildingById = (id: number) => {
  return request.get<unknown, Result<Building>>(`/building/${id}`)
}

export const saveBuilding = (building: Building) => {
  return request.post<unknown, Result<boolean>>('/building', building)
}

export const updateBuilding = (building: Building) => {
  return request.put<unknown, Result<boolean>>('/building', building)
}

export const deleteBuilding = (id: number) => {
  return request.delete<unknown, Result<boolean>>(`/building/${id}`)
}
