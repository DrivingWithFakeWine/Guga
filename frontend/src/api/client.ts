import axios from 'axios'

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('guga.token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 登录态失效：清理并回到登录页（避免在登录页死循环）
    if (error.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      localStorage.removeItem('guga.token')
      localStorage.removeItem('guga.user')
      window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

/** 从后端统一错误体中取出中文提示。 */
export function errorMessage(error: unknown, fallback = '请求失败，请稍后重试'): string {
  if (axios.isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message
    if (message) {
      return message
    }
  }
  return fallback
}
