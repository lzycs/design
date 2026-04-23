import request from '@/utils/request'

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface TimeSlot {
  id: number
  label: string
  startTime: string
  endTime: string
  sortOrder?: number
}

export interface UserAvailabilityItem {
  id?: number
  userId: number
  weekDay: number
  slotId: number
}

export const getTimeSlots = () => request.get<unknown, Result<TimeSlot[]>>('/time-slot/list')

export const getUserAvailability = (userId: number) =>
  request.get<unknown, Result<UserAvailabilityItem[]>>(`/user-availability/${userId}`)

export const saveUserAvailability = (payload: { userId: number; items: Array<{ weekDay: number; slotId: number }> }) =>
  request.put<unknown, Result<boolean>>('/user-availability', payload)

