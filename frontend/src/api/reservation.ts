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
  qrcodeExpireTime?: string
  qrcodeScanTime?: string
  qrcodeScanDeviceUid?: string
  qrcodeScanStatus?: number
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

/** 预约规则：每周次数上限、单次最长分钟数 */
export interface ReservationLimitVO {
  maxPerWeek: number
  maxDurationMinutes: number
}

export const getReservationLimits = () => {
  return request.get<any, Result<ReservationLimitVO>>('/reservation/limits')
}

export const createReservation = (payload: ReservationPayload) => {
  return request.post<any, Result<boolean>>('/reservation', payload)
}

export const getUserReservations = (userId: number) => {
  return request.get<any, Result<Reservation[]>>(`/reservation/user/${userId}`)
}

export const getReservationById = (id: number) => {
  return request.get<any, Result<Reservation>>(`/reservation/${id}`)
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

/** 生成预约二维码（用户端触发） */
export const generateReservationQrcode = (id: number) => {
  return request.post<any, Result<{ code: string; ok: boolean }>>(`/reservation/${id}/qrcode`)
}

/** 获取预约二维码状态（用户端轮询） */
export const getReservationQrcodeStatus = (code: string) => {
  return request.get<any, Result<{ ok: boolean; message: string; success?: boolean; expired?: boolean }>>(
    `/reservation/qrcode/${code}/status`,
  )
}

/** 设备端扫码核销预约二维码 */
export const scanReservationQrcode = (code: string, deviceUid: string) => {
  return request.post<any, Result<{ ok: boolean; message: string; success?: boolean }>>(
    `/reservation/qrcode/${code}/scan`,
    { deviceUid },
  )
}

