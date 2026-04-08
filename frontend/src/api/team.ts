import request from '@/utils/request'

export interface TeamRequest {
  id?: number
  userId?: number
  title?: string
  description?: string
  tags?: string
  expectedCount?: number
  currentCount?: number
  startTime?: string
  endTime?: string
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
  startTime?: string
  endTime?: string
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

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const getActiveTeamRequests = () => {
  return request.get<unknown, Result<TeamRequestVO[]>>('/team/requests/active')
}

export const getTeamRequestDetail = (id: number) => {
  return request.get<unknown, Result<TeamRequestVO>>(`/team/request/${id}`)
}

export const createTeamRequest = (payload: TeamRequest) => {
  return request.post<unknown, Result<TeamRequest>>('/team/request', payload)
}

export const updateTeamStatus = (id: number, status: number, userId: number) => {
  return request.put<unknown, Result<boolean>>(`/team/request/${id}/status`, { status, userId })
}

export const joinTeam = (requestId: number, member: TeamMember) => {
  return request.post<unknown, Result<boolean>>(`/team/request/${requestId}/join`, member)
}

export const quitTeam = (requestId: number, userId: number) => {
  return request.post<unknown, Result<boolean>>(`/team/request/${requestId}/quit`, { userId })
}

export const getUserTeams = (userId: number) => {
  return request.get<unknown, Result<TeamRequestVO[]>>(`/team/user/${userId}`)
}

export const getTeamMembers = (teamRequestId: number) => {
  return request.get<unknown, Result<TeamMemberVO[]>>(
    `/team/request/${teamRequestId}/members`,
  )
}

// ===== 加入申请相关 =====

export interface TeamJoinApplication {
  id?: number
  teamRequestId?: number
  applicantId?: number
  reason?: string
  status?: number // 0-待审核 1-已通过 2-已拒绝
  createTime?: string
}

export interface TeamJoinApplicationVO {
  id?: number
  teamRequestId?: number
  applicantId?: number
  reason?: string
  status?: number
  createTime?: string
  reviewerId?: number
  reviewTime?: string
  rejectReason?: string
  applicantName?: string
  applicantStudentId?: string
  reviewerName?: string
  teamTitle?: string
}

/** 提交加入小组申请 */
export const applyToJoinTeam = (teamRequestId: number, applicantId: number, reason: string) => {
  return request.post<unknown, Result<boolean>>(`/team/request/${teamRequestId}/apply`, {
    applicantId,
    reason,
  })
}

/** 组长审核申请：approve=true 通过，false 拒绝 */
export const reviewApplication = (
  applicationId: number,
  leaderId: number,
  approve: boolean,
  rejectReason?: string,
) => {
  return request.post<unknown, Result<boolean>>(`/team/application/${applicationId}/review`, {
    leaderId,
    approve,
    rejectReason,
  })
}

/** 获取组长的待审核申请列表 */
export const getPendingApplications = (leaderId: number) => {
  return request.get<unknown, Result<TeamJoinApplicationVO[]>>('/team/applications/pending', {
    params: { leaderId },
  })
}

/** 获取成员的申请审核结果列表 */
export const getApplicationResults = (applicantId: number) => {
  return request.get<unknown, Result<TeamJoinApplicationVO[]>>('/team/applications/results', {
    params: { applicantId },
  })
}

/** 获取成员待处理申请（用于禁用重复申请） */
export const getPendingByApplicant = (applicantId: number) => {
  return request.get<unknown, Result<TeamJoinApplicationVO[]>>('/team/applications/pending-by-applicant', {
    params: { applicantId },
  })
}

/** 获取红点数量 { pendingCount, resultCount, total } */
export const getTeamBadge = (userId: number) => {
  return request.get<unknown, Result<{ pendingCount: number; resultCount: number; total: number }>>(
    '/team/badge',
    { params: { userId } },
  )
}


