import request from '@/utils/request'

export interface TeamRequest {
  id?: number
  userId?: number
  title?: string
  description?: string
  tags?: string
  expectedCount?: number
  currentCount?: number
  status?: number
  createTime?: string
  updateTime?: string
}

/** 协作列表/详情 VO：含发起人姓名 */
export interface TeamRequestVO {
  id?: number
  userId?: number
  title?: string
  description?: string
  tags?: string
  expectedCount?: number
  currentCount?: number
  status?: number
  createTime?: string
  updateTime?: string
  creatorName?: string
}

export interface TeamMember {
  id?: number
  teamRequestId?: number
  userId: number
  role?: number
}

/** 成员展示 VO：含姓名 */
export interface TeamMemberVO {
  id?: number
  teamRequestId?: number
  userId: number
  role?: number
  joinTime?: string
  memberName?: string
}

export interface TeamMessage {
  id?: number
  teamRequestId?: number
  senderId?: number
  senderName?: string
  type?: number
  content?: string
  fileName?: string
  fileSize?: number
  createTime?: string
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const getActiveTeamRequests = () => {
  return request.get<unknown, Result<TeamRequest[]>>('/team/requests/active')
}

export const getTeamRequestDetail = (id: number) => {
  return request.get<unknown, Result<TeamRequestVO>>(`/team/request/${id}`)
}

export const createTeamRequest = (payload: TeamRequest) => {
  return request.post<unknown, Result<TeamRequest>>('/team/request', payload)
}

export const updateTeamStatus = (id: number, status: number) => {
  return request.put<unknown, Result<boolean>>(`/team/request/${id}/status`, { status })
}

export const joinTeam = (requestId: number, member: TeamMember) => {
  return request.post<unknown, Result<boolean>>(`/team/request/${requestId}/join`, member)
}

export const getUserTeams = (userId: number) => {
  return request.get<unknown, Result<TeamRequestVO[]>>(`/team/user/${userId}`)
}

export const getTeamMembers = (teamRequestId: number) => {
  return request.get<unknown, Result<TeamMemberVO[]>>(
    `/team/request/${teamRequestId}/members`,
  )
}

export const getTeamMessages = (teamRequestId: number, limit?: number) => {
  const params: Record<string, number> = {}
  if (limit && limit > 0) {
    params.limit = limit
  }
  return request.get<unknown, Result<TeamMessage[]>>(
    `/team/request/${teamRequestId}/messages`,
    { params },
  )
}

export const sendTeamMessage = (
  teamRequestId: number,
  payload: Pick<TeamMessage, 'senderId' | 'senderName' | 'type' | 'content' | 'fileName' | 'fileSize'>,
) => {
  return request.post<unknown, Result<TeamMessage>>(
    `/team/request/${teamRequestId}/messages`,
    payload,
  )
}


