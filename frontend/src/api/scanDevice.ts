import request from '@/utils/request'

export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 设备扫码兑换授权 token */
export const authorizeScanDevice = (token: string, deviceUid: string) => {
  return request.post<any, Result<boolean>>('/scan-devices/authorize', {
    token,
    deviceUid,
  })
}

