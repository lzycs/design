import request from '@/utils/request'

export interface ReservationPayload {
  id?: number
  userId: number
  resourceType: number // 1-教室, 2-图书馆座位
  classroomId?: number
  librarySeatId?: number
  reservationDate: string
  startTime: string
  endTime: string
  duration: number
  purpose?: string
}

export interface Reservation extends ReservationPayload {
  status?: number
  qrcode?: string
  createTime?: string
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const createReservation = (payload: ReservationPayload) => {
  return request.post<any, Result<boolean>>('/reservation', payload)
}

export const getUserReservations = (userId: number) => {
  return request.get<any, Result<Reservation[]>>(`/reservation/user/${userId}`)
}

