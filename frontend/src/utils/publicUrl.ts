const trimRightSlash = (s: string) => s.replace(/\/+$/, '')

/**
 * 用于二维码等“跨设备访问”场景的外部可达前端地址。
 * 优先级：
 * 1) VITE_PUBLIC_WEB_ORIGIN（推荐配置成局域网 IP 或域名）
 * 2) 当前页面 origin
 */
export const getPublicWebOrigin = () => {
  const configured = (import.meta.env.VITE_PUBLIC_WEB_ORIGIN as string | undefined)?.trim()
  if (configured) return trimRightSlash(configured)
  return trimRightSlash(window.location.origin)
}

export const buildPublicUrl = (path: string) => {
  const origin = getPublicWebOrigin()
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${origin}${normalizedPath}`
}

export const isLikelyLocalhostOrigin = () => {
  const origin = getPublicWebOrigin()
  return /\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(origin)
}

