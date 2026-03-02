import request from '@/utils/request'

export interface Library {
  id?: number
  name: string
  address?: string
  floorCount?: number
  description?: string
  latitude?: number
  longitude?: number
  openingHours?: string
  status?: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const getLibraryList = () => {
  return request.get<any, Result<Library[]>>('/library/list')
}

