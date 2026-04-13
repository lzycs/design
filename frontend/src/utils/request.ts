import axios from 'axios'

const configuredBaseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim()
// 开发环境推荐使用相对路径 + Vite 代理，避免直连后端导致超时/CORS
const apiBaseURL = configuredBaseURL || '/api'

const request = axios.create({
  baseURL: apiBaseURL,
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const adminToken = localStorage.getItem('adminToken')
    // 管理端接口：只对 /admin/* 生效
    if (adminToken && config.url && config.url.startsWith('/admin')) {
      config.headers = config.headers || {}
      config.headers['X-Admin-Token'] = adminToken
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    return Promise.reject(error)
  }
)

export default request
