import request from '@/utils/request'

export interface ResourceMarketItem {
  id?: number
  userId?: number
  publisherName?: string
  title?: string
  description?: string
  category?: string
  resourceType?: number
  price?: number | null
  isFree?: number
  originType?: number
  sourceReference?: string
  courseId?: number | null
  teamRequestId?: number | null
  tags?: string
  images?: string
  recommendedPlace?: string
  status?: number
  createTime?: string
}

export interface ResourceMarketTrade {
  id?: number
  itemId?: number
  itemTitle?: string
  publisherId?: number
  publisherName?: string
  receiverId?: number
  status?: number
  note?: string
  meetingPlace?: string
  confirmPublisher?: number
  confirmReceiver?: number
  createTime?: string
}

export interface CampusCredit {
  id?: number
  userId?: number
  creditScore?: number
  scoreComment?: string
}

export const getMarketItems = (params?: { category?: string; freeOnly?: boolean }) =>
  request.get<unknown, { data?: ResourceMarketItem[] }>('/resource-market/items', { params })

export const getMyMarketItems = (userId: number) =>
  request.get<unknown, { data?: ResourceMarketItem[] }>(`/resource-market/items/my/${userId}`)

export const publishMarketItem = (payload: ResourceMarketItem) =>
  request.post<unknown, { data?: ResourceMarketItem }>('/resource-market/items', payload)

export const createTradeIntent = (
  itemId: number,
  payload: { userId: number; note?: string; meetingPlace?: string },
) =>
  request.post<unknown, { data?: ResourceMarketTrade }>(`/resource-market/items/${itemId}/intent`, payload)

export const confirmTrade = (tradeId: number, userId: number) =>
  request.post<unknown, { data?: ResourceMarketTrade }>(`/resource-market/trades/${tradeId}/confirm`, { userId })

export const getUserTrades = (userId: number) =>
  request.get<unknown, { data?: ResourceMarketTrade[] }>(`/resource-market/trades/user/${userId}`)

export const getCampusCredit = (userId: number) =>
  request.get<unknown, { data?: CampusCredit }>(`/resource-market/credit/${userId}`)
