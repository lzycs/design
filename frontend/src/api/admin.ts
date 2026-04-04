import request from '@/utils/request'

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginResponse {
  token: string
  adminUserId: number
  username: string
  realName: string
  roleType: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface AdminMenu {
  id: number
  title: string
  path: string
  permissionKey: string | null
}

export interface AdminOverviewVO {
  pendingRepairCount: number
  pendingReviewCount: number
  classroomTotal: number
}

export interface AdminRepairVO {
  id: number
  userId: number
  requesterName: string
  requesterPhone: string | null
  title: string
  description: string
  type: number
  priority: number
  status: number
  createTime: string
  handleTime: string | null
  handleRemark: string | null
  handlerId: number | null
  handlerName: string | null
  handlerPhone: string | null
  location: string
}

export interface AdminRepairStatusUpdateRequest {
  status: number
  handleRemark?: string
}

export interface AdminReviewVO {
  id: number
  userId: number
  reviewerName: string
  classroomId: number
  location: string
  score: number
  content: string
  tags: string | null
  status: number
  createTime: string
}

export interface AdminReviewAuditRequest {
  approve: boolean
  remark?: string
}

export interface AdminClassroomVO {
  id: number
  buildingId: number | null
  buildingName: string | null
  name: string
  roomNumber: string
  floor: number
  type: number
  capacity: number
  equipment: string | null
  status: number
  isAvailable: number | null
  latitude: number | null
  longitude: number | null
}

export interface AdminCourseVO {
  id: number
  classroomId: number
  location: string
  courseName: string
  teacherName: string
  weekDay: number
  startTime: string
  endTime: string
  startWeek: number
  endWeek: number
}

export interface AdminBuildingVO {
  id: number
  name: string
  buildingNumber: string | null
  location: string | null
  status: number | null
  createTime: string | null
}

export const adminLogin = (payload: AdminLoginRequest) => {
  return request.post<unknown, Result<AdminLoginResponse>>('/admin/login', payload)
}

export const getAdminMenus = () => {
  return request.get<unknown, Result<AdminMenu[]>>('/admin/menus')
}

export const getAdminOverview = () => {
  return request.get<unknown, Result<AdminOverviewVO>>('/admin/overview')
}

export const getAdminRepairs = (params?: { status?: number; keyword?: string }) => {
  return request.get<unknown, Result<AdminRepairVO[]>>('/admin/repairs', { params })
}

export const updateAdminRepairStatus = (id: number, payload: AdminRepairStatusUpdateRequest) => {
  return request.put<unknown, Result<boolean>>(`/admin/repairs/${id}/status`, payload)
}

export const getAdminReviews = (params?: { status?: number; keyword?: string }) => {
  return request.get<unknown, Result<AdminReviewVO[]>>('/admin/reviews', { params })
}

export const auditAdminReview = (id: number, payload: AdminReviewAuditRequest) => {
  return request.put<unknown, Result<boolean>>(`/admin/reviews/${id}/audit`, payload)
}

export const getAdminClassrooms = (params?: { buildingId?: number; status?: number }) => {
  return request.get<unknown, Result<AdminClassroomVO[]>>('/admin/classrooms', { params })
}

export const createAdminClassroom = (payload: any) => {
  return request.post<unknown, Result<boolean>>('/admin/classrooms', payload)
}

export const updateAdminClassroom = (payload: any) => {
  return request.put<unknown, Result<boolean>>('/admin/classrooms', payload)
}

export const deleteAdminClassroom = (id: number) => {
  return request.delete<unknown, Result<boolean>>(`/admin/classrooms/${id}`)
}

export const getAdminCourses = (params?: { keyword?: string }) => {
  return request.get<unknown, Result<AdminCourseVO[]>>('/admin/courses', { params })
}

export const createAdminCourse = (payload: any) => {
  return request.post<unknown, Result<boolean>>('/admin/courses', payload)
}

export const updateAdminCourse = (payload: any) => {
  return request.put<unknown, Result<boolean>>('/admin/courses', payload)
}

export const deleteAdminCourse = (id: number) => {
  return request.delete<unknown, Result<boolean>>(`/admin/courses/${id}`)
}

// ===== 教学楼（基础数据）=====
export const getAdminBuildings = (params?: { keyword?: string }) => {
  return request.get<unknown, Result<AdminBuildingVO[]>>('/admin/buildings', { params })
}

export const createAdminBuilding = (payload: any) => {
  return request.post<unknown, Result<boolean>>('/admin/buildings', payload)
}

export const updateAdminBuilding = (payload: any) => {
  return request.put<unknown, Result<boolean>>('/admin/buildings', payload)
}

export const deleteAdminBuilding = (id: number) => {
  return request.delete<unknown, Result<boolean>>(`/admin/buildings/${id}`)
}

/** 预约上限（每人每周次数、单次最长分钟） */
export interface AdminReservationLimitVO {
  maxPerWeek: number
  maxDurationMinutes: number
}

export const getAdminReservationLimits = () => {
  return request.get<unknown, Result<AdminReservationLimitVO>>('/admin/reservation-limits')
}

export const updateAdminReservationLimits = (payload: AdminReservationLimitVO) => {
  return request.put<unknown, Result<AdminReservationLimitVO>>('/admin/reservation-limits', payload)
}

