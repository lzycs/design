import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
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
