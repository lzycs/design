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
  checkinTime?: string
  resourceName?: string
  buildingName?: string
  classroomRoomNumber?: string
  libraryName?: string
  seatLabel?: string
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

export interface ClassroomSlotStatus {
  label: string
  startTime: string
  endTime: string
  status: 'available' | 'occupied'
}

export const getClassroomSlots = (classroomId: number, date: string) => {
  return request.get<any, Result<ClassroomSlotStatus[]>>('/reservation/classroom/' + classroomId + '/slots', {
    params: { date }
  })
}

export const updateReservation = (reservation: Reservation) => {
  return request.put<any, Result<boolean>>('/reservation', reservation)
}

export const checkinReservation = (id: number) => {
  return request.post<any, Result<boolean>>(`/reservation/${id}/checkin`)
}

