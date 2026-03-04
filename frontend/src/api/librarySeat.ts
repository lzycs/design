import request from '@/utils/request'

export interface LibrarySeat {
  id?: number
  libraryId?: number
  seatLabel?: string
  floor?: number
  rowNum?: number
  colNum?: number
  seatType?: number
  status?: number
  realTimeStatus?: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const getSeatsByLibraryAndFloor = (libraryId: number, floor: number) => {
  return request.get<any, Result<LibrarySeat[]>>(`/library-seat/library/${libraryId}/floor/${floor}`)
}

